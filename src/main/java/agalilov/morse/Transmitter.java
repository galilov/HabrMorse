package agalilov.morse;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transmitter class for transmitting morse code to the audio system.
 */
class Transmitter {

    private static final int SPEED = 20; // words per minute
    private static final int FREQ = 800; // Hertz
    private static final int SAMPLE_RATE = 22050; // samples per second
    /*
     * Based upon a 50 dot duration standard word such as PARIS, the time for one
     * dot duration or one unit can
     * be computed by the formula:
     * dotDurationMilliseconds = 1200.0 / SPEED
     * https://en.wikipedia.org/wiki/Morse_code
     */
    private static final float DOT_DURATION_MILLISECONDS = 1200.0f / SPEED;
    private final Logger logger = Logger.getLogger(Transmitter.class.getSimpleName());


    /**
     * Generates sine wave samples and provides them to the consumer
     *
     * @param durationMilliseconds
     * @param consumer
     */
    private static void generateSineWave(float durationMilliseconds, IntConsumer consumer) {
        // get the number of samples
        int len = getNumOfSamples(durationMilliseconds);
        // align len to fit the wave period to avoid sound distortion at the end of the
        // wave
        len = len - (len % (SAMPLE_RATE / FREQ));
        // calculate the phase delta for the sine wave
        final double delta = 2 * Math.PI * FREQ / SAMPLE_RATE;
        // initialize the angle
        double angle = 0;
        // generate the sine wave
        for (int n = 0; n < len; n++) {
            // calculate the value of the sine wave sample
            consumer.accept((int) (Byte.MAX_VALUE * Math.sin(angle)));
            // increment the angle
            angle += delta;
        }
    }

    /**
     * Get the number of samples for a given duration
     *
     * @param durationMilliseconds the duration in milliseconds
     * @return the number of samples
     */
    private static int getNumOfSamples(float durationMilliseconds) {
        return (int) Math.round(SAMPLE_RATE * durationMilliseconds / 1000.0);
    }

    /**
     * Generates a pause of the given duration
     *
     * @param durationMilliseconds the duration in milliseconds
     * @param consumer             the consumer to receive the samples
     */
    private static void generatePause(float durationMilliseconds, IntConsumer consumer) {
        int len = getNumOfSamples(durationMilliseconds);
        for (int i = 0; i < len; i++) {
            consumer.accept(0);
        }
    }

    /**
     * Generates the image of the Morse code for the given string which is a sequence of dots, dashes, pipes and spaces.
     *
     * @param morseEncoded the Morse encoded string
     * @return the image of the Morse code for the given string
     */
    private byte[] generateSignalImage(String morseEncoded) {
        // convert Morse encoded string to byte array
        return morseEncoded.chars() // convert to stream of characters(=ints)
            .mapMulti(
                (c, consumer) -> {
                    switch (c) {
                        case '.':
                            // generate the dot wave and the space wave (zeroes)
                            generateSineWave(DOT_DURATION_MILLISECONDS, consumer);
                            generatePause(DOT_DURATION_MILLISECONDS, consumer);
                            break;
                        case '-':
                            // generate the dash wave and the space wave (zeroes)
                            generateSineWave(3 * DOT_DURATION_MILLISECONDS, consumer);
                            generatePause(DOT_DURATION_MILLISECONDS, consumer);
                            break;
                        case '|':
                            // generate the space (zeroes) 2 times (+1 spaceWave comes from the previous dot or dash)
                            generatePause(2 * DOT_DURATION_MILLISECONDS, consumer);
                            break;
                        case ' ':
                            // generate the long space (zeroes) 6 times (+1 spaceWave comes from the previous dot or dash)
                            generatePause(6 * DOT_DURATION_MILLISECONDS, consumer);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported symbol: " + c);

                    }
                }
            )
            .collect(
                // Supplier: Creates a new ByteArrayOutputStream
                () -> {
                    // the approximate buffer size
                    final int approxBufferSize =
                            (int) (4 * DOT_DURATION_MILLISECONDS * morseEncoded.length() * SAMPLE_RATE / 1000);
                    return new ByteArrayOutputStream(approxBufferSize);
                },
                // Accumulator: Adds samples to the buffer
                (buf, val) -> {
                    buf.write(val);
                },
                // Combiner: Combines two buffers (for parallel streams)
                (buf1, buf2) -> {
                    buf1.writeBytes(buf2.toByteArray());
                }
            )
            .toByteArray();
    }

    /**
     * Transmit the data to the audio system and wait for the clip to stop
     *
     * @param morseEncoded the morse encoded string
     */
    void transmit(String morseEncoded) {
        try {
            // transmit the data
            SoundPlayer player = new SoundPlayer(SAMPLE_RATE);
            player.playData(generateSignalImage(morseEncoded));
        } catch (Exception e) {
            // log the error
            logger.log(Level.SEVERE, "Can't play sound", e);
            // throw a runtime exception
            throw new RuntimeException(e);
        }
    }



}

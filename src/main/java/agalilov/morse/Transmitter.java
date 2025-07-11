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
    private final static Logger logger = Logger.getLogger(Transmitter.class.getSimpleName());


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
            transmitData(generateSignalImage(morseEncoded));
        } catch (LineUnavailableException | IOException | InterruptedException e) {
            // log the error
            logger.log(Level.SEVERE, "Can't play sound", e);
            // throw a runtime exception
            throw new RuntimeException(e);
        }
    }

    // clip for playing the sound
    private Clip clip;

    /**
     * Transmit the data to the audio system and wait for the clip to stop
     *
     * @param outputData the data to transmit
     * @throws LineUnavailableException if the line is unavailable
     * @throws IOException              if an I/O error occurs
     * @throws InterruptedException     if the thread is interrupted
     */
    private void transmitData(byte[] outputData) throws LineUnavailableException, IOException, InterruptedException {
        if (outputData == null || outputData.length == 0)
            return;
        // if the clip is not null, stop and close it
        if (clip != null) {
            clip.stop();
            clip.close();
        } else {
            // get the clip
            clip = AudioSystem.getClip();
        }

        // object for synchronization
        Object playSync = new Object();

        // listener for the line event
        LineListener listener = event -> {
            // if the event is a stop event
            if (event.getType() == LineEvent.Type.STOP) {
                // stop and close the clip
                clip.stop();
                clip.close();
                clip = null;
                logger.log(Level.INFO, "Data has been transmitted.");
                // notify all the threads that are waiting for the clip to stop
                synchronized (playSync) {
                    playSync.notifyAll();
                }
            }
        };
        // add the listener to the clip
        clip.addLineListener(listener);

        // create the audio format
        AudioFormat af = new AudioFormat(
                SAMPLE_RATE, // sample rate
                8, // bits per sample
                1, // channels
                true, // signed
                false); // big endian

        // create the audio input stream
        AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(outputData), // input stream
                af, // audio format
                outputData.length); // length of the output data

        // open the clip
        clip.open(ais);
        // log the start of the data transmitting
        logger.log(Level.INFO, "Start data transmitting...");
        // start the clip
        clip.start();
        // wait for the clip to stop
        synchronized (playSync) {
            // wait for the clip to stop
            playSync.wait();
        }
    }

}

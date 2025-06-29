package agalilov.morse;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Transmitter class for transmitting morse code to the audio system.
 */
class Transmitter {

    private static final int SPEED = 40; // words per second
    private static final int FREQ = 800; // Hertz
    private static final int SAMPLE_RATE = 22050; // samples per second
    /*
     * Based upon a 50 dot duration standard word such as PARIS, the time for one
     * dot duration or one unit can
     * be computed by the formula:
     * dotDurationMilliseconds = 1200.0 / SPEED
     * https://en.wikipedia.org/wiki/Morse_code
     */
    private static final double DOT_DURATION_MILLISECONDS = 1200.0 / SPEED;

    private final byte[] dotWave; // 1 dot duration
    private final byte[] dashWave; // 3 dots duration
    private final byte[] spaceWave; // 3 spaces duration
    private static final char dot = '.'; // dot character
    private static final char dash = '-'; // dash character
    private static final char shortSpace = '|'; // short space character

    private final static Logger logger = Logger.getLogger(Transmitter.class.getSimpleName());

    // clip for playing the sound
    private Clip clip;

    /**
     * Constructor
     */
    Transmitter() {
        // generate waves for dot, dash and space
        dotWave = generateSineWave(DOT_DURATION_MILLISECONDS);
        dashWave = generateSineWave(DOT_DURATION_MILLISECONDS * 3);
        spaceWave = generatePause(DOT_DURATION_MILLISECONDS);
    }

    /**
     * Transmit the data to the audio system and wait for the clip to stop
     *
     * @param morseEncoded the morse encoded string
     */
    void transmit(String morseEncoded) {
        // convert morse encoded string to byte array
        byte[] outputData = morseEncoded
                .chars() // convert to stream of characters(=ints)
                .mapToObj( // maps each character to a byte array
                        c -> {
                            if (dot == c) // if the character is a dot (.)
                                // return the dot wave and the space wave (zeroes)
                                return concatArrays(dotWave, spaceWave);
                            if (dash == c) // if the character is a dash (-)
                                // return the dash wave and the space wave (zeroes)
                                return concatArrays(dashWave, spaceWave);
                            if (shortSpace == c) // if the character is a short space (|)
                                // return the space (zeroes) 2 times
                                return concatArrays(spaceWave, spaceWave);
                            else // if the character is a space between words ( )
                                 // return the space (zeroes) 3 times
                                return concatArrays(spaceWave, spaceWave, spaceWave);
                        })
                .collect(Collectors.toList()) // collect the byte arrays into a list of byte arrays
                .stream() // convert the list of byte arrays to a stream of byte arrays
                .reduce(new byte[0], this::concatArrays); // reduce the stream of byte arrays to a single byte array

        try {
            // transmit the data
            transmitData(outputData);
        } catch (LineUnavailableException | IOException | InterruptedException e) {
            // log the error
            logger.log(Level.SEVERE, "Can't play sound", e);
            // throw a runtime exception
            throw new RuntimeException(e);
        }
    }

    /**
     * Concatenate arrays
     *
     * @param arrays the arrays to concatenate
     * @return the concatenated array
     */
    private byte[] concatArrays(byte[]... arrays) {
        // calculate the total length of the arrays
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        // create a new byte array with the total length of the arrays
        byte[] result = new byte[totalLength];
        // initialize the offset
        int offset = 0;
        // copy the arrays to the result array
        for (byte[] array : arrays) {
            // copy the array to the result array
            System.arraycopy(array, 0, result, offset, array.length);
            // increment the offset by the length of the array
            offset += array.length;
        }
        // return the result array
        return result;
    }

    /**
     * Transmit the data to the audio system and wait for the clip to stop
     *
     * @param outputData the data to transmit
     * @throws LineUnavailableException if the line is unavailable
     * @throws IOException              if an I/O error occurs
     * @throws InterruptedException     if the thread is interrupted
     */
    private void transmitData(byte[] outputData) throws LineUnavailableException, IOException, InterruptedException {
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

    /**
     * Generate a sine wave
     *
     * @param durationMilliseconds the duration in milliseconds
     * @return the sine wave data
     */
    private static byte[] generateSineWave(double durationMilliseconds) {
        // get the number of samples
        int len = getNumOfSamples(durationMilliseconds);
        // create a new byte array with the number of samples
        byte[] result = new byte[len];
        // calculate the phase delta for the sine wave
        final double delta = 2 * Math.PI * FREQ / SAMPLE_RATE;
        // initialize the phase
        double phase = 0;
        // generate the sine wave
        for (int n = 0; n < len; n++) {
            // calculate the value of the sine wave sample
            result[n] = (byte) (Byte.MAX_VALUE * Math.sin(phase));
            // increment the phase
            phase += delta;
        }
        // return the result
        return result;
    }

    /*
     * Get the number of samples for a given duration
     *
     * @param durationMilliseconds the duration in milliseconds
     *
     * @return the number of samples
     */
    private static int getNumOfSamples(double durationMilliseconds) {
        return (int) Math.round(SAMPLE_RATE * durationMilliseconds / 1000.0);
    }

    /**
     * Generate a pause
     *
     * @param durationMilliseconds the duration in milliseconds
     * @return the pause data (zeroes)
     */
    private static byte[] generatePause(double durationMilliseconds) {
        return new byte[getNumOfSamples(durationMilliseconds)];
    }
}

package agalilov.morse;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class SoundPlayer {

    private final int sampleRate;
    // clip for playing the sound
    private Clip clip;
    private final Logger logger = Logger.getLogger(SoundPlayer.class.getSimpleName());

    SoundPlayer(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Play the data via audio system and wait for the clip to stop
     *
     * @param data the data to transmit
     * @throws Exception when Line unavailable or I/O error
     */
    void playData(byte[] data) throws Exception {
        try {
            if (data == null || data.length == 0)
                return;
            // if the clip is not null, stop and close it
            if (clip != null) {
                clip.stop();
                clip.close();
            }

            // get the clip
            clip = AudioSystem.getClip();

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
                        playSync.notify();
                    }
                }
            };
            // add the listener to the clip
            clip.addLineListener(listener);

            // create the audio format
            AudioFormat af = new AudioFormat(
                    sampleRate, // sample rate
                    8, // bits per sample
                    1, // channels
                    true, // signed
                    false); // big endian

            // create the audio input stream
            AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(data), // input stream
                    af, // audio format
                    data.length); // length of the output data

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
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.toString());
        } catch (LineUnavailableException | IOException e) {
            throw new Exception(e);
        }
    }
}

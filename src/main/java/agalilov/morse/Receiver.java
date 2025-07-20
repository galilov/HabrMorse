package agalilov.morse;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

class Receiver {
    private static final int FREQ = 800; // Hertz
    private static final int SAMPLE_RATE = 22050; // samples per second

    private static final Logger logger = Logger.getLogger(Receiver.class.getSimpleName());
    private final ConcurrentLinkedQueue<byte[]> audioQueue = new ConcurrentLinkedQueue<>();
    private Receiver receiver;
    private Thread thrReadAudio;
    private Thread thrReceiver;
    private TargetDataLine targetDataLine;

    void start() {
        try {
            AudioFormat format = new AudioFormat(
                    SAMPLE_RATE,
                    8,
                    2,
                    true,
                    false);

            // microphone = AudioSystem.getTargetDataLine(format);

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            thrReadAudio = new Thread(() -> {
                targetDataLine.start();
                byte[] data = new byte[targetDataLine.getBufferSize() / 3];
                while (!thrReadAudio.isInterrupted()) {
                    int nBytesRead = targetDataLine.read(data, 0, data.length);
                    if (nBytesRead > 0) {
                        byte[] readData = Arrays.copyOf(data, nBytesRead);
                        synchronized (audioQueue) {
                            audioQueue.add(readData);
                            audioQueue.notify();
                        }
                    }
                    if (nBytesRead < data.length) {
                        break;
                    }
                }
            });
            thrReceiver = new Thread(() -> {
                try {
                    while (!thrReceiver.isInterrupted()) {
                        byte[] readData;
                        synchronized (audioQueue) {
                            do {
                                audioQueue.wait();
                                readData = audioQueue.poll();
                            } while (readData == null);
                        }
                        logger.log(Level.INFO, String.format("%d", signalPower(readData)));
                    }
                } catch (InterruptedException exception) {
                    logger.log(Level.WARNING, "Interrupted");
                }
            });
            thrReadAudio.start();
            thrReceiver.start();
        } catch (LineUnavailableException exception) {
            throw new RuntimeException(exception);
        }
    }

    private long signalPower(byte[] readData) {
        long power = 0;
        long avg = 0;
        for (byte b : readData) {
            power += (b * b);
            avg += b;
        }
        avg /= readData.length;
        return power - avg * avg;
    }

    void stop() {
        try {
            if (targetDataLine != null) {
                thrReadAudio.interrupt();
                thrReceiver.interrupt();
                thrReadAudio.join();
                thrReceiver.join();
                targetDataLine.close();
                targetDataLine = null;
            }
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

}

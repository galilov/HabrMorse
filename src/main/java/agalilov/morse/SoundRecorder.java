package agalilov.morse;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class SoundRecorder {
    private static final int FREQ = 800; // Hertz
    private static final int SAMPLE_RATE = 22050; // samples per second

    private static final Logger logger = Logger.getLogger(SoundRecorder.class.getSimpleName());
    private final Queue<short[]> audioQueue = new LinkedList<>();

    private Thread thrReadAudio;
    private TargetDataLine targetDataLine;
    private final AtomicBoolean canContinue = new AtomicBoolean(true);

    void start() {
        try {
            AudioFormat format = new AudioFormat(
                    SAMPLE_RATE,
                    16,
                    1,
                    true,
                    true);

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);

            thrReadAudio = new Thread(() -> {
                canContinue.set(true);
                targetDataLine.start();
                byte[] data = new byte[targetDataLine.getBufferSize()];
                while (canContinue.get() && !Thread.interrupted()) {
                    int nBytesRead = targetDataLine.read(data, 0, data.length);
                    if (nBytesRead > 0) {
                        short[] readData = bytesToShort(data, nBytesRead);
                        synchronized (audioQueue) {
                            audioQueue.add(readData);
                            audioQueue.notify();
                        }
                    }
                }
            });

            thrReadAudio.start();

        } catch (LineUnavailableException exception) {
            throw new RuntimeException(exception);
        }
    }

    private short[] bytesToShort(byte[] bytes, int length) {
        short[] shorts = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes, 0, length).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }

    void stop() {
        try {
            if (targetDataLine != null) {
                canContinue.set(false);
                thrReadAudio.join();
                targetDataLine.close();
                targetDataLine = null;
            }
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    private short[] get() {
        short[] readData;
        synchronized (audioQueue) {
            while(canContinue.get()) {
                readData = audioQueue.poll();
                if (readData != null) {
                    return readData;
                }
                try {
                    audioQueue.wait();
                } catch (InterruptedException e) {
                    canContinue.set(false);
                    logger.log(Level.WARNING, e.toString());
                    break;
                }
            }
            return new short[0];
        }
    }

    Stream<short[]> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<short[]>() {

                            @Override
                            public boolean hasNext() {
                                return canContinue.get();
                            }

                            @Override
                            public short[] next() {
                                return get();
                            }
                        },
                        Spliterator.IMMUTABLE),
                false);
    }
}

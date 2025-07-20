package agalilov.morse;

/**
 * Main class for the morse code transmitter.
 */
public class Main {
    private static long signalPower(short[] readData) {
        long power = 0;
        long avg = 0;
        for (var b : readData) {
            avg += b;
        }
        avg /= readData.length;
        for (var b : readData) {
            b -= (short)avg;
            power += (b * b);
        }
        return (long)Math.sqrt(power);
    }
    /**
     * Main method
     *
     * @param args the arguments
     */
    public static void main(String[] args) throws Exception {
//        if (args.length == 0) {
//            System.out.println("Usage: add a text line to send, use \"your text\" to send the line with spaces");
//            return;
//        }
//        // create a new MorseProcessor
//        MorseProcessor mp = new MorseProcessor();
//        // convert the text to morse code
//        String morse = mp.textToMorse(String.join(" ", args));
//        // log the morse code
//        System.out.println("Morse code: " + morse);
//        // create a new Transmitter
//        Transmitter transmitter = new Transmitter();
//        transmitter.transmit(morse);
        SoundRecorder receiver = new SoundRecorder();
        receiver.start();
        receiver.stream().forEach((data->{
            System.out.println(signalPower(data));
        }));
        System.in.read();
        receiver.stop();
    }
}

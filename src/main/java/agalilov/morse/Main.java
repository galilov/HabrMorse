package agalilov.morse;

import java.util.logging.Logger;

/**
 * Main class for the morse code transmitter.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getSimpleName());

    /**
     * Main method
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: add a text line to send, use \"your text\" to send the line with spaces");
            return;
        }
        // create a new MorseProcessor
        MorseProcessor mp = new MorseProcessor();
        // convert the text to morse code
        String morse = mp.textToMorse(String.join(" ", args));
        // log the morse code
        logger.info("Morse code: " + morse);
        // create a new Transmitter
        Transmitter transmitter = new Transmitter();
        transmitter.transmit(morse);
    }
}

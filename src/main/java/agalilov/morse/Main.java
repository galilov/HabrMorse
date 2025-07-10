package agalilov.morse;

import java.util.logging.Logger;

/**
 * Main class for the morse code transmitter.
 */
public class Main {

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
        System.out.println("Morse code: " + morse);
        // create a new Transmitter
        Transmitter transmitter = new Transmitter();
        transmitter.transmit(morse);
    }
}

package agalilov.morse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * MorseProcessor class for processing text to morse code.
 */
class MorseProcessor {
    /**
     * The morse codes map
     */
    private static final String MORSECODES = "morsecodes";
    private Map<Integer, String> morseCodes;

    private static Logger logger = Logger.getLogger(MorseProcessor.class.getSimpleName());

    /**
     * Constructor
     */
    MorseProcessor() {
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialize the morse codes map
     *
     * @throws IOException if an I/O error occurs
     */
    private void init() throws IOException {
        // get the input stream from the resource MORSECODES
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MORSECODES)) {
            // assert the input stream is not null
            assert inputStream != null;
            // read the lines from the input stream
            Stream<String> lines = (new BufferedReader(new InputStreamReader(inputStream))).lines();
            morseCodes = Collections.unmodifiableMap( // create an unmodifiable map
                    lines // stream of lines
                            .collect( // collect the lines into a map
                                      // typical pairs look like that:
                                      // A.-
                                      // B-...
                                      // C-.-.
                                      // ...
                                      // there is no space between the key and the value
                                    Collectors.toMap(
                                            // key for the map
                                            // convert the first character to uppercase and get the integer value
                                            pair -> (int) Character.toUpperCase(pair.charAt(0)),
                                            // value for the map
                                            // get the substring from the second character
                                            pair -> pair.substring(1))));
        }
    }

    /**
     * Get the morse codes map
     *
     * @return the morse codes map
     */
    Map<Integer, String> getMorseCodes() {
        return morseCodes;
    }

    /**
     * Convert text to morse code
     *
     * @param text the text to convert
     * @return the morse code string, for example: ".-|...|.-."
     */
    String textToMorse(String text) {
        List<String> codes = text
                // convert the text to uppercase
                .toUpperCase()
                // convert the text to a stream of characters
                .chars()
                // convert the stream of characters to a stream of strings
                .mapToObj((c) -> {
                    if (c == ' ') { // if the character is a space
                        return " "; // return a space
                    }
                    // get the morse code for the character
                    String code = morseCodes.get(c);
                    // return the morse code or a space if the code is not found (==null)
                    return code != null ? code : " ";
                })
                .collect(Collectors.toList()); // collect the codes into a list
        // remove repeated spaces and return
        return IntStream
                // create a stream of integers from 0 to the size of the codes list
                .range(0, codes.size())
                // filter the codes
                .filter(i ->
                // if the code is the first and not a space
                (isFirst(i, codes) && isNotSpace(i, codes))
                        // or the code is the last and not a space
                        || (isLast(i, codes) && isNotSpace(i, codes))
                        // or the code is not the first or last and the current or next code is not a
                        // space
                        || (isNotFirstAndNotLast(i, codes) && currentOrNextAreNotSpace(i, codes)))
                // convert the codes to a stream of strings
                .mapToObj(codes::get)
                // join the codes with a '|' character and return the result
                .collect(Collectors.joining("|"));
    }

    /**
     * Check if the morse code is the last in the list
     *
     * @param i     the index of the code
     * @param codes the list of morse codes
     * @return true if the code is the last, false otherwise
     */
    private static boolean isLast(int i, List<String> codes) {
        return i == codes.size() - 1;
    }

    /**
     * Check if the morse code is the first in the list
     *
     * @param i     the index of the code
     * @param codes the list of morse codes
     * @return true if the code is the first, false otherwise
     */
    private static boolean isFirst(int i, List<String> codes) {
        return i == 0;
    }

    /**
     * Check if the morse code is not the first and not the last in the list
     *
     * @param i     the index of the code
     * @param codes the list of morse codes
     * @return true if the code is not the first or last, false otherwise
     */
    private static boolean isNotFirstAndNotLast(int i, List<String> codes) {
        return i > 0 && i < codes.size() - 1;
    }

    /**
     * Check if the current or next morse code is not a space
     *
     * @param i     the index of the code
     * @param codes the list of morse codes
     * @return true if the current or next code is not a space, false otherwise
     */
    private static boolean currentOrNextAreNotSpace(int i, List<String> codes) {
        return !codes.get(i).equals(" ") || !codes.get(i + 1).equals(" ");
    }

    /**
     * Check if the morse code is not a space
     *
     * @param i     the index of the code
     * @param codes the list of morse codes
     * @return true if the code is not a space, false otherwise
     */
    private static boolean isNotSpace(int i, List<String> codes) {
        return !codes.get(i).equals(" ");
    }
}

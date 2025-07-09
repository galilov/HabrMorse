package agalilov.morse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MorseProcessorTest {

    private MorseProcessor mp;

    @BeforeEach
    public void setUp() {
        mp = new MorseProcessor();
    }

    @AfterEach
    public void tearDown() {
        mp = null;
    }
    @Test
    void getMorseCodes() {
        assertEquals(".-", mp.getMorseCodes().get((int)'A'));
        assertEquals("----.", mp.getMorseCodes().get((int)'9'));
        assertNull(mp.getMorseCodes().get((int)'a'));
    }

    @Test
    void textToMorse() {
        final String moreCode = "....|.|.-..|.-..|---| |.-|.-..|.-..";
        assertEquals(moreCode, mp.textToMorse(" Hello all"));
        assertEquals(moreCode, mp.textToMorse("Hello all"));
        assertEquals(moreCode, mp.textToMorse("Hello  all"));
        assertEquals(moreCode, mp.textToMorse(" Hello  all "));
        assertEquals(moreCode, mp.textToMorse("Hello all "));
        assertEquals(moreCode, mp.textToMorse("Hello   all  "));
        assertEquals(moreCode, mp.textToMorse("Hello   all"));
        assertEquals(moreCode, mp.textToMorse("Hello   all "));
        assertEquals(moreCode, mp.textToMorse("Hello   all  "));
        assertNotEquals(moreCode, mp.textToMorse("Hel lo   all  "));
    }
}
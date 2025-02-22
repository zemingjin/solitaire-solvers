package org.solitaire.klondike;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solitaire.util.IOHelper;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.solitaire.klondike.KlondikeHelper.LAST_DECK;
import static org.solitaire.util.CardHelper.useSuit;

class KlondikeHelperTest {
    private static final String TEST_FILE = "games/klondike/klondike-medium-122822.txt";
    protected static final String[] CARDS = IOHelper.loadFile(TEST_FILE);

    @BeforeEach
    void setup() {
        useSuit(false);
    }

    @Test
    void test_of() {
        assertNotNull(KlondikeHelper.of());
    }

    @Test
    void test_build() {
        var klondike = KlondikeHelper.build(CARDS).board();

        assertNotNull(klondike);
        assertEquals("20:Kd", klondike.deck().peek().toString());
        assertEquals("0:Jc", klondike.deck().get(0).toString());
        assertEquals(1, klondike.column(0).size());
        assertEquals("24:Th", klondike.column(0).get(0).toString());
        assertEquals("25:8h", klondike.column(1).get(0).toString());
    }

    @Test
    void test_clone() {
        var state = KlondikeHelper.build(CARDS).board();
        var clone = new KlondikeBoard(state);

        assertTrue(reflectionEquals(clone, state));
    }

    @Test
    void test_colStart() {
        assertEquals(LAST_DECK, KlondikeHelper.colStart(0));
        assertEquals(LAST_DECK + 1, KlondikeHelper.colStart(1));
        assertEquals(LAST_DECK + 3, KlondikeHelper.colStart(2));
        assertEquals(LAST_DECK + 6, KlondikeHelper.colStart(3));
        assertEquals(LAST_DECK + 10, KlondikeHelper.colStart(4));
        assertEquals(LAST_DECK + 15, KlondikeHelper.colStart(5));
        assertEquals(LAST_DECK + 21, KlondikeHelper.colStart(6));
    }

}
package org.solitaire.freecell;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.solitaire.freecell.CardShuffle.MAXCOL;
import static org.solitaire.util.CardHelper.useSuit;

class CardShuffleTest {
    private CardShuffle cardShuffle;

    @BeforeEach
    void setup() {
        useSuit = false;
        cardShuffle = new CardShuffle();
    }

    @Test
    public void test_genBoard() {
        var card = cardShuffle.genBoard(1);

        assertNotNull(card);
        assertEquals(MAXCOL, card.length);
        assertEquals(7, card[0].length);
        assertEquals(6, card[4].length);
        assertEquals("[JD, KD, 2S, 4C, 3S, 6D, 6S]", Arrays.toString(card[0]));
    }

}
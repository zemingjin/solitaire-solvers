package org.solitaire.klondike;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solitaire.model.Card;
import org.solitaire.util.IOHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.solitaire.klondike.KlondikeBoard.drawNumber;
import static org.solitaire.klondike.KlondikeHelper.build;
import static org.solitaire.model.Candidate.buildCandidate;
import static org.solitaire.model.Origin.COLUMN;
import static org.solitaire.model.Origin.DECKPILE;
import static org.solitaire.model.Origin.FOUNDATION;
import static org.solitaire.util.CardHelper.VALUES;
import static org.solitaire.util.CardHelper.buildCard;
import static org.solitaire.util.CardHelper.card;
import static org.solitaire.util.CardHelper.suit;
import static org.solitaire.util.CardHelper.suitCode;
import static org.solitaire.util.CardHelper.useSuit;

class KlondikeBoardTest {
    private static final String TEST_FILE = "games/klondike/klondike-122822-medium.txt";
    protected static final String[] CARDS = IOHelper.loadFile(TEST_FILE);

    private KlondikeBoard board;

    @BeforeEach
    void setup() {
        drawNumber(3);
        useSuit(false);
        board = build(CARDS).board();
    }

    @Test
    void test_isNotImmediateFoundationable() {
        assertTrue(board.isNotImmediateFoundationable.test(0));

        var card = card("9h");
        board.foundations().get(suitCode(card)).add(card);
        assertFalse(board.isNotImmediateFoundationable.test(0));

        card = card("Jh");
        board.foundations().get(suitCode(card)).add(card);
        assertTrue(board.isNotImmediateFoundationable.test(0));
    }

    @Test
    void test_fromFoundationToColumn() {
        var card = card("5d");
        var result = board.fromFoundationToColumn.apply(card);

        assertNull(result);

        board.columns().get(5).add(card("6s"));
        result = board.fromFoundationToColumn.apply(card);

        assertEquals(5, result.getLeft());

        board.columns().get(3).remove(3);
        result = board.fromFoundationToColumn.apply(card);

        assertNull(result);
    }

    @Test
    void test_findFoundationToColumnCandidates() {
        var result = board.findFoundationToColumnCandidates();

        assertEquals(0, result.size());

        var card = card("5d");
        board.foundations().get(suitCode(card)).add(card);

        result = board.findFoundationToColumnCandidates();
        assertEquals(0, result.size());

        board.columns().get(5).add(card("6s"));
        result = board.findFoundationToColumnCandidates();
        assertEquals("$5:5d", result.get(0).notation());

        board.columns().get(3).remove(3);
        result = board.findFoundationToColumnCandidates();
        assertEquals(0, result.size());
    }

    @Test
    void test_findFoundationToColumnCandidates_non_found() {
        var card = card("Th");
        board.foundations().get(suitCode(card)).add(card);

        assertEquals(0, board.findFoundationToColumnCandidates().size());

        board.columns().get(2).clear();
        board.columns().get(2).add(card("Jc"));

        assertEquals("$2:Th", board.findFoundationToColumnCandidates().get(0).notation());

        card = card("9h");
        board.foundations().get(suitCode(card)).add(card);
        board.columns().get(1).add(card("8c"));

        assertEquals("$6:9h", board.findFoundationToColumnCandidates().get(0).notation());
    }

    @Test
    void test_findCandidates() {
        var result = board.findCandidates();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("10:9s", result.get(0).notation());
    }

    @Test
    void test_updateStates() {
        var candidate = board.toColumnCandidate(6, 2, board.column(2).peek());

        assertEquals(7, board.column(6).size());
        assertEquals(3, board.column(2).size());
        board.stateChanged(false);

        board = board.updateBoard(candidate);

        assertEquals(6, board.column(6).size());
        assertEquals(4, board.column(2).size());
        assertTrue(board.stateChanged());
    }

    @Test
    void test_isScorable() {
        var card = board.columns().get(1).peek();

        assertTrue(board.isScorable(buildCandidate(1, COLUMN, card)));
        assertTrue(board.isScorable(buildCandidate(-1, DECKPILE, card)));

        board.columns().get(1).clear();
        assertFalse(board.isScorable(buildCandidate(1, COLUMN, card)));
    }

    @Test
    void test_moveToFoundation() {
        var card = card("Ad");
        var foundation = board.foundations().get(suitCode(card));
        board.moveToFoundation(buildCandidate(0, COLUMN, card));
        assertTrue(foundation.contains(card));
        assertEquals(15, board.totalScore());

        card = card("2d");
        board.moveToFoundation(buildCandidate(0, DECKPILE, card));
        assertEquals(2, foundation.size());
        assertEquals(25, board.totalScore());

        card = card("3d");
        board.moveToFoundation(buildCandidate(0, COLUMN, card));
        assertEquals(3, foundation.size());
        assertEquals(30, board.totalScore());
    }

    @Test
    void test_checkColumnsForFoundation() {
        var column = board.columns().get(5);
        var foundation = board.foundations().get(0);

        column.set(5, buildCard(11, "Ad"));

        assertEquals(6, column.size());
        assertEquals(5, column.openAt());
        assertTrue(foundation.isEmpty());
        assertEquals("11:Ad", column.peek().toString());
    }

    @Test
    void test_moveToTarget() {
        var candidate = board.toColumnCandidate(6, 2, board.column(2).peek());
        var column = board.columns().get(2);

        assertEquals(3, column.size());
        assertEquals("29:Jh", column.peek().toString());
        board.moveToTarget(candidate);

        assertEquals(4, column.size());
        assertEquals("51:Tc", column.peek().toString());
        assertEquals(5, board.totalScore());
    }

    @Test
    void test_removeFromSource_column() {
        var column = board.columns().get(6);
        assertEquals(7, column.size());
        assertEquals(6, column.openAt());

        var candidate = board.toColumnCandidate(6, 2, board.column(2).peek());

        board.removeFromSource(candidate);
        assertEquals(6, column.size());
        assertEquals(5, column.openAt());
        assertEquals(0, board.totalScore());

        drawDeckCards();
        assertFalse(board.deckPile().isEmpty());
        assertEquals(6, board.deckPile().size());
        assertTrue(board.stateChanged());

        candidate = buildCandidate(-1, DECKPILE, COLUMN, new LinkedList<>());

        board.removeFromSource(candidate);

        assertEquals(5, board.deckPile().size());
    }


    @Test
    void test_removeFromSource_foundation() {
        var card = card("Ad");
        var suitCode = suitCode(card);

        assertTrue(board.foundations().get(suitCode).isEmpty());
        board.foundations().get(suitCode).add(card);
        assertEquals(card.toString(), board.foundations().get(suitCode).peek().toString());

        var result = board.removeFromSource(buildCandidate(suitCode, FOUNDATION, card));

        assertTrue(board.foundations().get(suitCode).isEmpty());
        assertSame(board, result);
    }

    @Test
    void test_findFoundationCandidates() {
        drawDeckCards();
        var results = board.findToFoundationCandidate().toList();

        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertTrue(board.stateChanged());

        var card = card("Ad");
        board.columns().get(0).clear();
        board.columns().get(6).add(card);

        results = board.findToFoundationCandidate().toList();

        assertNotNull(results);
        assertEquals(1, results.size());

        board.moveToTarget(results.get(0));
        assertEquals(1, board.foundations().get(suitCode(card)).size());
        assertEquals("[0:Ad]", board.foundations().get(suitCode(card)).toString());
    }

    @Test
    void test_findFoundationCandidateFromDeck() {
        var result = board.findDeckToFoundationCandidates().toList();

        assertEquals(0, result.size());

        board.deckPile().push(card("Ad"));
        result = board.findDeckToFoundationCandidates().toList();

        assertEquals(1, result.size());
        assertEquals("^$:Ad", result.get(0).notation());
    }

    @Test
    void test_isFoundationCandidate() {
        var a = card("Ad");
        var b = card("2d");

        assertTrue(board.isFoundationCandidate.test(a));
        assertFalse(board.isFoundationCandidate.test(b));

        board.foundations().get(suitCode(a)).push(a);

        assertTrue(board.isFoundationCandidate.test(b));
        assertFalse(board.isFoundationCandidate.test(card("3d")));
    }

    @Test
    void test_isMovable() {
        var column = board.columns().get(3);

        var card = card("Kd");

        assertFalse(board.isMovable(card, column));

        column.clear();
        column.add(card);
        assertFalse(board.isMovable(card, column));

        card = card("Qd");
        column.add(card);
        assertTrue(board.isMovable(card, column));
    }

    @Test
    void test_findDeckToColumnCandidates() {
        board.deckPile().pop();
        board.deckPile().pop();
        var result = board.findDeckToColumnCandidates().toList();

        assertEquals(1, result.size());
        assertEquals("^2:Ts", result.get(0).notation());
        assertFalse(result.get(0).isFromColumn());
        assertTrue(result.get(0).isToColumn());
    }

    @Test
    void test_toColumnCandidate() {
        assertNull(board.toColumnCandidate(4, 3, board.column(3).peek()));
        assertNull(board.toColumnCandidate(4, 4, board.column(5).peek()));
        var result = board.toColumnCandidate(6, 2, board.column(2).peek());

        assertEquals("62:Tc", result.notation());
        assertTrue(result.isFromColumn());

        board.column(0).clear();
        result = board.toColumnCandidate(List.of(card("Kd")), 4, 0, null);
        assertEquals("40:Kd", result.notation());
    }

    @Test
    void test_findMovableCandidates() {
        var targets = board.findMovableCandidates().toList();

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertEquals("10:9s", targets.get(0).notation());
        assertEquals("45:5c", targets.get(2).notation());
    }

    @Test
    void test_getOrderedCards() {
        var result = board.getOrderedCards.apply(board.columns().get(0));

        assertEquals(1, result.size());
        assertEquals("24:Th", result.get(0).toString());

        result = board.getOrderedCards.apply(board.columns().get(6));
        assertEquals(1, result.size());
        assertEquals("[51:Tc]", result.toString());

        board.columns().get(5).add(board.columns().get(4).peek());
        result = board.getOrderedCards.apply(board.columns().get(5));

        assertEquals(2, result.size());

        board.columns().get(0).add(board.columns().get(1).pop());
        board.columns().get(0).add(board.columns().get(1).pop());
        result = board.getOrderedCards.apply(board.columns().get(0));

        assertEquals(3, result.size());

        board.columns().get(0).clear();
        assertTrue(board.getOrderedCards.apply(board.columns().get(0)).isEmpty());
    }

    @Test
    void test_isImmediateToFoundation() {
        var card = card("Ad");

        assertTrue(board.isImmediateToFoundation(card));
        assertFalse(board.isImmediateToFoundation(card("2d")));

        var foundation = board.foundations().get(suitCode(card));
        foundation.add(card);
        card = card("2d");

        assertTrue(board.isImmediateToFoundation(card));

        foundation.add(card);
        card = card("3d");
        assertFalse(board.isImmediateToFoundation(card));

        board.stateChanged(false);
        assertTrue(board.isImmediateToFoundation(card));
    }

    @Test
    void test_helpOpenCard() {
        assertTrue(board.helpOpenCard(card("Jh")));
        board.column(2).openAt(1);
        assertFalse(board.helpOpenCard(card("Jh")));

        assertFalse(board.helpOpenCard(card("Qd")));
    }

    @Test
    void test_toColumnCandidates() {
        var pair = Pair.of(2, board.column(2).peek());
        var result = board.toColumnCandidates.apply(pair).toList();

        assertEquals(1, result.size());
        assertEquals("62:Tc", result.get(0).notation());

        board.path().add(result.get(0).notation());
        result = board.toColumnCandidates.apply(pair).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    void test_isCleared() {
        assertFalse(board.isSolved());

        mockFullFoundations(board.foundations());

        assertTrue(board.isSolved());
    }

    @Test
    void test_score() {
        assertEquals(-36, board.score());

        var card = card("Ac");
        board.foundations().get(suitCode(card)).add(card);
        board.columns().get(6).remove(card);
        board.score(0);

        assertEquals(-36, board.score());

        range(0, 7).forEach(i -> drawDeckCards());
        board.score(0);
        assertEquals(-36, board.score());

        board.deckPile().remove(card("2c"));
        board.score(0);
        assertThrows(NoSuchElementException.class, () -> board.score());
    }

    @Test
    void test_calcBlockers() {
        assertEquals(15, board.calcBlockers());

        var card = card("Ks");
        board.foundations().get(suitCode(card)).add(card);
        assertEquals(9, board.calcBlockers());
    }

    @Test
    void test_drawDeck() {
        var candidate = board.drawDeck().get(0);

        board.updateBoard(candidate);

        assertEquals(DECKPILE, candidate.origin());
        assertEquals(DECKPILE, candidate.target());
        assertEquals(drawNumber(), candidate.cards().size());

        while (!board.deck().isEmpty()) {
            board.updateBoard(board.drawDeck().get(0));
        }

        assertTrue(board.deck().isEmpty());
        board.stateChanged(false);

        assertTrue(board.drawDeck().isEmpty());

        board.stateChanged(true);
        assertFalse(board.drawDeck().isEmpty());
        assertFalse(board.stateChanged());
    }

    @Test
    void test_verify() {
        var result = board.verify();

        assertTrue(result.isEmpty());

        board.columns().get(0).remove(0);
        board.columns().get(1).add(card("Kd"));

        result = board.verify();
        assertEquals("[Extra card: Kd, Missing card: Th]", result.toString());
    }

    private void mockFullFoundations(List<Stack<Card>> foundations) {
        range(0, 4)
                .forEach(i -> range(0, 13)
                        .forEach(j -> foundations.get(i).add(card(VALUES.charAt(i) + suit(i)))));
    }

    private void drawDeckCards() {
        board.updateBoard(board.drawDeck().get(0));
    }
}
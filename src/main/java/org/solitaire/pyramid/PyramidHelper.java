package org.solitaire.pyramid;

import org.apache.commons.lang3.tuple.Pair;
import org.solitaire.model.Card;
import org.solitaire.model.Column;
import org.solitaire.model.Path;
import org.solitaire.util.CardHelper;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.copyOf;
import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;

public class PyramidHelper {
    protected static final int LAST_BOARD = 28;
    protected static final int LAST_DECK = 52;
    protected static final int LAST_BOARD_INDEX = LAST_BOARD - 1;
    protected static final int[] ROW_SCORES = new int[]{500, 250, 150, 100, 75, 50, 25};
    private static final Function<Card[], PyramidBoard> buildPyramidBoard = cards -> {
        assert cards.length == 52 : "Invalid # of cards: " + cards.length;

        return new PyramidBoard(copyOf(cards, LAST_BOARD), buildDeck(cards), new Column(), new Path<>(), 3);
    };

    public static Pyramid build(String[] cards) {
        return Optional.of(cards)
                .map(CardHelper::toCards)
                .map(buildPyramidBoard)
                .map(Pyramid::new)
                .orElseThrow();
    }

    private static Column buildDeck(Card[] cards) {
        var deck = new Column();

        deck.addAll(stream(cards, LAST_BOARD, LAST_DECK).toList());
        return deck;
    }

    protected static int row(int at) {
        return switch (at) {
            case 21, 22, 23, 24, 25, 26, 27 -> 7;
            case 15, 16, 17, 18, 19, 20 -> 6;
            case 10, 11, 12, 13, 14 -> 5;
            case 6, 7, 8, 9 -> 4;
            case 3, 4, 5 -> 3;
            case 1, 2 -> 2;
            case 0 -> 1;
            default -> throw new RuntimeException("Invalid board card index: " + at);
        };
    }

    @SuppressWarnings("rawtypes")
    protected static Pair<Integer, List> getScore(List<?> list) {
        var scoringOnly = scoringOnly(list);
        return Pair.of(
                range(0, scoringOnly.size())
                        .map(i -> getClickScore(i, scoringOnly))
                        .sum(),
                list);
    }

    /*
     * Score Rules:
     * - 5: each pair
     * - 25 for row 7
     * - 50 for row 6
     * - 75 for row 5
     * - 100 for row 4
     * - 150 for row 3
     * - 250 for row 2
     * - 500 for clear board
     */
    protected static int getClickScore(int at, List<Card[]> list) {
        var item = list.get(at);
        return Optional.of(item)
                .filter(it -> it.length == 1)
                .map(it -> scoreForSingleCard(it[0], at, list))
                .orElseGet(() -> scoreForPair(at, list));
    }

    private static int scoreForSingleCard(Card card, int at, List<Card[]> list) {
        return isBoardCard(card.at()) ? getScore(row(card.at()), at, list) : card.isKing() ? 5 : 0;
    }

    protected static int scoreForPair(int at, List<Card[]> list) {
        return Optional.of(cardAt(list.get(at)))
                .filter(PyramidHelper::isBoardCard)
                .map(it -> row(it.at()))
                .map(row -> getScore(row, at, list))
                .orElse(5);
    }

    protected static int getScore(int row, int at, List<Card[]> list) {
        return 5 + (isRowCleared(row, at, list) ? scoreByRow(row) : 0);
    }

    protected static Card cardAt(Card[] cards) {
        var a = cards[0];
        var b = cards[1];

        if (isBoardCard(a) == isBoardCard(b)) {
            return a.at() >= b.at() ? a : b;
        } else if (isBoardCard(a)) {
            return a;
        }
        return b;
    }

    protected static boolean isBoardCard(Card card) {
        return isBoardCard(card.at());
    }

    protected static boolean isBoardCard(int at) {
        return 0 <= at && at < LAST_BOARD;
    }

    protected static int scoreByRow(int row) {
        assert 0 < row && row <= ROW_SCORES.length : "Invalid row number: " + row;
        return ROW_SCORES[row - 1];
    }

    protected static boolean isRowCleared(int row, int at, List<Card[]> list) {
        return countCardsCleared(row, at, list) == row;
    }

    protected static int countCardsCleared(int row, int at, List<Card[]> list) {
        return (int) rangeClosed(0, at)
                .mapToObj(list::get)
                .flatMap(Stream::of)
                .filter(PyramidHelper::isBoardCard)
                .filter(it -> row(it.at()) == row)
                .count();
    }

    @SuppressWarnings("rawtypes unchecked")
    protected static List<Card[]> scoringOnly(List list) {
        return ((List<Card[]>) list).stream()
                .filter(it -> it.length > 1 || it[0].isKing())
                .toList();
    }

}

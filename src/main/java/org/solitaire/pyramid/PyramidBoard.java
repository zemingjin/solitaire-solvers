package org.solitaire.pyramid;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.solitaire.model.Card;
import org.solitaire.model.CardHelper;
import org.solitaire.model.GameSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.solitaire.model.CardHelper.VALUES;
import static org.solitaire.model.CardHelper.cloneArray;
import static org.solitaire.model.CardHelper.cloneList;
import static org.solitaire.model.CardHelper.incTotal;
import static org.solitaire.model.CardHelper.isCleared;

@SuppressWarnings("rawtypes")
@Getter
@Builder
public class PyramidBoard implements GameSolver {
    public static final int LAST_BOARD = 28;
    public static final int LAST_DECK = 52;
    public static final char KING = 'K';
    private static final int[] ROW_SCORES = new int[]{500, 250, 150, 100, 75, 50, 25};
    private final IntUnaryOperator reverse = i -> LAST_BOARD + LAST_DECK - i - 1;
    private Card[] cards;
    private List<Card[]> wastePile;
    private int recycleCount;

    public static PyramidBoard build(String[] cards) {
        return Optional.of(cards)
                .map(CardHelper::toCards)
                .map(PyramidBoard::buildBoard)
                .orElseThrow();
    }

    private static PyramidBoard buildBoard(Card[] cards) {
        return PyramidBoard.builder().cards(cards).recycleCount(3).wastePile(new ArrayList<>()).build();
    }

    @Override
    public List<List> solve() {
        if (isCleared(cards, LAST_BOARD)) {
            return singletonList(wastePile);
        }
        incTotal();
        return Optional.of(findCardsOf13())
                .filter(it -> !it.isEmpty())
                .map(this::clickCards)
                .orElseGet(this::clickDeck);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<Integer, List> getMaxScore(List<List> results) {
        return results.stream()
                .map(it -> (List<Card[]>) it)
                .map(this::getScore)
                .reduce(Pair.of(0, null), (a, b) -> a.getLeft() >= b.getLeft() ? a : b);
    }

    private List<List> clickDeck() {
        checkDeck();
        return drawCard()
                .map(this::clickCard)
                .orElseGet(Collections::emptyList);
    }

    protected void checkDeck() {
        if (isNull(cards[LAST_BOARD]) && getRecycleCount() > 0) {
            recycleDeck();
        }
    }

    private void recycleDeck() {
        wastePile.stream()
                .flatMap(Stream::of)
                .filter(it -> it.at() >= LAST_BOARD)
                .forEach(it -> cards[it.at()] = it);
        recycleCount--;
    }

    protected Optional<Card[]> drawCard() {
        return IntStream.range(LAST_BOARD, LAST_DECK)
                .map(reverse)
                .filter(this::isOpenDeckCard)
                .mapToObj(i -> new Card[]{cards[i]})
                .findFirst();
    }

    private List<List> clickCards(List<Card[]> clickable) {
        return clickable.stream()
                .map(this::clickCard)
                .filter(it -> !it.isEmpty())
                .flatMap(List::stream)
                .toList();
    }

    public List<List> clickCard(Card[] cards) {
        return cloneBoard()
                .click(cards)
                .solve();
    }

    protected PyramidBoard click(Card[] clickable) {
        Stream.of(clickable).forEach(it -> cards[it.at()] = null);
        wastePile.add(clickable);
        return this;
    }

    protected PyramidBoard cloneBoard() {
        return PyramidBoard.builder()
                .cards(cloneArray(cards))
                .wastePile(cloneList(wastePile))
                .recycleCount(recycleCount)
                .build();
    }

    protected List<Card[]> findCardsOf13() {
        var collect = new LinkedList<Card[]>();
        var openCards = findOpenCards();

        IntStream.range(0, openCards.length - 1)
                .peek(i -> checkKing(openCards[i], collect))
                .forEach(i -> findPairsOf13(collect, openCards, i));

        checkKing(openCards[openCards.length - 1], collect);
        return collect;
    }

    private void findPairsOf13(LinkedList<Card[]> collect, Card[] openCards, int i) {
        IntStream.range(i + 1, openCards.length)
                .filter(j -> isAddingTo13(openCards[i], openCards[j]))
                .forEach(j -> collect.add(new Card[]{openCards[i], openCards[j]}));
    }

    private void checkKing(Card card, List<Card[]> collect) {
        if (isKing(card)) {
            collect.add(0, new Card[]{card});
        }
    }

    private boolean isKing(Card card) {
        return card.value() == KING;
    }

    private boolean isAddingTo13(Card a, Card b) {
        requireNonNull(a);
        requireNonNull(b);

        return VALUES.indexOf(a.value()) + VALUES.indexOf(b.value()) == 11;
    }

    protected Card[] findOpenCards() {
        return stream(cards)
                .filter(Objects::nonNull)
                .filter(this::isOpen)
                .toArray(Card[]::new);
    }

    protected boolean isOpen(Card card) {
        var at = card.at();
        return isOpenBoardCard(at) || isOpenDeckCard(at);
    }

    protected boolean isOpenBoardCard(int at) {
        return 0 <= at && at < LAST_BOARD && isOpenAt(at);
    }

    protected boolean isOpenAt(int at) {
        var row = getRow(at);
        var coveredBy = at + row;

        return row == 7 || (isNull(cards[coveredBy]) && isNull(cards[coveredBy + 1]));
    }

    protected int getRow(int at) {
        assert 0 <= at && at < LAST_BOARD : "Invalid board card index: " + at;

        var rowMax = LAST_BOARD;

        for (int row = 7; row > 0; row--) {
            if (rowMax - row <= at) {
                return row;
            } else {
                rowMax -= row;
            }
        }
        return 0;
    }

    protected boolean isOpenDeckCard(int at) {
        return isDeckCard(at) && nonNull(cards[at]) &&
                (at == LAST_DECK - 1 || isNull(cards[at + 1]));
    }

    @Override
    public boolean equals(Object obj) {
        return Optional.ofNullable(obj)
                .filter(it -> it instanceof PyramidBoard)
                .map(PyramidBoard.class::cast)
                .filter(it -> Arrays.equals(cards, it.cards))
                .filter(it -> wastePile.equals(it.wastePile))
                .filter(it -> recycleCount == it.recycleCount)
                .isPresent();
    }

    private Pair<Integer, List> getScore(List<Card[]> list) {
        var distinct = removeMultiples(list);
        return Pair.of(
                IntStream.range(0, distinct.size())
                        .map(i -> getClickScore(i, distinct))
                        .reduce(0, Integer::sum),
                list);
    }

    private List<Card[]> removeMultiples(List<Card[]> list) {
        var distinct = new ArrayList<>(list);

        for (int i = 0; i < distinct.size() - 1; i++) {
            for (int j = i + 1; j < distinct.size(); j++) {
                if (Arrays.equals(distinct.get(i), distinct.get(j))) {
                    distinct.remove(j);
                    break;
                }
            }
        }
        return distinct;
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
    protected int getClickScore(int at, List<Card[]> list) {
        var item = list.get(at);
        return Optional.of(item)
                .filter(it -> it.length == 1)
                .map(it -> isDeckCard(it[0].at()) && isKing(it[0]) ? 5 : 0)
                .orElseGet(() -> getRowClearingScore(at, list));
    }

    private int getRowClearingScore(int at, List<Card[]> list) {
        return Optional.ofNullable(getCardAt(list.get(at)))
                .map(it -> getRow(it.at()))
                .map(row -> getScore(row, at, list))
                .orElse(0);
    }

    private int getScore(int row, int at, List<Card[]> list) {
        return 5 + ((IntStream.rangeClosed(0, at)
                .mapToObj(list::get)
                .flatMap(Stream::of)
                .filter(this::isBoardCard)
                .filter(it -> getRow(it.at()) == row)
                .count() == row) ? scoreByRow(row) : 0);
    }

    private Card getCardAt(Card[] cards) {
        return Stream.of(cards)
                .filter(this::isBoardCard)
                .reduce(cards[0], (a, b) -> a.at() >= b.at() ? a : b);
    }

    private boolean isBoardCard(Card card) {
        return isBoardCard(card.at());
    }

    private boolean isBoardCard(int at) {
        return 0 <= at && at < LAST_BOARD;
    }

    private boolean isDeckCard(int at) {
        return !isBoardCard(at);
    }

    private int scoreByRow(int row) {
        assert 0 < row && row <= ROW_SCORES.length : "Invalid row number: " + row;
        return ROW_SCORES[row - 1];
    }
}
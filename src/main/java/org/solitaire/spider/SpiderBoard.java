package org.solitaire.spider;

import lombok.extern.slf4j.Slf4j;
import org.solitaire.model.Candidate;
import org.solitaire.model.Card;
import org.solitaire.model.Column;
import org.solitaire.model.Columns;
import org.solitaire.model.Deck;
import org.solitaire.model.GameBoard;
import org.solitaire.model.Path;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.solitaire.model.Origin.COLUMN;
import static org.solitaire.model.Origin.DECKPILE;
import static org.solitaire.model.Origin.FOUNDATION;
import static org.solitaire.model.SolveExecutor.isPrint;
import static org.solitaire.util.BoardHelper.isNotNull;
import static org.solitaire.util.BoardHelper.listNotEmpty;
import static org.solitaire.util.BoardHelper.verifyBoard;
import static org.solitaire.util.CardHelper.stringOfRaws;
import static org.solitaire.util.CardHelper.suitCode;
import static org.solitaire.util.CardHelper.toArray;

@Slf4j
public class SpiderBoard extends GameBoard {
    protected final Deck deck;
    private transient final IntPredicate isThereARun = i -> isThereARun(column(i));
    private transient final Predicate<Candidate> isNotFromSequenceOfSameSuit = candidate -> {
        var column = column(candidate.from());
        return column.size() >= 2 && !column.get(column.size() - 2).isHigherOfSameSuit(candidate.peek());
    };

    public SpiderBoard(Columns columns, Path<String> path, int totalScore, Deck deck) {
        super(columns, path, totalScore);
        this.deck = deck;
    }

    public SpiderBoard(SpiderBoard that) {
        this(new Columns(that.columns()), new Path<>(that.path()), that.totalScore(), new Deck(that.deck()));
    }

    /**************************************************************************************************************
     * Find/Match/Sort Candidates
     *************************************************************************************************************/
    @Override
    public List<Candidate> findCandidates() {
        return Optional.of(findBoardCandidates())
                .filter(listNotEmpty)
                .orElseGet(this::drawDeck);
    }

    private List<Candidate> findBoardCandidates() {
        return Optional.of(findCandidatesOfSameSuit())
                .map(it -> concat(it.stream(), findCandidatesByRanks(it)))
                .map(Stream::toList)
                .filter(listNotEmpty)
                .orElseGet(Collections::emptyList);
    }

    protected List<Candidate> findCandidatesOfSameSuit() {
        isInSequence(Card::isHigherOfSameSuit);
        return Optional.of(findColumnToColumnCandidates())
                .map(Stream::toList)
                .map(this::optimizeCandidatesOfSameSuit)
                .orElseGet(Collections::emptyList);
    }

    private List<Candidate> optimizeCandidatesOfSameSuit(List<Candidate> candidates) {
        return range(0, candidates.size())
                .filter(i -> isLongerTargetSequence(candidates.get(i)))
                .mapToObj(i -> checkForLongerTargets(candidates.get(i), candidates))
                .filter(isNotNull)
                .toList();
    }

    private Candidate checkForLongerTargets(Candidate candidate, List<Candidate> candidates) {
        return candidates.stream()
                .filter(candidate::isSameOrigin)
                .filter(it -> hasLongerTargetSequence(it, candidate))
                .findFirst()
                .orElse(null);
    }

    private boolean hasLongerTargetSequence(Candidate a, Candidate b) {
        return a.cards().length + getOrderedCards().apply(column(a.to())).length >=
                b.cards().length + getOrderedCards().apply(column(b.to())).length;
    }

    @Override
    protected Candidate candidateToEmptyColumn(Card[] cards, int from, int to) {
        return new Candidate(cards, COLUMN, from, COLUMN, to);
    }

    protected Stream<Candidate> findCandidatesByRanks(List<Candidate> candidates) {
        return range(0, columns().size())
                .filter(isNotEmpty)
                .mapToObj(this::findCandidatesByRanks)
                .flatMap(flattenStream)
                .filter(it -> notFromSameOrigin(it, candidates))
                .filter(this::isMovable);
    }

    private boolean notFromSameOrigin(Candidate candidate, List<Candidate> candidates) {
        return candidates.stream().noneMatch(it -> it.isSameOrigin(candidate));
    }

    private Stream<Candidate> findCandidatesByRanks(int to) {
        return range(0, columns().size())
                .filter(isNotEmpty)
                .mapToObj(from -> findCandidatesByRanks(to, from))
                .filter(isNotNull);
    }

    private Candidate findCandidatesByRanks(int to, int from) {
        var target = peek(to);
        var origin = peek(from);

        if (isNull(target) || target.isHigherRank(origin)) {
            return new Candidate(toArray(origin), COLUMN, from, COLUMN, to);
        }
        return null;
    }

    protected boolean isMovable(Candidate candidate) {
        return Optional.of(candidate)
                .filter(isNotFromSequenceOfSameSuit)
                .filter(isNotRepeatingCandidate)
                .filter(isMovableToEmptyColumn)
                .map(it -> !(column(it.to()).isEmpty() && isAtTop(it)))
                .orElse(false);
    }

    private boolean isAtTop(Candidate candidate) {
        return column(candidate.from()).indexOf(candidate.peek()) == 0;
    }

    /**************************************************************************************************************
     * Update Board
     *************************************************************************************************************/
    @Override
    public SpiderBoard updateBoard(Candidate candidate) {
        return removeFromSource(candidate)
                .appendToTarget(candidate)
                .checkForRun(candidate);
    }

    protected SpiderBoard removeFromSource(Candidate candidate) {
        switch (candidate.origin()) {
            case COLUMN -> removeFromColumn(candidate);
            case DECKPILE -> removeFromDeck(candidate);
        }
        return this;
    }

    private void removeFromDeck(Candidate candidate) {
        Stream.of(candidate.cards()).forEach(it -> deck().remove(it));
    }

    protected SpiderBoard appendToTarget(Candidate candidate) {
        path.add(candidate.notation());

        switch (candidate.origin()) {
            case DECKPILE -> range(0, columns().size()).forEach(i -> column(i).add(candidate.cards()[i]));
            case COLUMN -> {
                addToTargetColumn(candidate);
                totalScore--;
            }
        }
        return this;
    }

    protected SpiderBoard checkForRun(Candidate candidate) {
        IntStream.of(candidate.to())
                .filter(isThereARun)
                .forEach(this::removeTheRun);
        return this;
    }

    private boolean isThereARun(Column column) {
        if (nonNull(column) && 13 <= column.size()) {
            for (int i = column.size() - 1, floor = column.size() - 13; i > floor; i--) {
                if (!column.get(i - 1).isHigherOfSameColor(column.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void removeTheRun(int i) {
        var column = column(i);
        assert nonNull(column) && 13 <= column.size();

        var run = column.subList(column.size() - 13, column.size());
        var candidate = new Candidate(run.toArray(Card[]::new), COLUMN, i, FOUNDATION, suitCode(run.get(0)));

        path().add(candidate.notation());
        if (isPrint()) {
            System.out.printf("Run: %s\n", stringOfRaws(run));
        }
        totalScore += 100;
        removeFromColumn(candidate);
    }

    protected List<Candidate> drawDeck() {
        if (isNoEmptyColumn() && isNotEmpty(deck)) {
            var cards = deck().subList(0, columns.size()).toArray(Card[]::new);

            return List.of(new Candidate(cards, DECKPILE, 0, COLUMN, 0));
        }
        return emptyList();
    }

    protected boolean isNoEmptyColumn() {
        return columns().stream().allMatch(Column::isNotEmpty);
    }

    /*****************************************************************************************************************
     * Scoring board
     ****************************************************************************************************************/
    @Override
    public int score() {
        if (isNotScored()) {
            // The smaller, the better.
            var blockerCount = countBlockers();
            var boardScore = columns().stream().mapToInt(Column::size).sum() + deck().size();
            // The larger, the better.
            var sequences = calcSequences();
            super.score(sequences - blockerCount - boardScore);
        }
        return super.score();
    }

    private int countBlockers() {
        return range(0, columns().size()).map(this::countBlockers).sum();
    }

    protected int countBlockers(int col) {
        var column = column(col);

        if (column.isEmpty()) {
            return 0;
        }
        isInSequence(Card::isHigherOfSameSuit);
        var cards = getOrderedCards().apply(column);
        var card = cards[0];

        if (card.isNotKing()) {
            var next = card.next();
            var value = valueFromColumns(col, next);

            if (value == MAX_VALUE) {
                return deck.indexOf(next) / columns().size();
            }
            return value;
        }
        return 0;
    }

    private int valueFromColumns(int col, Card next) {
        return range(0, columns().size())
                .filter(i -> i != col)
                .mapToObj(this::column)
                .filter(listNotEmpty)
                .filter(it -> it.contains(next))
                .mapToInt(it -> it.size() - it.lastIndexOf(next) - 1)
                .reduce(MAX_VALUE, Math::min);
    }

    // The bigger, the better
    protected int calcSequences() {
        return range(0, columns().size())
                .filter(isNotEmpty)
                .map(i -> getOrderedCards().apply(column(i)).length * 2)
                .sum();
    }

    @Override
    public List<String> verify() {
        return verifyBoard(columns(), deck());
    }

    public Deck deck() {
        return deck;
    }

    @Override
    public boolean isSolved() {
        return super.isSolved() && deck.isEmpty();
    }

}

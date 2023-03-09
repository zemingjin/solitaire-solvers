package org.solitaire.model;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Slf4j
public class GameBoard<R> implements Board<R, Candidate> {
    protected final Columns columns;
    protected final Path<R> path;
    protected int totalScore;
    private transient int score = 0;

    public GameBoard(Columns columns, Path<R> path) {
        this(columns, path, 0);
    }

    public GameBoard(Columns columns, Path<R> path, int totalScore) {
        this.columns = columns;
        this.path = path;
        this.totalScore = totalScore;
    }

    protected void removeFromColumn(Candidate candidate) {
        Optional.of(candidate)
                .map(Candidate::from)
                .map(columns::get)
                .ifPresent(it -> removeFromColumn(candidate, it));
    }

    private void removeFromColumn(Candidate candidate, Column column) {
        var colSize = column.size();

        Optional.of(candidate)
                .map(Candidate::cards)
                .filter(it -> column.contains(it.get(0)))
                .ifPresent(it -> column.subList(colSize - it.size(), colSize).clear());
    }

    protected void addToTargetColumn(Candidate candidate) {
        var cards = candidate.cards();

        columns.get(candidate.to()).addAll(cards);
    }

    @Override
    public boolean isCleared() {
        return columns.isCleared();
    }

    @Override
    public Path<R> path() {
        return path;
    }

    public Columns columns() {
        return columns;
    }

    public int totalScore() {
        return totalScore;
    }

    @Override
    public List<String> verify() {
        return emptyList();
    }

    @Override
    public int score() {
        return score;
    }

    @Override
    public Board<R, Candidate> updateBoard(Candidate candidate) {
        return null;
    }

    public void score(int score) {
        this.score = score;
    }

    public List<Candidate> add(List<Candidate> collector, Candidate item) {
        collector.add(item);
        return collector;
    }

    public int countEmptyColumns() {
        return (int) columns.stream().filter(ObjectUtils::isEmpty).count();
    }
}

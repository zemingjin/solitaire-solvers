package org.solitaire.freecell;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.solitaire.model.Candidate;
import org.solitaire.model.Card;
import org.solitaire.model.Columns;
import org.solitaire.model.Path;
import org.solitaire.model.SolveExecutor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.IntStream.range;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

/**
 * G. Heineman’s Staged Deepening (HSD)
 * 1. Performing a depth-first search with a depth-bound of six.
 * 2. Apply the heuristic to evaluate all the board states exactly six moves away from the initial board state.
 * 3. Take the board state with the best score and do another depth-first search with a depth-bound of six from
 * that state.
 * 4. Repeat steps 2-3 and throw away the rest until a solution or some limit is reached.
 */
public class FreeCell extends SolveExecutor<FreeCellBoard> {
    private static final Function<List<FreeCellBoard>, List<FreeCellBoard>> reduceBoards =
            boards -> range(boards.size() * 3 / 5, boards.size()).mapToObj(boards::get).toList();

    public FreeCell(Columns columns) {
        super(new FreeCellBoard(columns, new Path<>(), new Card[4], new Card[4]), FreeCellBoard::new);
        solveBoard(singleSolution() ? this::solveByHSD : this::solveByDFS);
    }

    protected void solveByDFS(FreeCellBoard board) {
        Optional.of(board)
                .map(FreeCellBoard::findCandidates)
                .filter(ObjectUtils::isNotEmpty)
                .map(it -> applyCandidates(it, board))
                .map(it -> it.sorted(comparingInt(FreeCellBoard::score)).toList())
                .map(reduceBoards)
                .ifPresent(this::addBoards);
    }

    protected void solveByHSD(FreeCellBoard board) {
        var boards = List.of(board);

        for (int i = 1; i <= hsdDepth() && isNotEmpty(boards); i++) {
            boards = boards.stream().flatMap(this::search).toList();
        }
        Optional.of(boards)
                .filter(ObjectUtils::isNotEmpty)
                .map(List::stream)
                .map(it -> it.sorted(comparingInt(FreeCellBoard::score)))
                .map(this::getBestBoard)
                .ifPresent(super::addBoard);
    }

    private Stream<FreeCellBoard> search(FreeCellBoard board) {
        return Optional.of(board)
                .map(FreeCellBoard::findCandidates)
                .filter(ObjectUtils::isNotEmpty)
                .map(it -> applyCandidates(it, board))
                .stream()
                .flatMap(it -> it);
    }

    protected Stream<FreeCellBoard> applyCandidates(List<Candidate> candidates, FreeCellBoard board) {
        return candidates.stream()
                .map(it -> ofNullable(clone(board)).map(b -> b.updateBoard(it)).orElse(null))
                .filter(Objects::nonNull);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Pair<Integer, List> getMaxScore(List<List> results) {
        throw new RuntimeException("Not applicable");
    }
}
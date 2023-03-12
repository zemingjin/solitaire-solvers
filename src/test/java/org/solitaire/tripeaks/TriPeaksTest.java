package org.solitaire.tripeaks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.solitaire.model.SolveExecutor.isPrint;
import static org.solitaire.model.SolveExecutor.singleSolution;
import static org.solitaire.tripeaks.TriPeaksHelper.build;
import static org.solitaire.util.CardHelper.useSuit;
import static org.solitaire.util.IOHelper.loadFile;

class TriPeaksTest {
    static final String TEST_FILE = "games/tripeaks/tripeaks-120822-expert.txt";

    static final String[] cards = loadFile(TEST_FILE);

    private TriPeaks triPeaks;

    @BeforeEach
    void setup() {
        useSuit(false);
        isPrint(false);
        singleSolution(false);
        triPeaks = build(cards);
    }

    @Test
    void test_solve() {
        singleSolution(false);
        triPeaks.solve();

        assertEquals(3300, triPeaks.totalSolutions());
        assertEquals(15, triPeaks.maxDepth());
        assertEquals(853574, triPeaks.totalScenarios());
        assertEquals(45, triPeaks.shortestPath().size());
        assertEquals(50, triPeaks.longestPath().size());
        assertEquals(16900, triPeaks.maxScore().getLeft());
    }

    @Test
    void test_solve_HDS() {
        singleSolution(true);
        triPeaks.solve();

        assertEquals(0, triPeaks.totalSolutions());
        assertEquals(1, triPeaks.maxDepth());
    }

    @Test
    void test_verify_exception() {
        triPeaks.board().cards()[0] = null;

        var result = assertThrows(RuntimeException.class, () -> triPeaks.solve());

        assertEquals("[Missing card: 5c]", result.getMessage());
    }
}
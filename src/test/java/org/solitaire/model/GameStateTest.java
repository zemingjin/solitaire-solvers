package org.solitaire.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solitaire.util.CardHelper;
import org.solitaire.util.IOHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.solitaire.model.Candidate.buildCandidate;
import static org.solitaire.model.Origin.COLUMN;
import static org.solitaire.spider.SpiderHelper.build;

public class GameStateTest {
    protected static final String TEST_FILE = "games/spider/spider-122922-expert.txt";

    public final static String[] cards = IOHelper.loadFile(TEST_FILE);

    private GameState<String> state;

    private static GameState<String> mockState(GameState<String> state) {
        return new GameState<>(new Columns(state.columns), new Path<>(state.path), state.totalScore);
    }

    @BeforeEach
    public void setup() {
        CardHelper.useSuit = false;
        state = mockState(build(cards));
    }

    @Test
    public void test_equals() {
        var other = mockState(state);

        assertTrue(EqualsBuilder.reflectionEquals(other, state));
    }

    @Test
    public void test_removeFromColumn_success() {
        var candidate = buildCandidate(0, COLUMN, state.columns.get(0).peek());

        state.removeFromColumn(candidate);
        assertEquals(5, state.columns.get(0).size());
    }

    @Test
    public void test_removeFromColumn_skip() {
        var candidate = buildCandidate(1, COLUMN, state.columns.get(0).peek());

        state.removeFromColumn(candidate);
        assertEquals(6, state.columns.get(0).size());
        assertEquals(6, state.columns.get(1).size());
    }

    @Test
    public void test_setTotalScore() {
        state.setTotalScore(state.totalScore - 1);
        assertEquals(499, state.totalScore);
    }
}
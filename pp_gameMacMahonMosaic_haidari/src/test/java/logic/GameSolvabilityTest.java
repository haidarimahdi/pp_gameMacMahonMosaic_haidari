package logic;

import com.google.gson.JsonSyntaxException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class GameSolvabilityTest {
    private Game game;

    @Before
    public void setUp() {
        GUIConnector fakeGui = new FakeGUI();
        game = new Game(fakeGui);
    }

    @Test
    public void testSolvability_EmptyFieldIsSolvable() {
        String puzzleJson = """
        {
          "field": [
            ["NNNN", "NNGN", "NNGN", "NNGN", "NNNN"],
            ["NGNN", "NNNN", "NNNN", "NNNN", "NNNG"],
            ["NRNN", "NNNN", "NNNN", "NNNN", "NNNR"],
            ["NGNN", "NNNN", "NNNN", "NNNN", "NNNG"],
            ["NNNN", "YNNN", "GNNN", "YNNN", "NNNN"]
          ]
        }
        """;

        try {
            // Call the new method directly on the game object
            game.loadGameFromString(puzzleJson);
        } catch (IOException | JsonSyntaxException e) {
            // Fail the test if the string parsing throws an unexpected error
            fail("Loading puzzle from string failed: " + e.getMessage());
        }

        assertTrue("An empty field with valid border constraints should be solvable.", game.isPuzzleSolvable());
    }

    @Test
    public void testSolvability_NearlySolvedFieldIsSolvable() {
        String nearlySolvedJson = """
        {
          "field": [
            ["NNNN", "NNGN", "NNGN", "NNGN", "NNNN"],
            ["NGNN", "GRYG", "GRYR", "GGYR", "NNNG"],
            ["NRNN", "YGRR", "NNNN", "YRRG", "NNNR"],
            ["NGNN", "RYYG", "RYGY", "RGYY", "NNNG"],
            ["NNNN", "YNNN", "GNNN", "YNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(nearlySolvedJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }
        assertTrue("A nearly solved field with a valid configuration should be solvable.",
                game.isPuzzleSolvable());
    }

    @Test
    public void testSolvability_IncorrectlyPlacedPieceMakesItUnsolvable() {
        String unsolvableFieldJson = """
        {
          "field": [
            ["NNNN", "NNGN", "NNNN", "NNNN", "NNNN"],
            ["NNNN", "RYGY", "NNNN", "NNNN", "NNNN"],
            ["NYNN", "YGRY", "NNNN", "NNNN", "NNNR"],
            ["NNNN", "NNNN", "RNNN", "NNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(unsolvableFieldJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }
        assertFalse("A field with an incorrectly placed piece creating a conflict should be unsolvable.",
                game.isPuzzleSolvable());

    }

    // --- TESTS WITH A HOLE ---

    @Test
    public void testSolvability_EmptyFieldWithHoleIsSolvable() {
        String emptyFieldWithHoleJson = """
        {
          "field": [
            ["NNNN", "NNGN", "NNGN", "NNGN", "NNNN"],
            ["NGNN", "NNNN", "HHHH", "NNNN", "NNNG"],
            ["NRNN", "NNNN", "NNNN", "NNNN", "NNNR"],
            ["NGNN", "NNNN", "NNNN", "NNNN", "NNNG"],
            ["NNNN", "YNNN", "GNNN", "YNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(emptyFieldWithHoleJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }
        assertTrue("An empty field with a hole and valid borders should be solvable.", game.isPuzzleSolvable());
    }

    @Test
    public void testSolvability_NearlySolvedFieldWithHoleIsSolvable() {
        String nearlySolvedWithHoleJson = """
        {
          "field": [
            ["NNNN", "NNGN", "NNGN", "NNGN", "NNNN"],
            ["NGNN", "GRYG", "GRYR", "GGYR", "NNNG"],
            ["NRNN", "YGRR", "HHHH", "YRRG", "NNNR"],
            ["NGNN", "RYYG", "RYGY", "RGYY", "NNNG"],
            ["NNNN", "YNNN", "GNNN", "YNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(nearlySolvedWithHoleJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }
        assertTrue("A nearly solved field where the last space is a hole should be considered solvable.",
                game.isPuzzleSolvable());
    }

    @Test
    public void testSolvability_IncorrectlyPlacedPieceWithHoleIsUnsolvable() {
        String unsolvableWithHoleJson = """
        {
          "field": [
            ["NNNN", "NNGN", "NNNN", "NNNN", "NNNN"],
            ["NNNN", "RYGY", "HHHH", "NNNN", "NNNN"],
            ["NNNN", "YGRY", "NNNN", "NNNN", "NNNN"],
            ["NNNN", "NNNN", "NNNN", "NNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(unsolvableWithHoleJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }
        assertFalse("An incorrect placement conflict should make a puzzle unsolvable, even with a hole.",
                game.isPuzzleSolvable());
    }

}

package logic;

import com.google.gson.JsonSyntaxException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class GameWinConditionTest {
    private Game game;

    @Before
    public void setUp() {
        GUIConnector fakeGui = new FakeGUI();
        game = new Game(fakeGui);
    }

    @Test
    public void testGameWon_CorrectlySolvedField() {
        String solvedFieldJson = """
        {
          "field": [
            ["NNNN", "NNNN", "NNNN", "NNNN"],
            ["NNNN", "YGYG", "RGRG", "NNNN"],
            ["NNNN", "YRYR", "RRRR", "NNNN"],
            ["NNNN", "NNNN", "NNNN", "NNNN"]
          ]
        }
        """;
        try {
            // Load the game state from the string in one step
            game.loadGameFromString(solvedFieldJson);
        } catch (IOException | JsonSyntaxException e)  {
            fail("Test setup failed. Could not load puzzle from string: " + e.getMessage());
        }


        MosaicPiece topLeft = game.getGameField().getPieceAt(0, 0);     // YGYG
        MosaicPiece topRight = game.getGameField().getPieceAt(0, 1);    // RGRG
        MosaicPiece bottomLeft = game.getGameField().getPieceAt(1, 0);  // YRYR
        MosaicPiece bottomRight = game.getGameField().getPieceAt(1, 1); // RRRR

        assertEquals("Top left's east edge should match top right's west edge",
                topLeft.getEdgeColor(Direction.RIGHT), topRight.getEdgeColor(Direction.LEFT));
        assertEquals("Top left's south edge should match bottom left's north edge",
                topLeft.getEdgeColor(Direction.BOTTOM), bottomLeft.getEdgeColor(Direction.TOP));
        assertEquals("bottom right's north edge should match top right's south edge",
                bottomRight.getEdgeColor(Direction.TOP), topRight.getEdgeColor(Direction.BOTTOM));
        assertTrue("A correctly and fully solved field should result in a win.", game.isGameWon());
    }

    @Test
    public void testGameWon_NotYetSolvedField() {
        String notSolvedFieldJson = """
        {
          "field": [
            ["NNNN", "NNNN", "NNNN", "NNNN", "NNNN"],
            ["NNNN", "GRYG", "GRYR", "GGYR", "NNNN"],
            ["NNNN", "YGRR", "NNNN", "YRRG", "NNNN"],
            ["NNNN", "RYYG", "RYGY", "RGYY", "NNNN"],
            ["NNNN", "NNNN", "NNNN", "NNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(notSolvedFieldJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }

        MosaicPiece topLeft = game.getGameField().getPieceAt(0, 0);
        MosaicPiece topMiddle = game.getGameField().getPieceAt(0, 1);
        MosaicPiece topRight = game.getGameField().getPieceAt(0, 2);
        MosaicPiece midLeft = game.getGameField().getPieceAt(1, 0);
        MosaicPiece midRight = game.getGameField().getPieceAt(1, 2);
        MosaicPiece bottomLeft = game.getGameField().getPieceAt(2, 0);
        MosaicPiece bottomCenter = game.getGameField().getPieceAt(2, 1);
        MosaicPiece bottomRight = game.getGameField().getPieceAt(2, 2);

        assertEquals("Top Left's east edge should match top middle's west edge",
                topLeft.getEdgeColor(Direction.RIGHT),  topMiddle.getEdgeColor(Direction.LEFT));
        assertEquals("Top right's south edge should match midlle right's north edge",
                topRight.getEdgeColor(Direction.BOTTOM),  midRight.getEdgeColor(Direction.TOP));
        assertEquals("Middle left's south edge should match bottom left's north edge",
                midLeft.getEdgeColor(Direction.BOTTOM), bottomLeft.getEdgeColor(Direction.TOP));
        assertEquals("Bottom center's east edge should match bottom right's west edge",
                bottomCenter.getEdgeColor(Direction.RIGHT), bottomRight.getEdgeColor(Direction.LEFT));
        assertFalse("A partially filled field should not be considered won.", game.isGameWon());
    }

    @Test
    public void testGameWon_FullButIncorrectField() {
        String incorrectFieldJson = """
        {
          "field": [
            ["NNNN", "NNNN", "NNNN", "NNNN"],
            ["NNNN", "YGYG", "GRGG", "NNNN"],
            ["NNNN", "YYYY", "RRRR", "NNNN"],
            ["NNNN", "NNNN", "NNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(incorrectFieldJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }

        MosaicPiece topLeft = game.getGameField().getPieceAt(0, 0);     // YGYG
        MosaicPiece topRight = game.getGameField().getPieceAt(0, 1);    // GRGG
        MosaicPiece bottomLeft = game.getGameField().getPieceAt(1, 0);  // YYYY
        MosaicPiece bottomRight = game.getGameField().getPieceAt(1, 1); // RRRR

        assertEquals("Top left's east edge should match top right's west edge",
                topLeft.getEdgeColor(Direction.RIGHT), topRight.getEdgeColor(Direction.LEFT));
        assertEquals("Top left's south edge should match bottom left's north edge",
                topLeft.getEdgeColor(Direction.BOTTOM), bottomLeft.getEdgeColor(Direction.TOP));
        assertNotEquals("bottom right's north edge should NOT match top right's south edge",
                bottomRight.getEdgeColor(Direction.TOP), topRight.getEdgeColor(Direction.BOTTOM));

        assertFalse("A fully occupied field with an invalid placement should not be won.", game.isGameWon());
    }

    // --- TESTS WITH A HOLE ---

    @Test
    public void testGameWon_CorrectlySolvedFieldWithHole() {
        String solvedWithHoleJson = """
        {
          "field": [
            ["NNNN", "NNNN", "NNNN", "NNNN"],
            ["NNNN", "YGYG", "RGRG", "NNNN"],
            ["NNNN", "HHHH", "RRRR", "NNNN"],
            ["NNNN", "NNNN", "NNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(solvedWithHoleJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }
        assertTrue("A solved field with a hole should result in a win.", game.isGameWon());
    }

    @Test
    public void testGameWon_NotYetSolvedFieldWithHole() {
        String notSolvedWithHoleJson = """
        {
          "field": [
            ["NNNN", "NNNN", "NNNN", "NNNN", "NNNN"],
            ["NNNN", "GRYG", "GRYR", "GGYR", "NNNN"],
            ["NNNN", "YGRR", "HHHH", "YRRG", "NNNN"],
            ["NNNN", "RYYG", "RYGY", "NNNN", "NNNN"],
            ["NNNN", "NNNN", "NNNN", "NNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(notSolvedWithHoleJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }
        assertFalse("A partially filled field with a hole should not be won.", game.isGameWon());
    }

    @Test
    public void testGameWon_FullButIncorrectFieldWithHole() {
        String incorrectWithHoleJson = """
        {
          "field": [
            ["NNNN", "NNNN", "NNNN", "NNNN"],
            ["NNNN", "YGYG", "GRGR", "NNNN"],
            ["NNNN", "HHHH", "YYYY", "NNNN"],
            ["NNNN", "NNNN", "NNNN", "NNNN"]
          ]
        }
        """;
        try {
            game.loadGameFromString(incorrectWithHoleJson);
        } catch (IOException | JsonSyntaxException e) {
            fail("Test setup failed: " + e.getMessage());
        }
        assertFalse("A full but incorrect field with a hole should not be won.", game.isGameWon());
    }

}

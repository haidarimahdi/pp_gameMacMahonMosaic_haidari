package logic;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameTest {

    private Game game;

    @Before
    public void setUp() {
        GUIConnector fakeGui = new FakeGUI();
        game = new Game(fakeGui);
        // We now initialize the game in each test to ensure a clean state.
    }

    @Test
    public void testPlacePieceOnEmptyCell() {
        // Setup the game board
        game.startEditor(3, 3); // A simple 3x3 board

        // Get a piece to place
        MosaicPiece pieceToPlace = game.getAvailablePieces().get(0);
        assertNotNull("Should be able to get a piece from the available list.", pieceToPlace);

        // Attempt to place the piece on an empty cell
        boolean placedSuccessfully = game.placePiece(pieceToPlace, 1, 1);

        // Assert that the placement was successful
        assertTrue("Should be able to place a piece on an empty cell.", placedSuccessfully);
        assertFalse("The cell should now be occupied.", game.getGameField().isCellEmpty(1, 1));
        assertEquals("The correct piece should be at the location.", pieceToPlace, game.getGameField().getPieceAt(1, 1));
    }

    @Test
    public void testPlacePieceOnOccupiedCell() {
        // 1. Setup the game board and place an initial piece
        game.startEditor(3, 3);
        MosaicPiece firstPiece = game.getAvailablePieces().get(0);
        game.placePiece(firstPiece, 1, 1); // Pre-occupy a cell

        // 2. Get another piece to attempt to place in the same spot
        MosaicPiece secondPiece = game.getAvailablePieces().get(1);

        // 3. Attempt to place the second piece on the already occupied cell
        boolean placedSuccessfully = game.placePiece(secondPiece, 1, 1);

        // 4. Assert that the placement was not successful
        assertFalse("Should not be able to place a piece on an occupied cell.", placedSuccessfully);
        assertEquals("The original piece should still be in the cell.", firstPiece, game.getGameField().getPieceAt(1, 1));
    }

    @Test
    public void testPlacePieceOnHole() {
        // 1. Setup the game board and create a hole
        game.startEditor(3, 3);
        game.getGameField().setHole(1, 1); // Designate a cell as a hole

        // 2. Get a piece to place
        MosaicPiece pieceToPlace = game.getAvailablePieces().get(0);

        // 3. Attempt to place the piece on the hole
        boolean placedSuccessfully = game.placePiece(pieceToPlace, 1, 1);

        // 4. Assert that the placement was not successful
        assertFalse("Should not be able to place a piece on a hole.", placedSuccessfully);
        assertTrue("The cell should remain empty.", game.getGameField().isCellEmpty(1, 1));
        assertTrue("The cell should still be a hole.", game.getGameField().isCellHole(1, 1));
    }
}
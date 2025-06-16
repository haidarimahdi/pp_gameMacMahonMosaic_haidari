package logic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private Game game;

    @BeforeEach
    void setUp() {
        GUIConnector fakeGui = new FakeGUI();
        game = new Game(fakeGui);
        game.startGame(4, 3);
        Field field = game.getGameField();
    }

    @Test
    void testGameInitialization() {
        assertNotNull(game);
        assertNotNull(game.getAvailablePieces());

        assertEquals(24, game.getAvailablePieces().size());
        assertEquals(3, game.getGameField().getColumns());
        assertEquals(4, game.getGameField().getRows());
    }

    @Test
    void testPiecePlacement() {
        GUIConnector fakeGui = new FakeGUI();
        Game game = new Game(fakeGui);
        game.startGame(4, 3);

        MosaicPiece pieceToPlace = game.getAvailablePieces().get(0); // Get a piece
        assertNotNull(pieceToPlace, "Piece should not be null.");
        boolean placed = game.placePiece(pieceToPlace, 0, 0);
        assertTrue(placed, "Piece should be placed on empty board.");

        MosaicPiece pieceToPlace2 = game.getAvailablePieces().get(1);
        assertNotNull(pieceToPlace2, "Piece should not be null.");
        boolean placedAgain = game.placePiece(pieceToPlace2, 0, 0);
        assertFalse(placedAgain, "Piece should not be placed again.");
    }


}
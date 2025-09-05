package logic;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SolverTest {

    private Field field;
    private Map<BorderPosition, Color> borderColors;

    @Before
    public void setUp() {
        // This setup creates a blank 3x3 field for each test
        borderColors = new HashMap<>();
        field = new Field(3, 3, borderColors, new HashSet<>());
    }

    @Test
    public void testPlacement_AllNeighborsMatch() {
        // --- Setup ---
        // Place pieces around the center cell (1, 1)
        // Piece NORTH of center (at 0,1), its SOUTH edge is RED
        field.setPieceAt(0, 1, new MosaicPiece("RRRR"));
        // Piece EAST of center (at 1,2), its WEST edge is GREEN
        field.setPieceAt(1, 2, new MosaicPiece("GGGG"));
        // Piece SOUTH of center (at 2,1), its NORTH edge is YELLOW
        field.setPieceAt(2, 1, new MosaicPiece("YYYY"));
        // Piece WEST of center (at 1,0), its EAST edge is RED
        field.setPieceAt(1, 0, new MosaicPiece("RRRR"));

        // --- Test ---
        // Create a piece that will match all surrounding pieces
        // Pattern: N=Y, E=R, S=G, W=R
        MosaicPiece pieceToPlace = new MosaicPiece("RGYR");

        // --- Assertion ---
        assertTrue("Placement should be valid when all edges match their neighbors.",
                Solver.checkPlacementValidity(pieceToPlace, 1, 1, field, borderColors));
    }

    @Test
    public void testPlacement_OneNeighborDoesNotMatch() {
        // --- Setup ---
        field.setPieceAt(0, 1, new MosaicPiece("RRRR")); // South edge is RED
        field.setPieceAt(1, 2, new MosaicPiece("GGGG")); // West edge is GREEN
        field.setPieceAt(2, 1, new MosaicPiece("YYYY")); // North edge is YELLOW
        field.setPieceAt(1, 0, new MosaicPiece("RRRR")); // East edge is RED

        // --- Test ---
        MosaicPiece pieceToPlace = new MosaicPiece("RGYG");

        // --- Assertion ---
        assertFalse("Placement should be invalid if even one edge does not match.",
                Solver.checkPlacementValidity(pieceToPlace, 1, 1, field, borderColors));
    }

    @Test
    public void testPlacement_BorderMatch() {
        // --- Setup ---
        // Set the top border (NORTH side) at index 0 to be RED
        borderColors.put(new BorderPosition(Direction.TOP, 0), Color.RED);
        Field fieldWithBorder = new Field(3, 3, borderColors, new HashSet<>());

        // --- Test ---
        // This piece has a RED north edge, which matches the border
        MosaicPiece pieceToPlace = new MosaicPiece("RGYG"); // N=R, E=G, S=Y, W=G

        // --- Assertion ---
        assertTrue("Placement should be valid when the piece's edge matches the border color.",
                Solver.checkPlacementValidity(pieceToPlace, 0, 0, fieldWithBorder, borderColors));
    }

    @Test
    public void testPlacement_BorderMismatch() {
        // --- Setup ---
        // Set the top border (NORTH side) at index 0 to be RED
        borderColors.put(new BorderPosition(Direction.TOP, 0), Color.RED);
        Field fieldWithBorder = new Field(3, 3, borderColors, new HashSet<>());

        // --- Test ---
        // This piece has a GREEN north edge, which does NOT match the RED border
        MosaicPiece pieceToPlace = new MosaicPiece("GRGY"); // N=G, E=R, S=G, W=Y

        // --- Assertion ---
        assertFalse("Placement should be invalid when the piece's edge does not match the border.",
                Solver.checkPlacementValidity(pieceToPlace, 0, 0, fieldWithBorder, borderColors));
    }

    @Test
    public void testPlacement_NeighboringHoleIsValid() {
        // --- Setup ---
        // Place a piece at (1,0). Its EAST edge is GREEN.
        field.setPieceAt(1, 0, new MosaicPiece("GYGG"));
        // Create a hole at (1,2). The cell at (1,1) is between the piece and the hole.
        field.setHole(1, 2);

        // --- Test ---
        MosaicPiece pieceToPlace = new MosaicPiece("RGYG"); // N=R, E=G, S=Y, W=G
        pieceToPlace.setOrientation(90);

        // --- Assertion ---
        // The placement should be considered valid because the only hard constraint (the piece
        // to the West) is met. The edge facing the hole is ignored.
        assertTrue("Placement should be valid if the only non-matching neighbor is a hole.",
                Solver.checkPlacementValidity(pieceToPlace, 1, 1, field, borderColors));

    }
}

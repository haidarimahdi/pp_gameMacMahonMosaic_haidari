package logic;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;


public class EdgeAvailabilityTest {
    private Game game;
    private GUIConnector fakeGui;
    private PuzzleEditor editor;
    private List<MosaicPiece> allPieces;

    @Before
    public void setUp() {
        fakeGui = new FakeGUI();
        game = new Game(fakeGui);
        editor = new PuzzleEditor(null, fakeGui);
    }

    /**
     * Helper to create a Game instance with a specific field, borders, and a custom
     * list of available pieces for precise testing.
     */
    private Game setupGameForEdgeTest(int rows, int cols, Map<BorderPosition, Color> borders, List<MosaicPiece> availablePieces) {
        Set<Position> holes = new HashSet<>();
        Field field = new Field(rows, cols, borders, holes);
        return new Game(fakeGui, field, availablePieces, allPieces, borders);
    }

    @Test
    public void testHasEnoughEdges_SufficientEdges_ShouldPass() {
        // --- Setup ---
        // A 1x1 field with a RED top border. This creates a "demand" for one RED edge.
        Map<BorderPosition, Color> borders = new HashMap<>();
        borders.put(new BorderPosition(Direction.TOP, 0), Color.RED);
        Field field = new Field(1, 1, borders, new HashSet<>());

        // Provide a "supply" of pieces that includes at least one RED edge.
        List<MosaicPiece> available = new ArrayList<>();
        available.add(new MosaicPiece("RRRR")); // Has 4 red edges
        available.add(new MosaicPiece("GGGG"));

        // --- Assertion ---
        assertTrue("Should return true when available edges meet or exceed required edges.",
                editor.hasEnoughAvailableEdges(field, available));
    }

    @Test
    public void testHasEnoughEdges_InsufficientEdges_ShouldFail() {
        // --- Setup ---
        // A 1x1 field with RED top and RED left borders. This demands two RED edges.
        Map<BorderPosition, Color> borders = new HashMap<>();
        borders.put(new BorderPosition(Direction.TOP, 0), Color.RED);
        borders.put(new BorderPosition(Direction.LEFT, 0), Color.RED);

        // Provide a "supply" of pieces that only has one RED edge in total.
        List<MosaicPiece> available = new ArrayList<>();
        available.add(new MosaicPiece("RGYG")); // Only one red edge
        available.add(new MosaicPiece("GGGG"));

        // --- Assertion ---
        assertFalse("Should return false when required RED edges (2) exceed available (1).",
                editor.hasEnoughAvailableEdges(new Field(1, 1, borders, new HashSet<>()), available));
    }

    @Test
    public void testHasEnoughEdges_NoEmptyCells_ShouldPass() {
        // --- Setup ---
        // A 1x1 field that is already full. The demand for all edges is zero.
        Map<BorderPosition, Color> borders = new HashMap<>();
        List<MosaicPiece> available = new ArrayList<>(); // No available pieces needed
        game = setupGameForEdgeTest(1, 1, borders, available);
        game.getGameField().setPieceAt(0, 0, new MosaicPiece("RRRR")); // Occupy the only cell

        // --- Assertion ---
        assertTrue("Should always return true for a completely filled board (no demand).",
                editor.hasEnoughAvailableEdges(new Field(1, 1, borders, new HashSet<>()), available));
    }

    @Test
    public void testHasEnoughEdges_ComplexUnsolvableScenario_ShouldFail() {
        // --- Setup ---
        // A 2x1 field surrounded by RED borders, demanding 6 RED edges in total.
        Map<BorderPosition, Color> borders = new HashMap<>();
        borders.put(new BorderPosition(Direction.TOP, 0), Color.RED);
        borders.put(new BorderPosition(Direction.LEFT, 0), Color.RED);
        borders.put(new BorderPosition(Direction.LEFT, 1), Color.RED);
        borders.put(new BorderPosition(Direction.BOTTOM, 0), Color.RED);
        borders.put(new BorderPosition(Direction.RIGHT, 0), Color.RED);
        borders.put(new BorderPosition(Direction.RIGHT, 1), Color.RED);


        // The entire set of 24 pieces only has a certain number of red edges.
        // By demanding 6 for just two spots, and having other colors on pieces,
        // it's very likely to be impossible. Let's make it definitively impossible
        // by providing only pieces with no red edges.
        List<MosaicPiece> available = new ArrayList<>();
        available.add(new MosaicPiece("GGGG"));
        available.add(new MosaicPiece("YYYY"));
        available.add(new MosaicPiece("YGYG"));

        // --- Assertion ---
        assertFalse("Should fail because the demand for 6 RED edges cannot be met by the 0 available.",
                editor.hasEnoughAvailableEdges(new Field(1, 1, borders, new HashSet<>()), available));
    }
}

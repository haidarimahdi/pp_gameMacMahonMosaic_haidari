package logic;

import java.util.*;

public class PuzzleEditor {

    private final Game game;
    private final GUIConnector gui;


    static final int MAX_PIECES = 24; // Maximum number of pieces on the board


    public PuzzleEditor(Game game, GUIConnector gui) {
        this.game = game;
        this.gui = gui;
    }

    public Field startEditor(int newRows, int newCols) {
        Map<BorderPosition, Color> borderColors = new HashMap<>();
        return new Field(newRows, newCols, borderColors, new HashSet<>());
    }

    public void toggleHoleState(int gameLogicRow, int gameLogicCol) {
        Field gameField = game.getGameField();
        if (gameField == null) {
            gui.showStatusMessage("error.field.not.initialized");
            return;
        }

        if (gameField.isCellHole(gameLogicRow, gameLogicCol)) {
            gameField.removeHole(gameLogicRow, gameLogicCol);
            gui.updateGameCell(gameLogicRow, gameLogicCol, null, 0, false, false);
            gui.showStatusMessage("editor.hole.remove.success", gameLogicRow, gameLogicCol);
            return;
        }

        int totalCells = gameField.getRows() * gameField.getColumns();
        if (totalCells <= MAX_PIECES) {
            gui.showStatusMessage("editor.holes.not.allowed", MAX_PIECES);
            return;
        }

        int requiredHoles = totalCells - MAX_PIECES;
        int currentHoles = gameField.getNumberOfHoles();

        if (currentHoles >= requiredHoles) {
            gui.showStatusMessage("editor.holes.limit.reached", + requiredHoles);
            return;
        }

        gameField.setHole(gameLogicRow, gameLogicCol);
        gui.updateGameCell(gameLogicRow, gameLogicCol, null, 0,
                false, true); // Mark the cell as a hole
        gui.showStatusMessage("editor.hole.add.success", gameLogicRow, gameLogicCol, (currentHoles + 1),
                requiredHoles);
    }

    public void setEditorBorderColor(logic.BorderPosition borderPosition, Color newColor) {
        game.getGameField().getBorderColors().put(borderPosition, newColor);
        gui.updateBorderColor(borderPosition, newColor);
//        game.setDirty(true);
    }

    public boolean isPuzzleReadyToPlay() {
        Field gameField = game.getGameField();
        if (gameField == null) {
            gui.showStatusMessage("error.field.not.initialized");
            return false;
        }

        if (!hasEnoughAvailableEdges(gameField, game.getAvailablePieces())) {
            return false;
        }

        int cols = gameField.getColumns();
        int rows = gameField.getRows();
        for (int c = 0; c < cols; c++) {
            // Check TOP/Bottom border
            if (!isBorderSegmentValid(new BorderPosition(Direction.TOP, c)) ||
                    !isBorderSegmentValid(new BorderPosition(Direction.BOTTOM, c))) {
                gui.showAlert("alert.not.playable.title","alert.not.playable.body");
                gui.showStatusMessage("editor.border.color.missing");
                return false;
            }
        }

        for (int r = 0; r < rows; r++) {
            // Check LEFT border
            if (!isBorderSegmentValid(new BorderPosition(Direction.LEFT, r)) ||
                    !isBorderSegmentValid(new BorderPosition(Direction.RIGHT, r))) {
                gui.showAlert("alert.not.playable.title","alert.not.playable.body");
                gui.showStatusMessage("editor.border.color.missing");
                return false;
            }
        }

        int totalCells = rows * cols;
        if (totalCells > MAX_PIECES) {
            int requiredHoles = totalCells - MAX_PIECES;
            if (gameField.getNumberOfHoles() < requiredHoles) {
                gui.showStatusMessage("Error: For a " + rows + "x" + cols +
                        " board, you need at least " + requiredHoles + " holes.");
                return false;
            }
        }

        int freeCells = game.getNumberOfFreeCells();
        if (freeCells > Game.MIN_ALLOWED_FREE_CELL) {
            // As per Nils' new rule, the solvability check is skipped when more than 18 cells are empty
            // and the game is allowed to start.
            gui.showAlert("alert.solvability.skipped.title","alert.solvability.skipped.body");

            game.clearSavedSolution();
            return true;
        }

//        this.savedSolution = solvePuzzle(gameField.deepCopy(), new ArrayList<>(availablePieces));
//        if (this.savedSolution == null) {
//            gui.showStatusMessage("Error: This puzzle is not solvable (checked with " + freeCells + " cells).");
//            return false;
//        }



        if (!game.isPuzzleSolvable()) {
            gui.showStatusMessage("error.unsolvable");
            return false;
        }

        gui.showStatusMessage("game.puzzle.ready");

        return true; // All checks passed, puzzle is ready to play
    }

    /**
     * Checks if a border segment is valid. A segment is valid if it has a color,
     * or if it is adjacent to a hole (in which case it doesn't need a color).
     *
     * @param borderPosition The BorderPosition object representing the segment to check.
     * @return true if the segment is valid, false otherwise.
     */
    private boolean isBorderSegmentValid(BorderPosition borderPosition) {
        Field gameField = game.getGameField();
        if (gameField == null) {
            return false;
        }

        Color color = gameField.getBorderColors().get(borderPosition);
        boolean isColorMissing = (color == null || color == Color.NONE);

        boolean isBorderOkay = true;
        if (isColorMissing) {
            Position adjacentCell = getAdjacentCellForBorderKey(borderPosition);
            if (adjacentCell == null || !gameField.isCellHole(adjacentCell.row(), adjacentCell.column())) {
                gui.showStatusMessage("editor.border.color.missing");
                isBorderOkay = false;
            }
        }

        return isBorderOkay;    // The segment has a color and is not next to a hole, so it's valid.
    }

    /**
     * Helper method to get the coordinates of the game cell adjacent to a given border segment.
     *
     * @param borderPosition The BorderPosition object for the border segment.
     * @return The Position of the adjacent cell.
     */
    private Position getAdjacentCellForBorderKey(BorderPosition borderPosition) {
        if (borderPosition == null) return null;

        Field gameField = game.getGameField();

        return switch (borderPosition.side()) {
            case TOP -> new Position(0, borderPosition.index());
            case BOTTOM -> new Position(gameField.getRows() - 1, borderPosition.index());
            case LEFT -> new Position(borderPosition.index(), 0);
            case RIGHT -> new Position(borderPosition.index(), gameField.getColumns() - 1);
        };
    }

    /**
     * Performs a fast pre-check to see if there are enough colored edges on the
     * available pieces to satisfy the requirements of the current board state.
     *
     * @return True if there are enough edges for a potential solution, false otherwise.
     */
    boolean hasEnoughAvailableEdges(Field field, List<MosaicPiece> availablePieces) {
        // 1. Count the total number of available edges from the remaining pieces.
        Map<Color, Integer> availableEdges = new EnumMap<>(Color.class);
        availableEdges.put(Color.RED, 0);
        availableEdges.put(Color.GREEN, 0);
        availableEdges.put(Color.YELLOW, 0);

//        for (Color c : Color.values()) {
//            if (c != Color.NONE && c!= Color.HOLE) availableEdges.put(c, 0);
//        }
        for (MosaicPiece piece : availablePieces) {
            for (Color color : piece.getColorPattern()) {
                if (availableEdges.containsKey(color)) {
                    availableEdges.put(color, availableEdges.get(color) + 1);
                }
            }
        }

        // 2. Count the number of required edges from empty spots on the board.
        Map<Color, Integer> requiredEdges = new EnumMap<>(Color.class);
        requiredEdges.put(Color.RED, 0);
        requiredEdges.put(Color.GREEN, 0);
        requiredEdges.put(Color.YELLOW, 0);

//        for (Color c : Color.values()) {
//            if (c != Color.NONE) requiredEdges.put(c, 0);
//        }

        int rows = field.getRows();
        int cols = field.getColumns();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (field.isCellEmpty(r, c) && !field.isCellHole(r, c)) {
                    for (Direction dir : Direction.values()) {
                        Color requiredColor = Solver.getRequiredEdgeColorFor(r, c, dir, field,
                                field.getBorderColors());
                        if (requiredEdges.containsKey(requiredColor)) {
                            requiredEdges.put(requiredColor, requiredEdges.get(requiredColor) + 1);
                        }
                    }
                }
            }
        }

        // 3. Compare required edges to available edges. If any requirement is too high, fail fast.
        for (Color color : requiredEdges.keySet()) {
            if (requiredEdges.get(color) > availableEdges.get(color)) {
                gui.showStatusMessage("error.not.enough.edges",color.name());
                return false;
            }
        }

        return true;
    }

}

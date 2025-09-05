package logic;

import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.*;

public class Game {
    private final GUIConnector gui;
    private Field gameField;
    private List<MosaicPiece> availablePieces;
    private final List<MosaicPiece> allPuzzlePieces; // Master list from TileLoader
    private final PuzzleFileManager fileManager;
    private final PuzzleEditor puzzleEditor;
    public static final int MIN_ALLOWED_FREE_CELL = 18;
    static final int EDGE_COUNT = Direction.values().length;
    private boolean isEditorMode = false; // Flag to track if the game is in editor mode
    private boolean isDirty = false; // Flag to track if the game state has been modified
    private Map<BorderPosition, Color> currentBoardBorderColors;
    private Field savedSolution = null; // Cache for the puzzle solution, if available

    public Game(GUIConnector gui) {
        this.gui = gui;
        this.allPuzzlePieces = new ArrayList<>();
        loadAndInitializeAllPieces();
        this.fileManager = new PuzzleFileManager(this.allPuzzlePieces);
        this.puzzleEditor = new PuzzleEditor(this, this.gui);
    }

    // Constructor for testing purposes
    public Game(GUIConnector gui, Field gameField, List<MosaicPiece> availablePieces, List<MosaicPiece> allPuzzlePieces,
                Map<BorderPosition, Color> currentBoardBorderColors) {
        this.gui = gui;
        this.gameField = gameField;
        this.availablePieces = availablePieces;
        this.allPuzzlePieces = allPuzzlePieces;
        this.currentBoardBorderColors = currentBoardBorderColors;
        this.fileManager = new PuzzleFileManager(this.allPuzzlePieces);
        this.puzzleEditor = new PuzzleEditor(this, this.gui);
    }

    public boolean isEditorMode() {
        return isEditorMode;
    }
    public boolean isDirty() {
        return isDirty;
    }


    /**
     * Loads a puzzle configuration from a specified file.
     * This method handles the file reading and JSON parsing.
     *
     * @param file The .json file to load the puzzle from.
     * @throws IOException         If there is an error reading the file.
     * @throws JsonSyntaxException If the file content is not valid JSON.
     */
    public void loadGameFromFile(File file) throws IOException, JsonSyntaxException {
        PuzzleState state = fileManager.loadPuzzleFromFile(file);
        initializeGameFromState(state);
    }

    private void initializeGameFromState(PuzzleState state) {
        this.gameField = state.field();
        this.currentBoardBorderColors = state.borderColors();
        this.availablePieces = new ArrayList<>(this.allPuzzlePieces);

        for (MosaicPiece piece : state.piecesOnBoard()) {
            this.availablePieces.removeIf(p ->
                    Arrays.equals(p.getColorPattern(), piece.getColorPattern()));
        }

        gui.initializeBoardView(gameField.getRows(), gameField.getColumns(), this.currentBoardBorderColors);
        redrawFullBoardState();
        updateAvailablePiecesInGUI();

        if (isGameWon()) {
            this.isEditorMode = false;
            gui.showGameEndMessage("game.load.solved.title", "game.load.solved.message");
        } else if (!puzzleEditor.isPuzzleReadyToPlay()) {
            this.isEditorMode = true;
        } else {
            this.isEditorMode = false;
            gui.showStatusMessage("game.load.success");
        }

        isDirty = false;
    }

    public void loadGameFromString(String jsonString) throws IOException, JsonSyntaxException {
        PuzzleState state = fileManager.loadPuzzleFromString(jsonString);
        initializeGameFromState(state);
    }

    public static BorderPosition getBorderPositionForCoords(int r, int c, int gameRows, int gameCols) {
        if (c > 0 && c <= gameCols) {
            if (r == 0) { // Top border (row 0), excluding corners
                return new BorderPosition(Direction.TOP, c - 1);
            }

            if (r == gameRows + 1) { // Bottom border (last row), excluding corners
                return new BorderPosition(Direction.BOTTOM, c - 1);
            }
        }
        if (r > 0 && r <= gameRows) {
            if (c == 0) { // Left border (column 0), excluding corners
                return new BorderPosition(Direction.LEFT, r - 1);
            }

            if (c == gameCols + 1) { // Right border (last column), excluding corners
                return new BorderPosition(Direction.RIGHT, r - 1);
            }
        }
        return null; // It's a corner or a game cell
    }

    private void redrawFullBoardState() {
        if (gameField == null) return;
        for (int r = 0; r < gameField.getRows(); r++) {
            for (int c = 0; c < gameField.getColumns(); c++) {
                MosaicPiece piece = gameField.getPieceAt(r, c);
                if (piece != null) {
                    boolean isError = !Solver.checkPlacementValidity(piece, r, c,
                            gameField, currentBoardBorderColors);
                    gui.updateGameCell(r, c, getPatternStringFromPiece(piece),
                            piece.getOrientation(), isError, false);
                } else {
                    boolean isHole = gameField.isCellHole(r, c);
                    gui.updateGameCell(r, c, null, 0, false, isHole);
                }
            }
        }
    }

    public void toggleHoleState(int row, int col) {
        if (isEditorMode) {
            puzzleEditor.toggleHoleState(row, col);
//            isDirty = true;
        }
    }

    /**
     * Saves the current state of the puzzle to a file in the "instructor's format".
     *
     * @param file The file to save the puzzle to.
     * @throws IOException If there is an error writing to the file.
     */
    public void saveGameToFile(File file) throws IOException {
        fileManager.saveGameToFile(file, this.gameField, this.currentBoardBorderColors);
        isDirty = false;
    }



    public void startEditor(int newRows, int newCols) {
        this.isEditorMode = true;
//        this.currentBoardBorderColors = new HashMap<>();
        this.gameField = puzzleEditor.startEditor(newRows, newCols);
        this.currentBoardBorderColors = this.gameField.getBorderColors();
        this.availablePieces = new ArrayList<>(this.allPuzzlePieces); // Reset available pieces to all puzzle pieces
        gui.initializeBoardView(newRows, newCols, this.currentBoardBorderColors);
        gui.displayAvailablePieces(Collections.emptyList()); // No pieces in editor mode

        isDirty = false;
    }





    private void loadAndInitializeAllPieces() {
        List<String> tilePatterns = TileLoader.loadTilePatterns();
        if (tilePatterns.isEmpty()) {
            // Fallback or critical error handling if tiles couldn't be loaded
            gui.showStatusMessage("error.load.game.tile");
            System.err.println("GameLogic: Using empty tile set due to loading failure.");
        }
        this.allPuzzlePieces.clear();
        for (String pattern : tilePatterns) {
            MosaicPiece piece = new MosaicPiece(pattern);
            this.allPuzzlePieces.add(piece);
        }
    }

    /**
     * Creates a list of color patterns from the currently available pieces
     * and tells the GUI to display them.
     */
    private void updateAvailablePiecesInGUI() {
        List<String> pieceRepresentationsForGUI = new ArrayList<>();
        if (this.availablePieces != null) {
            for (MosaicPiece piece : this.availablePieces) {
                Color[] pattern = piece.getColorPattern();

                StringBuilder patternString = new StringBuilder(EDGE_COUNT);

                for (Color color : pattern) {
                    patternString.append(color.getChar());
                }

                pieceRepresentationsForGUI.add(patternString.toString());
            }
        }
        gui.displayAvailablePieces(pieceRepresentationsForGUI);
    }

    /**
     * Handles the logic for a player attempting to place a piece on the board.
     *
     * @param pieceToPlace The MosaicPiece object being placed.
     * @param gameRow      The row on the board where the piece is being placed.
     * @param gameCol      The column on the board where the piece is being placed.
     */
    public void attemptPlacePiece(MosaicPiece pieceToPlace, int gameRow, int gameCol) {
        // Ensure the move is possible.
        if (gameField.isCellHole(gameRow, gameCol)) {
            gui.showStatusMessage("error.place.hole");
            return;
        }
        if (!gameField.isCellEmpty(gameRow, gameCol)) {
            gui.showStatusMessage("error.place.occupied", gameRow, gameCol);
            return;
        }

        // Whether the placement is valid according to color-matching rules.
        boolean isValidPlacement = Solver.checkPlacementValidity(pieceToPlace, gameRow, gameCol,
                gameField, currentBoardBorderColors);

        // Update the logical game state.
        gameField.setPieceAt(gameRow, gameCol, pieceToPlace);
        availablePieces.removeIf(p -> Arrays.equals(p.getColorPattern(), pieceToPlace.getColorPattern()));

        // Update the GUI.
        String pattern = getPatternStringFromPiece(pieceToPlace);
        gui.updateGameCell(gameRow, gameCol, pattern, pieceToPlace.getOrientation(), !isValidPlacement, false);
        updateAvailablePiecesInGUI();
        gui.clearSelectionFromPanel();

        // Invalidate the cached hint solution if the player's move deviates.
//        if (this.savedSolution != null) {
//            MosaicPiece solutionPiece = this.savedSolution.getPieceAt(gameRow, gameCol);
//            // A deviation occurs if there's no piece in the solution here, the patterns don't match,
//            // or the orientation is different.
//            if (solutionPiece == null ||
//                    !Arrays.equals(solutionPiece.getColorPattern(), pieceToPlace.getColorPattern()) ||
//                    solutionPiece.getOrientation() != pieceToPlace.getOrientation()) {
//
//                this.savedSolution = null; // Invalidate the cache.
//                gui.showStatusMessage("hint.path.invalidated");
//            }
//        }
        isDirty = true;

        // Check if the board is now full (no more empty playable cells)
        if (gameField.findMostConstrainedEmptyCell() == null) {
            if (isGameWon()) {
                gui.showAlert("game.won.title", "game.won.body");
            }
        }

    }

    private String getPatternStringFromPiece(MosaicPiece piece) {
        if (piece == null) {
            return null; // No piece to get pattern from
        }
        StringBuilder pattern = new StringBuilder(EDGE_COUNT);
        for (Color color : piece.getColorPattern()) {
            pattern.append(color.getChar());
        }

        if (pattern.length() != EDGE_COUNT) {
            System.err.println("GameLogic Error: Piece has invalid color pattern.");
            return "NNNN"; // Default empty pattern
        }
        return pattern.toString();
    }

    /**
     * Clears all pieces from the board, leaving holes intact.
     * Updates the GUI and marks the game state as modified.
     */
    public void clearBoard() {
        if (gameField == null) {
            gui.showStatusMessage("error.field.not.initialized");
            return;
        }

        for (int r = 0; r < gameField.getRows(); r++) {
            for (int c = 0; c < gameField.getColumns(); c++) {
                if (!gameField.isCellEmpty(r, c) && !gameField.isCellHole(r, c)) {
                    removePieceFromField(r, c);
                }
            }
        }
        gui.showStatusMessage("game.board.cleared");
        isDirty = true;
    }

    /**
     * Creates a piece from panel data and attempts to place it on the board.
     * This contains the logic for interpreting UI data, which previously was in the controller.
     *
     * @param pattern The color pattern of the piece.
     * @param rotationDegrees The rotation applied in the UI panel.
     * @param gameRow The target row for placement.
     * @param gameCol The target column for placement.
     */
    public void attemptPlacePieceFromPanel(String pattern, double rotationDegrees, int gameRow, int gameCol) {
        // Create the piece and apply the correct number of rotations
        MosaicPiece pieceToPlace = new MosaicPiece(pattern);
        int numRotations = (int) (Math.round(rotationDegrees) / 90.0);
        for (int i = 0; i < numRotations; i++) {
            pieceToPlace.rotate();
        }

        attemptPlacePiece(pieceToPlace, gameRow, gameCol);

    }
    /**
     * Removes a piece from the specified cell on the game field and returns it
     * to the list of available pieces. Updates the GUI accordingly.
     *
     * @param gameRow The 0-indexed row of the cell from which to remove the piece.
     * @param gameCol The 0-indexed column of the cell from which to remove the piece.
     */
    public void removePieceFromField(int gameRow, int gameCol) {
        if (gameField == null) {
            gui.showStatusMessage("error.field.not.initialized");
            return;
        }
        if (gameField.isCellHole(gameRow, gameCol)) {
            gui.showStatusMessage("error.remove.hole");
            return;
        }
        if (gameField.isCellEmpty(gameRow, gameCol)) {
            gui.showStatusMessage("error.remove.empty", gameRow, gameCol);
            return;
        }

        MosaicPiece removedPiece = gameField.getPieceAt(gameRow, gameCol);

        if (removedPiece != null) {
            // Remove the piece from the gameField model
            gameField.setPieceAt(gameRow, gameCol, null); // Set the cell to empty

            // Add the piece back to the availablePieces list.
            // add back the 'canonical' unrotated piece.
            // allPuzzlePieces should hold the 24 unique, original, unrotated pieces.
            MosaicPiece originalPieceToAddBack = null;
            for (MosaicPiece masterPiece : allPuzzlePieces) {
                if (Arrays.equals(masterPiece.getColorPattern(), removedPiece.getColorPattern())) {
                    originalPieceToAddBack = masterPiece; // This is the unrotated master piece
                    break;
                }
            }

            if (originalPieceToAddBack != null) {
                // Avoid adding duplicates if it somehow wasn't properly removed from availablePieces during placement
                boolean alreadyInAvailableList = false;
                for (MosaicPiece availableP : availablePieces) {
                    if (Arrays.equals(availableP.getColorPattern(), originalPieceToAddBack.getColorPattern())) {
                        alreadyInAvailableList = true;
                        break;
                    }
                }
                if (!alreadyInAvailableList) {
                    availablePieces.add(originalPieceToAddBack); // Add the original piece back
                }
            } else {
                // This should ideally not happen if allPuzzlePieces is correctly managed
                System.err.println("Game Logic CRITICAL ERROR: Could not find a master piece for pattern " +
                        Arrays.toString(removedPiece.getColorPattern()) + " to return to available list.");
                // Fallback: could create a new one, but it's better to ensure allPuzzlePieces is sound.
                // availablePieces.add(new MosaicPiece(removedPiece.getColorPattern()));
            }

            // Update the GUI to show the cell as empty
            // pieceId and pieceImagePath are null, rotation is irrelevant (0), no error.
            gui.updateGameCell(gameRow, gameCol, null, 0, false, false);
            updateAvailablePiecesInGUI();
            gui.clearSelectionFromPanel();
            this.savedSolution = null;
            isDirty = true; // Mark the game state as modified

            gui.showStatusMessage("piece.removed.success",
                    Arrays.toString(removedPiece.getColorPattern()), gameRow, gameCol);

        } else {
            // This case should not be reached if gameField.isCellEmpty() was false.
            System.err.println("Game Logic Error: Tried to remove piece from (" + gameRow + "," + gameCol +
                    ") but getPieceAt returned null despite not being empty.");
            gui.showStatusMessage("error.no.piece.remove", gameRow, gameCol);
        }
    }

//    public void switchToEditorMode() {
////        this.editorMode = true;
//        clearBoard();
//        gui.showStatusMessage("Switched to Editor Mode. You can now edit the game board.");
//    }

    public void switchToGameMode() {
        this.isEditorMode = false;

        this.availablePieces = getAvailablePieces();

        List<String> pieceRepresentationsForGUI = new ArrayList<>();
        for (MosaicPiece piece : this.availablePieces) {
            StringBuilder patternString = new StringBuilder();
            for (Color color : piece.getColorPattern()) {
                patternString.append(color.getChar());
            }
            pieceRepresentationsForGUI.add(patternString.toString());
        }
        gui.displayAvailablePieces(pieceRepresentationsForGUI);
        gui.showStatusMessage("game.mode.game");
    }

    public boolean isPuzzleReadyToPlay() {
        return puzzleEditor.isPuzzleReadyToPlay();
    }

    public void restartGame() {
        if (gameField == null) {
            gui.showStatusMessage("error.no.puzzle.to.restart");
            return;
        }

        if (!isEditorMode) {
            // Create a new empty field with the same size, borders, and holes
            this.gameField = new Field(
                    gameField.getRows(),
                    gameField.getColumns(),
                    new HashMap<>(currentBoardBorderColors),
                    new HashSet<>(gameField.getHoles())
            );

            this.availablePieces = new ArrayList<>(this.allPuzzlePieces);
            redrawFullBoardState();
            updateAvailablePiecesInGUI();
            gui.showStatusMessage("game.restart.success");
            isDirty = false; // Reset dirty flag after restart
        } else {
            clearBoard();
            gui.showStatusMessage("error.restart.in.editor");
        }
    }

    public boolean placePiece(MosaicPiece piece, int row, int column) {
        if (gameField.isCellEmpty(row, column) && !gameField.isCellHole(row, column)) {
            gameField.setPieceAt(row, column, piece);
            return true;
        }
        return false;
    }

    /**
     * Checks if the game is won by verifying that all non-hole cells are filled
     * and all placed pieces are valid according to the MacMahon Mosaic rules.
     *
     * @return {@code true} if the game is won, {@code false} otherwise.
     */
    public boolean isGameWon() {
        int rows = gameField.getRows();
        int cols = gameField.getColumns();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!gameField.isCellHole(r, c) && gameField.isCellEmpty(r, c)) {
                    return false; // Found an empty, non-hole cell
                }

                MosaicPiece p = gameField.getPieceAt(r,c);
                if (p != null && !Solver.checkPlacementValidity(p, r, c, gameField, currentBoardBorderColors)) {
                    return false; // Found an invalidly placed piece
                }
            }
        }

        return availablePieces.size() == (PuzzleEditor.MAX_PIECES
                - (gameField.getRows() * gameField.getColumns()
                - gameField.getNumberOfHoles()));
    }

    /**
     * Sets the color of a border segment during editor mode by delegating
     * the call to the PuzzleEditor.
     *
     * @param borderPosition The position of the border to change.
     * @param newColor       The new color for the border segment.
     */
    public void setEditorBorderColor(BorderPosition borderPosition, Color newColor) {
        if (isEditorMode) {
            // Delegate the call to the responsible class
            puzzleEditor.setEditorBorderColor(borderPosition, newColor);
            isDirty = true;
        }
    }



    public boolean isPuzzleSolvable() {
        // check if the pieces already on the board are valid
        if (!isBoardStateValid()) {
            gui.showStatusMessage("game.puzzle.state.unsolvable");
            return false;
        }

        int freeCells = getNumberOfFreeCells();
        if (freeCells > MIN_ALLOWED_FREE_CELL) {
            gui.showAlert("alert.solvability.skipped.title", "alert.solvability.skipped.body");
            return false;
        }
        if (!puzzleEditor.hasEnoughAvailableEdges(gameField, availablePieces)) {
            return false;
        }

        Solver solver = new Solver();

        // Start with a copy of the game field and all available pieces
        Field fieldCopy = this.gameField.deepCopy();
        List<MosaicPiece> piecesCopy = new ArrayList<>(this.availablePieces);

        this.savedSolution = solver.findSolution(fieldCopy, piecesCopy, this.currentBoardBorderColors);

        return savedSolution != null;
    }

    /**
     * Validates that all pieces currently placed on the board adhere to the
     * color-matching rules against their neighbors and borders.
     *
     * @return {@code true} if the current board state is valid, {@code false} otherwise.
     */
    private boolean isBoardStateValid() {
        int rows = gameField.getRows();
        int cols = gameField.getColumns();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                MosaicPiece piece = gameField.getPieceAt(r, c);
                if (piece != null) {
                    if (!Solver.checkPlacementValidity(piece, r, c, gameField, currentBoardBorderColors)) {
                        return false; // Found an invalidly placed piece
                    }
                }
            }
        }
        return true;
    }

    public void provideHint() {
        if (!isBoardStateValid()) {
            gui.showStatusMessage("game.puzzle.state.unsolvable");
            return;
        }
        int freeCells = getNumberOfFreeCells();
        if (freeCells > MIN_ALLOWED_FREE_CELL) {
            gui.showAlert("hint.unavailable.title", "hint.unavailable.body",freeCells);
            return;
        }
        if (this.gameField.findNextEmptyCell() == null) {
            gui.showStatusMessage("hint.no.empty.cells");
            return;
        }

        Field fieldCopy = this.gameField.deepCopy();
        List<MosaicPiece> availablePiecesCopy = new ArrayList<>(this.availablePieces);
        Solver solver = new Solver();

        this.savedSolution = solver.findSolution(fieldCopy, availablePiecesCopy, this.currentBoardBorderColors);

        if (savedSolution != null) {
            Position hintPosition = this.gameField.findNextEmptyCell();
            if (hintPosition != null) {
                MosaicPiece hintPiece = savedSolution.getPieceAt(hintPosition.row(), hintPosition.column());

                if (hintPiece != null) {
                    gui.showStatusMessage("hint.success",  Arrays.toString(hintPiece.getColorPattern()),
                            hintPosition.row(), hintPosition.column());
                    attemptPlacePiece(hintPiece, hintPosition.row(), hintPosition.column());
                    isDirty = true;
                } else {
                    gui.showStatusMessage("hint.fail");
                }
            }
        }
    }

    public int getNumberOfFreeCells() {
        if (this.gameField == null) return 0;

        int count = 0;
        int rows = gameField.getRows();
        int columns = gameField.getColumns();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (gameField.isCellEmpty(r, c) && !gameField.isCellHole(r, c)) {
                    count++;
                }
            }
        }
        return count;
    }

    public void editCurrentPuzzle() {
        this.isEditorMode = true;
        clearBoard();
        this.availablePieces = new ArrayList<>(this.allPuzzlePieces);
        gui.initializeBoardView(this.gameField.getRows(), this.gameField.getColumns(), this.currentBoardBorderColors);
        updateAvailablePiecesInGUI(); // Refresh the available pieces in the GUI
        isDirty = true;
    }

    public List<MosaicPiece> getAvailablePieces() {
        return availablePieces;
    }


    public Field getGameField() {
        return gameField;
    }

    public void clearSavedSolution() {
        this.savedSolution = null;
    }
}

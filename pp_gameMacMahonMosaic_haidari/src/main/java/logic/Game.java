package logic;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.*;

public class Game {
    private final GUIConnector gui;
    private Field gameField;
    private List<MosaicPiece> availablePieces;
    private final List<MosaicPiece> allPuzzlePieces; // Master list from TileLoader
    private static final int MAX_PIECES = 24; // Maximum number of pieces on the board
    public static final int MIN_ALLOWED_FREE_CELL = 18;
    private static final int EDGE_COUNT = Direction.values().length;
    private boolean isEditorMode = false; // Flag to track if the game is in editor mode
    private boolean isDirty = false; // Flag to track if the game state has been modified
    private Map<String, Color> currentBoardBorderColors;
    private Field savedSolution = null; // Cache for the puzzle solution, if available

    public Game(GUIConnector gui) {
        this.gui = gui;
        this.currentBoardBorderColors = new HashMap<>();
        this.allPuzzlePieces = new ArrayList<>();
        loadAndInitializeAllPieces();
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Reader reader = new FileReader(file)) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            if (jsonObject == null || !jsonObject.has("field")) {
                throw new JsonSyntaxException("The selected file is not a valid puzzle format.");
            }
            loadPuzzleFromPuzzleFile(gson.fromJson(jsonObject, PuzzleFile.class));
        }
        isDirty = false; // Reset dirty flag after loading
    }

    private void loadPuzzleFromPuzzleFile(PuzzleFile puzzleData) {
        if (puzzleData.field.isEmpty() || puzzleData.field.get(0).isEmpty()) {
            gui.showStatusMessage("Error: The puzzle file is empty");
            return;
        }

        int totalRows = puzzleData.field.size();
        int totalCols = puzzleData.field.get(0).size();
        int gameRows = totalRows -2; // Exclude top and bottom borders
        int gameCols = totalCols - 2; // Exclude left and right borders

        Map<String, Color> newBorderColors = new HashMap<>();
        Set<Position> newHoles = new HashSet<>();
        List<MosaicPiece> piecesOnBoard = new ArrayList<>();
        Field newField = new Field(gameRows, gameCols, newBorderColors, newHoles);

        for (int r = 0; r < totalRows; r++) {
            for (int c = 0; c < totalCols; c++) {
                String patternFromFile = puzzleData.field.get(r).get(c);
                boolean isBorder = (r == 0 || r == totalRows - 1 || c == 0 || c == totalCols - 1);

                if (isBorder) {
                    String key = deriveBorderKey(r, c, gameRows, gameCols);
                    if (key != null) {
                        Color color = deriveColorFromBorderPattern(patternFromFile, key);
                        newBorderColors.put(key, color);
                    }
                } else { // This is a game cell, not a border
                    int gameR = r - 1; // Adjust for border rows
                    int gameC = c - 1; // Adjust for border columns
                    if (!patternFromFile.equals("NNNN")) {
                        MosaicPiece orientedPiece = findCanonicalPieceForPattern(patternFromFile);

                        if (orientedPiece != null) {
                            newField.setPieceAt(gameR, gameC, orientedPiece);
                            piecesOnBoard.add(orientedPiece);
                        } else {
                            System.err.println("Could not find a matching canonical piece for pattern: " + patternFromFile);
                        }
                    } else {
                        // If the patternFromFile is "NNNN", it means this cell is empty
                        newField.setPieceAt(gameR, gameC, null);
                    }
                }
            }
        }
        this.gameField = newField;
        this.currentBoardBorderColors = newBorderColors;
        this.availablePieces = new ArrayList<>(this.allPuzzlePieces);
        // Set available pieces by removing those already on the board
        for (MosaicPiece pieceOnBoard : piecesOnBoard) {
            this.availablePieces.removeIf(p ->
                    Arrays.equals(p.getColorPattern(), pieceOnBoard.getColorPattern()));
        }


        gui.initializeBoardView(gameRows, gameCols, this.currentBoardBorderColors);
        redrawFullBoardState();
        updateAvailablePiecesInGUI();

        boolean hasInvalidPlacements = piecesOnBoard.stream()
                .anyMatch(p -> !Solver.checkPlacementValidity(p, p.getPlacementRow(), p.getPlacementCol(),
                        this.gameField, this.currentBoardBorderColors));

        if (hasInvalidPlacements) {
            this.isEditorMode = true;
            gui.showStatusMessage("Error: Loaded puzzle has invalid piece placements and cannot be played.");
        } else if (isGameWon()) {
            this.isEditorMode = false;
            gui.showGameEndMessage("Puzzle Solved!", "This puzzle was already solved upon loading.");
        } else if (!isPuzzleReadyToPlay()) {
            this.isEditorMode = true;
            gui.showStatusMessage("Warning: Loaded puzzle is valid, but may not be solvable.");
        } else {
            this.isEditorMode = false;
            gui.showStatusMessage("Loaded playable puzzle. Continue!");
        }
    }

    /**
     * Finds the canonical (unrotated) piece that matches the given pattern from the file,
     * and returns a new MosaicPiece object with the correct orientation set.
     *
     * @param patternFromFile The 4-character string pattern from the loaded JSON file.
     * @return A new MosaicPiece with the correct base pattern and rotation, or null if no match is found.
     */
    private MosaicPiece findCanonicalPieceForPattern(String patternFromFile) {

        Color[] targetPattern = new Color[EDGE_COUNT];
        try {
            for (int i = 0; i < EDGE_COUNT; i++) {
                targetPattern[i] = Color.fromChar(patternFromFile.charAt(i));
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid pattern in file: " + patternFromFile);
            return null;
        }

        // Iterate through each of the 24 master tiles
        for (MosaicPiece canonicalPiece : this.allPuzzlePieces) {
            MosaicPiece tempPiece = new MosaicPiece(canonicalPiece.getColorPattern());
            // Try all 4 possible rotations for each tile
            for (int orientation = 0; orientation < 360; orientation += 90) {

                tempPiece.setOrientation(orientation);

                Color[] effectivePattern = new Color[] {
                        tempPiece.getEdgeColor(Direction.NORTH),
                        tempPiece.getEdgeColor(Direction.EAST),
                        tempPiece.getEdgeColor(Direction.SOUTH),
                        tempPiece.getEdgeColor(Direction.WEST)
                };
                // Build the effective pattern string for the current rotation
//                char n = tempPiece.getEdgeColor(Direction.NORTH);
//                char e = tempPiece.getEdgeColor(Direction.EAST);
//                char s = tempPiece.getEdgeColor(Direction.SOUTH);
//                char w = tempPiece.getEdgeColor(Direction.WEST);
//                String effectivePattern = "" + n + e + s + w;

                // If the rotated pattern matches the pattern from the file, we found it!
                if (Arrays.equals(effectivePattern, targetPattern)) {
                    MosaicPiece resultPiece = new MosaicPiece(canonicalPiece.getColorPattern());
                    resultPiece.setOrientation(orientation);
                    return resultPiece; // Return the piece with the correct canonical pattern and orientation
                }
            }
        }
        return null; // No matching piece was found
    }

    private Color deriveColorFromBorderPattern(String pattern, String borderKey) {
        char colorChar = 'N';
        if (borderKey.startsWith("TOP")) colorChar = pattern.charAt(2); // South edge of top border
        else if (borderKey.startsWith("BOTTOM")) colorChar = pattern.charAt(0); // North edge of bottom border
        else if (borderKey.startsWith("LEFT")) colorChar = pattern.charAt(1); // East edge of left border
        else if (borderKey.startsWith("RIGHT")) colorChar = pattern.charAt(3); // West edge of right border
        return Color.fromChar(colorChar);
    }

    private String deriveBorderKey(int r, int c, int gameRows, int gameCols) {
        if (r == 0 && c > 0 && c <= gameCols) return "TOP_" + (c - 1); // Top border
        if (r == gameRows + 1 && c > 0 && c <= gameCols) return "BOTTOM_" + (c - 1); // Bottom border
        if (c == 0 && r > 0 && r <= gameRows) return "LEFT_" + (r - 1); // Left border
        if (c == gameCols + 1 && r > 0 && r <= gameRows) return "RIGHT_" + (r - 1); // Right border

        return null; // It's a border
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

    public void setEditorBorderColor(String borderKey, Color newColor) {

//        @TODO I am using ENUM for boder colors
        //        String[] parts = borderKey.split("_");
//        String side = parts[0];
//        int index = Integer.parseInt(parts[1]);
//        Position position = switch (side) {
//            case "TOP" -> new Position(0, index);
//            case "BOTTOM" -> new Position(gameField.getRows() - 1, index);
//            case "LEFT" -> new Position(index, 0);
//            case "RIGHT" -> new Position(index, gameField.getColumns() - 1);
//            default -> throw new IllegalArgumentException("Invalid border key: " + borderKey);
//        };
        currentBoardBorderColors.put(borderKey, newColor);
        gui.updateBorderColor(borderKey, newColor);
        isDirty = true; // Mark the game state as modified
    }

    public void toggleHoleState(int gameLogicRow, int gameLogicCol) {
        if (gameField == null) {
            gui.showStatusMessage("Error: Game field not initialized.");
            return;
        }

        if (gameField.isCellHole(gameLogicRow, gameLogicCol)) {
            gameField.removeHole(gameLogicRow, gameLogicCol);
            gui.updateGameCell(gameLogicRow, gameLogicCol, null, 0,
                    false, false); // Clear the cell
            gui.showStatusMessage("Removed hole at (" + gameLogicRow + "," + gameLogicCol + ").");
            return;
        }

        int totalCells = gameField.getRows() * gameField.getColumns();
        if (totalCells <= MAX_PIECES) {
            gui.showStatusMessage("Holes are only allowed on boards larger than " + MAX_PIECES + " cells.");
            return;
        }

        int requiredHoles = totalCells - MAX_PIECES;
        int currentHoles = gameField.getNumberOfHoles();

        if (currentHoles >= requiredHoles) {
            gui.showStatusMessage("You have already placed the required " + requiredHoles + " holes.");
            return;
        }

        gameField.setHole(gameLogicRow, gameLogicCol);
        gui.updateGameCell(gameLogicRow, gameLogicCol, null, 0,
                true, true); // Mark the cell as a hole
        gui.showStatusMessage("Added hole at (" + gameLogicRow + "," + gameLogicCol + ")." +
                (currentHoles + 1) + " of " + requiredHoles + " holes placed.");
        isDirty = true; // Mark the game state as modified
    }

    /**
     * Saves the current state of the puzzle to a file in the "instructor's format".
     *
     * @param file The file to save the puzzle to.
     * @throws IOException If there is an error writing to the file.
     */
    public void saveGameToFile(File file) throws IOException {
        // Create the main object that will be serialized to JSON
        PuzzleFile saveData = new PuzzleFile();
        saveData.field = new ArrayList<>();

        int totalRows = gameField.getRows() + 2;
        int totalCols = gameField.getColumns() + 2;

        // Iterate over the entire grid, including borders, to build the 2D list
        for (int r = 0; r < totalRows; r++) {
            List<String> rowList = new ArrayList<>();
            for (int c = 0; c < totalCols; c++) {
                boolean isBorder = r == 0 || c == 0 || r == totalRows - 1 || c == totalCols - 1;
                if (isBorder) {
                    rowList.add(getPatternForBorderCell(r, c));
                } else {
                    rowList.add(getPatternForGameCell(r - 1, c - 1));
                }
            }
            saveData.field.add(rowList);
        }

        // Use Gson to write the object to the file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(saveData, writer);
        }
        isDirty = false; // Reset dirty flag after saving
    }

    /**
     * Helper method to get the 4-character pattern for a cell within the game area.
     */
    private String getPatternForGameCell(int gameRow, int gameCol) {
        if (gameField.isCellHole(gameRow, gameCol)) {
            return "NNNN";
        }
        MosaicPiece piece = gameField.getPieceAt(gameRow, gameCol);
        if (piece == null) {
            return "NNNN"; // Empty cell
        }
        // For a placed piece, calculate its effective pattern based on rotation
        char n = piece.getEdgeColor(Direction.NORTH).getChar();
        char e = piece.getEdgeColor(Direction.EAST).getChar();
        char s = piece.getEdgeColor(Direction.SOUTH).getChar();
        char w = piece.getEdgeColor(Direction.WEST).getChar();
        return String.valueOf(n) + e + s + w;
    }

    /**
     * Helper method to get the 4-character pattern for a border or corner cell.
     */
    private String getPatternForBorderCell(int guiRow, int guiCol) {
        int gameRows = gameField.getRows();
        int gameCols = gameField.getColumns();
        char n = 'N', e = 'N', s = 'N', w = 'N';

        Color borderColor = null;

        // Top border
        if (guiRow == 0 && guiCol > 0 && guiCol <= gameCols) {
            borderColor = currentBoardBorderColors.get("TOP_" + (guiCol - 1));
            if (borderColor != null) {
                n = borderColor.getChar(); // The south-facing edge of the top border
            }
        }
        // Bottom border
        else if (guiRow == gameRows + 1 && guiCol > 0 && guiCol <= gameCols) {
            borderColor = currentBoardBorderColors.get("BOTTOM_" + (guiCol - 1));
            if (borderColor != null) {
                s = borderColor.getChar(); // The north-facing edge of the bottom border
            }
        }
        // Left border
        else if (guiCol == 0 && guiRow > 0 && guiRow <= gameRows) {
            borderColor = currentBoardBorderColors.get("LEFT_" + (guiRow - 1));
            if (borderColor != null) {
                e = borderColor.getChar(); // The east-facing edge of the left border
            }
        }
        // Right border
        else if (guiCol == gameCols + 1 && guiRow > 0 && guiRow <= gameRows) {
            borderColor = currentBoardBorderColors.get("RIGHT_" + (guiRow - 1));
            if (borderColor != null) {
                w = borderColor.getChar(); // The west-facing edge of the right border
            }
        }

        // Corners and any other cells will default to "NNNN"
        return "" + n + e + s + w;
    }

    /**
     * Helper method to convert a color string ("RED") to a character ('R').
     */
    private char getCharFromColorString(String color) {
        if (color == null) return 'N';
        return switch (color.toUpperCase()) {
            case "RED" -> 'R';
            case "GREEN" -> 'G';
            case "YELLOW" -> 'Y';
            default -> 'N';
        };
    }
    public void startEditor(int newRows, int newCols) {
        this.isEditorMode = true;
        this.currentBoardBorderColors = new HashMap<>();
        this.gameField = new Field(newRows, newCols, this.currentBoardBorderColors, new HashSet<>());
        this.availablePieces = new ArrayList<>(this.allPuzzlePieces); // Reset available pieces to all puzzle pieces

        gui.initializeBoardView(newRows, newCols, this.currentBoardBorderColors);
        gui.displayAvailablePieces(Collections.emptyList()); // No pieces in editor mode
    }

    // Helper record for predefined puzzle configurations for testing
//    private static class PredefinedPuzzle {
//        final String[] topBorderSegments;    // Expected size 3 (for 3 columns)
//        final String[] bottomBorderSegments; // Expected size 3
//        final String[] leftBorderSegments;   // Expected size 4 (for 4 rows)
//        final String[] rightBorderSegments;  // Expected size 4
//
//        PredefinedPuzzle(String[] top, String[] bottom, String[] left, String[] right) {
//            this.topBorderSegments = top;
//            this.bottomBorderSegments = bottom;
//            this.leftBorderSegments = left;
//            this.rightBorderSegments = right;
//        }
//
//        /**
//         * Parses the "NNXN"-style border segment strings and converts them
//         * into the Map format required by GUIConnector.initializeBoardView.
//         */
//        Map<String, String> getBorderColors(int numCols, int numRows) {
//            Map<String, String> borderMap = new HashMap<>();
//            // Top borders
//            for (int c = 0; c < numCols && c < topBorderSegments.length; c++) {
//                borderMap.put("TOP_" + c, getColorStringFromChar(topBorderSegments[c].charAt(2)));
//            }
//            // Bottom borders
//            for (int c = 0; c < numCols && c < bottomBorderSegments.length; c++) {
//                borderMap.put("BOTTOM_" + c, getColorStringFromChar(bottomBorderSegments[c].charAt(0)));
//            }
//            // Left borders
//            for (int r = 0; r < numRows && r < leftBorderSegments.length; r++) {
//                borderMap.put("LEFT_" + r, getColorStringFromChar(leftBorderSegments[r].charAt(1)));
//            }
//            // Right borders
//            for (int r = 0; r < numRows && r < rightBorderSegments.length; r++) {
//                borderMap.put("RIGHT_" + r, getColorStringFromChar(rightBorderSegments[r].charAt(3)));
//            }
//            return borderMap;
//        }
//
//
//        private String getColorStringFromChar(char colorChar) {
//            return switch (colorChar) {
//                case 'R' -> "RED";
//                case 'G' -> "GREEN";
//                case 'Y' -> "YELLOW";
//                case 'N' -> "NONE"; // If a border segment facing is explicitly uncolored
//                default -> {
//                    System.err.println("Game.PredefinedPuzzle: Encountered unexpected character '" + colorChar +
//                            "' in border pattern. Defaulting to NONE.");
//                    yield "NONE"; // Default for any unexpected char
//                }
//            };
//        }
//    }

//    private List<PredefinedPuzzle> predefinedPuzzles;

    // Constructor for testing purposes
    public Game(GUIConnector gui, Field gameField, List<MosaicPiece> availablePieces, List<MosaicPiece> allPuzzlePieces,
                Map<String, Color> currentBoardBorderColors) {
        this.gui = gui;
        this.gameField = gameField;
        this.availablePieces = availablePieces;
        this.allPuzzlePieces = allPuzzlePieces;
        this.currentBoardBorderColors = currentBoardBorderColors;
//        this.editorMode = false;
    }



    private void loadAndInitializeAllPieces() {
        List<String> tilePatterns = TileLoader.loadTilePatterns();
        if (tilePatterns.isEmpty()) {
            // Fallback or critical error handling if tiles couldn't be loaded
            gui.showStatusMessage("CRITICAL ERROR: Could not load game tile definitions!");
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
        // Pre-condition checks: Ensure the move is possible.
        if (gameField.isCellHole(gameRow, gameCol)) {
            gui.showStatusMessage("Cannot place a piece in a hole.");
            return;
        }
        if (!gameField.isCellEmpty(gameRow, gameCol)) {
            gui.showStatusMessage("This cell is already occupied.");
            return;
        }

        // Check if the placement is valid according to color-matching rules.
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
        if (this.savedSolution != null) {
            MosaicPiece solutionPiece = this.savedSolution.getPieceAt(gameRow, gameCol);
            // A deviation occurs if there's no piece in the solution here, the patterns don't match,
            // or the orientation is different.
            if (solutionPiece == null ||
                    !Arrays.equals(solutionPiece.getColorPattern(), pieceToPlace.getColorPattern()) ||
                    solutionPiece.getOrientation() != pieceToPlace.getOrientation()) {

                this.savedSolution = null; // Invalidate the cache.
                gui.showStatusMessage("Hint path invalidated. A new solution will be calculated if a hint is requested.");
            }
        }
        // Mark the game state as changed.
        isDirty = true;

        // Check if the board is now full (no more empty playable cells)
        if (gameField.findMostConstrainedEmptyCell() == null) {
            if (isGameWon()) {
                gui.showAlert("Congratulations!", "You have successfully solved the puzzle!");
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
     * Clears the game board by removing all pieces from the field
     * and returning them to the list of available pieces.
     */
    public void clearBoard() {
        for (int r = 0; r < gameField.getRows(); r++) {
            for (int c = 0; c < gameField.getColumns(); c++) {
                if (!gameField.isCellEmpty(r, c) && !gameField.isCellHole(r, c)) {
                    removePieceFromField(r, c);
                }
            }
        }
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
//        System.out.println("Game: removePieceFromField called for (" + gameRow + "," + gameCol + ")");

        if (gameField == null) {
            gui.showStatusMessage("Error: Game field not initialized.");
            return;
        }
        if (gameField.isCellHole(gameRow, gameCol)) {
            gui.showStatusMessage("Cannot remove from a hole location.");
            return;
        }
        if (gameField.isCellEmpty(gameRow, gameCol)) {
            gui.showStatusMessage("Cell (" + gameRow + "," + gameCol + ") is already empty.");
            return;
        }

        MosaicPiece removedPiece = gameField.getPieceAt(gameRow, gameCol);

        if (removedPiece != null) {
            // Remove the piece from the gameField model
            gameField.setPieceAt(gameRow, gameCol, null); // Set the cell to empty

            // This implementation does not work as the piece does not appear back on the list of available pieces
//            allPuzzlePieces.stream()
//                    .filter(masterPiece -> Arrays.equals(masterPiece.getColorPattern(), removedPiece.getColorPattern()))
//                    .findFirst()
//                    .ifPresent(allPuzzlePieces::add);

            // Add the piece back to the availablePieces list.
            // It's important to add back the 'canonical' unrotated piece.
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

            gui.showStatusMessage("Piece " + Arrays.toString(removedPiece.getColorPattern()) +
                    " removed from (" + gameRow + "," + gameCol + ").");

        } else {
            // This case should not be reached if gameField.isCellEmpty() was false.
            System.err.println("Game Logic Error: Tried to remove piece from (" + gameRow + "," + gameCol +
                    ") but getPieceAt returned null despite not being empty.");
            gui.showStatusMessage("Error: No piece found at (" + gameRow + "," + gameCol + ") to remove.");
        }
    }

    public void switchToEditorMode() {
//        this.editorMode = true;
        clearBoard();
        gui.showStatusMessage("Switched to Editor Mode. You can now edit the game board.");
    }

    public void switchToGameMode() {
        this.isEditorMode = false;

        this.availablePieces = new ArrayList<>(this.allPuzzlePieces); // Reset available pieces to all


        List<String> pieceRepresentationsForGUI = new ArrayList<>();
        for (MosaicPiece piece : this.availablePieces) {
            StringBuilder patternString = new StringBuilder();
            for (Color color : piece.getColorPattern()) {
                patternString.append(color.getChar());
            }
            pieceRepresentationsForGUI.add(patternString.toString());
        }
        gui.displayAvailablePieces(pieceRepresentationsForGUI);
        gui.showStatusMessage("Switched to Game Mode. You can now play the game.");
    }

    public boolean isPuzzleReadyToPlay() {
        if (gameField == null) {
            gui.showStatusMessage("Error: Game field is not initialized.");
            return false;
        }

        if (!hasEnoughAvailableEdges()) {
            return false;
        }

        for (int c = 0; c < gameField.getColumns(); c++) {
            // Check TOP border
            if (isBorderSegmentValid("TOP_" + c)) return false;
            // Check BOTTOM border
            if (isBorderSegmentValid("BOTTOM_" + c)) return false;
        }

        for (int r = 0; r < gameField.getRows(); r++) {
            // Check LEFT border
            if (isBorderSegmentValid("LEFT_" + r)) return false;
            // Check RIGHT border
            if (isBorderSegmentValid("RIGHT_" + r)) return false;
        }

        int totalCells = gameField.getRows() * gameField.getColumns();
        if (totalCells > MAX_PIECES) {
            int requiredHoles = totalCells - MAX_PIECES;
            if (gameField.getNumberOfHoles() < requiredHoles) {
                gui.showStatusMessage("Error: For a " + gameField.getRows() + "x" + gameField.getColumns() +
                        " board, you need at least " + requiredHoles + " holes.");
                return false;
            }
        }

        int freeCells = getNumberOfFreeCells();
//        if (freeCells > MIN_ALLOWED_FREE_CELL) {
//            // As per Nils' new rule, the solvability check is skipped when more than 18 cells are empty
//            // and the game is allowed to start.
//            gui.showStatusMessage("Warning: More than 18 empty cells. Solvability not verified.");
//            gui.showAlert("Solvability Check Skipped",
//                    "The puzzle has more than 18 empty cells, so the automated solvability check was skipped. " +
//                            "Please be aware that this puzzle might be unsolvable.");
//
//            this.savedSolution = null; // Ensure no previous solution is cached
//            return true;
//        }

//        this.savedSolution = solvePuzzle(gameField.deepCopy(), new ArrayList<>(availablePieces));
//        if (this.savedSolution == null) {
//            gui.showStatusMessage("Error: This puzzle is not solvable (checked with " + freeCells + " cells).");
//            return false;
//        }



        if (!isPuzzleSolvable()) {
            gui.showStatusMessage("Error: The current puzzle configuration is not solvable.");
            return false;
        }

        gui.showStatusMessage("Puzzle is solvable! Starting game.");

        return true; // All checks passed, puzzle is ready to play
    }

    /**
     * Checks if the border segment is valid according to the rules:
     * - If it has a color, it's valid.
     * - If it doesn't have a color, it must be adjacent to a hole.
     * @param borderKey The key for the border segment (e.g., "TOP_0", "LEFT_1").
     * @return true if the segment is valid, false if it needs a color or is invalid.
     */
    private boolean isBorderSegmentValid(String borderKey) {
        String color = currentBoardBorderColors.get(borderKey) != null
                ? currentBoardBorderColors.get(borderKey).getChar() + ""
                : null;
        boolean isColorMissing = color == null || color.equals("NONE");

        Position adjacentCell = getAdjacentCellForBorderKey(borderKey);
        if (adjacentCell != null && gameField.isCellHole(adjacentCell.row(), adjacentCell.column())) {
            return false;
        }

        if (!isColorMissing) {
            return false; // Color is present, segment is valid.
        }

        // Color is missing and it's not next to a hole. Invalid.
        gui.showStatusMessage("Error: Border segment " + borderKey + " needs a color.");
        return true;
    }

    /**
     * Helper method to get the coordinates of the game cell adjacent to a given border segment.
     *
     * @param borderKey The key for the border segment (e.g., "TOP_0").
     * @return The Position of the adjacent cell.
     */
    private Position getAdjacentCellForBorderKey(String borderKey) {
        if (borderKey == null || !borderKey.contains("_")) return null;

        String[] parts = borderKey.split("_");
        String side = parts[0];
        int index = Integer.parseInt(parts[1]);

        return switch (side) {
            case "TOP" -> new Position(0, index);
            case "BOTTOM" -> new Position(gameField.getRows() - 1, index);
            case "LEFT" -> new Position(index, 0);
            case "RIGHT" -> new Position(index, gameField.getColumns() - 1);
            default -> null;
        };
    }


//    public MosaicPiece getSelectedPieceForPlacement() {
//        return this.selectedPiece;
//    }

    public void restartGame() {
        if (gameField == null) {
            gui.showStatusMessage("Error: No puzzle loaded to restart.");
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
            gui.showStatusMessage("Puzzle restarted. All pieces and holes have been cleared.");
            isDirty = false; // Reset dirty flag after restart
        } else {
            gui.showStatusMessage("Cannot restart game in Editor Mode. Please switch to Game Mode first.");
        }
    }

    public boolean saveGame(String filePath) {
        // Placeholder for save game logic
        return true;
    }

    public boolean loadGame(String filePath) {
        // Placeholder for load game logic
        return true;
    }


    public boolean placePiece(MosaicPiece piece, int row, int column) {
        if (gameField.isCellEmpty(row, column) && !gameField.isCellHole(row, column)) {
            gameField.setPieceAt(row, column, piece);
            return true;
        }
        return false;
    }

    public MosaicPiece removePiece(int row, int column) {
        if (!gameField.isCellEmpty(row, column)) {
            MosaicPiece piece = gameField.getPieceAt(row, column);
            gameField.setPieceAt(row, column, null);
            return piece;
        }
        return null;
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

        return availablePieces.size() == (MAX_PIECES
                - (gameField.getRows() * gameField.getColumns()
                - gameField.getNumberOfHoles()));
    }






//    /**
//     * Converts a color string (e.g., "RED", "GREEN", "NONE") to its corresponding
//     * character representation (e.g., 'R', 'G', 'N').
//     * This is used for comparing piece edge colors (chars) with border color definitions (Strings).
//     *
//     * @param colorName The string representation of the color.
//     * @return The character representation of the color. 'N' for "NONE", 'X' for unknown/error.
//     */
//    private char getColorCharFromColorString(String colorName) {
//        if (colorName == null) {
//            return 'X'; // Represents an undefined or error state for a color string
//        }
//        return switch (colorName.toUpperCase()) {
//            case "RED" -> 'R';
//            case "GREEN" -> 'G';
//            case "YELLOW" -> 'Y';
//            case "NONE" -> 'N'; // 'N' signifies that any color can match this border (or it's transparent)
//            default -> {
//                // This case should ideally not be reached if border color data is clean
//                System.err.println("GameLogic Error: Unknown color string encountered: '" + colorName + "'");
//                yield 'X'; // Fallback for an unexpected color string
//            }
//        };
//    }
//
//    private String getColorStringFromChar(char c) {
//        return switch (c) {
//            case 'R' -> "RED";
//            case 'G' -> "GREEN";
//            case 'Y' -> "YELLOW";
//            case 'N' -> "NONE"; // No color
//            default -> {
//                System.err.println("Game.PredefinedPuzzle: Unexpected character '" + c + "' in border pattern. Defaulting to NONE.");
//                yield "NONE"; // Default for any unexpected char
//            }
//        };
//    }

    /**
     * Performs a fast pre-check to see if there are enough colored edges on the
     * available pieces to satisfy the requirements of the current board state.
     * This is a powerful pruning technique.
     *
     * @return True if there are enough edges for a potential solution, false otherwise.
     */
    private boolean hasEnoughAvailableEdges() {
        // 1. Count the total number of available edges from the remaining pieces.
        Map<Color, Integer> availableEdges = new EnumMap<>(Color.class);
        for (Color c : Color.values()) {
            if (c != Color.NONE) availableEdges.put(c, 0);
        }
        for (MosaicPiece piece : this.availablePieces) {
            for (Color color : piece.getColorPattern()) {
                if (availableEdges.containsKey(color)) {
                    availableEdges.put(color, availableEdges.get(color) + 1);
                }
            }
        }

        // 2. Count the number of required edges from empty spots on the board.
        Map<Color, Integer> requiredEdges = new EnumMap<>(Color.class);
        for (Color c : Color.values()) {
            if (c != Color.NONE) requiredEdges.put(c, 0);
        }

        for (int r = 0; r < gameField.getRows(); r++) {
            for (int c = 0; c < gameField.getColumns(); c++) {
                if (gameField.isCellEmpty(r, c) && !gameField.isCellHole(r, c)) {
                    for (Direction dir : Direction.values()) {
                        Color requiredColor = Solver.getRequiredEdgeColorFor(r, c, dir, this.gameField, this.currentBoardBorderColors);
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
                gui.showStatusMessage("Error: Not enough " + color.name() + " edges available to solve this puzzle.");
                return false;
            }
        }

        return true;
    }





    public boolean isPuzzleSolvable() {

//        int freeCells = getNumberOfFreeCells();
//        if (freeCells > 18) {
//            gui.showAlert("Check Skipped", "The solvability check is only performed " +
//                    "for puzzles with 18 or fewer empty cells. Currently, there are " + freeCells + ".");
//            return false;
//        }
        if (!hasEnoughAvailableEdges()) {
            return false;
        }

        Solver solver = new Solver();

        // Start with a copy of the game field and all available pieces
        Field fieldCopy = this.gameField.deepCopy();
        List<MosaicPiece> piecesCopy = new ArrayList<>(this.availablePieces);

        this.savedSolution = solver.findSolution(fieldCopy, piecesCopy, this.currentBoardBorderColors);

        return savedSolution != null;
    }

    public void provideHint() {
        int freeCells = getNumberOfFreeCells();
//        if (freeCells > MIN_ALLOWED_FREE_CELL) {
//            gui.showAlert("Hint Unavailable",
//                    "Hints are only available for puzzles with 18 or fewer empty cells. " +
//                    "Currently, there are " + freeCells + ".");
//            return; // Exit the method without providing a hint.
//        }
        if (this.gameField.findNextEmptyCell() == null) {
            gui.showStatusMessage("No empty cells available for hints.");
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
                    gui.showStatusMessage("Hint: Place piece '" + Arrays.toString(hintPiece.getColorPattern()) +
                            "' at (" + hintPosition.row() + "," + hintPosition.column() + ").");
                    attemptPlacePiece(hintPiece, hintPosition.row(), hintPosition.column());
//                    gui.highlightCell(hintPosition.row(), hintPosition.column(), hintPiece.getColorPattern());
                } else {
                    gui.showStatusMessage("No valid hint available for the next empty cell.");
                }
            }
        }
    }

    private int getNumberOfFreeCells() {
        if (this.gameField == null) return 0;

//        int totalPlayableCells = (gameField.getRows() * gameField.getColumns()) - gameField.getNumberOfHoles();
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
        updateAvailablePiecesInGUI(); // Refresh the available pieces in the GUI
    }

    public List<MosaicPiece> getAvailablePieces() {
        return availablePieces;
    }


    public Field getGameField() {
        return gameField;
    }
}

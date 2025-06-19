package logic;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Game {
    private final GUIConnector gui;
    private Field gameField;
    private List<MosaicPiece> availablePieces;
    private final List<MosaicPiece> allPuzzlePieces; // Master list from TileLoader
    private boolean isEditorMode = false; // Flag to track if the game is in editor mode
    private boolean isDirty = false; // Flag to track if the game state has been modified
    private Map<String, String> currentBoardBorderColors;

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

        Map<String, String> newBorderColors = new HashMap<>();
        Set<Position> newHoles = new HashSet<>();
        List<MosaicPiece> piecesOnBoard = new ArrayList<>();
        Field newField = new Field(gameRows, gameCols, new HashMap<>(), new HashSet<>());

        for (int r = 0; r < totalRows; r++) {
            for (int c = 0; c < totalCols; c++) {
                String patternFromFile = puzzleData.field.get(r).get(c);
                boolean isBorder = (r == 0 || r == totalRows - 1 || c == 0 || c == totalCols - 1);

                if (isBorder) {
                    String key = deriveBorderKey(r, c, gameRows, gameCols);
                    if (key != null) {
                        String color = deriveColorFromBorderPattern(patternFromFile, key);
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
            this.availablePieces.removeIf(p -> p.getColorPattern().equals(pieceOnBoard.getColorPattern()));
        }


        gui.initializeBoardView(gameRows, gameCols, this.currentBoardBorderColors);
        redrawFullBoardState();
        updateAvailablePiecesInGUI();

        boolean hasInvalidPlacements = piecesOnBoard.stream()
                .anyMatch(p -> !checkPlacementValidity(p, p.getPlacementRow(), p.getPlacementCol()));

        if (hasInvalidPlacements) {
            this.isEditorMode = true;
            gui.setEditorMode(true);
            gui.showStatusMessage("Error: Loaded puzzle has invalid piece placements and cannot be played.");
        } else if (isGameWon()) {
            this.isEditorMode = false;
            gui.setEditorMode(false);
            gui.showGameEndMessage("Puzzle Solved!", "This puzzle was already solved upon loading.");
        } else if (!isPuzzleSolvable()) {
            this.isEditorMode = true;
            gui.setEditorMode(true);
            gui.showStatusMessage("Warning: Loaded puzzle is valid, but may not be solvable.");
        } else {
            this.isEditorMode = false;
            gui.setEditorMode(false);
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
        // Iterate through each of the 24 master tiles
        for (MosaicPiece canonicalPiece : this.allPuzzlePieces) {
            // Try all 4 possible rotations for each tile
            for (int orientation = 0; orientation < 360; orientation += 90) {
                MosaicPiece tempPiece = new MosaicPiece(canonicalPiece.getColorPattern(), orientation);

                // Build the effective pattern string for the current rotation
                char n = tempPiece.getEdgeColor(Direction.NORTH);
                char e = tempPiece.getEdgeColor(Direction.EAST);
                char s = tempPiece.getEdgeColor(Direction.SOUTH);
                char w = tempPiece.getEdgeColor(Direction.WEST);
                String effectivePattern = "" + n + e + s + w;

                // If the rotated pattern matches the pattern from the file, we found it!
                if (effectivePattern.equals(patternFromFile)) {
                    return tempPiece; // Return the piece with the correct canonical pattern and orientation
                }
            }
        }
        return null; // No matching piece was found
    }

    private String deriveColorFromBorderPattern(String pattern, String borderKey) {
        char colorChar = 'N';
        if (borderKey.startsWith("TOP")) colorChar = pattern.charAt(2); // South edge of top border
        else if (borderKey.startsWith("BOTTOM")) colorChar = pattern.charAt(0); // North edge of bottom border
        else if (borderKey.startsWith("LEFT")) colorChar = pattern.charAt(1); // East edge of left border
        else if (borderKey.startsWith("RIGHT")) colorChar = pattern.charAt(3); // West edge of right border
        return getColorStringFromChar(colorChar);
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
                boolean isHole = gameField.isCellHole(r, c);
                if (piece != null) {
                    boolean isError = !checkPlacementValidity(piece, r, c);
                    gui.updateGameCell(r, c, piece.getColorPattern(), piece.getColorPattern(),
                            piece.getOrientation(), isError, false);
                } else {
                    gui.updateGameCell(r, c, null, null, 0, false, isHole);
                }
            }
        }
    }

    public void setEditorBorderColor(String borderKey, String newColor) {
        String[] parts = borderKey.split("_");
        String side = parts[0];
        int index = Integer.parseInt(parts[1]);
        Position position = switch (side) {
            case "TOP" -> new Position(0, index);
            case "BOTTOM" -> new Position(gameField.getRows() - 1, index);
            case "LEFT" -> new Position(index, 0);
            case "RIGHT" -> new Position(index, gameField.getColumns() - 1);
            default -> throw new IllegalArgumentException("Invalid border key: " + borderKey);
        };
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
            gui.updateGameCell(gameLogicRow, gameLogicCol, null, null, 0,
                    false, false); // Clear the cell
            gui.showStatusMessage("Removed hole at (" + gameLogicRow + "," + gameLogicCol + ").");
            return;
        }

        int totalCells = gameField.getRows() * gameField.getColumns();
        if (totalCells <= 24) {
            gui.showStatusMessage("Holes are only allowed on boards larger than 24 cells.");
            return;
        }

        int requiredHoles = totalCells - 24;
        int currentHoles = gameField.getNumberOfHoles();

        if (currentHoles >= requiredHoles) {
            gui.showStatusMessage("You have already placed the required " + requiredHoles + " holes.");
            return;
        }

        gameField.setHole(gameLogicRow, gameLogicCol);
        gui.updateGameCell(gameLogicRow, gameLogicCol, null, null, 0,
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
        char n = piece.getEdgeColor(Direction.NORTH);
        char e = piece.getEdgeColor(Direction.EAST);
        char s = piece.getEdgeColor(Direction.SOUTH);
        char w = piece.getEdgeColor(Direction.WEST);
        return "" + n + e + s + w;
    }

    /**
     * Helper method to get the 4-character pattern for a border or corner cell.
     */
    private String getPatternForBorderCell(int guiRow, int guiCol) {
        int gameRows = gameField.getRows();
        int gameCols = gameField.getColumns();
        char n = 'N', e = 'N', s = 'N', w = 'N';

        // Top border
        if (guiRow == 0 && guiCol > 0 && guiCol <= gameCols) {
            String color = currentBoardBorderColors.get("TOP_" + (guiCol - 1));
            s = getCharFromColorString(color); // The south-facing edge of the top border
        }
        // Bottom border
        else if (guiRow == gameRows + 1 && guiCol > 0 && guiCol <= gameCols) {
            String color = currentBoardBorderColors.get("BOTTOM_" + (guiCol - 1));
            n = getCharFromColorString(color); // The north-facing edge of the bottom border
        }
        // Left border
        else if (guiCol == 0 && guiRow > 0 && guiRow <= gameRows) {
            String color = currentBoardBorderColors.get("LEFT_" + (guiRow - 1));
            e = getCharFromColorString(color); // The east-facing edge of the left border
        }
        // Right border
        else if (guiCol == gameCols + 1 && guiRow > 0 && guiRow <= gameRows) {
            String color = currentBoardBorderColors.get("RIGHT_" + (guiRow - 1));
            w = getCharFromColorString(color); // The west-facing edge of the right border
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
        gui.setEditorMode(true);
    }

    // Helper record for predefined puzzle configurations for testing
    private static class PredefinedPuzzle {
        final String[] topBorderSegments;    // Expected size 3 (for 3 columns)
        final String[] bottomBorderSegments; // Expected size 3
        final String[] leftBorderSegments;   // Expected size 4 (for 4 rows)
        final String[] rightBorderSegments;  // Expected size 4

        PredefinedPuzzle(String[] top, String[] bottom, String[] left, String[] right) {
            this.topBorderSegments = top;
            this.bottomBorderSegments = bottom;
            this.leftBorderSegments = left;
            this.rightBorderSegments = right;
        }

        /**
         * Parses the "NNXN"-style border segment strings and converts them
         * into the Map format required by GUIConnector.initializeBoardView.
         */
        Map<String, String> getBorderColors(int numCols, int numRows) {
            Map<String, String> borderMap = new HashMap<>();
            // Top borders
            for (int c = 0; c < numCols && c < topBorderSegments.length; c++) {
                borderMap.put("TOP_" + c, getColorStringFromChar(topBorderSegments[c].charAt(2)));
            }
            // Bottom borders
            for (int c = 0; c < numCols && c < bottomBorderSegments.length; c++) {
                borderMap.put("BOTTOM_" + c, getColorStringFromChar(bottomBorderSegments[c].charAt(0)));
            }
            // Left borders
            for (int r = 0; r < numRows && r < leftBorderSegments.length; r++) {
                borderMap.put("LEFT_" + r, getColorStringFromChar(leftBorderSegments[r].charAt(1)));
            }
            // Right borders
            for (int r = 0; r < numRows && r < rightBorderSegments.length; r++) {
                borderMap.put("RIGHT_" + r, getColorStringFromChar(rightBorderSegments[r].charAt(3)));
            }
            return borderMap;
        }


        private String getColorStringFromChar(char colorChar) {
            return switch (colorChar) {
                case 'R' -> "RED";
                case 'G' -> "GREEN";
                case 'Y' -> "YELLOW";
                case 'N' -> "NONE"; // If a border segment facing is explicitly uncolored
                default -> {
                    System.err.println("Game.PredefinedPuzzle: Encountered unexpected character '" + colorChar +
                            "' in border pattern. Defaulting to NONE.");
                    yield "NONE"; // Default for any unexpected char
                }
            };
        }
    }

    private List<PredefinedPuzzle> predefinedPuzzles;

    // Constructor for testing purposes
    public Game(GUIConnector gui, Field gameField, List<MosaicPiece> availablePieces, List<MosaicPiece> allPuzzlePieces,
                Map<String, String> currentBoardBorderColors) {
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

//    private void initializePredefinedPuzzles() {
//        this.predefinedPuzzles = new ArrayList<>();
//        // Example predefined puzzles
//        predefinedPuzzles.add(new PredefinedPuzzle(
//                new String[]{"NNGN", "NNRN", "NNYN"}, // Top: Green, Red, Yellow (South edges of border pieces)
//                new String[]{"GNNN", "RNNN", "YNNN"}, // Bottom: Green, Red, Yellow (North edges of border pieces)
//                new String[]{"NRNN", "NGNN", "NYNN", "NRNN"}, // Left: Red, Green, Yellow, Red (East edges of border pieces)
//                new String[]{"NNNY", "NNNG", "NNNR", "NNNY"}  // Right: Yellow, Green, Red, Yellow (West edges of border pieces)
//        ));
//    }

    /**
     * Creates a list of color patterns from the currently available pieces
     * and tells the GUI to display them.
     */
    private void updateAvailablePiecesInGUI() {
        List<String> pieceRepresentationsForGUI = new ArrayList<>();
        if (this.availablePieces != null) {
            for (MosaicPiece piece : this.availablePieces) {
                pieceRepresentationsForGUI.add(piece.getColorPattern());
            }
        }
        gui.displayAvailablePieces(pieceRepresentationsForGUI);
    }

    public void attemptPlacePiece(MosaicPiece pieceToPlace, int gameRow, int gameCol) {
        if (pieceToPlace == null) { // Should be caught by controller, but defensive check
            gui.showStatusMessage("Internal Error: No piece provided for placement.");
            return;
        }
        if (gameField.isCellHole(gameRow, gameCol)) {
            gui.showStatusMessage("Cannot place a piece on a hole.");
            return;
        }
        if (!gameField.isCellEmpty(gameRow, gameCol)) {
            gui.showStatusMessage("This cell is already occupied.");
            // Future: Logic to remove/replace if rules allow (e.g., click on occupied cell with new piece selected)
            return;
        }

        boolean isValidPlacement = checkPlacementValidity(pieceToPlace, gameRow, gameCol);
        gameField.setPieceAt(gameRow, gameCol, pieceToPlace); // Place in model

        // Remove the placed piece (original, unrotated version) from availablePieces list
        MosaicPiece pieceToRemoveFromMasterList = null;
        for (MosaicPiece p : availablePieces) {
            if (p.getColorPattern().equals(pieceToPlace.getColorPattern())) {
                pieceToRemoveFromMasterList = p;
                break;
            }
        }
        if (pieceToRemoveFromMasterList != null) {
            availablePieces.remove(pieceToRemoveFromMasterList);
        } else {
            System.err.println("GameLogic Error: Placed piece pattern '" + pieceToPlace.getColorPattern() + "' not found in master available list.");
        }

        // Update the specific game cell view, indicating error if not valid
        gui.updateGameCell(gameRow, gameCol, pieceToPlace.getColorPattern(),
                pieceToPlace.getColorPattern(), pieceToPlace.getOrientation(),
                !isValidPlacement, // isError = true if placement is NOT valid
                false);

        // Refresh the entire list of available pieces in the GUI
        List<String> currentAvailableReps = new ArrayList<>();
        for (MosaicPiece p : availablePieces) {
            currentAvailableReps.add(p.getColorPattern());
        }
        gui.displayAvailablePieces(currentAvailableReps);

        // Clear the selection from the side panel (both data and style)
        gui.clearSelectionFromPanel();

        if (isValidPlacement) {
            gui.showStatusMessage("Piece '" + pieceToPlace.getColorPattern() + "' placed at (" + gameRow + "," + gameCol + ").");
        } else {
            gui.showStatusMessage("Error: Piece at (" + gameRow + "," + gameCol + ") conflicts with neighbors or border!");
        }

        // Check for win condition
        if (isGameWon()) { // You need to implement isGameWon()
            gui.showGameEndMessage("Congratulations!", "You solved the puzzle!");
        }
        isDirty = true; // Mark the game state as modified

    }

//    public void selectPieceFromPanel(MosaicPiece piece) {
//        this.selectedPiece = piece;
//        if (piece != null) {
//            gui.enableRotationButton(true);
//            gui.showStatusMessage("Selected piece: " + piece.getColorPattern() + " (Rotated " + piece.getOrientation() + ")");
//        } else {
//            gui.enableRotationButton(false);
//        }
//    }

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

        MosaicPiece removedPieceFromBoard = gameField.getPieceAt(gameRow, gameCol);
//        System.out.println("Game: Piece at (" + gameRow + "," + gameCol + ") is " + (removedPieceFromBoard == null ? "null" : removedPieceFromBoard.getColorPattern()));


        if (removedPieceFromBoard != null) {
            // 1. Remove the piece from the gameField model
            gameField.setPieceAt(gameRow, gameCol, null); // Set the cell to empty
//            System.out.println("Logic: Piece " + removedPieceFromBoard.getColorPattern() + " removed from model at (" + gameRow + "," + gameCol + ").");

            // 2. Add the piece back to the availablePieces list.
            // It's important to add back the 'canonical' unrotated piece.
            // allPuzzlePieces should hold the 24 unique, original, unrotated pieces.
            MosaicPiece originalPieceToAddBack = null;
            for (MosaicPiece masterPiece : allPuzzlePieces) {
                if (masterPiece.getColorPattern().equals(removedPieceFromBoard.getColorPattern())) {
                    originalPieceToAddBack = masterPiece; // This is the unrotated master piece
                    break;
                }
            }

            if (originalPieceToAddBack != null) {
                // Avoid adding duplicates if it somehow wasn't properly removed from availablePieces during placement
                boolean alreadyInAvailableList = false;
                for (MosaicPiece availableP : availablePieces) {
                    if (availableP.getColorPattern().equals(originalPieceToAddBack.getColorPattern())) {
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
                        removedPieceFromBoard.getColorPattern() + " to return to available list.");
                // Fallback: could create a new one, but it's better to ensure allPuzzlePieces is sound.
                // availablePieces.add(new MosaicPiece(removedPieceFromBoard.getColorPattern()));
            }

            // 3. Update the GUI to show the cell as empty
            // pieceId and pieceImagePath are null, rotation is irrelevant (0), no error.
            gui.updateGameCell(gameRow, gameCol, null, null, 0, false, false);

            // 4. Refresh the "Available Pieces" panel in the GUI
            List<String> currentAvailablePiecePatterns = new ArrayList<>();
            for (MosaicPiece p : availablePieces) {
                currentAvailablePiecePatterns.add(p.getColorPattern());
            }
            gui.displayAvailablePieces(currentAvailablePiecePatterns);

            // 5. Show a status message
            gui.showStatusMessage("Piece " + removedPieceFromBoard.getColorPattern() +
                    " removed from (" + gameRow + "," + gameCol + ").");

            // 6. Clear any selection from the "Available Pieces" panel, as the context has changed
            gui.clearSelectionFromPanel();

        } else {
            // This case should not be reached if gameField.isCellEmpty() was false.
            System.err.println("Game Logic Error: Tried to remove piece from (" + gameRow + "," + gameCol +
                    ") but getPieceAt returned null despite not being empty.");
            gui.showStatusMessage("Error: No piece found at (" + gameRow + "," + gameCol + ") to remove.");
        }
        isDirty = true; // Mark the game state as modified
    }

    public void switchToEditorMode() {
//        this.editorMode = true;
        clearBoard();
        gui.setEditorMode(true);
        gui.showStatusMessage("Switched to Editor Mode. You can now edit the game board.");
    }

    public void switchToGameMode() {
        this.isEditorMode = false;

        this.availablePieces = new ArrayList<>(this.allPuzzlePieces); // Reset available pieces to all

        clearBoard();

        List<String> pieceRepresentationsForGUI = new ArrayList<>();
        for (MosaicPiece piece : this.availablePieces) {
            pieceRepresentationsForGUI.add(piece.getColorPattern());
        }
        gui.setEditorMode(false);
        gui.displayAvailablePieces(pieceRepresentationsForGUI);
        gui.showStatusMessage("Switched to Game Mode. You can now play the game.");
    }

    public boolean isPuzzleReadyToPlay() {
        if (gameField == null) {
            gui.showStatusMessage("Error: Game field is not initialized.");
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
        if (totalCells > 24) {
            int requiredHoles = totalCells - 24;
            if (gameField.getNumberOfHoles() < requiredHoles) {
                gui.showStatusMessage("Error: For a " + gameField.getRows() + "x" + gameField.getColumns() +
                        " board, you need at least " + requiredHoles + " holes.");
                return false;
            }
        }
    if (!isPuzzleSolvable()) {
            gui.showStatusMessage("Error: The current puzzle configuration is not solvable.");
            return false;
        }

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
        String color = currentBoardBorderColors.get(borderKey);
        boolean isColorMissing = (color == null || color.isEmpty() || color.equals("NONE"));

        if (!isColorMissing) {
            return true; // Color is present, segment is valid.
        }

        // Color is missing, check if the adjacent cell is a hole.
        Position adjacentCell = getAdjacentCellForBorderKey(borderKey);
        if (adjacentCell != null && gameField.isCellHole(adjacentCell.row(), adjacentCell.column())) {
            return true; // Color is missing, but it's next to a hole, so it's valid.
        }

        // Color is missing and it's not next to a hole. Invalid.
        gui.showStatusMessage("Error: Border segment " + borderKey + " needs a color.");
        return false;
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
        try {
            URL puzzleResource = Game.class.getResource("/logic/defaultPuzzleField.json");
            if (puzzleResource == null) {
                throw new IOException("Cannot find default puzzle file.");
            }
            File defaultPuzzleFile = new File(puzzleResource.toURI());
            loadGameFromFile(defaultPuzzleFile);

        } catch (IOException | URISyntaxException | JsonSyntaxException e) {
            e.printStackTrace();
            gui.showStatusMessage("CRITICAL ERROR: Could not reload default puzzle. " + e.getMessage());
        }
        isDirty = false; // Reset dirty flag after restart
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

    public boolean isGameWon() {
        // Placeholder: A game is won if all non-hole cells are filled
        // AND all placed pieces are valid (no red borders).
        // This requires iterating through the gameField.
        for (int r = 0; r < gameField.getRows(); r++) {
            for (int c = 0; c < gameField.getColumns(); c++) {
                if (!gameField.isCellHole(r, c) && gameField.isCellEmpty(r, c)) {
                    return false; // Found an empty, non-hole cell
                }
                // Also need to check if the piece at (r,c) is validly placed
                // This means checkPlacementValidity should be true for all placed pieces
                MosaicPiece p = gameField.getPieceAt(r,c);
                if (p != null && !checkPlacementValidity(p,r,c)){
                    return false; // Found an invalidly placed piece
                }
            }
        }
        // A simple check if correct number of pieces are used
        return availablePieces.size() == (24 - (gameField.getRows() * gameField.getColumns() - gameField.getNumberOfHoles())); // All cells filled and valid
    }

    /**
     * Checks if placing a given {@link MosaicPiece} at the specified row and column
     * on the game board is valid according to MacMahon Mosaic rules.
     * This involves checking matching colors with adjacent pieces and with the
     * predefined board border colors.
     *
     * @param piece The {@link MosaicPiece} to check, with its current orientation.
     * @param row   The 0-indexed row on the game board where the piece is intended to be placed.
     * @param col   The 0-indexed column on the game board where the piece is intended to be placed.
     * @return {@code true} if the placement is valid, {@code false} otherwise.
     */
    private boolean checkPlacementValidity(MosaicPiece piece, int row, int col) {
        return checkPlacementValidity(piece, row, col, this.gameField);
    }

    /**
     * Checks if placing a given piece at a specific position on a GIVEN field is valid.
     * This version is used by the backtracking solver which operates on copies of the game state.
     *
     * @param piece The MosaicPiece to check, with its current orientation.
     * @param row The 0-indexed row on the game board.
     * @param col The 0-indexed column on the game board.
     * @param field The Field object (a copy of the board) to check against.
     * @return {@code true} if the placement is valid, {@code false} otherwise.
     */
    private boolean checkPlacementValidity(MosaicPiece piece, int row, int col, Field field) {
        if (piece == null || field == null) {
            System.err.println("checkPlacementValidity: Called with a null piece or field.");
            return false;
        }

        for (Direction edgeDirectionOfPiece : Direction.values()) {
            char pieceEdgeColor = piece.getEdgeColor(edgeDirectionOfPiece);

            int neighborRow = row;
            int neighborCol = col;

            switch (edgeDirectionOfPiece) {
                case NORTH: neighborRow--; break;
                case EAST:  neighborCol++; break;
                case SOUTH: neighborRow++; break;
                case WEST:  neighborCol--; break;
            }

            // Check against game board borders, using the provided 'field' object
            if (neighborRow < 0 || neighborRow >= field.getRows() ||
                    neighborCol < 0 || neighborCol >= field.getColumns()) {

                String borderKey = getBorderSide(row, col, edgeDirectionOfPiece, field); // Pass field to helper

                if (borderKey != null && this.currentBoardBorderColors != null) {
                    String requiredBorderColorStr = this.currentBoardBorderColors.get(borderKey);
                    char requiredBorderColorChar = getColorCharFromColorString(requiredBorderColorStr);

                    if (requiredBorderColorChar != 'N' && requiredBorderColorChar != 'X' &&
                            pieceEdgeColor != requiredBorderColorChar) {
                        return false; // Border mismatch
                    }
                }
            } else { // Check against adjacent pieces or holes, using the provided 'field' object
                if (field.isCellHole(neighborRow, neighborCol)) {
                    continue; // Any color is fine next to a hole
                }

                MosaicPiece adjacentPiece = field.getPieceAt(neighborRow, neighborCol);
                if (adjacentPiece != null) {
                    char adjacentPieceEdgeColor = adjacentPiece.getEdgeColor(edgeDirectionOfPiece.opposite());
                    if (pieceEdgeColor != adjacentPieceEdgeColor) {
                        return false; // Neighbor mismatch
                    }
                }
            }
        }
        return true; // All checks passed
    }


    /**
     * Determines the border side (e.g., "TOP_0", "BOTTOM_1") based on the piece's
     * edge direction and its position on the game board, using the current game field.
     *
     * @param row The row index of the piece on the game board.
     * @param col The column index of the piece on the game board.
     * @param edgeDirectionOfPiece The direction of the edge of the piece being checked.
     * @param field The current game field to check against.
     * @return The border key string, or null if no border is applicable.
     */
    private String getBorderSide(int row, int col, Direction edgeDirectionOfPiece, Field field) {
        if (field == null) return null;
        return switch (edgeDirectionOfPiece) {
            case NORTH -> row == 0 ? "TOP_" + col : null;
            case SOUTH -> row == field.getRows() - 1 ? "BOTTOM_" + col : null;
            case WEST -> col == 0 ? "LEFT_" + row : null;
            case EAST -> col == field.getColumns() - 1 ? "RIGHT_" + row : null;
        };
    }

    /**
     * Determines the border side (e.g., "TOP_0", "BOTTOM_1") based on the piece's
     * edge direction and its position on the game board.
     *
     * @param row The row index of the piece on the game board.
     * @param col The column index of the piece on the game board.
     * @param edgeDirectionOfPiece The direction of the edge of the piece being checked.
     * @return The border key string, or null if no border is applicable.
     */
    private String getBorderSide(int row, int col, Direction edgeDirectionOfPiece) {
        return getBorderSide(row, col, edgeDirectionOfPiece, this.gameField);
    }

    /**
     * Converts a color string (e.g., "RED", "GREEN", "NONE") to its corresponding
     * character representation (e.g., 'R', 'G', 'N').
     * This is used for comparing piece edge colors (chars) with border color definitions (Strings).
     *
     * @param colorName The string representation of the color.
     * @return The character representation of the color. 'N' for "NONE", 'X' for unknown/error.
     */
    private char getColorCharFromColorString(String colorName) {
        if (colorName == null) {
            return 'X'; // Represents an undefined or error state for a color string
        }
        return switch (colorName.toUpperCase()) {
            case "RED" -> 'R';
            case "GREEN" -> 'G';
            case "YELLOW" -> 'Y';
            case "NONE" -> 'N'; // 'N' signifies that any color can match this border (or it's transparent)
            default -> {
                // This case should ideally not be reached if border color data is clean
                System.err.println("GameLogic Error: Unknown color string encountered: '" + colorName + "'");
                yield 'X'; // Fallback for an unexpected color string
            }
        };
    }

    private String getColorStringFromChar(char c) {
        return switch (c) {
            case 'R' -> "RED";
            case 'G' -> "GREEN";
            case 'Y' -> "YELLOW";
            case 'N' -> "NONE"; // No color
            default -> {
                System.err.println("Game.PredefinedPuzzle: Unexpected character '" + c + "' in border pattern. Defaulting to NONE.");
                yield "NONE"; // Default for any unexpected char
            }
        };
    }
    /**
     * The main recursive backtracking method to find a valid solution.
     *
     * @param field The current state of the board to solve.
     * @param availablePieces The list of pieces available to use.
     * @return A solved Field object if a solution is found, otherwise null.
     */
    private Field solvePuzzle(Field field, List<MosaicPiece> availablePieces) {
        Position nextEmpty = field.findNextEmptyCell();
        if (nextEmpty == null) {
            return field;
        }

        for (int i = 0; i < availablePieces.size(); i++) {
            MosaicPiece currentPiece = availablePieces.get(i);

            List<MosaicPiece> remainingPieces = new ArrayList<>(availablePieces);
            if (i >= 0 && i < remainingPieces.size()) {
                remainingPieces.remove(i); // Remove the piece that was just placed
            }

            for (int j = 0; j < 4; j++) {
                MosaicPiece orientedPiece = new MosaicPiece(currentPiece.getColorPattern(), j * 90);

                if (checkPlacementValidity(orientedPiece, nextEmpty.row(), nextEmpty.column(), field)) {
                    field.setPieceAt(nextEmpty.row(), nextEmpty.column(), orientedPiece);

                    Field solution = solvePuzzle(field, remainingPieces);
                    if (solution != null) {
                        return solution; // Found a valid solution
                    }
                    field.setPieceAt(nextEmpty.row(), nextEmpty.column(), null); // Backtrack
                }
            }
        }

        // If we've tried all pieces in all orientations and none led to a solution,
        // this path is a dead end.
        return null;

    }

    public boolean isPuzzleSolvable() {
        // Start with a copy of the game field and all available pieces
        Field fieldCopy = this.gameField.deepCopy();
        List<MosaicPiece> piecesCopy = new ArrayList<>(this.availablePieces);

        Field solution = solvePuzzle(fieldCopy, piecesCopy);

        return solution != null;
    }

    public void provideHint() {
        if (this.gameField.findNextEmptyCell() == null) {
            gui.showStatusMessage("No empty cells available for hints.");
            return;
        }

        Field fieldCopy = this.gameField.deepCopy();
        List<MosaicPiece> availablePiecesCopy = new ArrayList<>(this.availablePieces);

        Field solution = solvePuzzle(fieldCopy, availablePiecesCopy);

        if (solution != null) {
            Position hintPosition = this.gameField.findNextEmptyCell();
            if (hintPosition != null) {
                MosaicPiece hintPiece = solution.getPieceAt(hintPosition.row(), hintPosition.column());

                if (hintPiece != null) {
                    gui.showStatusMessage("Hint: Place piece '" + hintPiece.getColorPattern() + "' at (" +
                            hintPosition.row() + "," + hintPosition.column() + ").");
                    attemptPlacePiece(hintPiece, hintPosition.row(), hintPosition.column());
                    gui.highlightCell(hintPosition.row(), hintPosition.column(), hintPiece.getColorPattern());
                } else {
                    gui.showStatusMessage("No valid hint available for the next empty cell.");
                }
            }
        }
    }

    public void editCurrentPuzzle() {
        this.isEditorMode = true;
        gui.setEditorMode(true);
//        gui.showStatusMessage("Editing current puzzle. You can now modify the game board.");
        // Clear the board and reset available pieces to allPuzzlePieces
        clearBoard();
        this.availablePieces = new ArrayList<>(this.allPuzzlePieces);
        updateAvailablePiecesInGUI(); // Refresh the available pieces in the GUI
//        gui.clearCellHighlights(); // Clear any previous highlights
//        gui.showStatusMessage("You can now edit the game board. Use the available pieces to fill the cells.");
    }

    public List<MosaicPiece> getAvailablePieces() {
        return availablePieces;
    }


    public Field getGameField() {
        return gameField;
    }
}

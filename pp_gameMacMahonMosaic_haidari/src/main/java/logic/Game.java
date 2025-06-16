package logic;


import java.util.*;

public class Game {
    private final GUIConnector gui;
    private Field gameField;
    private List<MosaicPiece> availablePieces;
    private List<MosaicPiece> allPuzzlePieces; // Master list from TileLoader
    private boolean editorMode;
    private Random randomGenerator = new Random();
    private MosaicPiece selectedPiece;
    private Map<String, String> currentBoardBorderColors;

    public Map<String, String> predefinedBorderColors() {
        Map<String, String> borderColors = new HashMap<>();
        // Example: Top border colors for a 4x3 puzzle
        borderColors.put("TOP_0", "GREEN");
        borderColors.put("TOP_1", "RED");
        borderColors.put("TOP_2", "YELLOW");
        // Add other borders as needed
        return borderColors;
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
            // Top borders (puzzle row 0, game cell's North edge)
            for (int c = 0; c < numCols && c < topBorderSegments.length; c++) {
                borderMap.put("TOP_" + c, getColorStringFromChar(topBorderSegments[c].charAt(2)));
            }
            // Bottom borders (puzzle row numRows-1, game cell's South edge)
            for (int c = 0; c < numCols && c < bottomBorderSegments.length; c++) {
                borderMap.put("BOTTOM_" + c, getColorStringFromChar(bottomBorderSegments[c].charAt(0))); // Border piece's North edge faces South to game cell
            }
            // Left borders (puzzle col 0, game cell's West edge)
            for (int r = 0; r < numRows && r < leftBorderSegments.length; r++) {
                borderMap.put("LEFT_" + r, getColorStringFromChar(leftBorderSegments[r].charAt(1))); // Border piece's East edge faces West to game cell
            }
            // Right borders (puzzle col numCols-1, game cell's East edge)
            for (int r = 0; r < numRows && r < rightBorderSegments.length; r++) {
                borderMap.put("RIGHT_" + r, getColorStringFromChar(rightBorderSegments[r].charAt(3))); // Border piece's West edge faces East to game cell
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
    public Game(GUIConnector gui, Field gameField, List<MosaicPiece> availablePieces, List<MosaicPiece> allPuzzlePieces, Map<String, String> currentBoardBorderColors) {
        this.gui = gui;
        this.gameField = gameField;
        this.availablePieces = availablePieces;
        this.allPuzzlePieces = allPuzzlePieces;
        this.currentBoardBorderColors = currentBoardBorderColors;
        this.editorMode = false;
    }

    public Game(GUIConnector gui) {
        this.gui = gui;
        this.currentBoardBorderColors = predefinedBorderColors();
        this.allPuzzlePieces = new ArrayList<>();
        loadAndInitializeAllPieces();
        initializePredefinedPuzzles();

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

    private void initializePredefinedPuzzles() {
        this.predefinedPuzzles = new ArrayList<>();
        // Example predefined puzzles
        predefinedPuzzles.add(new PredefinedPuzzle(
                new String[]{"NNGN", "NNRN", "NNYN"}, // Top: Green, Red, Yellow (South edges of border pieces)
                new String[]{"GNNN", "RNNN", "YNNN"}, // Bottom: Green, Red, Yellow (North edges of border pieces)
                new String[]{"NRNN", "NGNN", "NYNN", "NRNN"}, // Left: Red, Green, Yellow, Red (East edges of border pieces)
                new String[]{"NNNY", "NNNG", "NNNR", "NNNY"}  // Right: Yellow, Green, Red, Yellow (West edges of border pieces)
        ));
    }

    public void startGame(int initialGameRows, int initialGameColumns) {
        if (allPuzzlePieces.isEmpty()) {
            gui.showStatusMessage("Cannot start game: Tile definitions are missing.");
            return;
        }
        System.out.println("all pieces are available");

        Map<String, String> borderColorsForGUI;
        Set<Position> holesForField = Collections.emptySet(); // Default: no holes for 4x3

        if (initialGameRows == 4 && initialGameColumns == 3) {
            PredefinedPuzzle config = new PredefinedPuzzle(
                    new String[]{"NNGN", "NNRN", "NNYN"}, // Top: Green, Red, Yellow (South edges of border pieces)
                    new String[]{"GNNN", "RNNN", "YNNN"}, // Bottom: Green, Red, Yellow (North edges of border pieces)
                    new String[]{"NRNN", "NGNN", "NYNN", "NRNN"}, // Left: Red, Green, Yellow, Red (East edges of border pieces)
                    new String[]{"NNNY", "NNNG", "NNNY", "NNNY"}  // Right: Yellow, Green, Red, Yellow (West edges of border pieces)
            );
            borderColorsForGUI = config.getBorderColors(initialGameColumns, initialGameRows);
            this.currentBoardBorderColors = borderColorsForGUI;
//            System.out.println("DEBUG: Initialized currentBoardBorderColors: " + this.currentBoardBorderColors);
//            System.out.println("Using predefined 4x3 puzzle configuration.");
        } else {
            // Handle non-default sizes: requires dynamic generation or more predefined puzzles.
            // For now, create generic borders and warn about solvability.
            gui.showStatusMessage("Warning: Using generic borders for " + initialGameRows + "x" + initialGameColumns + " size. Solvability not guaranteed.");
            borderColorsForGUI = new HashMap<>(); // Placeholder: fill with some default (e.g., all RED)
            for(int c=0; c<initialGameColumns; c++) { borderColorsForGUI.put("TOP_"+c, "RED"); borderColorsForGUI.put("BOTTOM_"+c, "RED");}
            for(int r=0; r<initialGameRows; r++) { borderColorsForGUI.put("LEFT_"+r, "RED"); borderColorsForGUI.put("RIGHT_"+r, "RED");}

            if ((initialGameRows * initialGameColumns) > 24) {
                // TODO: Implement logic to add (rows * cols - 24) holes ensuring solvability.
                // For now, no holes added.
                gui.showStatusMessage("Field is larger than 24 cells; holes need to be implemented.");
            }
        }

        // The Field's internal border representation. For now, it's null as Game handles border logic.
        // If Field needed its own map, you'd convert/pass the chosen border config here.
        this.gameField = new Field(initialGameRows, initialGameColumns, null, holesForField);

        this.availablePieces = new ArrayList<>(this.allPuzzlePieces);

        gui.initializeBoardView(initialGameRows, initialGameColumns, this.currentBoardBorderColors);

        List<String> pieceRepresentationsForGUI = new ArrayList<>();
        for (MosaicPiece piece : this.availablePieces) {
            pieceRepresentationsForGUI.add(piece.getColorPattern());
        }
        gui.displayAvailablePieces(pieceRepresentationsForGUI);

        this.editorMode = false;
        gui.setEditorMode(false);
        gui.showStatusMessage("New game started! " + availablePieces.size() + " pieces available.");

        // if (!checkSolvability()) { // Should be true if using a good predefined puzzle
        //     gui.showStatusMessage("Error: Puzzle may not be solvable!");
        // }
    }

    private PredefinedPuzzle getSolvableConfig() {
        if (predefinedPuzzles.isEmpty()) {
            // Fallback if no predefined puzzles are set up
            gui.showStatusMessage("Warning: No predefined 4x3 puzzles available! Using random (likely unsolvable) borders.");
            // Create a dummy random one for structure, but this won't guarantee solvability.
            Random r = new Random();
            char[] colors = {'R', 'G', 'Y'};
            String[] randomTop = new String[3];
            String[] randomBottom = new String[3];
            String[] randomLeft = new String[4];
            String[] randomRight = new String[4];
            for(int i=0; i<3; i++) randomTop[i] = "NN" + colors[r.nextInt(3)] + "N";
            for(int i=0; i<3; i++) randomBottom[i] = colors[r.nextInt(3)] + "NNN";
            for(int i=0; i<4; i++) randomLeft[i] = "N" + colors[r.nextInt(3)] + "NN";
            for(int i=0; i<4; i++) randomRight[i] = "NNN" + colors[r.nextInt(3)];
            return new PredefinedPuzzle(randomTop, randomBottom, randomLeft, randomRight);
        }
        // Select one of the predefined 4x3 puzzles (e.g., randomly)
        return predefinedPuzzles.get(randomGenerator.nextInt(predefinedPuzzles.size()));
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
                pieceToPlace.getColorPattern(), // pieceImagePath (can be pattern)
                pieceToPlace.getOrientation(),
                !isValidPlacement); // isError = true if placement is NOT valid

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

    }

    public void selectPieceFromPanel(MosaicPiece piece) {
        this.selectedPiece = piece;
        if (piece != null) {
            gui.enableRotationButton(true);
            gui.showStatusMessage("Selected piece: " + piece.getColorPattern() + " (Rotated " + piece.getOrientation() + ")");
        } else {
            gui.enableRotationButton(false);
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
        System.out.println("Game: removePieceFromField called for (" + gameRow + "," + gameCol + ")");

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
//                    System.out.println("Logic: Piece " + originalPieceToAddBack.getColorPattern() + " added back to available pieces.");
                } else {
//                    System.out.println("Logic: Piece " + originalPieceToAddBack.getColorPattern() + " was already in available pieces (no re-add).");
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
            gui.updateGameCell(gameRow, gameCol, null, null, 0, false);

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
    }

    public MosaicPiece getSelectedPieceForPlacement() {
        return this.selectedPiece;
    }

    public void restartGame() {
        this.gameField = new Field(5, 5, null, null); // Placeholder for border colors and holes
        this.availablePieces = List.of(); // Placeholder for available pieces
        this.allPuzzlePieces = List.of(); // Placeholder for all possible pieces
    }

    public boolean saveGame(String filePath) {
        // Placeholder for save game logic
        return true;
    }

    public boolean loadGame(String filePath) {
        // Placeholder for load game logic
        return true;
    }

    public void toggleEditorMode() {
        this.editorMode = !this.editorMode;
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
        if (availablePieces.size() == (24 - (gameField.getRows() * gameField.getColumns() - gameField.getNumberOfHoles()))){ // A simple check if correct number of pieces are used
            return true; // All cells filled and valid
        }
        return false; // Default
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
//        if (piece == null) {
//            System.err.println("checkPlacementValidity: Called with a null piece.");
//            return false; // Cannot validate a null piece
//        }
//        if (gameField == null) {
//            System.err.println("checkPlacementValidity: gameField is null.");
//            return false;
//        }
////        System.out.println(">>> Validating piece " + piece.getColorPattern() + " (Orient: " + piece.getOrientation() + "°) at (" + row + "," + col + ")"); // Added log
//
//        for (Direction edgeDirectionOfPiece : Direction.values()) {
//            char pieceEdgeColor = piece.getEdgeColor(edgeDirectionOfPiece);
//
//            int neighborRow = row;
//            int neighborCol = col;
//
//            // Determine coordinates of the cell adjacent to the current edge
//            switch (edgeDirectionOfPiece) {
//                case NORTH: neighborRow--; break;
//                case EAST:  neighborCol++; break;
//                case SOUTH: neighborRow++; break;
//                case WEST:  neighborCol--; break;
//            }
//
//            // Check against game board borders
//            if (neighborRow < 0 || neighborRow >= gameField.getRows() ||
//                    neighborCol < 0 || neighborCol >= gameField.getColumns()) {
//
//                String borderKey = getBorderSide(row, col, edgeDirectionOfPiece);
////                System.out.println("    Edge " + edgeDirectionOfPiece + ": Border check. Key: " + borderKey + ", Piece Edge Color: " + pieceEdgeColor); // Added log
//
//
//                if (borderKey != null && this.currentBoardBorderColors != null) {
//                    String requiredBorderColorStr = this.currentBoardBorderColors.get(borderKey);
//                    char requiredBorderColorChar = getColorCharFromColorString(requiredBorderColorStr);
////                    System.out.println("        Required Border Color Str: " + requiredBorderColorStr +
////                            ", Required Border Char: " + requiredBorderColorChar);
//
//                    if (requiredBorderColorChar != 'N' && requiredBorderColorChar != 'X' &&
//                            pieceEdgeColor != requiredBorderColorChar) {
////                        System.out.println("Validation Fail: Border mismatch at (" + row + "," + col +
////                                "), edge " + edgeDirectionOfPiece + " (key: " + borderKey +
////                                "). Piece has '" + pieceEdgeColor +
////                                "', border requires '" + requiredBorderColorChar + "'.");
//                        return false;
//                    }else {
////                        System.out.println("        Validation Pass for this border edge."); // Added log
//                    }
//                } else {
////                    System.out.println("        Skipping border check: borderKey is null or currentBoardBorderColors is null."); // Added log
//                }
//            } else {
//                if (gameField.isCellHole(neighborRow, neighborCol)) {
//                    // Any color is allowed next to a hole, so this edge is fine.
//                    continue;
//                }
//
//                MosaicPiece adjacentPiece = gameField.getPieceAt(neighborRow, neighborCol);
//                if (adjacentPiece != null) {
//                    // Get the color of the adjacent piece's edge that touches the current piece's edge.
//                    char adjacentPieceEdgeColor = adjacentPiece.getEdgeColor(edgeDirectionOfPiece.opposite());
//
//                    if (pieceEdgeColor != adjacentPieceEdgeColor) {
////                        System.out.println("Validation Fail: Neighbor mismatch between (" + row + "," + col +
////                                ") edge " + edgeDirectionOfPiece + " (color '" + pieceEdgeColor + "') and " +
////                                "neighbor (" + neighborRow + "," + neighborCol + ") edge " +
////                                edgeDirectionOfPiece.opposite() + " (color '" + adjacentPieceEdgeColor + "').");
//                        return false;
//                    }
//                }
//            }
//        }
//        System.out.println("<<< Validation Succeeded for piece at (" + row + "," + col + "). Returning true."); // Added log
//        return true;
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

    /**
     * The main recursive backtracking method to find a valid solution.
     *
     * @param field The current state of the board to solve.
     * @param availablePieces The list of pieces available to use.
     * @return A solved Field object if a solution is found, otherwise null.
     */
    private Field solvePuzzle(Field field, List<MosaicPiece> availablePieces) {
        Position nextEmpty = field.findNextEmptyCell();
        System.out.println("Next empty cell found at: " + (nextEmpty == null ? "None" : "(" + nextEmpty.row() + "," + nextEmpty.column() + ")"));
        if (nextEmpty == null) {
            System.out.println("All cells filled! Returning solved field.");
            return field;
        }

        for (int i = 0; i < availablePieces.size(); i++) {
            MosaicPiece currentPiece = availablePieces.get(i);

            List<MosaicPiece> remainingPieces = new ArrayList<>(availablePieces);
            remainingPieces.remove(i); // Remove the piece that was just placed

            for (int j = 0; j < 4; j++) {
                MosaicPiece orientedPiece = new MosaicPiece(currentPiece.getColorPattern(), j * 90);

                if (checkPlacementValidity(orientedPiece, nextEmpty.row(), nextEmpty.column(), field)) {
                    field.setPieceAt(nextEmpty.row(), nextEmpty.column(), orientedPiece);

                    Field solution = solvePuzzle(field, remainingPieces);
                    if (solution != null) {
                        System.out.println("Backtracking: Found a valid placement for piece " + orientedPiece.getColorPattern() +
                                " at (" + nextEmpty.row() + "," + nextEmpty.column() + ").");
                        return solution; // Found a valid solution
                    }
                    field.setPieceAt(nextEmpty.row(), nextEmpty.column(), null); // Backtrack
                }
            }
        }

        System.out.println("Backtracking: No valid placement found for piece at (" + nextEmpty.row() + "," + nextEmpty.column() + ").");
        // If we've tried all pieces in all orientations and none led to a solution,
        // this path is a dead end.
        return null;

    }

    public boolean isPuzzleSolvable() {
        // Start with a copy of the game field and all available pieces
        Field fieldCopy = this.gameField.deepCopy();
        List<MosaicPiece> piecesCopy = new ArrayList<>(this.availablePieces);

        Field solution = solvePuzzle(fieldCopy, piecesCopy);
        System.out.println("Solvability check complete. Solution " + (solution != null ? "found." : "not found."));

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

    public List<MosaicPiece> getAvailablePieces() {
        return availablePieces;
    }


    public Field getGameField() {
        return gameField;
    }
}

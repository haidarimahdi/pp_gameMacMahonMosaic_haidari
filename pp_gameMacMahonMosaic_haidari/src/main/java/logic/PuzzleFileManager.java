package logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.*;

public class PuzzleFileManager {
    private final List<MosaicPiece> allPuzzlePieces;

    public PuzzleFileManager(List<MosaicPiece> allPuzzlePieces) {
        this.allPuzzlePieces = allPuzzlePieces;
    }

    private PuzzleState loadPuzzleFromStream(InputStream inputStream) throws IOException, JsonSyntaxException {
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            if (jsonObject == null || !jsonObject.has("field")) {
                throw new JsonSyntaxException("The selected file is not a valid puzzle file format.");
            }
            return parsePuzzleFile(gson.fromJson(jsonObject, PuzzleFile.class));
        }
    }


    public PuzzleState loadPuzzleFromFile(File file) throws IOException, JsonSyntaxException {
//        Gson gson = new Gson();
//        try (Reader reader = new FileReader(file)) {
//            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
//            if (jsonObject == null || !jsonObject.has("field")) {
//                throw new JsonSyntaxException("The selected file is not a valid puzzle file format.");
//            }
//            return parsePuzzleFile(gson.fromJson(jsonObject, PuzzleFile.class));
//        }
        try (InputStream inputStream = new FileInputStream(file)) {
            return loadPuzzleFromStream(inputStream);
        }
    }

    /**
     * Loads a puzzle configuration from a JSON string.
     * This demonstrates the flexibility of using InputStream.
     *
     * @param jsonContent The string containing the puzzle in JSON format.
     * @return A fully parsed PuzzleState object.
     * @throws IOException If an I/O error occurs (unlikely for strings).
     * @throws JsonSyntaxException If the string is not valid JSON.
     */
    public PuzzleState loadPuzzleFromString(String jsonContent) throws IOException, JsonSyntaxException {
        try (InputStream inputStream =
                     new ByteArrayInputStream(jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return loadPuzzleFromStream(inputStream);
        }
    }
    private PuzzleState parsePuzzleFile(PuzzleFile puzzleFile) {
        if (puzzleFile.field.isEmpty() || puzzleFile.field.get(0).isEmpty()) {
            throw new JsonSyntaxException("The puzzle file is not a valid puzzle file format.");
        }

        int totalRows = puzzleFile.field.size();
        int totalCols = puzzleFile.field.get(0).size();
        int gameRows = totalRows - 2; // Exclude top and bottom borders
        int gameCols = totalCols - 2; // Exclude left and right borders

        Map<BorderPosition, Color> newBorderColors = new HashMap<>();
        Set<Position> newHoles = new HashSet<>();
        List<MosaicPiece> canonicalPiecesToRemove = new ArrayList<>();
        Field newField = new Field(gameRows, gameCols, newBorderColors, newHoles);

        for (int r = 0; r < totalRows; r++) {
            for (int c = 0; c < totalCols; c++) {
                String patternFromFile = puzzleFile.field.get(r).get(c);
                boolean isBorder = (r == 0 || r == totalRows - 1 || c == 0 || c == totalCols - 1);

                if (isBorder) {
                    BorderPosition key = deriveBorderPosition(r, c, gameRows, gameCols);
                    if (key != null) {
                        Color color = deriveColorFromBorderPattern(patternFromFile, key);
                        newBorderColors.put(key, color);
                    }
                } else { // This is a game cell, not a border
                    int gameR = r - 1; // Adjust for border rows
                    int gameC = c - 1; // Adjust for border columns

                    if (patternFromFile.equals("HHHH")) {
                        newHoles.add(new Position(gameR, gameC));
                        newField.setPieceAt(gameR, gameC, null); // Ensure the cell is empty
                    } else if (!patternFromFile.equals("NNNN")) {
                        MosaicPiece canonicalPiece = findCanonicalPieceForPattern(patternFromFile);

                        if (canonicalPiece != null) {
                            MosaicPiece pieceForBoard = createOrientedPieceForBoard(canonicalPiece, patternFromFile);
                            newField.setPieceAt(gameR, gameC, pieceForBoard);
                            canonicalPiecesToRemove.add(pieceForBoard);
                        } else {
                            System.err.println("Could not find a matching canonical piece for pattern: " + patternFromFile);
                        }

                    }
//                    else {
                        // If the patternFromFile is "NNNN", it means this cell is empty
//                        newField.setPieceAt(gameR, gameC, null);
//                    }
                }
            }
        }

        // return the complete parsed state
        return new PuzzleState(newField, newBorderColors, canonicalPiecesToRemove);

    }

    public void saveGameToFile(File file, Field gameField, Map<BorderPosition, Color> borderColors) throws IOException {
        PuzzleFile saveFile = new PuzzleFile();
        saveFile.field = new ArrayList<>();
        int totalRows = gameField.getRows() + 2; // Include borders
        int totalCols = gameField.getColumns() + 2; // Include borders

        // Iterate over the entire grid, including borders, to build the 2D list
        for (int r = 0; r < totalRows; r++) {
            List<String> rowList = new ArrayList<>();
            for (int c = 0; c < totalCols; c++) {
                boolean isBorder = ((r == 0) || (c == 0) || (r == totalRows - 1) || (c == totalCols - 1));
                if (isBorder) {
                    rowList.add(getPatternForBorderCell(r, c, gameField, borderColors));
                } else {
                    rowList.add(getPatternForGameCell(r - 1, c - 1, gameField));
                }
            }
            saveFile.field.add(rowList);
        }

        // Use Gson to write the object to the file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(saveFile, writer);
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

        Color[] targetPattern = new Color[Game.EDGE_COUNT];
        try {
            for (int i = 0; i < Game.EDGE_COUNT; i++) {
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
                        tempPiece.getEdgeColor(Direction.TOP),
                        tempPiece.getEdgeColor(Direction.RIGHT),
                        tempPiece.getEdgeColor(Direction.BOTTOM),
                        tempPiece.getEdgeColor(Direction.LEFT)
                };

                // If the rotated pattern matches the pattern from the file, we found it!
                if (Arrays.equals(effectivePattern, targetPattern)) {
//                    MosaicPiece resultPiece = new MosaicPiece(canonicalPiece.getColorPattern());
//                    resultPiece.setOrientation(orientation);
                    return canonicalPiece; // Return the actual conanical piece from the master list
                }
            }
        }
        return null; // No matching piece was found
    }

    private MosaicPiece createOrientedPieceForBoard(MosaicPiece canonicalPiece, String patternFromFile) {
        MosaicPiece pieceForBoard = new MosaicPiece(canonicalPiece.getColorPattern());
        for (int o = 0; o < 360; o += 90) {
            pieceForBoard.setOrientation(o);
            char n = pieceForBoard.getEdgeColor(Direction.TOP).getChar();
            char e = pieceForBoard.getEdgeColor(Direction.RIGHT).getChar();
            char s = pieceForBoard.getEdgeColor(Direction.BOTTOM).getChar();
            char w = pieceForBoard.getEdgeColor(Direction.LEFT).getChar();
            String effectivePattern = "" + n + e + s + w;

            if (effectivePattern.equals(patternFromFile)) {
                return pieceForBoard; // Found correct orientation
            }
        }
        return pieceForBoard; // Should have found it, but return with default orientation as fallback
    }


    /**
     * Derives the border color from the 4-character pattern string read from a file.
     * This method is now type-safe, using a switch on the Direction enum from the BorderPosition.
     *
     * @param pattern The 4-character pattern (e.g., "NNRN") from the saved file.
     * @param borderPosition The BorderPosition object representing the border segment.
     * @return The determined Color enum.
     */
    private Color deriveColorFromBorderPattern(String pattern, BorderPosition borderPosition) {
        char colorChar = switch (borderPosition.side()) {
            case TOP -> // This is the TOP border of the board
                    pattern.charAt(2); // The South-facing edge of the border cell
            case BOTTOM -> // This is the BOTTOM border of the board
                    pattern.charAt(0); // The North-facing edge of the border cell
            case LEFT ->  // This is the LEFT border of the board
                    pattern.charAt(1); // The East-facing edge of the border cell
            case RIGHT ->  // This is the RIGHT border of the board
                    pattern.charAt(3); // The West-facing edge of the border cell
        }; // Default to NONE

        // This switch is safer and more efficient than string comparisons.
        return Color.fromChar(colorChar);
    }

    /**
     * Determines the BorderPosition for a given cell based on its coordinates in the full grid layout.
     * This replaces the old string-based deriveBorderKey method.
     *
     * @param r The row in the full grid (including borders).
     * @param c The column in the full grid (including borders).
     * @param gameRows The number of rows in the actual game field.
     * @param gameCols The number of columns in the actual game field.
     * @return A BorderPosition object if the coordinates correspond to a border, otherwise null.
     */
    private BorderPosition deriveBorderPosition(int r, int c, int gameRows, int gameCols) {
        return Game.getBorderPositionForCoords(r, c, gameRows, gameCols);
    }

    /**
     * Helper method to get the 4-character pattern for a cell within the game area.
     */
    private String getPatternForGameCell(int gameRow, int gameCol, Field gameField) {
        if (gameField.isCellHole(gameRow, gameCol)) {
            char holeChar = Color.HOLE.getChar();
            return String.valueOf(new char[]{holeChar, holeChar, holeChar, holeChar});
        }
        MosaicPiece piece = gameField.getPieceAt(gameRow, gameCol);
        if (piece == null) {
            // return Empty cell
            char noneChar = Color.NONE.getChar();
            return String.valueOf(new char[]{noneChar, noneChar, noneChar, noneChar});
        }
        // For a placed piece, calculate its effective pattern based on rotation
        char n = piece.getEdgeColor(Direction.TOP).getChar();
        char e = piece.getEdgeColor(Direction.RIGHT).getChar();
        char s = piece.getEdgeColor(Direction.BOTTOM).getChar();
        char w = piece.getEdgeColor(Direction.LEFT).getChar();
        return String.valueOf(n) + e + s + w;
    }

    /**
     * Helper method to get the 4-character pattern for a border or corner cell.
     */
    private String getPatternForBorderCell(int guiRow, int guiCol, Field gameField,
                                           Map<BorderPosition, Color> borderColors) {
        int gameRows = gameField.getRows();
        int gameCols = gameField.getColumns();
        char n = 'N', e = 'N', s = 'N', w = 'N';

        Color borderColor = null;
        BorderPosition borderPosition = Game.getBorderPositionForCoords(guiRow, guiCol, gameRows, gameCols);

        if (borderPosition != null) {
            borderColor = borderColors.get(borderPosition);
        }

        if (borderColor != null) {
            switch (borderPosition.side()) {
                case TOP -> s = borderColor.getChar(); // The south-facing edge of the top border
                case BOTTOM -> n = borderColor.getChar(); // The north-facing edge of the bottom border
                case LEFT -> e = borderColor.getChar(); // The east-facing edge of the left border
                case RIGHT -> w = borderColor.getChar(); // The west-facing edge of the right border
            }
        }
        return String.valueOf(n) + e + s + w;
    }

}

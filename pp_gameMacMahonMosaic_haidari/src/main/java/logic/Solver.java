package logic;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Solver {

    /**
     * Public entry point for the solver.
     * Takes a puzzle state and returns a solved state.
     * @param fieldToSolve The field configuration to solve.
     * @param availablePieces The list of pieces available to use.
     * @return A Field object representing the solution, or null if unsolvable.
     */
    public Field findSolution(Field fieldToSolve, List<MosaicPiece> availablePieces,
                              Map<BorderPosition, Color> borderColors) {
        return solvePuzzle(fieldToSolve, availablePieces, borderColors);
    }

    /**
     * The core recursive backtracking algorithm. It attempts to find a valid
     * placement of pieces for the given field.
     *
     * @param field           The current state of the board being solved.
     * @param availablePieces The list of pieces not yet placed.
     * @param borderColors    The map of required border colors for the puzzle.
     * @return A solved Field object if a solution is found, otherwise null.
     */
    private Field solvePuzzle(Field field, List<MosaicPiece> availablePieces, Map<BorderPosition, Color> borderColors) {
        Position nextEmpty = field.findMostConstrainedEmptyCell();
        if (nextEmpty == null) {
            return field;
        }

        Map<Direction, Color> constraints = getConstraintsForCell(nextEmpty.row(), nextEmpty.column(), field, borderColors);
        List<MosaicPiece> candidatePieces = new ArrayList<>();
        for (MosaicPiece piece : availablePieces) {
            if (canPieceMeetConstraints(piece, constraints)) {
                candidatePieces.add(piece);
            }
        }

        // Iterate through only the promising candidates.
        for (MosaicPiece piece : candidatePieces) {
            List<MosaicPiece> remainingPieces = new ArrayList<>(availablePieces);
            remainingPieces.remove(piece);

            for (int orientation = 0; orientation < 360; orientation += 90) {
                piece.setOrientation(orientation);
                if (checkPlacementValidity(piece, nextEmpty.row(), nextEmpty.column(), field, borderColors)) {
                    field.setPieceAt(nextEmpty.row(), nextEmpty.column(), piece);
                    Field solution = solvePuzzle(field, remainingPieces, borderColors);
                    if (solution != null) {
                        return solution;
                    }
                    field.setPieceAt(nextEmpty.row(), nextEmpty.column(), null); // Backtrack
                }
            }

        }

        return null;

    }

    /**
     * Gets the required edge colors for a given empty cell based on its neighbors.
     * @param row   The row of the empty cell.
     * @param col   The column of the empty cell.
     * @param field The current state of the board.
     * @return A Map where the key is the direction and the value is the required Color enum.
     */
    private Map<Direction, Color> getConstraintsForCell(int row, int col, Field field, Map<BorderPosition, Color> borderColors) {
        Map<Direction, Color> constraints = new EnumMap<>(Direction.class);

        // Check all four directions for a constraint.
        for (Direction dir : Direction.values()) {
            Color requiredColor = getRequiredEdgeColorFor(row, col, dir, field, borderColors);
            if (requiredColor != Color.NONE) {
                constraints.put(dir, requiredColor);
            }
        }
        return constraints;
    }

    /**
     * Checks if a given piece can satisfy a set of color constraints in any of its orientations.
     * This is used to pre-filter pieces before trying to place them.
     * @param piece       The piece to check.
     * @param constraints The map of required colors from getConstraintsForCell.
     * @return True if the piece can meet the constraints, false otherwise.
     */
    private boolean canPieceMeetConstraints(MosaicPiece piece, Map<Direction, Color> constraints) {
        // Try all 4 rotations to see if any of them match the constraints.
        for (int orientation = 0; orientation < 360; orientation += 90) {
            piece.setOrientation(orientation);
            boolean rotationMatches = true;
            for (Map.Entry<Direction, Color> entry : constraints.entrySet()) {
                // If any edge does not match the constraint, this rotation is invalid.
                if (piece.getEdgeColor(entry.getKey()) != entry.getValue()) {
                    rotationMatches = false;
                    break;
                }
            }
            if (rotationMatches) {
                return true; // Found an orientation that works.
            }
        }
        return false; // No orientation of this piece can satisfy the constraints.
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
    public static boolean checkPlacementValidity(MosaicPiece piece, int row, int col, Field field,
                                                Map<BorderPosition, Color> borderColors) {
        for (Direction dir : Direction.values()) {
            Color pieceEdgeColor = piece.getEdgeColor(dir);

            int[] neighbor = getNeighborCoordinates(row, col, dir);
            int neighborRow = neighbor[0];
            int neighborCol = neighbor[1];

            // Check against borders
            if (neighborRow < 0 || neighborRow >= field.getRows() || neighborCol < 0 || neighborCol >= field.getColumns()) {
                BorderPosition borderKey = getBorderSide(row, col, field.getRows(), field.getColumns(), dir);
                Color requiredBorderColor = borderColors.getOrDefault(borderKey, Color.NONE);
                if (requiredBorderColor != Color.NONE && pieceEdgeColor != requiredBorderColor) {
                    return false; // Mismatch with a defined border
                }
            } else { // Check against adjacent pieces
                MosaicPiece neighborPiece = field.getPieceAt(neighborRow, neighborCol);
                if (neighborPiece != null) {
                    Color requiredNeighborColor = neighborPiece.getEdgeColor(dir.opposite());
                    if (pieceEdgeColor != requiredNeighborColor) {
                        return false; // Mismatch with a neighbor piece
                    }
                }
            }
        }
        return true; // All sides are valid
    }

    private static int[] getNeighborCoordinates(int row, int col, Direction dir) {
        int neighborRow = row;
        int neighborCol = col;
        switch (dir) {
            case TOP -> neighborRow--;
            case RIGHT -> neighborCol++;
            case BOTTOM -> neighborRow++;
            case LEFT -> neighborCol--;
        }
        return new int[]{neighborRow, neighborCol};
    }

    /**
     * Determines the required color for a single edge of an empty cell by checking
     * for adjacent borders or neighboring pieces.
     *
     * @param row       The row of the empty cell being checked.
     * @param col       The column of the empty cell being checked.
     * @param direction The direction of the edge (NORTH, EAST, SOUTH, WEST) to check.
     * @return The required Color enum (RED, GREEN, YELLOW), or Color.NONE if there is no constraint.
     */
    public static Color getRequiredEdgeColorFor(int row, int col, Direction direction, Field field,
                                                Map<BorderPosition, Color> borderColors) {

        int[] neighbor = getNeighborCoordinates(row, col, direction);
        int neighborRow = neighbor[0];
        int neighborCol = neighbor[1];

        // Case 1: The neighbor is outside the board (it's a border).
        if (neighborRow < 0 || neighborRow >= field.getRows() ||
                neighborCol < 0 || neighborCol >= field.getColumns()) {

            BorderPosition borderKey = getBorderSide(row, col, field.getRows(), field.getColumns(), direction);
            return borderColors.getOrDefault(borderKey, Color.NONE);
        }

        // Case 2: The neighbor is another piece on the board.
        MosaicPiece neighborPiece = field.getPieceAt(neighborRow, neighborCol);
        if (neighborPiece != null) {
            // The required color is the color of the neighbor's opposite-facing edge.
            return neighborPiece.getEdgeColor(direction.opposite());
        }

        // Case 3: The neighbor is another empty cell, so there is no color requirement.
        return Color.NONE;
    }

    /**
     * Determines the border side key for a given cell based on its position and the edge direction.
     * This is used to map the cell's edge to the corresponding border color in the GUI.
     *
     * @param row The row of the cell.
     * @param col The column of the cell.
     * @param gameRows The total number of rows in the game board.
     * @param gameCols The total number of columns in the game board.
     * @param edgeDirectionOfPiece The direction of the edge being checked (NORTH, EAST, SOUTH, WEST).
     * @return A string representing the border side key, or null if not applicable.
     */
    private static BorderPosition getBorderSide(int row, int col, int gameRows, int gameCols, Direction edgeDirectionOfPiece) {
        return switch (edgeDirectionOfPiece) {
            case TOP -> row == 0 ? new BorderPosition(Direction.TOP, col) : null;
            case RIGHT -> col == gameCols - 1 ? new BorderPosition(Direction.RIGHT, row) : null;
            case BOTTOM -> row == gameRows - 1 ? new BorderPosition(Direction.BOTTOM, col): null;
            case LEFT -> col == 0 ? new BorderPosition(Direction.LEFT, row) : null;
        };
    }


}

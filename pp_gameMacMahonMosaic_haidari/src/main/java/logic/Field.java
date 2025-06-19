package logic;

import java.util.Map;
import java.util.Set;

public class Field {

    private final int rows;
    private final int columns;
    private final MosaicPiece[][] pieces;
    private final Map<String, String> borderColors;
    private final Set<Position> holes;

    public Field(int rows, int columns, Map<String, String> borderColors, Set<Position> holes) {
        this.rows = rows;
        this.columns = columns;
        this.pieces = new MosaicPiece[rows][columns];
        this.borderColors = borderColors;
        this.holes = holes;
    }

    // Getters and Setters
    public int getRows() {
        return rows;
    }
    public int getColumns() {
        return columns;
    }
    public MosaicPiece getPieceAt(int row, int column) {
        return pieces[row][column];
    }
    public Set<Position> getHoles() {
        return holes;
    }
    public int getNumberOfHoles() {
        return holes != null ? holes.size() : 0;
    }
    public boolean isCellEmpty(int row, int column) {
        return pieces[row][column] == null;
    }
    public boolean isCellHole(int row, int column) {
        return holes != null && holes.contains(new Position(row, column));
    }
    public void setPieceAt(int row, int column, MosaicPiece piece) {
        if (row >= 0 && row < this.rows && column >=0 && column < this.columns) {
            if (piece != null) {
                piece.setPlacement(row, column); // Update the piece's placement position
            }
            pieces[row][column] = piece; // Allows setting to null or a new piece
        }
    }
    public void setHole(int row, int column) {
        holes.add(new Position(row, column));
    }
    public void removeHole(int gameLogicRow, int gameLogicCol) {
        holes.remove(new Position(gameLogicRow, gameLogicCol));
    }


    /**
     * Finds the next empty cell in the field.
     * This method iterates through the field to find the first cell that is empty
     * @return Position of the next empty cell, or null if no empty cell is found.
     */
    public Position findNextEmptyCell() {
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.columns; c++) {
                if (isCellEmpty(r, c) && !isCellHole(r, c)) {
                    return new Position(r, c);
                }
            }
        }
        return null; // No empty cell found
    }

    /**
     * Creates a deep copy of the current field.
     * This method creates a new Field object with the same dimensions, border colors, and holes,
     * and copies the pieces from the current field to the new field.
     * @return A new Field object that is a deep copy of this field.
     */
    public Field deepCopy() {
        Field copy = new Field(this.rows, this.columns, this.borderColors, this.holes);
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.columns; c++) {
                if (pieces[r][c] != null) {
                    copy.pieces[r][c] = this.pieces[r][c];
                }
            }
        }
        return copy;
    }
}

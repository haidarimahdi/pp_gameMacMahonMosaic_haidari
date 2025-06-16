package logic;

/**
 * Represents a position in a 2D grid with row and column indices.
 * This record is immutable and provides a simple way to represent positions.
 *
 * @param row    The row index of the position.
 * @param column The column index of the position.
 */
public record Position(int row, int column) {}

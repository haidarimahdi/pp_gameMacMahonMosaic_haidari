package logic;

/**
 * Represents a specific segment on the puzzle's border.
 * Uses a compile-time safe Direction enum and an integer index.
 *
 * @param side The side of the border (NORTH, EAST, SOUTH, WEST).
 * @param index The 0-based index of the segment along that side.
 */
public record BorderPosition(Direction side, int index) {
}

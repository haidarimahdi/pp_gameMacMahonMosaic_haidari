package logic;

public class MosaicPiece {
    private final String colorPattern;
    private int orientation; // 0, 90, 180, 270 degrees clockwise
    private int row = -1; // Default value indicating not placed
    private int col = -1; // Default value indicating not placed

    public MosaicPiece(String colorPattern, int orientation) {
        this.colorPattern = colorPattern;
        this.orientation = orientation;
    }
    public MosaicPiece(String colorPattern) {
        this(colorPattern, 0);
    }

    public void rotate() {
        orientation = (orientation + 90) % 360;
    }

    /**
     * Returns the color of the specified edge, taking into account the piece's
     * current orientation.
     *
     * @param direction The logical direction (NORTH, EAST, SOUTH, WEST) for which
     * to get the edge color.
     * @return The character representing the color of that edge after rotation.
     */
public char getEdgeColor(Direction direction) {
        int baseIndex = switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
        };
        int actualIndex = (baseIndex + (360 - orientation) / 90) % 4;
        return colorPattern.charAt(actualIndex);
    }

    public String getColorPattern() {
        return colorPattern;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setPlacement(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getPlacementRow() {
        return row;
    }
    public int getPlacementCol() {
        return col;
    }

    public void setOrientation(int rotation) {
        if (rotation % 90 != 0) {
            throw new IllegalArgumentException("Orientation must be a multiple of 90 degrees.");
        }
        this.orientation = (rotation % 360 + 360) % 360; // Normalize to [0, 360)
    }
}

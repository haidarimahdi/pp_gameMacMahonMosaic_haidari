package logic;

public class MosaicPiece {
    private final String colorPattern;
    private int orientation; // 0, 90, 180, 270 degrees clockwise

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
}

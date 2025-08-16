package logic;

public class MosaicPiece {
    private final Color[] colorPattern;
    private int orientation; // 0, 90, 180, 270 degrees clockwise
    private int row = -1; // Default value indicating not placed
    private int col = -1; // Default value indicating not placed
    private static final int EDGE_COUNT = 4; // Number of edges in a piece

    public MosaicPiece(Color[] colorPattern) {
        if (colorPattern.length != EDGE_COUNT) {
            throw new IllegalArgumentException("Color pattern must have exactly 4 colors for the edges.");
        }
        this.colorPattern = colorPattern;
        this.orientation = 0;
    }
//    public MosaicPiece(String colorPattern) {
//        this(colorPattern, 0);
//    }

    /**
     * Constructs a MosaicPiece with a color pattern represented as a string.
     *
     * @param stringPattern The string representation of the color pattern, e.g., "RGYB".
     * @throws IllegalArgumentException if the string pattern does not have exactly 4 characters.
     */
    public MosaicPiece(String stringPattern) {
        this(parsePattern(stringPattern));
    }

    /**
     * Constructs a MosaicPiece with a color pattern and an initial orientation.
     *
     * @param stringPattern The string representation of the color pattern, e.g., "RGYB".
     * @throws IllegalArgumentException if the string pattern does not have exactly 4 characters.
     * @return A new MosaicPiece instance with the specified color pattern.
     */
    private static Color[] parsePattern(String stringPattern) {
        if (stringPattern.length() != EDGE_COUNT) {
            throw new IllegalArgumentException("Color pattern must have exactly 4 characters.");
        }
        Color[] pattern = new Color[EDGE_COUNT];
        for (int i = 0; i < EDGE_COUNT; i++) {
            pattern[i] = Color.fromChar(stringPattern.charAt(i));
        }
        return pattern;
    }

    /**
     * Rotates the piece 90 degrees clockwise.
     * The orientation is updated accordingly.
     */
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
public Color getEdgeColor(Direction direction) {
        int baseIndex = switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
        };
        int actualIndex = (baseIndex + (360 - orientation) / 90) % 4;
        return colorPattern[actualIndex];
    }

    public Color[] getColorPattern() {
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

package logic;

public enum Color {
    RED('R'),
    GREEN('G'),
    YELLOW('Y'),
    HOLE('H'), // Represents edge of a hole in the puzzle
    NONE('N'); // Represents an empty cell or no color, used for empty borders or cells

    private final char colorCode;

    Color(char colorCode) {
        this.colorCode = colorCode;
    }

    public char getChar() {
        return colorCode;
    }

    public static Color fromChar(char c) {
        for (Color color : values()) {
            if (color.colorCode == c) {
                return color;
            }
        }
        throw new IllegalArgumentException("No Color found for character: " + c);
    }
}

package logic;

/** A specific exception for when the puzzle file has an invalid format. */
public class InvalidPuzzleFormatException extends PuzzleLoadException {
    // You could add specific attributes here later, e.g., line number

    public InvalidPuzzleFormatException(String message) {
        super(message);
    }
}
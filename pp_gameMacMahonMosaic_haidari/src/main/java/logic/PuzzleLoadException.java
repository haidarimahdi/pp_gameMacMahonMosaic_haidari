package logic;

/** A general exception for errors that occur while loading a puzzle. */
public class PuzzleLoadException extends Exception {
    public PuzzleLoadException(String message) {
        super(message);
    }

    public PuzzleLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}

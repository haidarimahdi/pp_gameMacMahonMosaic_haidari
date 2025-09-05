package logic;

import java.util.List;
import java.util.Map;

public interface GUIConnector {
    /**
     * Initializes or re-initializes the main game board view with a specific structure.
     * This includes setting up game cells, border cells, and corner cells.
     *
     * @param gameRows      Number of rows for actual game cells.
     * @param gameCols      Number of columns for actual game cells.
     * @param borderColors  A map to specify initial border colors,
     * e.g., key "TOP_0" value "RED", "LEFT_1" value "GREEN".
     * (Key format needs to be defined by logic and understood by GUI).
     */
    void initializeBoardView(int gameRows, int gameCols, Map<BorderPosition, Color> borderColors);

    /**
     * Updates a specific game cell on the board.
     *
     * @param gameRow         0-indexed row of the game cell.
     * @param gameCol         0-indexed column of the game cell.
     * @param pieceId         Identifier for the piece (e.g., "RGYB"), or null if cell is empty.
     * @param rotationDegrees The rotation (0, 90, 180, 270) to apply to the piece's visual.
     * @param isError         True if this piece is currently in an error state.
     */
    void updateGameCell(int gameRow, int gameCol, String pieceId, int rotationDegrees,
                        boolean isError, boolean isHole);

    /**
     * Updates the visual representation of available pieces.
     * @param pieceRepresentationForGUI A list of paths or identifiers for the images of available pieces.
     * Each string could also be a more complex representation if needed.
     */
    void displayAvailablePieces(List<String> pieceRepresentationForGUI);

    /**
     * Clears any currently highlighted cells.
     */
    void clearCellHighlights();

    /**
     * Retrieves the pattern and current rotation of the piece visually selected
     * in the 'available pieces' panel.
     *
     * @return SelectedPieceDataFromPanel containing the pattern and rotation,
     * or null if no piece is currently selected.
     */
    SelectedPieceDataFromPanel getSelectedPieceDataFromPanel();

    /**
     * Instructs the GUI to visually clear the current selection highlight
     * from the 'available pieces' panel and reset any related state.
     */
    void clearSelectionFromPanel();

    /**
     * Instructs the GUI to apply a rotation to the currently selected piece in the
     * 'available pieces' panel, if a piece is selected.
     * @return The new rotation degrees of the piece, or -1 if no piece was selected/rotated.
     */
    double rotateSelectedPieceInPanel();

    /**
     * Indicates if the game is in editor mode, allowing the GUI to change its behavior.
     * @param isEditorMode true if editor mode is active, false otherwise.
     */
    void setEditorMode(boolean isEditorMode);

    /** Enables or disables the rotation button in the GUI.
     * This is useful for both during the game and editor mode where rotation might be necessary.
     * @param enable true to enable the rotation button, false to disable it.
     */
    void enableRotationButton(boolean enable);

    /**
     * Highlights a specific cell in the game board with a color pattern.
     * @param row          The row index of the cell to highlight (0-indexed).
     * @param column       The column index of the cell to highlight (0-indexed).
     * @param colorPattern A string representing the color pattern to apply to the cell.
     */
    void highlightCell(int row, int column, String colorPattern);

    void updateBorderColor(BorderPosition borderPosition, Color newColor);

//    void showAlert(String solvabilityCheckSkipped, String s);

    /**
     * Displays a status message to the user.
     * @param key The key for the message in the properties file.
     * @param args Optional arguments to format into the message.
     */
    void showStatusMessage(String key, Object... args);

    /**
     * Announces a game-ending event.
     * @param titleKey   The key for the title.
     * @param messageKey The key for the detailed message.
     * @param args       Optional arguments for the message.
     */
    void showGameEndMessage(String titleKey, String messageKey, Object... args);

    /**
     * Shows a generic alert.
     * @param titleKey The key for the alert title/header.
     * @param bodyKey  The key for the alert body text.
     * @param args     Optional arguments for the body text.
     */
    void showAlert(String titleKey, String bodyKey, Object... args);
}

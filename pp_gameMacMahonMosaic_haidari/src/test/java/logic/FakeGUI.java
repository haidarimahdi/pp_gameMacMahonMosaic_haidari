package logic;

import java.util.List;
import java.util.Map;

/**
 * A Fake GUI implementation of GUIConnector for testing game logic
 * without a real JavaFX user interface.
 * All methods are implemented as no-ops (do nothing) or with simple logging.
 */
public class FakeGUI implements GUIConnector{
    @Override
    public void initializeBoardView(int gameRows, int gameCols, Map<String, String> borderColors) {
        System.out.println("FakeGUI: initializeBoardView called with " + gameRows + "x" + gameCols);
    }

    @Override
    public void updateGameCell(int gameRow, int gameCol, String pieceId, String pieceImagePath, int rotationDegrees, boolean isError) {
        System.out.println("FakeGUI: updateGameCell called for (" + gameRow + "," + gameCol + ") with piece: "
                + pieceId + ", error: " + isError);
    }

    @Override
    public void displayAvailablePieces(List<String> pieceRepresentationForGUI) {
        System.out.println("FakeGUI: displayAvailablePieces called with " + pieceRepresentationForGUI.size() + " pieces.");
    }

    @Override
    public void showStatusMessage(String message) {
         System.out.println("FakeGUI: showStatusMessage: " + message);
    }

    @Override
    public void showGameEndMessage(String title, String message) {
         System.out.println("FakeGUI: showGameEndMessage: Title='" + title + "', Message='" + message + "'");
    }

    @Override
    public void highlightGameCells(List<Object> coordinates) {
         System.out.println("FakeGUI: highlightGameCells called for coordinates: " + coordinates);
    }

    @Override
    public void clearCellHighlights() {
         System.out.println("FakeGUI: clearCellHighlights called.");
    }

    @Override
    public SelectedPieceDataFromPanel getSelectedPieceDataFromPanel() {
        return null;
    }

    @Override
    public void clearSelectionFromPanel() {

    }

    @Override
    public double rotateSelectedPieceInPanel() {
        return 0;
    }

    @Override
    public void setEditorMode(boolean isEditorMode) {
         System.out.println("FakeGUI: setEditorMode called with: " + isEditorMode);
    }

    @Override
    public void enableRotationButton(boolean enable) {
         System.out.println("FakeGUI: enableRotationButton called with: " + enable);

    }
}

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
    public void initializeBoardView(int gameRows, int gameCols, Map<BorderPosition, Color> borderColors) {
        System.out.println("FakeGUI: initializeBoardView called with " + gameRows + "x" + gameCols);
    }

    @Override
    public void updateGameCell(int gameRow, int gameCol, String pieceId, int rotationDegrees,
                               boolean isError, boolean isHole) {
        System.out.println("FakeGUI: updateGameCell called for (" + gameRow + "," + gameCol + ") with piece: "
                + pieceId + ", error: " + isError + ", isHole: " + isHole);
    }

    @Override
    public void displayAvailablePieces(List<String> pieceRepresentationForGUI) {
        System.out.println("FakeGUI: displayAvailablePieces called with " + pieceRepresentationForGUI.size() + " pieces.");
    }

//    @Override
//    public void highlightGameCells(List<Object> coordinates) {
//         System.out.println("FakeGUI: highlightGameCells called for coordinates: " + coordinates);
//    }

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

    @Override
    public void highlightCell(int row, int column, String colorPattern) {
            System.out.println("FakeGUI: highlightCell called for cell (" + row + "," + column +
                    ") with color pattern: " + colorPattern);
    }

    @Override
    public void updateBorderColor(BorderPosition borderPosition, Color newColor) {
        System.out.println("FakeGUI: updateBorderColor called for borderKey: " + borderPosition + " with new color: " + newColor);
    }

    @Override
    public void showStatusMessage(String key, Object... args) {
        System.out.println("FakeGUI: showStatusMessage called with key: " + key + " args: " + List.of(args));

    }

    @Override
    public void showGameEndMessage(String titleKey, String messageKey, Object... args) {
        System.out.println("FakeGUI: showGameEndMessage called with titleKey: " + titleKey +
                ", messageKey: " + messageKey + ", args: " + List.of(args));

    }

    @Override
    public void showAlert(String titleKey, String bodyKey, Object... args) {
        System.out.println("FakeGUI: showAlert called with titleKey: " + titleKey +
                ", bodyKey: " + bodyKey + ", args: " + List.of(args));

    }
}

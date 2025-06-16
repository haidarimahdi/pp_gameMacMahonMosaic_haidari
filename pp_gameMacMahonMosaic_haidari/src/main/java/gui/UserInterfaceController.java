package gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import logic.GUIConnector;
import logic.Game;
import logic.MosaicPiece;
import logic.SelectedPieceDataFromPanel;

import java.net.URL;
import java.util.*;

/**
 * Main class for the user interface.
 *
 * @author mjo, cei
 */
public class UserInterfaceController implements Initializable {


    // --- FXML Injected Fields ---
    @FXML
    private Button hintButton;

    // Right panel for available pieces
    @FXML
    private TilePane availablePiecesPane;
    @FXML
    private ScrollPane availablePiecesScrollPane;
    @FXML
    private Button rotateSelectedPieceButton;

    // Center GridPane for the board
    @FXML private StackPane boardWrapperPane;

    @FXML
    private GridPane boardGridPane;

    // MenuBar and MenuItems
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem menuCheckSolvability;
    @FXML
    private CheckMenuItem menuEditorMode;
    @FXML
    private MenuItem menuExit;
    @FXML
    private MenuItem menuLoad;
    @FXML
    private MenuItem menuRestart;
    @FXML
    private MenuItem menuSave;
    @FXML
    private MenuItem menuSolutionHint;

    // Bottom status label
    @FXML
    private Label statusLabel;

    // Default dimensions for the game board
//    private int currentNumCols = 3;
//    private int currentNumRows = 4;

    // Constants for initial setup
    private static final int INITIAL_GAME_ROWS = 4;
    private static final int INITIAL_GAME_COLUMNS = 3;
    private static final double BORDER_PROPORTION_IN_CONTROLLER = 0.25; // For adjustGridPaneSize
    private boolean isResizing = false;


    private Game game;
    private GUIConnector guiConnector;


    /**
     * This method is called when the application is initialized. It sets up the
     * game field and populates the available pieces.
     * It also sets up the listeners for resizing the game board and the available pieces.
     *
     * @param location  probably not used
     * @param resources probably not used
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        this.guiConnector = new JavaFXGUI(
                this.boardGridPane,
                this.availablePiecesPane,
                this.availablePiecesScrollPane,
                this.rotateSelectedPieceButton,
                this.statusLabel
        );

        this.game = new Game(this.guiConnector);
        // Initialize the game via Game class
        // This will trigger calls to guiConnector.initializeBoardView(), etc.
        game.startGame(INITIAL_GAME_ROWS, INITIAL_GAME_COLUMNS);

        if (boardGridPane != null) {
            boardGridPane.setOnMouseClicked(event -> {
                Node clickedTarget = (Node) event.getTarget(); // Get the deepest node clicked
                Node cellNode = getClickedCellNode(clickedTarget, boardGridPane); // Use helper method

                if (cellNode == null) {
                    System.out.println("UserInterfaceController: Click on board background or unresolvable cell. No action.");
                    return; // Exit if no valid cell node was found
                }

                Integer guiRow = GridPane.getRowIndex(cellNode);
                Integer guiCol = GridPane.getColumnIndex(cellNode);

                if (guiRow == null || guiCol == null) {
                    System.out.println("UserInterfaceController: Could not determine GridPane row/col for cell: " + cellNode);
                    return; // Exit if row/col cannot be determined
                }

                // Log the identified cell (this is your existing useful log)
                System.out.println("UserInterfaceController: boardGridPane DELEGATED click. GUI Cell (" + guiRow + "," + guiCol + ")");

                // Ensure game and gameField are ready for dimension checks
                if (game == null || game.getGameField() == null) {
                    System.err.println("UserInterfaceController: Game or GameField not initialized. Cannot process click further.");
                    return;
                }

                // Check if the click was on an actual game cell (not border/corner)
                boolean isGameCell = guiRow > 0 && guiRow <= game.getGameField().getRows() &&
                        guiCol > 0 && guiCol <= game.getGameField().getColumns();

                if (isGameCell) {
                    handleBoardCellClickedLogic(guiRow, guiCol); // Call your existing detailed logic handler
                } else {
                    System.out.println("UserInterfaceController: Delegated click was on a border/corner cell GUI (" +
                            guiRow + "," + guiCol + "). No game action.");
                }
            });
            System.out.println("UserInterfaceController: Attached single event delegator to boardGridPane.");
        } else {
            System.err.println("UserInterfaceController: boardGridPane is null, cannot attach event delegator.");
        }


        addResizingListeners();
        rotateSelectedPieceButton.setDisable(true);
        System.out.println("UserInterfaceController initialized. Game start triggered.");
    }

    /**
     * Traverses up from the event's target node to find the direct child node
     * of the specified GridPane that represents the clicked cell.
     *
     * @param eventTarget The node that was the direct target of the mouse event.
     * @param gridPane    The GridPane container whose direct child we are seeking.
     * @return The cell Node (direct child of gridPane) if found, otherwise null.
     */
    private Node getClickedCellNode(Node eventTarget, GridPane gridPane) {
        while (eventTarget != null) {
            // If the parent of the current node is the gridPane, we've found the cell node
            if (eventTarget.getParent() == gridPane) return eventTarget;
            // If we've reached the gridPane itself, the click was not on a cell
            if (eventTarget == gridPane) return null;
            // Move up to the parent node
            eventTarget = eventTarget.getParent();
        }

        return null; // If we exit the loop, no cell node was found
    }

    private void handleBoardCellClickedLogic(int guiClickedRow, int guiClickedCol) {
        System.out.println("UserInterfaceController: handleBoardCellClickedLogic INVOKED for GUI cell (" + guiClickedRow + "," + guiClickedCol + ")");

        if (guiConnector == null || game == null || game.getGameField() == null) {
            System.err.println("UserInterfaceController: Essential components (guiConnector, game, or gameField) are null. Cannot handle board click.");
            return;
        }

        // Convert GUI 1-based index to game logic 0-based index
        int gameLogicRow = guiClickedRow - 1;
        int gameLogicCol = guiClickedCol - 1;

        // Basic bounds check for the logical coordinates
        if (gameLogicRow < 0 || gameLogicRow >= game.getGameField().getRows() ||
                gameLogicCol < 0 || gameLogicCol >= game.getGameField().getColumns()) {
            System.err.println("UserInterfaceController: Clicked cell GUI(" + guiClickedRow + "," + guiClickedCol +
                    ") is outside valid game board bounds.");

            return;
        }

        SelectedPieceDataFromPanel selectedDataFromPanel = guiConnector.getSelectedPieceDataFromPanel();

        System.out.println("Controller: selectedDataFromPanel is " + (selectedDataFromPanel == null ? "null" : "NOT null (Pattern: " + selectedDataFromPanel.pattern() + ")"));

        if (selectedDataFromPanel != null) {
            System.out.println("Controller: Entering PLACEMENT logic.");

            // --- A piece IS selected from the panel: Attempt to PLACE it ---
            MosaicPiece pieceToPlace = new MosaicPiece(selectedDataFromPanel.pattern());
            int numRotations = (int) (Math.round(selectedDataFromPanel.rotationDegrees()) / 90.0);
            for (int i = 0; i < numRotations; i++) {
                pieceToPlace.rotate();
            }

            System.out.println("Controller: Piece selected from panel. Attempting to place " +
                    pieceToPlace.getColorPattern() + " (Orientation: " + pieceToPlace.getOrientation() +
                    "°) at game cell (" + gameLogicRow + "," + gameLogicCol + ")");
            game.attemptPlacePiece(pieceToPlace, gameLogicRow, gameLogicCol); // This method will check if the cell is empty

        } else {
            // --- NO piece selected from the panel: Check if we should attempt to REMOVE a piece ---
            // Only attempt to remove if the clicked cell is actually occupied (and not a hole)
            if (!game.getGameField().isCellEmpty(gameLogicRow, gameLogicCol) &&
                    !game.getGameField().isCellHole(gameLogicRow, gameLogicCol)) {

                System.out.println("Controller: No piece selected from panel. Clicked occupied cell (" +
                        gameLogicRow + "," + gameLogicCol + "). Attempting to remove piece.");
                game.removePieceFromField(gameLogicRow, gameLogicCol);

            } else {
                // No piece selected from panel, and the clicked cell is empty or a hole.
                // The user's intention is that clicking an empty cell here does nothing specific related to removal.
                System.out.println("Controller: No piece selected from panel. Clicked cell (" +
                        gameLogicRow + "," + gameLogicCol + ") is " +
                        (game.getGameField().isCellHole(gameLogicRow, gameLogicCol) ? "a hole." : "empty.") +
                        " No removal action taken.");
                if (game.getGameField().isCellHole(gameLogicRow, gameLogicCol)){
                    guiConnector.showStatusMessage("This is a hole. Pieces cannot be placed or removed here.");
                } else {
                     guiConnector.showStatusMessage("Cell is empty. Select a piece to place.");
                }
            }
        }
    }

    private void addResizingListeners() {
        if (boardWrapperPane == null) {
            System.err.println("CRITICAL: boardWrapperPane is null. Resizing will fail.");
            return;
        }
        // Listeners on the wrapper pane (analogous to example's fieldPane)
        boardWrapperPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && boardWrapperPane.getHeight() > 0) {
                adjustBoardGridPaneSize(newVal.doubleValue(), boardWrapperPane.getHeight());
            }
        });
        boardWrapperPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && boardWrapperPane.getWidth() > 0) {
                adjustBoardGridPaneSize(boardWrapperPane.getWidth(), newVal.doubleValue());
            }
        });
    }
    private static final double ACTUAL_BORDER_THICKNESS_PIXELS = 25.0;
    /**
     * Adapts the example's adjustGridPaneSize method.
     * Sets boardGridPane's pref and max size so its "effective" game cells can be square.
     * The "effective" count considers borders as 1/4 of a game cell each.
     *
     * @param wrapperWidth  Available width from the parent container (boardWrapperPane).
     * @param wrapperHeight Available height from the parent container (boardWrapperPane).
     */
    private void adjustBoardGridPaneSize(double wrapperWidth, double wrapperHeight) {
        if (isResizing) return;
        isResizing = true;

        try {
            if (boardGridPane == null || boardGridPane.getColumnConstraints().isEmpty() || boardGridPane.getRowConstraints().isEmpty() ||
                    wrapperWidth <= 0 || wrapperHeight <= 0) {
                isResizing = false;
                return;
            }

            final int gameCols = INITIAL_GAME_COLUMNS; // Get from game logic if dynamic
            final int gameRows = INITIAL_GAME_ROWS;   // Get from game logic if dynamic

            // Effective number of "full game cell equivalents" for width and height
            // This uses BORDER_PROPORTION_IN_CONTROLLER (0.25)
            final double effectiveColsForSizing = gameCols + (2 * BORDER_PROPORTION_IN_CONTROLLER);
            final double effectiveRowsForSizing = gameRows + (2 * BORDER_PROPORTION_IN_CONTROLLER);

            // Padding and gaps of the boardGridPane itself
            double hPaddingGrid = boardGridPane.getPadding().getLeft() + boardGridPane.getPadding().getRight();
            double vPaddingGrid = boardGridPane.getPadding().getTop() + boardGridPane.getPadding().getBottom();
            double hGapsGrid = boardGridPane.getHgap() * (boardGridPane.getColumnConstraints().size() - 1);
            double vGapsGrid = boardGridPane.getVgap() * (boardGridPane.getRowConstraints().size() - 1);

            // Usable space within the wrapper for boardGridPane's content area (cells + internal gaps)
            double cellSpaceWidth = wrapperWidth - hPaddingGrid;
            double cellSpaceHeight = wrapperHeight - vPaddingGrid;

            if (cellSpaceWidth <= hGapsGrid || cellSpaceHeight <= vGapsGrid) {
                isResizing = false;
                return; // Not enough space
            }

            // Calculate the size of one "effective full game cell unit"
            // This unit size will apply to game cells (1 unit) and border cells (0.25 unit)
            // due to the percentage constraints set in JavaFXGUI.
            double effectiveUnitSize = Math.min(
                    (cellSpaceWidth - hGapsGrid) / effectiveColsForSizing,
                    (cellSpaceHeight - vGapsGrid) / effectiveRowsForSizing
            );

            if (effectiveUnitSize < 1.0) effectiveUnitSize = 1.0;

            // Target size for the boardGridPane (including its own padding and internal gaps)
            double targetBoardWidth = (effectiveUnitSize * effectiveColsForSizing) + hGapsGrid + hPaddingGrid;
            double targetBoardHeight = (effectiveUnitSize * effectiveRowsForSizing) + vGapsGrid + vPaddingGrid;

            // --- Critical for Shrinking ---
            boardGridPane.setMinSize(0, 0); // Force allow shrinking

            boardGridPane.setPrefSize(targetBoardWidth, targetBoardHeight);
            boardGridPane.setMaxSize(targetBoardWidth, targetBoardHeight); // Cap growth

            // --- Resize the actual game cell content ---
            // The size of a visual game cell will be 1 * effectiveUnitSize
            double actualGameCellVisualSize = effectiveUnitSize;

            for (Node node : boardGridPane.getChildren()) {
                if (node instanceof Region) {
                    Region region = (Region) node;
                    Integer rowIndex = GridPane.getRowIndex(node);
                    Integer colIndex = GridPane.getColumnIndex(node);

                    if (rowIndex == null || colIndex == null) continue;

                    // Game Cells are at GUI indices (1..gameRows) and (1..gameCols)
                    if (rowIndex > 0 && rowIndex <= gameRows && colIndex > 0 && colIndex <= gameCols) {
                        region.setMinSize(actualGameCellVisualSize, actualGameCellVisualSize);
                        region.setPrefSize(actualGameCellVisualSize, actualGameCellVisualSize);
                        region.setMaxSize(actualGameCellVisualSize, actualGameCellVisualSize);
                    }
                    // Border cells (the Panes created in JavaFXGUI) will automatically size
                    // to fill the space allocated by their percentage constraints.
                    // Their thickness will be BORDER_PROPORTION_FOR_CONSTRAINTS * effectiveUnitSize.
                }
            }
        } finally {
            isResizing = false;
        }
    }

    @FXML
    void handleRotateSelectedPiece(ActionEvent event) {
//        ImageView selectedView = null;
//        for (Node node : availablePiecesPane.getChildren()) {
//            if (node instanceof ImageView && node.getStyle() != null && node.getStyle().contains("dropshadow")) {
//                selectedView = (ImageView) node;
//                break;
//            } else if (node instanceof StackPane && node.getStyle() != null && node.getStyle().contains("dropshadow")){
//                // If your available pieces are StackPanes with ImageViews inside,
//                // you might need to find the ImageView within the StackPane to rotate it.
//                // For now, assuming ImageView itself gets the style.
//                // Or rotate the StackPane if that's the intended visual unit.
//                // Let's assume we rotate the node that received the style.
//                selectedView = (ImageView) ((StackPane)node).getChildren().stream().filter(ImageView.class::isInstance).findFirst().orElse(null);
//                if(selectedView == null && node instanceof StackPane) { // If only StackPane, rotate it
//                    node.setRotate((node.getRotate() + 90) % 360);
//                    if (guiConnector != null) guiConnector.showStatusMessage("Piece rotated visually.");
//                    return;
//                }
//
//            }
//        }
//
//        if (selectedView != null) {
//            Object userData = selectedView.getUserData();
//            String pieceRep = (userData instanceof String) ? (String) userData : "Unknown";
//
//            selectedView.setRotate((selectedView.getRotate() + 90) % 360);
//            String msg = "Rotated " + pieceRep + " to " + selectedView.getRotate() + "°";
//            System.out.println(msg);
//            if (guiConnector != null) guiConnector.showStatusMessage(msg);
//        } else {
//            if (guiConnector != null) guiConnector.showStatusMessage("No piece selected to rotate.");
//        }
        if (guiConnector != null) {
            double newRotation = guiConnector.rotateSelectedPieceInPanel();
            // Status message is handled by JavaFXGUI's implementation of rotateSelectedPieceInPanel
            if (newRotation == -1) {
                 guiConnector.showStatusMessage("No piece selected to rotate."); // Already handled
            }
        }
    }

    @FXML void handleRestartPuzzle(ActionEvent event) { if (game != null) game.restartGame(); }
    @FXML void handleExitGame(ActionEvent event) { Platform.exit(); }

    @FXML void handleSaveGame(ActionEvent event) { if (guiConnector != null) guiConnector.showStatusMessage("Save action...");}
    @FXML void handleLoadGame(ActionEvent event) { if (guiConnector != null) guiConnector.showStatusMessage("Load action...");}
    @FXML void handleToggleEditorMode(ActionEvent event) { if (guiConnector != null) guiConnector.setEditorMode(menuEditorMode.isSelected());}
    @FXML void handleCheckSolvability(ActionEvent event) {
        if (game != null) {
            if (guiConnector != null) guiConnector.showStatusMessage("Check Solvability...");

            boolean isSolvable = game.isPuzzleSolvable();
            String message = isSolvable ? "This puzzle is solvable from the current state!"
                    : "This puzzle is not solvable from the current state. You may need to remove some pieces.";

            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.setTitle("Puzzle Solvability Check");
            alert.setHeaderText(null); // No header text
            alert.showAndWait();

        } else {
            guiConnector.showStatusMessage("Game not initialized. Cannot check solvability.");
        }
    }


    @FXML void handleGetSolutionHint(ActionEvent event) { if (guiConnector != null) guiConnector.showStatusMessage("Get Hint...");}

    @FXML void handleHintButtonAction(ActionEvent actionEvent) {
        if (game != null) {
            if (guiConnector != null) guiConnector.showStatusMessage("Hint button clicked. Processing hint...");
            game.provideHint();

        } else {
            guiConnector.showStatusMessage("Game not initialized. Cannot provide a hint.");
        }
    }
}
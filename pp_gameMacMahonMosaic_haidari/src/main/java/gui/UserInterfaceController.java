package gui;

import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Pair;

import logic.BorderPosition;
import logic.Color;
import logic.Game;
import logic.SelectedPieceDataFromPanel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * Main class for the user interface.
 *
 * @author mjo, cei, mahdi
 */
public class UserInterfaceController implements Initializable {



    // ================== FXML Injected Fields ==================
    @FXML
    private Button hintButton;
    @FXML
    private TilePane availablePiecesPane;
    @FXML
    private ScrollPane availablePiecesScrollPane;
    @FXML
    private Button rotateSelectedPieceButton;
    @FXML
    private StackPane boardWrapperPane;
    @FXML
    private GridPane boardGridPane;
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem menuCheckSolvability;
    @FXML
    private CheckMenuItem menuEditorMode;
    @FXML
    private MenuItem menuClearBoard;
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
    @FXML
    private Label statusLabel;

    private ButtonType editCurrentPuzzleBtn, createNewPuzzleBtn;

    private static final double BORDER_PROPORTION_IN_CONTROLLER = 0.25; // For adjustGridPaneSize
    private boolean isResizing = false;
    boolean isPuzzleConfigured;

    private Game game;
    private JavaFXGUI gui;


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

        this.gui = new JavaFXGUI(
                this.boardGridPane,
                this.availablePiecesPane,
                this.rotateSelectedPieceButton,
                this.statusLabel
        );

        this.game = new Game(this.gui);

        try {
            File puzzleFile = new File("src/main/resources/logic/json/defaultPuzzleField.json");
            game.loadGameFromFile(puzzleFile);
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
            gui.showStatusMessage("CRITICAL ERROR: Could not load default puzzle." + e.getMessage());
        }

        updateUIForGameMode(!game.isEditorMode());
        menuEditorMode.setSelected(game.isEditorMode());
        hintButton.setDisable(game.isEditorMode());
        addResizingListeners();

        boardGridPane.setOnMouseClicked(this::handleBoardClick);


//        if (boardGridPane != null) {
//            boardGridPane.setOnMouseClicked(event -> {
//                if (game.isEditorMode()) {
//                    Node clickedNode = getClickedCellNode((Node) event.getTarget(), boardGridPane);
//                    if (clickedNode == null) return;
//
//                    Integer guiRow = GridPane.getRowIndex(clickedNode);
//                    Integer guiCol = GridPane.getColumnIndex(clickedNode);
//                    if (guiRow == null || guiCol == null) return;
//                    if (event.getButton() == MouseButton.SECONDARY) {
//                        boolean isGameCell = guiRow > 0 && guiRow <= game.getGameField().getRows() &&
//                                guiCol > 0 && guiCol <= game.getGameField().getColumns();
//                        if (isGameCell) {
//                            // Right-clicked a game cell -> toggle hole state
//                            int gameLogicRow = guiRow - 1;
//                            int gameLogicCol = guiCol - 1;
//                            game.toggleHoleState(gameLogicRow, gameLogicCol);
//                        } else {
//                            BorderPosition borderPosition = determineBorderKeyFromGuiCoords(guiRow, guiCol);
//                            if (borderPosition != null) {
//                                // Right-clicked a border cell -> toggle border color
//                                Color newColor = showColorChoiceDialog();
//                                if (newColor != null) {
//                                    game.setEditorBorderColor(borderPosition, newColor);
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    Node clickedTarget = (Node) event.getTarget(); // Get the deepest node clicked
//                    Node cellNode = getClickedCellNode(clickedTarget, boardGridPane); // Use helper method
//
//                    if (cellNode == null) {
//                        return; // Exit if no valid cell node was found
//                    }
//
//                    Integer guiRow = GridPane.getRowIndex(cellNode);
//                    Integer guiCol = GridPane.getColumnIndex(cellNode);
//
//                    if (guiRow == null || guiCol == null) {
//                        return; // Exit if row/col cannot be determined
//                    }
//
//                    // Ensure game and gameField are ready for dimension checks
//                    if (game == null || game.getGameField() == null) {
//                        System.err.println("UserInterfaceController: Game or GameField not initialized. Cannot process click further.");
//                        return;
//                    }
//
//                    // Check if the click was on an actual game cell (not border/corner)
//                    boolean isGameCell = guiRow > 0 && guiRow <= game.getGameField().getRows() &&
//                            guiCol > 0 && guiCol <= game.getGameField().getColumns();
//
//                    if (isGameCell) {
//                        handleBoardCellClickedLogic(guiRow, guiCol); // Call your existing detailed logic handler
//                    }
//
//                }
//            });
//        } else {
//            System.err.println("UserInterfaceController: boardGridPane is null, cannot attach event delegator.");
//        }

        rotateSelectedPieceButton.setDisable(true);
    }

    /**
     * Centralized method to update UI controls based on the game mode.
     *
     * @param isGameMode true if the game is in play mode, false for editor mode.
     */
    private void updateUIForGameMode(boolean isGameMode) {
        menuEditorMode.setSelected(!isGameMode);
        hintButton.setDisable(!isGameMode);
        availablePiecesScrollPane.setDisable(!isGameMode);
        menuCheckSolvability.setDisable(!isGameMode);
        menuSolutionHint.setDisable(!isGameMode);
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

    /**
     * Main handler for all clicks on the board. Delegates to mode-specific handlers.
     */
    private void handleBoardClick(MouseEvent event) {
        if (game == null || game.getGameField() == null) {
            System.err.println("UserInterfaceController: Game not initialized. Cannot process click.");
            return;
        }

        Node clickedNode = getClickedCellNode((Node) event.getTarget(), boardGridPane);
        if (clickedNode == null) return;

        Integer guiRow = GridPane.getRowIndex(clickedNode);
        Integer guiCol = GridPane.getColumnIndex(clickedNode);
        if (guiRow == null || guiCol == null) return;

        if (game.isEditorMode()) {
            if (event.getButton() == MouseButton.SECONDARY) {
                handleEditorRightClick(guiRow, guiCol);
            }
        } else {
            handleGameModeClick(clickedNode);
        }
    }

    /**
     * Handles clicks on the board when in Game Mode.
     */
    private void handleGameModeClick(Node clickedNode) {
        Integer guiRow = GridPane.getRowIndex(clickedNode);
        Integer guiCol = GridPane.getColumnIndex(clickedNode);

        if (guiRow == null || guiCol == null) return;

        boolean isGameCell = guiRow > 0 && guiRow <= game.getGameField().getRows() &&
                guiCol > 0 && guiCol <= game.getGameField().getColumns();

        if (isGameCell) {
            handleBoardCellClickedLogic(guiRow, guiCol);
        }
    }

    /**
     * Core logic for handling clicks on game cells in Game Mode.
     * This method assumes the clicked cell is a valid game cell within bounds.
     *
     * @param guiClickedRow The 1-based row index from the GUI.
     * @param guiClickedCol The 1-based column index from the GUI.
     */
    private void handleBoardCellClickedLogic(int guiClickedRow, int guiClickedCol) {

        if (gui == null || game == null || game.getGameField() == null) {
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

        SelectedPieceDataFromPanel selectedDataFromPanel = gui.getSelectedPieceDataFromPanel();


        if (selectedDataFromPanel != null) {
            game.attemptPlacePieceFromPanel(
                    selectedDataFromPanel.pattern(),
                    selectedDataFromPanel.rotationDegrees(),
                    gameLogicRow,
                    gameLogicCol
            );
        } else if (!game.getGameField().isCellEmpty(gameLogicRow, gameLogicCol) &&
                !game.getGameField().isCellHole(gameLogicRow, gameLogicCol)) {
            game.removePieceFromField(gameLogicRow, gameLogicCol);
        } else {
            String message = game.getGameField().isCellHole(gameLogicRow, gameLogicCol)
                    ? "This is a hole. Pieces cannot be placed or removed here."
                    : "Cell is empty. Select a piece to place.";
            gui.showStatusMessage(message);
        }
    }

    /**
     * Handles clicks on the board when in Editor Mode (right-clicks).
     */
    private void handleEditorRightClick(int guiRow, int guiCol) {
        boolean isGameCell = guiRow > 0 && guiRow <= game.getGameField().getRows() &&
                guiCol > 0 && guiCol <= game.getGameField().getColumns();

        if (isGameCell) {
            game.toggleHoleState(guiRow - 1, guiCol - 1);
        } else {
            BorderPosition borderPosition = Game.getBorderPositionForCoords(guiRow, guiCol,
                    game.getGameField().getRows(), game.getGameField().getColumns());
            if (borderPosition != null) {
                Color newColor = showColorChoiceDialog();
                if (newColor != null) {
                    game.setEditorBorderColor(borderPosition, newColor);
                }
            }
        }
    }

    private Color showColorChoiceDialog() {
        List<Color> colorOptions = Arrays.asList(Color.RED, Color.GREEN, Color.YELLOW);
        ChoiceDialog<Color> dialog = new ChoiceDialog<>(colorOptions.get(0), colorOptions);
        dialog.setTitle("Choose Border Color");
        dialog.setHeaderText("Select a color for the border cell:");
        dialog.setContentText("Color:");
        Optional<Color> result = dialog.showAndWait();

        return result.orElse(null); // Return the selected color or null if cancelled
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
            // If the parent of the current node is the gridPane, the cell node is found
            if (eventTarget.getParent() == gridPane) return eventTarget;
            // If the gridPane itself is reached, the click was not on a cell
            if (eventTarget == gridPane) return null;
            // Move up to the parent node
            eventTarget = eventTarget.getParent();
        }

        return null; // When the loop is exited => no cell node was found
    }


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

            final int gameCols = game.getGameField().getColumns();
            final int gameRows = game.getGameField().getRows();

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
            boardGridPane.setMinSize(200, 300);
            boardGridPane.setPrefSize(targetBoardWidth, targetBoardHeight);
            boardGridPane.setMaxSize(targetBoardWidth, targetBoardHeight); // Cap growth

            // --- Resize the actual game cell content ---
            // The size of a visual game cell will be 1 * effectiveUnitSize
            double actualGameCellVisualSize = effectiveUnitSize;

            if (gui != null) {
                gui.setCurrentCellSize(actualGameCellVisualSize);
            }

            for (Node node : boardGridPane.getChildren()) {
                if (node instanceof Region region) {
                    Integer rowIndex = GridPane.getRowIndex(node);
                    Integer colIndex = GridPane.getColumnIndex(node);

                    if (rowIndex == null || colIndex == null) continue;

                    // Game Cells are at GUI indices (1..gameRows) and (1..gameCols)
                    if (rowIndex > 0 && rowIndex <= gameRows && colIndex > 0 && colIndex <= gameCols) {
                        region.setMinSize(actualGameCellVisualSize, actualGameCellVisualSize);
                        region.setPrefSize(actualGameCellVisualSize, actualGameCellVisualSize);
                        region.setMaxSize(actualGameCellVisualSize, actualGameCellVisualSize);
                    }
                }
            }
        } finally {
            isResizing = false;
        }
    }

    private void showBoardConfigurationDialog() {
        isPuzzleConfigured = false;
        Dialog<Pair<Integer, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Configure Puzzle Board");
        dialog.setHeaderText("Enter the number of rows and columns for the puzzle board:");

        // Set the button types
        ButtonType congfigureButtonType = new ButtonType("Configure", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(congfigureButtonType, ButtonType.CANCEL);

        // Create the layout for the dialog
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));

        TextField rowsField = new TextField();
        rowsField.setPromptText("Rows (2-6)");
        TextField colsField = new TextField();
        colsField.setPromptText("Columns (2-6)");

        gridPane.add(new Label("Rows:"), 0, 0);
        gridPane.add(rowsField, 1, 0);
        gridPane.add(new Label("Columns:"), 0, 1);
        gridPane.add(colsField, 1, 1);

        dialog.getDialogPane().setContent(gridPane);

        // Request focus on the rows field by default
        Platform.runLater(rowsField::requestFocus);

        // Convert the result to a Pair of integers when the configure button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == congfigureButtonType) {
                try {
                    int rows = Integer.parseInt(rowsField.getText());
                    int cols = Integer.parseInt(colsField.getText());

                    if (rows >= 2 && rows <= 6 && cols >= 2 && cols <= 6) {
                        return new Pair<>(rows, cols);
                    } else {
                        // Handle invalid dimensions
                        Alert alert = new Alert(Alert.AlertType.ERROR,
                                "Rows and columns must be between 2 and 6.",ButtonType.OK);
                        alert.setTitle("Invalid Dimensions");
                        alert.showAndWait();
                        return null; // Cancelled or invalid input
                    }
                } catch (NumberFormatException e) {
                    // Handle invalid input
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "Please enter valid integers for rows and columns.", ButtonType.OK);
                    alert.setTitle("Invalid Input");
                    alert.showAndWait();
                }
            }
            return null; // Cancelled or invalid input
        });

        Optional<Pair<Integer, Integer>> result = dialog.showAndWait();

        result.ifPresent(dimensions -> {
            int newRows = dimensions.getKey();
            int newCols = dimensions.getValue();

            isPuzzleConfigured = true;

            game.startEditor(newRows, newCols); // Start editor mode with new dimensions

            Platform.runLater(() -> {
                // After starting the editor, adjust the board size
                if (boardWrapperPane != null) {
                    adjustBoardGridPaneSize(boardWrapperPane.getWidth(), boardWrapperPane.getHeight());
                }
            });
        });
    }

    private enum ConfirmationResult {
        SAVE, DONT_SAVE, CANCEL
    }

    private ConfirmationResult showUnsavedChangesDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText(content);

        ButtonType buttonTypeSave = new ButtonType("Save");
        ButtonType buttonTypeDontSave = new ButtonType("Don't Save");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeSave, buttonTypeDontSave, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == buttonTypeSave) {
                return ConfirmationResult.SAVE;
            } else if (result.get() == buttonTypeDontSave) {
                return ConfirmationResult.DONT_SAVE;
            }
        }
        return ConfirmationResult.CANCEL;
    }

    @FXML
    void handleRotateSelectedPiece(ActionEvent event) {
        if (gui != null) {
            double newRotation = gui.rotateSelectedPieceInPanel();
            // Status message is handled by JavaFXGUI's implementation of rotateSelectedPieceInPanel
            if (newRotation == -1) {
                 gui.showStatusMessage("No piece selected to rotate."); // Already handled
            }
        }
    }

    @FXML void handleRestartPuzzle() {
        if (game != null) {
            if (game.isDirty() && !game.isEditorMode()) {
                ConfirmationResult result = showUnsavedChangesDialog(
                        "Restart Puzzle",
                        "Do you want to save your changes before restarting?");
                switch (result) {
                    case SAVE:
                        handleSaveGame(); // Save the game
                        game.restartGame(); // Restart after saving
                        break;
                    case DONT_SAVE:
                        game.restartGame();
                        break;
                    case CANCEL:
                        break; // User cancelled the action
                }
            } else {
                game.restartGame();
            }

            Platform.runLater(() -> {
                menuEditorMode.setSelected(game.isEditorMode());
                hintButton.setDisable(game.isEditorMode());
                availablePiecesScrollPane.setDisable(game.isEditorMode());
                if (boardWrapperPane != null) {
                    adjustBoardGridPaneSize(boardWrapperPane.getWidth(), boardWrapperPane.getHeight());
                }
            });
        }
    }

    @FXML void handleExitGame() {
        if (game.isDirty()) {
            ConfirmationResult result = showUnsavedChangesDialog(
                    "Exit Game",
                    "Do you want to save your changes before exiting?");
            switch (result) {
                case SAVE:
                    handleSaveGame(); // Save the game
                    Platform.exit(); // Exit after saving
                    break;
                case DONT_SAVE:
                    Platform.exit(); // Exit without saving
                    break;
                case CANCEL:
                    break; // User cancelled the action, do nothing
            }
        } else {
            // No unsaved changes, exit directly
            Platform.exit();
        }
    }

    @FXML void handleSaveGame() {
        if (gui != null) gui.showStatusMessage("Save action...");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Puzzle");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(menuBar.getScene().getWindow());

        if (file != null) {
            try {
                // Delegate all file writing and data formatting to the Game class.
                game.saveGameToFile(file);
                gui.showStatusMessage("Puzzle saved successfully to " + file.getName());
            } catch (IOException e) {
                // Catch file writing errors and show a message.
                gui.showStatusMessage("Error: Could not save file. " + e.getMessage());
            }
        }
    }

    @FXML void handleLoadGame() {
        if (gui != null) gui.showStatusMessage("Load action...");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Puzzle");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(menuBar.getScene().getWindow());
        if (file != null) {
            try {
                game.loadGameFromFile(file);
                menuEditorMode.setSelected(game.isEditorMode());
                availablePiecesScrollPane.setDisable(game.isEditorMode());
                hintButton.setDisable(game.isEditorMode());

                Platform.runLater(() -> {
                    if (boardWrapperPane != null) {
                        adjustBoardGridPaneSize(boardWrapperPane.getWidth(), boardWrapperPane.getHeight());
                    }
                });
            } catch (JsonSyntaxException e) {
                gui.showStatusMessage("Failed to load puzzle: " + e.getMessage());
            } catch (IOException  e) {
                gui.showStatusMessage("Error loading puzzle: " + e.getMessage());
            }
        }

    }

    @FXML void handleEditorMode() {
        if (game == null) {
            gui.showStatusMessage("Game not initialized. Cannot toggle editor mode.");
            return;
        }

        if (menuEditorMode.isSelected()) {
            enterEditorModeFlow();
        } else if (game.isPuzzleReadyToPlay()) {
            switchToGameMode();
            gui.showStatusMessage("Switched to game mode. You can now play the puzzle.");
        } else {
            menuEditorMode.setSelected(true);
            gui.showStatusMessage("Not yet ready to switch to Game Mode.");
        }
    }

    private void enterEditorModeFlow() {
        Alert alert = createEditorModeAlert();
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            setGameModeUI(false);
            gui.showStatusMessage("Editor mode activated. You can now edit the puzzle.");

            if (result.get() == ButtonType.CANCEL) {
                exitEditorMode();
            } else if (result.get().equals(editCurrentPuzzleBtn)) {
                game.editCurrentPuzzle();
                isPuzzleConfigured = true;
            } else if (result.get().equals(createNewPuzzleBtn)) {
                handleCreateNewPuzzleFlow();
            }
        }

        if (!isPuzzleConfigured || result.isEmpty()) {
            exitEditorMode();
        }
    }

    private void handleCreateNewPuzzleFlow() {
        if (game.isDirty()) {
            ConfirmationResult saveResult = showUnsavedChangesDialog(
                    "Create New Puzzle",
                    "Do you want to save your changes before creating a new puzzle?"
            );
            if (saveResult == ConfirmationResult.CANCEL) {
                menuEditorMode.setSelected(false);
                return;
            }
            if (saveResult == ConfirmationResult.SAVE) {
                handleSaveGame();
            }
        }
        showBoardConfigurationDialog();
        setGameModeUI(false);
    }

    private void switchToGameMode() {
        game.switchToGameMode();
        setGameModeUI(true);
        gui.showStatusMessage("Switched to game mode. You can now play the puzzle.");
    }

    private Alert createEditorModeAlert() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "What would you like to edit?");
        alert.setTitle("Enter Editor Mode");
        editCurrentPuzzleBtn = new ButtonType("Edit Current Puzzle");
        createNewPuzzleBtn = new ButtonType("Create New Puzzle");
        alert.getButtonTypes().setAll(
                editCurrentPuzzleBtn,
                createNewPuzzleBtn,
                ButtonType.CANCEL
        );
        return alert;
    }

    /**
     * Helper method to enable or disable game-related UI controls.
     * @param isGameMode True to enable controls, false to disable.
     */
    private void setGameModeUI(boolean isGameMode) {
        if (hintButton != null) {
            hintButton.setDisable(!isGameMode);
        }
        if (availablePiecesScrollPane != null) {
            availablePiecesScrollPane.setDisable(!isGameMode);
        }
    }

    private void exitEditorMode() {
        menuEditorMode.setSelected(false);
        hintButton.setDisable(false);
        availablePiecesScrollPane.setDisable(false);
        gui.showStatusMessage("Editor mode cancelled.");
    }

    @FXML void handleCheckSolvability() {
        if (game == null) {
            gui.showStatusMessage("Game not initialized. Cannot check solvability.");
            return;
        }

        menuCheckSolvability.setDisable(true);
        gui.showStatusMessage("Checking Solvability, please wait...");
        boolean isSolvable = game.isPuzzleSolvable();

        if (game.getNumberOfFreeCells() <= Game.MIN_ALLOWED_FREE_CELL) {
            String message;
            Alert alert;
            if (isSolvable) {
                 message = "This puzzle is solvable from the current state!";
                 alert = new Alert(Alert.AlertType.INFORMATION, message);
            } else {
                    message = "This puzzle is not solvable from the current state. " +
                            "You may need to remove some pieces.";
                    alert = new Alert(Alert.AlertType.ERROR, message);
            }
            alert.setTitle("Puzzle Solvability Check");
            alert.setHeaderText(isSolvable ? "Solvability check completed." : "Solvability check failed.");
            alert.showAndWait();
        }

        gui.showStatusMessage("Solvability check finished.");
        menuCheckSolvability.setDisable(false);
    }


    @FXML void handleGetSolutionHint() { if (gui != null) gui.showStatusMessage("Get Hint...");}

    @FXML void handleHintButtonAction() {
        if (game != null) {
            if (gui != null) gui.showStatusMessage("Hint button clicked. Processing hint...");
            game.provideHint();

        } else {
            gui.showStatusMessage("Game not initialized. Cannot provide a hint.");
        }
    }

    @FXML void handleClearBoard() {
        if (game != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Clear Board");
            alert.setHeaderText("Are you sure you want to remove all pieces from the board?");
            alert.setContentText("This action cannot be undone.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                game.clearBoard();
            }
        }
    }
}
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import logic.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;

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

    private ButtonType editCurrentPuzzleBtn, createNewPuzzleBtn;
    // Default dimensions for the game board
//    private int currentNumCols = 3;
//    private int currentNumRows = 4;

    // Constants for initial setup
    private static final int INITIAL_GAME_ROWS = 4;
    private static final int INITIAL_GAME_COLUMNS = 3;
    private static final double BORDER_PROPORTION_IN_CONTROLLER = 0.25; // For adjustGridPaneSize
    private boolean isResizing = false;
    boolean isPuzzleConfigured;


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
                this.rotateSelectedPieceButton,
                this.statusLabel
        );

        this.game = new Game(this.guiConnector);
        // Initialize the game via Game class
        // This will trigger calls to guiConnector.initializeBoardView(), etc.
        try {
            URL puzzleResource = Game.class.getResource("/logic/defaultPuzzleField.json");
            if (puzzleResource == null) {
                throw new IOException("Cannot find default puzzle file.");
            }

            File puzzleFile = new File(puzzleResource.toURI());
            game.loadGameFromFile(puzzleFile);
        } catch (IOException | URISyntaxException | JsonSyntaxException e) {
            e.printStackTrace();
            guiConnector.showStatusMessage("CRITICAL ERROR: Could not load default puzzle." + e.getMessage());
        }

        menuEditorMode.setSelected(game.isEditorMode());
        hintButton.setDisable(game.isEditorMode());

        addResizingListeners();

        if (boardGridPane != null) {
            boardGridPane.setOnMouseClicked(event -> {
                if (game.isEditorMode()) {
                    Node clickedNode = getClickedCellNode((Node) event.getTarget(), boardGridPane);
                    if (clickedNode == null) return;

                    Integer guiRow = GridPane.getRowIndex(clickedNode);
                    Integer guiCol = GridPane.getColumnIndex(clickedNode);
                    if (guiRow == null || guiCol == null) return;
                    if (event.getButton() == MouseButton.SECONDARY) {
                        boolean isGameCell = guiRow > 0 && guiRow <= game.getGameField().getRows() &&
                                guiCol > 0 && guiCol <= game.getGameField().getColumns();
                        if (isGameCell) {
                            // Right-clicked a game cell -> toggle hole state
                            int gameLogicRow = guiRow - 1;
                            int gameLogicCol = guiCol - 1;
                            game.toggleHoleState(gameLogicRow, gameLogicCol);
                        } else {
                            String borderKey = determineBorderKeyFromGuiCoords(guiRow, guiCol);
                            if (borderKey != null) {
                                // Right-clicked a border cell -> toggle border color
                                String newColor = showColorChoiceDialog();
                                if (newColor != null) {
                                    game.setEditorBorderColor(borderKey, newColor);
                                }
                            }
                        }
                    }
                } else {
                    Node clickedTarget = (Node) event.getTarget(); // Get the deepest node clicked
                    Node cellNode = getClickedCellNode(clickedTarget, boardGridPane); // Use helper method

                    if (cellNode == null) {
                        return; // Exit if no valid cell node was found
                    }

                    Integer guiRow = GridPane.getRowIndex(cellNode);
                    Integer guiCol = GridPane.getColumnIndex(cellNode);

                    if (guiRow == null || guiCol == null) {
                        return; // Exit if row/col cannot be determined
                    }

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
                    }

                }
            });
        } else {
            System.err.println("UserInterfaceController: boardGridPane is null, cannot attach event delegator.");
        }


        addResizingListeners();
        rotateSelectedPieceButton.setDisable(true);
    }

    private String determineBorderKeyFromGuiCoords(int guiRow, int guiCol) {
        int gameRows = game.getGameField().getRows();
        int gameCols = game.getGameField().getColumns();
        // Check if the click is on a border cell and not a corner
        boolean onTopBorder = guiRow == 0 && guiCol > 0 && guiCol <= gameCols;
        boolean onBottomBorder = guiRow == gameRows + 1 && guiCol > 0 && guiCol <= gameCols;
        boolean onLeftBorder = guiCol == 0 && guiRow > 0 && guiRow <= gameRows;
        boolean onRightBorder = guiCol == gameCols + 1 && guiRow > 0 && guiRow <= gameRows;
        // Top border
        if (onTopBorder) {
            return "TOP_" + (guiCol - 1); // Subtract 1 for 0-based index
        }
        // Bottom border
        if (onBottomBorder) {
            return "BOTTOM_" + (guiCol - 1); // Subtract 1 for 0-based index
        }
        // Left border
        if (onLeftBorder) {
            return "LEFT_" + (guiRow - 1); // Subtract 1 for 0-based index
        }
        // Right border
        if (onRightBorder) {
            return "RIGHT_" + (guiRow - 1); // Subtract 1 for 0-based index
        }
        return null; // Not a border cell
    }

    private String showColorChoiceDialog() {
        List<String> colorOptions = Arrays.asList("RED", "GREEN", "YELLOW");
        ChoiceDialog<String> dialog = new ChoiceDialog<>(colorOptions.get(0), colorOptions);
        dialog.setTitle("Choose Border Color");
        dialog.setHeaderText("Select a color for the border cell:");
        dialog.setContentText("Color:");
        Optional<String> result = dialog.showAndWait();

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


        if (selectedDataFromPanel != null) {
            game.attemptPlacePieceFromPanel(
                    selectedDataFromPanel.pattern(),
                    selectedDataFromPanel.rotationDegrees(),
                    gameLogicRow,
                    gameLogicCol
            );
        } else {
            // --- NO piece selected from the panel: Check if we should attempt to REMOVE a piece ---
            // Only attempt to remove if the clicked cell is actually occupied (and not a hole)
            if (!game.getGameField().isCellEmpty(gameLogicRow, gameLogicCol) &&
                    !game.getGameField().isCellHole(gameLogicRow, gameLogicCol)) {

                game.removePieceFromField(gameLogicRow, gameLogicCol);

            } else {
                // No piece selected from panel, and the clicked cell is empty or a hole.
                // The user's intention is that clicking an empty cell here does nothing specific related to removal.
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
            boardGridPane.setMinSize(0, 0); // Force allow shrinking

            boardGridPane.setPrefSize(targetBoardWidth, targetBoardHeight);
            boardGridPane.setMaxSize(targetBoardWidth, targetBoardHeight); // Cap growth

            // --- Resize the actual game cell content ---
            // The size of a visual game cell will be 1 * effectiveUnitSize
            double actualGameCellVisualSize = effectiveUnitSize;

            if (guiConnector instanceof JavaFXGUI) {
                ((JavaFXGUI) guiConnector).setCurrentCellSize(actualGameCellVisualSize);
            }

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
        if (guiConnector != null) {
            double newRotation = guiConnector.rotateSelectedPieceInPanel();
            // Status message is handled by JavaFXGUI's implementation of rotateSelectedPieceInPanel
            if (newRotation == -1) {
                 guiConnector.showStatusMessage("No piece selected to rotate."); // Already handled
            }
        }
    }

    @FXML void handleRestartPuzzle(ActionEvent event) {
        if (game != null) {
            if (game.isDirty() && !game.isEditorMode()) {
                ConfirmationResult result = showUnsavedChangesDialog(
                        "Restart Puzzle",
                        "Do you want to save your changes before restarting?");
                switch (result) {
                    case SAVE:
                        handleSaveGame(null); // Save the game
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

    @FXML void handleExitGame(ActionEvent event) {
        if (game.isDirty()) {
            ConfirmationResult result = showUnsavedChangesDialog(
                    "Exit Game",
                    "Do you want to save your changes before exiting?");
            switch (result) {
                case SAVE:
                    handleSaveGame(null); // Save the game
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

    @FXML void handleSaveGame(ActionEvent event) {
        if (guiConnector != null) guiConnector.showStatusMessage("Save action...");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Puzzle");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(menuBar.getScene().getWindow());

        if (file != null) {
            try {
                // Delegate all file writing and data formatting to the Game class.
                game.saveGameToFile(file);
                guiConnector.showStatusMessage("Puzzle saved successfully to " + file.getName());
            } catch (IOException e) {
                // Catch file writing errors and show a message.
                guiConnector.showStatusMessage("Error: Could not save file. " + e.getMessage());
            }
        }
    }

    @FXML void handleLoadGame(ActionEvent event) {
        if (guiConnector != null) guiConnector.showStatusMessage("Load action...");

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
                guiConnector.showStatusMessage("Failed to load puzzle: " + e.getMessage());
            } catch (IOException  e) {
                guiConnector.showStatusMessage("Error loading puzzle: " + e.getMessage());
            }
        }

    }

    @FXML void handleEditorMode(ActionEvent event) {
        if (game == null) {
            guiConnector.showStatusMessage("Game not initialized. Cannot toggle editor mode.");
            return;
        }

        if (menuEditorMode.isSelected()) {
            enterEditorModeFlow();
        } else if (game.isPuzzleReadyToPlay()) {
            switchToGameMode();
            guiConnector.showStatusMessage("Switched to game mode. You can now play the puzzle.");
        } else {
            menuEditorMode.setSelected(true);
            guiConnector.showStatusMessage("Not yet ready to switch to Game Mode.");
        }
    }

    private void enterEditorModeFlow() {
        Alert alert = createEditorModeAlert();
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            setGameModeUI(false);
            guiConnector.showStatusMessage("Editor mode activated. You can now edit the puzzle.");

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
                handleSaveGame(null);
            }
        }
        showBoardConfigurationDialog();
        setGameModeUI(false);
    }

    private void switchToGameMode() {
        game.switchToGameMode();
        setGameModeUI(true);
        guiConnector.showStatusMessage("Switched to game mode. You can now play the puzzle.");
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
        guiConnector.showStatusMessage("Editor mode cancelled.");
    }

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
package gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import logic.GUIConnector;
import logic.Position;
import logic.SelectedPieceDataFromPanel;

import java.util.*;

public class JavaFXGUI implements GUIConnector {

    // --- FXML Injected Fields ---

    // Right panel for available pieces
    private final TilePane availablePiecesPane;
    private final Button rotateSelectedPieceButton;

    // Center GridPane for the board
    private final GridPane boardGridPane;

    // MenuBar and MenuItems
    private MenuBar menuBar;
    private MenuItem menuCheckSolvability;
    private CheckMenuItem menuEditorMode;
    private MenuItem menuExit;
    private MenuItem menuLoad;
    private MenuItem menuRestart;
    private MenuItem menuSave;
    private MenuItem menuSolutionHint;

    // Bottom status label
    private final Label statusLabel;

    // Constants for visual representation

    private static final double BORDER_PROPORTION_FOR_CONSTRAINTS = 0.25; // Border is 1/4 of a game cell
    private static final double AVAILABLE_PIECE_SIZE = 60.0;

    private int currentGuiGameRows;
    private int currentGuiGameCols;
    private final Random random = new Random(); // For placeholder border colors
    private SelectedPieceDataFromPanel currentSelectedPieceInfo = null;
    private Node currentlySelectedNodeVisual = null; // To keep track of the styled node
    private double currentCellSize = 60;



    public JavaFXGUI(GridPane boardGridPane, TilePane availablePiecesPane,
                     Button rotateSelectedPieceButton,  Label statusLabel) {
        this.boardGridPane = boardGridPane;
        this.availablePiecesPane = availablePiecesPane;
        this.rotateSelectedPieceButton = rotateSelectedPieceButton;
        this.statusLabel = statusLabel;
    }

    public void setCurrentCellSize(double size) {
        this.currentCellSize = size;
    }


    @Override
    public void initializeBoardView(int gameRows, int gameCols, Map<String, String> borderColors) {
        this.currentGuiGameRows = gameRows;
        this.currentGuiGameCols = gameCols;

        Platform.runLater(() -> {
            if (boardGridPane == null) {
                System.err.println("JavaFXGUI Error: boardGridPane is null in initializeBoardView!");
                return;
            }
            boardGridPane.getChildren().clear();
            boardGridPane.getColumnConstraints().clear();
            boardGridPane.getRowConstraints().clear();

            int totalGridRows = gameRows + 2;
            int totalGridCols = gameCols + 2;

            // --- Setup ColumnConstraints with Percentages ---
            double effectiveGameColsForPercentage = gameCols + (2 * BORDER_PROPORTION_FOR_CONSTRAINTS);
            double middleColPercentage = 100.0 / effectiveGameColsForPercentage;
            double borderColPercentage = middleColPercentage * BORDER_PROPORTION_FOR_CONSTRAINTS;

            ColumnConstraints ccLeftBorder = new ColumnConstraints();
            ccLeftBorder.setPercentWidth(borderColPercentage);
            ccLeftBorder.setHgrow(Priority.NEVER);
            boardGridPane.getColumnConstraints().add(ccLeftBorder);

            for (int i = 0; i < gameCols; i++) {
                ColumnConstraints ccGame = new ColumnConstraints();
                ccGame.setPercentWidth(middleColPercentage);
                ccGame.setHgrow(Priority.SOMETIMES); // Game columns share space
                boardGridPane.getColumnConstraints().add(ccGame);
            }
            ColumnConstraints ccRightBorder = new ColumnConstraints();
            ccRightBorder.setPercentWidth(borderColPercentage);
            ccRightBorder.setHgrow(Priority.NEVER);
            boardGridPane.getColumnConstraints().add(ccRightBorder);

            // --- Setup RowConstraints with Percentages ---
            double effectiveGameRowsForPercentage = gameRows + (2 * BORDER_PROPORTION_FOR_CONSTRAINTS);
            double middleRowPercentage = 100.0 / effectiveGameRowsForPercentage;
            double borderRowPercentage = middleRowPercentage * BORDER_PROPORTION_FOR_CONSTRAINTS;

            RowConstraints rcTopBorder = new RowConstraints();
            rcTopBorder.setPercentHeight(borderRowPercentage);
            rcTopBorder.setVgrow(Priority.NEVER);
            boardGridPane.getRowConstraints().add(rcTopBorder);

            for (int i = 0; i < gameRows; i++) {
                RowConstraints rcGame = new RowConstraints();
                rcGame.setPercentHeight(middleRowPercentage);
                rcGame.setVgrow(Priority.SOMETIMES);
                boardGridPane.getRowConstraints().add(rcGame);
            }
            RowConstraints rcBottomBorder = new RowConstraints();
            rcBottomBorder.setPercentHeight(borderRowPercentage);
            rcBottomBorder.setVgrow(Priority.NEVER);
            boardGridPane.getRowConstraints().add(rcBottomBorder);

            // --- Populate Grid Cells ---
            Map<String, String> currentBorderColors = (borderColors != null) ? borderColors : Collections.emptyMap();

            for (int r_gui = 0; r_gui < totalGridRows; r_gui++) {
                for (int c_gui = 0; c_gui < totalGridCols; c_gui++) {
                    Node cellNode;
                    boolean isGameCell = (r_gui > 0 && r_gui <= gameRows && c_gui > 0 && c_gui <= gameCols);

                    if (isGameCell) {
                        cellNode = createGameCellVisual(null, null, 0, false, false);
                    } else {
                        String key = "";
                        if (r_gui == 0 && c_gui > 0 && c_gui <= gameCols) { // Top Border
                            key = "TOP_" + (c_gui - 1);
                        } else if (r_gui == totalGridRows - 1 && c_gui > 0 && c_gui <= gameCols) { // Bottom Border
                            key = "BOTTOM_" + (c_gui - 1);
                        } else if (c_gui == 0 && r_gui > 0 && r_gui <= gameRows) { // Left Border
                            key = "LEFT_" + (r_gui - 1);
                        } else if (c_gui == totalGridCols - 1 && r_gui > 0 && r_gui <= gameRows) { // Right Border
                            key = "RIGHT_" + (r_gui - 1);
                        }
                        Color color = colorFromString(currentBorderColors.getOrDefault(key, "NONE"));
                        cellNode = createCornerOrBorderCellVisual(color);
                    }
                    boardGridPane.add(cellNode, c_gui, r_gui);
                }
            }
            boardGridPane.requestLayout();
        });
    }

    private Pane createCornerOrBorderCellVisual(Color color) {
        Pane pane = new Pane(); // Use Pane for simple colored backgrounds
        if (color == Color.TRANSPARENT || color == null) { // "NONE" color
            pane.setStyle("-fx-background-color: transparent;"); // Or match boardGridPane background
        } else {
            pane.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
        }
        // The size of this pane will be dictated by the GridPane's percentage constraints
        return pane;
    }

    private Color colorFromString(String colorName) {
        if (colorName == null) return Color.GRAY;
        return switch (colorName.toUpperCase()) {
            case "RED" -> Color.web("#B60000");
            case "GREEN" -> Color.web("#007F0E");
            case "YELLOW" -> Color.web("#FFD800");
            case "NONE" -> Color.TRANSPARENT; // So "NONE" border segments are not explicitly colored
            default -> Color.LIGHTGRAY; // Fallback for unexpected color strings
        };
    }

    private Node createGameCellVisual(String pieceId, String pieceImagePath, int rotationDegrees,
                                      boolean isError, boolean isHole) {
        StackPane cellPane = new StackPane();
        cellPane.setAlignment(Pos.CENTER);

        // Apply the stored size constraints to the new cell pane
        if (this.currentCellSize > 1) {
            cellPane.setPrefSize(this.currentCellSize, this.currentCellSize);
            cellPane.setMinSize(this.currentCellSize, this.currentCellSize);
            cellPane.setMaxSize(this.currentCellSize, this.currentCellSize);
        }
//        else {
//            // If currentCellSize is not set or invalid, use default size
//            cellPane.setPrefSize(60, 60);
//            cellPane.setMinSize(60, 60);
//            cellPane.setMaxSize(60, 60);
//        }
        // Placeholder visual content (e.g., a semi-transparent rectangle or imageview)
        Rectangle bgRect = new Rectangle(); // No fixed size here
        bgRect.setFill(Color.LIGHTSLATEGRAY); // Default empty cell color
        bgRect.setStroke(Color.DIMGRAY);
        bgRect.setStrokeWidth(0.5);
        // Bind background rectangle to fill the StackPane
        bgRect.widthProperty().bind(cellPane.widthProperty());
        bgRect.heightProperty().bind(cellPane.heightProperty());
        cellPane.getChildren().add(bgRect);

        if (isHole) {
            Circle holeMarker = new Circle();
            holeMarker.setFill(Color.WHITE);
            holeMarker.setStroke(Color.LIGHTGRAY);
            holeMarker.setStrokeWidth(1.5);

            holeMarker.radiusProperty().bind(
                    javafx.beans.binding.Bindings.min(
                            cellPane.widthProperty(), cellPane.heightProperty()).divide(2).subtract(3) // Leave some padding
            );
            cellPane.getChildren().add(holeMarker);
        } else if (pieceId != null) {
            ImageView pieceImageView = getImageViewForPiece(pieceId, pieceImagePath);
            pieceImageView.setRotate(rotationDegrees);
            pieceImageView.setMouseTransparent(true);
            pieceImageView.setPreserveRatio(true);
            pieceImageView.fitWidthProperty().bind(cellPane.widthProperty());
            pieceImageView.fitHeightProperty().bind(cellPane.heightProperty());
            cellPane.getChildren().add(pieceImageView);
        }

        if (isError && !isHole) { // If this cell is in an error state
            cellPane.setStyle("-fx-effect: dropshadow(gaussian, red, 10, 0.6, 0, 0); -fx-border-color: red; -fx-border-width: 1.5px;");

        } else {
            cellPane.setStyle("-fx-border-color: transparent; -fx-border-width: 0;"); // No border or default
        }
        return cellPane;
    }

    private ImageView getImageViewForPiece(String pieceId, String pieceImagePath) {
        ImageView imageView = new ImageView();
        try {
            // Assuming pieceImagePath is just the pattern like "RGYB"
            // and that the board pieces might also need rotation if you display them rotated on board.
            // For now, just load image.
            Image tileImage = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/gui/tiles/" + pieceId + ".png")));
            imageView.setImage(tileImage);
            // Bind size to cell or set fixed size as appropriate for board cells
            // This needs careful handling to match cell size from adjustBoardGridPaneSize
            // For now, let it be managed by StackPane, might need fitWidth/Height
        } catch (Exception e) {
            System.err.println("JavaFXGUI: Failed to load image for piece: " + pieceImagePath + " - " + e.getMessage());
            // Fallback visual: show the piece ID as text
            Text textFallback = new Text(pieceId != null ? pieceId.substring(0, Math.min(pieceId.length(), 4)) : "?");
            textFallback.setStyle("-fx-font-size: 10px;"); // Adjust font size as needed
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            Image textImage = textFallback.snapshot(params, null); // Create an image from the text
            imageView.setImage(textImage);
            imageView.setFitWidth(30); // Set some small size for the error text image
            imageView.setFitHeight(30);
            imageView.setPreserveRatio(true);
        }
        return imageView;
    }


    @Override
    public void updateGameCell(int gameRow, int gameCol, String pieceId, String pieceImagePath, int rotationDegree,
                               boolean isError, boolean isHole) {
        Platform.runLater(() -> {
            if (boardGridPane == null) {
                System.err.println("JavaFXGUI Error: boardGridPane is null in updateGameCell!");
                return;
            }

            int gridR = gameRow + 1;
            int gridC = gameCol + 1;

            Node existingNode = getNodeByRowColumnIndex(gridR, gridC, boardGridPane);
            // Pass currentGuiGameRows/Cols to createGameCellVisual if needed for bindings
            Node newVisual = createGameCellVisual(pieceId, pieceImagePath, rotationDegree, isError, isHole);
            GridPane.setHalignment(newVisual, HPos.CENTER);
            GridPane.setValignment(newVisual, VPos.CENTER);

            if (existingNode != null) {
                boardGridPane.getChildren().remove(existingNode);
            }
            boardGridPane.add(newVisual, gridC, gridR);
        });
    }

    private Node getNodeByRowColumnIndex(final int row, final int column, GridPane gridPane) {
        for (Node node : gridPane.getChildren()) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == column) {
                return node;
            }
        }
        return null;
    }


    @Override
    public void displayAvailablePieces(List<String> pieceRepresentationForGUI) {
        Platform.runLater(() -> {
            if (availablePiecesPane == null) return;
            availablePiecesPane.getChildren().clear(); // Clear old pieces

            for (String pieceRep : pieceRepresentationForGUI) {
                // Create ImageView (or StackPane with ImageView and Label)
                // This logic is mostly from your existing code
                ImageView pieceImageView = new ImageView();
                Node displayNode; // This will be what's added to the pane
                try {
                    Image tileImage = new Image(Objects.requireNonNull(
                            getClass().getResourceAsStream("/gui/tiles/" + pieceRep + ".png")));
                    pieceImageView.setImage(tileImage);
                    pieceImageView.setFitWidth(AVAILABLE_PIECE_SIZE);
                    pieceImageView.setFitHeight(AVAILABLE_PIECE_SIZE);
                    pieceImageView.setPreserveRatio(true);
                    pieceImageView.setSmooth(true);
                    pieceImageView.setUserData(pieceRep); // Store pieceRep for identification
                    displayNode = pieceImageView;
                } catch (Exception e) {
                    Rectangle placeholder = new Rectangle(AVAILABLE_PIECE_SIZE, AVAILABLE_PIECE_SIZE, Color.LIGHTGRAY);
                    placeholder.setStroke(Color.BLACK);
                    Label l = new Label(pieceRep.substring(0, Math.min(pieceRep.length(),4)));
                    StackPane sp = new StackPane(placeholder, l);
                    sp.setUserData(pieceRep); // Store pieceRep here too
                    displayNode = sp;
                }

                // IMPORTANT: Attach the click handler here
                final String finalPieceRep = pieceRep; // For lambda
                final Node finalDisplayNode = displayNode;
                finalDisplayNode.setOnMouseClicked(event -> handleAvailablePieceClick(finalDisplayNode, finalPieceRep));
                availablePiecesPane.getChildren().add(finalDisplayNode);
            }
        });
    }

    private void handleAvailablePieceClick(Node pieceViewNode, String pieceRep) {
        // Check if the clicked node is already selected
        if (this.currentlySelectedNodeVisual == pieceViewNode) {
            clearSelectionFromPanel();
            return;
        }

        if (this.currentlySelectedNodeVisual != null) {
            this.currentlySelectedNodeVisual.setStyle("-fx-effect: null;");
        }

        // Apply new selection
        pieceViewNode.setStyle("-fx-effect: dropshadow(gaussian, blue, 10, 0.5, 0, 0);");
        this.currentlySelectedNodeVisual = pieceViewNode;
        if (rotateSelectedPieceButton!=null) {
            rotateSelectedPieceButton.setDisable(false);
        }


        double currentRotation;
        // Determine the rotation from the node that would be rotated
        // (ImageView or StackPane)
        Node visualRepresentation = pieceViewNode;
        if(pieceViewNode instanceof StackPane){
            visualRepresentation = ((StackPane) pieceViewNode).getChildren().stream()
                    .filter(ImageView.class::isInstance).findFirst().orElse(pieceViewNode);
        }
        currentRotation = visualRepresentation.getRotate();


        this.currentSelectedPieceInfo = new SelectedPieceDataFromPanel(pieceRep, currentRotation);

        if (statusLabel != null) {
            statusLabel.setText("Selected: " + pieceRep + " (" + (int)currentRotation + "°)");
        }
    }

    @Override
    public void showStatusMessage(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    @Override
    public void showGameEndMessage(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @Override
    public void clearCellHighlights() {
        Platform.runLater(() -> {
            if (boardGridPane == null) {
                System.err.println("JavaFXGUI Error: boardGridPane is null in clearCellHighlights!");
                return;
            }
            for (Node node : boardGridPane.getChildren()) {
                if (node instanceof StackPane stackPane) {
                    stackPane.setStyle("-fx-effect: null; -fx-border-color: transparent; -fx-border-width: 0;");
                } else {
                    node.setStyle("-fx-effect: null; -fx-border-color: transparent; -fx-border-width: 0;");
                }
            }
        });

    }

    @Override
    public SelectedPieceDataFromPanel getSelectedPieceDataFromPanel() {
        return this.currentSelectedPieceInfo;
    }

    @Override
    public void clearSelectionFromPanel() {
        Platform.runLater(() -> {
            if (this.currentlySelectedNodeVisual != null) {
                this.currentlySelectedNodeVisual.setStyle("-fx-effect: null;");
            }
            this.currentSelectedPieceInfo = null;
            this.currentlySelectedNodeVisual = null;
            if (rotateSelectedPieceButton != null) { // rotateSelectedPieceButton is from UserInterfaceController
                rotateSelectedPieceButton.setDisable(true);
            }
            if (statusLabel != null) {
                 statusLabel.setText("Select a piece."); // Or some default message
            }
        });


    }

    @Override
    public double rotateSelectedPieceInPanel() {
        if (currentlySelectedNodeVisual == null || currentSelectedPieceInfo == null) {
            showStatusMessage("No piece selected to rotate.");
            return -1;
        }

        Node nodeToRotate = currentlySelectedNodeVisual;
        ImageView imageView = null;

        if (currentlySelectedNodeVisual instanceof ImageView) {
            imageView = (ImageView) currentlySelectedNodeVisual;
        } else if (currentlySelectedNodeVisual instanceof StackPane) {
            // Attempt to find an ImageView within the StackPane to rotate
            // This logic should match how rotation is determined in handleAvailablePieceClick
            imageView = (ImageView) ((StackPane)currentlySelectedNodeVisual).getChildren().stream()
                    .filter(ImageView.class::isInstance).findFirst().orElse(null);
            if (imageView == null) nodeToRotate = currentlySelectedNodeVisual; // Rotate StackPane if no ImageView
            else nodeToRotate = imageView;
        }

        nodeToRotate.setRotate((nodeToRotate.getRotate() + 90) % 360);
        double newRotation = nodeToRotate.getRotate();

        // Update the stored selection info
        this.currentSelectedPieceInfo = new SelectedPieceDataFromPanel(currentSelectedPieceInfo.pattern(), newRotation);

        String msg = "Rotated " + currentSelectedPieceInfo.pattern() + " to " + newRotation + "°";
        System.out.println("JavaFXGUI: " + msg);
        showStatusMessage(msg);
        return newRotation;
    }


    @Override
    public void setEditorMode(boolean isEditorMode) {
        Platform.runLater(() -> {
            // Example: change background or enable/disable certain controls
            if (isEditorMode) {
                // boardGrid.setStyle("-fx-background-color: #FFE0B2;"); // Light orange for editor
                showStatusMessage("Editor Mode Active");
            } else {
                // boardGrid.setStyle("-fx-background-color: #CDCDCD;"); // Default game background
                showStatusMessage("Game Mode Active");
            }
        });
    }

    @Override
    public void enableRotationButton(boolean enable) {
        Platform.runLater(() -> rotateSelectedPieceButton.setDisable(!enable));
    }

    @Override
    public void highlightCell(int row, int column, String colorPattern) {
        Platform.runLater(() -> {
            Node cellNode = getNodeByRowColumnIndex(row + 1, column + 1, boardGridPane);
            if (cellNode != null) {
                // Apply a background color based on the colorPattern
                Color highlightColor = switch (colorPattern.toUpperCase()) {
                    case "RED" -> Color.RED;
                    case "GREEN" -> Color.GREEN;
                    case "YELLOW" -> Color.YELLOW;
                    default -> Color.LIGHTGRAY; // Fallback color
                };
                cellNode.setStyle("-fx-border-color: black; -fx-border-width: 2px;");
            }
        });
    }

    @Override
    public void updateBorderColor(String borderKey, String newColor) {
        Platform.runLater(() -> {
            if (boardGridPane == null) {
                System.err.println("JavaFXGUI Error: boardGridPane is null in updateBorderColor!");
                return;
            }

            // Determine which border segment to update based on the key
            String[] parts = borderKey.split("_");
            if (parts.length != 2) {
                System.err.println("JavaFXGUI Error: Invalid border key format: " + borderKey);
                return;
            }

            String position = parts[0].toUpperCase(); // TOP, BOTTOM, LEFT, RIGHT
            int index = Integer.parseInt(parts[1]);

            int gridRow = 0, gridCol = 0;

            switch (position) {
                case "TOP":
                    gridCol = index + 1; // +1 for the left border
                    break;
                case "BOTTOM":
                    gridRow = currentGuiGameRows + 1; // +1 for the top border
                    gridCol = index + 1; // +1 for the left border
                    break;
                case "LEFT":
                    gridRow = index + 1; // +1 for the top border
                    break;
                case "RIGHT":
                    gridRow = index + 1; // +1 for the top border
                    gridCol = currentGuiGameCols + 1; // +1 for the left border
                    break;
                default:
                    System.err.println("JavaFXGUI Error: Unknown position in border key: " + position);
                    return;
            }

            Node existingNode = getNodeByRowColumnIndex(gridRow, gridCol, boardGridPane);
            if (existingNode != null) {
                existingNode.setStyle("-fx-background-color: " + newColor + ";");
            } else {
                System.err.println("JavaFXGUI Error: No node found at (" + gridRow + ", " + gridCol + ")");
            }
        });
    }
}

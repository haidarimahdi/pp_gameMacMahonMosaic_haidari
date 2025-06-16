package logic;

/**
 * A simple data record to hold information about a piece selected
 * from the UI panel, specifically its pattern and current visual rotation.
 */
public record SelectedPieceDataFromPanel(String pattern, double rotationDegrees) {
}

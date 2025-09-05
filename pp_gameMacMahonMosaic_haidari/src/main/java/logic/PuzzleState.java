package logic;

import java.util.List;
import java.util.Map;

/**
 * A record to hold the complete state of a loaded puzzle.
 */
public record PuzzleState(
    Field field,
    Map<BorderPosition, Color> borderColors,
    List<MosaicPiece> piecesOnBoard
) {}

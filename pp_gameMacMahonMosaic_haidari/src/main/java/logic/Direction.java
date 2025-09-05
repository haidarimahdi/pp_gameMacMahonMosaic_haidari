package logic;

public enum Direction {
    TOP, RIGHT, BOTTOM, LEFT;

    public Direction opposite() {
        return switch (this) {
            case TOP -> BOTTOM;
            case RIGHT -> LEFT;
            case BOTTOM -> TOP;
            case LEFT -> RIGHT;
        };
    }
}

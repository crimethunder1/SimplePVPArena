package de.simplepvparena.arena;

import java.util.Optional;

public final class ArenaDebugCellLimit {

    public static final int MINIMUM_CELL_COUNT = 1;

    private final int cellCount;
    private final String rejectionMessage;

    private ArenaDebugCellLimit(int cellCount, String rejectionMessage) {
        this.cellCount = cellCount;
        this.rejectionMessage = rejectionMessage;
    }

    public static ArenaDebugCellLimit parse(String cellCountArgument) {
        int cellCount;
        try {
            cellCount = Integer.parseInt(cellCountArgument.trim());
        } catch (NumberFormatException notANumber) {
            return new ArenaDebugCellLimit(0, "The cell limit " + cellCountArgument + " is not a number");
        }
        if (cellCount < MINIMUM_CELL_COUNT || cellCount > ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT) {
            return new ArenaDebugCellLimit(0, "The cell limit has to be between " + MINIMUM_CELL_COUNT
                    + " and " + ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT);
        }
        return new ArenaDebugCellLimit(cellCount, null);
    }

    public boolean isAccepted() {
        return rejectionMessage == null;
    }

    public int getCellCount() {
        if (!isAccepted()) {
            throw new IllegalStateException("a rejected cell limit has no value");
        }
        return cellCount;
    }

    public Optional<String> getRejectionMessage() {
        return Optional.ofNullable(rejectionMessage);
    }
}

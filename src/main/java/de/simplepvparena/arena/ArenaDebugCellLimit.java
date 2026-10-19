package de.simplepvparena.arena;

import de.simplepvparena.input.ChatInputValues;
import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

import java.util.Optional;

public final class ArenaDebugCellLimit {

    public static final int MINIMUM_CELL_COUNT = 1;
    public static final int REQUIRED_VALUE_COUNT = 1;

    private final int cellCount;
    private final String rejectionMessage;

    private ArenaDebugCellLimit(int cellCount, String rejectionMessage) {
        this.cellCount = cellCount;
        this.rejectionMessage = rejectionMessage;
    }

    public static ArenaDebugCellLimit parseLine(String line) {
        ChatInputValues values = ChatInputValues.of(line);
        Optional<String> countRejection = values.findCountRejection(REQUIRED_VALUE_COUNT, describeUsage());
        if (countRejection.isPresent()) {
            return new ArenaDebugCellLimit(0, countRejection.get());
        }
        return parse(values.get(0));
    }

    public static ArenaDebugCellLimit parse(String cellCountArgument) {
        int cellCount;
        try {
            cellCount = Integer.parseInt(cellCountArgument.trim());
        } catch (NumberFormatException notANumber) {
            return new ArenaDebugCellLimit(0, Messages.text(ArenaSetupMessage.DEBUG_CELL_LIMIT_NOT_A_NUMBER,
                    Placeholder.of("value", cellCountArgument)));
        }
        if (cellCount < MINIMUM_CELL_COUNT || cellCount > ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT) {
            return new ArenaDebugCellLimit(0, Messages.text(ArenaSetupMessage.DEBUG_CELL_LIMIT_OUT_OF_RANGE,
                    Placeholder.of("minimum", MINIMUM_CELL_COUNT),
                    Placeholder.of("maximum", ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT)));
        }
        return new ArenaDebugCellLimit(cellCount, null);
    }

    public static String describeUsage() {
        return Messages.text(ArenaSetupMessage.DEBUG_CELL_LIMIT_USAGE);
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

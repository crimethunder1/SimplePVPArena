package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaHeightMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;

public enum ArenaHeightMarkerMode {

    LOWEST(ArenaHeightMessage.LOWEST_POINT),
    HIGHEST(ArenaHeightMessage.HIGHEST_POINT);

    private final MessageKey displayName;

    ArenaHeightMarkerMode(MessageKey displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return Messages.text(displayName);
    }

    public MessageKey select(MessageKey lowestText, MessageKey highestText) {
        return this == LOWEST ? lowestText : highestText;
    }

    public ArenaHeightMarkerMode toggle() {
        return this == LOWEST ? HIGHEST : LOWEST;
    }
}

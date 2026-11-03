package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaSpawnMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;

public enum ArenaSpawnType {

    FIRST_TEAM(ArenaSpawnMessage.FIRST_TEAM_NAME, false, ArenaSpawnMessage.FIRST_TEAM_PLACEMENT_HINT),
    SECOND_TEAM(ArenaSpawnMessage.SECOND_TEAM_NAME, false, ArenaSpawnMessage.SECOND_TEAM_PLACEMENT_HINT),
    TRIBUNE(ArenaSpawnMessage.TRIBUNE_NAME, true, ArenaSpawnMessage.TRIBUNE_PLACEMENT_HINT);

    private final MessageKey displayName;
    private final boolean outsideFightArea;
    private final MessageKey placementHint;

    ArenaSpawnType(MessageKey displayName, boolean outsideFightArea, MessageKey placementHint) {
        this.displayName = displayName;
        this.outsideFightArea = outsideFightArea;
        this.placementHint = placementHint;
    }

    public String getDisplayName() {
        return Messages.text(displayName);
    }

    public boolean mustLieOutsideFightArea() {
        return outsideFightArea;
    }

    public String getPlacementHint() {
        return Messages.text(placementHint);
    }
}

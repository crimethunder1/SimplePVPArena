package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaSpawnMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;

public enum ArenaSpawnType {

    FIRST_TEAM(ArenaSpawnMessage.FIRST_TEAM_NAME, true, ArenaSpawnMessage.FIRST_TEAM_PLACEMENT_HINT),
    SECOND_TEAM(ArenaSpawnMessage.SECOND_TEAM_NAME, true, ArenaSpawnMessage.SECOND_TEAM_PLACEMENT_HINT),
    TRIBUNE(ArenaSpawnMessage.TRIBUNE_NAME, false, ArenaSpawnMessage.TRIBUNE_PLACEMENT_HINT);

    private final MessageKey displayName;
    private final boolean insideFightArea;
    private final MessageKey placementHint;

    ArenaSpawnType(MessageKey displayName, boolean insideFightArea, MessageKey placementHint) {
        this.displayName = displayName;
        this.insideFightArea = insideFightArea;
        this.placementHint = placementHint;
    }

    public String getDisplayName() {
        return Messages.text(displayName);
    }

    public boolean belongsIntoFightArea() {
        return insideFightArea;
    }

    public String getPlacementHint() {
        return Messages.text(placementHint);
    }
}

package de.simplepvparena.message;

import java.util.Set;

public enum KitChoiceMessage implements MessageKey {

    TITLE("kit-choice-menu.title", "match"),
    CHOICE_OVER("kit-choice-menu.choice-over", "match"),
    INFO_TITLE("kit-choice-menu.tile.info.title"),
    INFO_MATCH("kit-choice-menu.tile.info.match", "match", "firstTeam", "secondTeam"),
    INFO_DEFAULT_KIT("kit-choice-menu.tile.info.default-kit", "kit"),
    INFO_TIME_LEFT("kit-choice-menu.tile.info.time-left", "count"),
    INFO_HINT("kit-choice-menu.tile.info.hint"),
    INFO_OVER_MATCH("kit-choice-menu.tile.info.over-match", "match"),
    KIT_DEFAULT_KIT("kit-choice-menu.tile.kit.default-kit"),
    KIT_LORE("kit-choice-menu.tile.kit.lore"),
    BEGIN("kit-choice.begin", "match", "firstTeam", "secondTeam", "count", "kit"),
    REMINDER("kit-choice.reminder", "count", "match"),
    CHOSEN("kit-choice.chosen", "player", "kit", "match"),
    EXPIRED("kit-choice.expired", "match", "kit"),
    CANCELLED("kit-choice.cancelled");

    private final String path;
    private final Set<String> placeholderNames;

    KitChoiceMessage(String path, String... placeholderNames) {
        this.path = path;
        this.placeholderNames = Set.of(placeholderNames);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Set<String> placeholderNames() {
        return placeholderNames;
    }
}

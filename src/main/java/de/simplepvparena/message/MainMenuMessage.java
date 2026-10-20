package de.simplepvparena.message;

import java.util.Set;

public enum MainMenuMessage implements MessageKey {

    TITLE("main-menu.title"),
    REGISTERED_TEAMS("main-menu.registered-teams", "count"),
    INFO_TITLE("main-menu.tile.info.title"),
    INFO_ARENA_READY("main-menu.tile.info.arena-ready"),
    INFO_ARENA_INCOMPLETE("main-menu.tile.info.arena-incomplete"),
    ARENA_TITLE("main-menu.tile.arena.title"),
    ARENA_LORE("main-menu.tile.arena.lore"),
    ARENA_READY("main-menu.tile.arena.ready"),
    ARENA_INCOMPLETE("main-menu.tile.arena.incomplete"),
    TEAMS_TITLE("main-menu.tile.teams.title"),
    TEAMS_LORE("main-menu.tile.teams.lore"),
    MATCH_TITLE("main-menu.tile.match.title"),
    MATCH_LORE("main-menu.tile.match.lore"),
    BORDER_SELECTOR_TITLE("main-menu.tile.border-selector.title"),
    BORDER_SELECTOR_LORE("main-menu.tile.border-selector.lore"),
    BORDER_SELECTOR_MODE_ON("main-menu.tile.border-selector.mode-on"),
    BORDER_SELECTOR_MODE_OFF("main-menu.tile.border-selector.mode-off"),
    BORDER_SELECTOR_SWITCHED_ON("main-menu.border-selector.switched-on"),
    BORDER_SELECTOR_SWITCHED_OFF("main-menu.border-selector.switched-off"),
    STATUS_TITLE("main-menu.tile.status.title"),
    STATUS_LORE("main-menu.tile.status.lore"),
    BRACKET_TITLE("main-menu.tile.bracket.title"),
    BRACKET_LORE("main-menu.tile.bracket.lore"),
    DRY_RUN_TITLE("main-menu.tile.dry-run.title"),
    DRY_RUN_LORE("main-menu.tile.dry-run.lore"),
    RELOAD_TITLE("main-menu.tile.reload.title"),
    RELOAD_LORE("main-menu.tile.reload.lore"),
    RELOAD_MATCH_RUNNING("main-menu.tile.reload.match-running"),
    RELOAD_RUNNING_MATCH_DELAY("main-menu.reload.running-match-delay"),
    KITS_TITLE("main-menu.tile.kits.title"),
    KIT_COUNT("main-menu.tile.kits.count", "count"),
    DEFAULT_KIT("main-menu.tile.kits.default-kit", "kit"),
    NO_DEFAULT_KIT("main-menu.tile.kits.no-default-kit"),
    KITS_LORE("main-menu.tile.kits.lore"),
    KIT_CHOICE_TITLE("main-menu.tile.kit-choice.title"),
    KIT_CHOICE_MATCH("main-menu.tile.kit-choice.match", "match", "firstTeam", "secondTeam"),
    KIT_CHOICE_LORE("main-menu.tile.kit-choice.lore"),
    CLOSE_TITLE("main-menu.tile.close.title"),
    CLOSE_LORE("main-menu.tile.close.lore");

    private final String path;
    private final Set<String> placeholderNames;

    MainMenuMessage(String path, String... placeholderNames) {
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

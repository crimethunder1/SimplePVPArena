package de.simplepvparena.message;

import java.util.Set;

public enum MenuMessage implements MessageKey {

    BACK_NAME("menu.back.name"),
    BACK_TO_MAIN_MENU("menu.back.main-menu"),
    BACK_TO_MATCH_MENU("menu.back.match-menu"),
    BACK_TO_WAITING_MATCHES("menu.back.waiting-matches"),
    BACK_TO_TEAM_LIST("menu.back.team-list"),
    BACK_TO_TEAM_OPTIONS("menu.back.team-options", "team"),
    BACK_TO_KIT_LIST("menu.back.kit-list"),
    PREVIOUS_PAGE("menu.page.previous"),
    NEXT_PAGE("menu.page.next"),
    PAGE_OF_PAGES("menu.page.target", "page", "pageCount"),
    NOT_ALLOWED("menu.not-allowed"),
    UNKNOWN_PLAYER("menu.unknown-player", "player");

    private final String path;
    private final Set<String> placeholderNames;

    MenuMessage(String path, String... placeholderNames) {
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

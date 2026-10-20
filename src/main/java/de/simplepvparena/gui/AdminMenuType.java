package de.simplepvparena.gui;

public enum AdminMenuType {

    MAIN(false),
    ARENA(false),
    KIT(false),
    KIT_ACTIONS(false),
    KIT_CHOICE(false),
    TEAMS(false),
    TEAM_ACTIONS(false),
    TEAM_MEMBERS(false),
    MATCH(false),
    FORCE_RESULT(false),
    WAITING_MATCHES(false),
    WAITING_MATCH_RESULT(false),
    CONFIRMATION(false),
    BRACKET_VIEW(true);

    private final boolean readableByPlayers;

    AdminMenuType(boolean readableByPlayers) {
        this.readableByPlayers = readableByPlayers;
    }

    public boolean isReadableByPlayers() {
        return readableByPlayers;
    }
}

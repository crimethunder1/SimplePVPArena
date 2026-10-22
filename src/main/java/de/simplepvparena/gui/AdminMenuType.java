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
    REGISTRATIONS(false),
    MATCH(false),
    FORCE_RESULT(false),
    WAITING_MATCHES(false),
    WAITING_MATCH_RESULT(false),
    CONFIRMATION(false),
    BRACKET_VIEW(true),
    UNEQUAL_TEAM_VOTE(false, true);

    private final boolean readableByPlayers;
    private final boolean readableByVotingTeam;

    AdminMenuType(boolean readableByPlayers) {
        this(readableByPlayers, false);
    }

    AdminMenuType(boolean readableByPlayers, boolean readableByVotingTeam) {
        this.readableByPlayers = readableByPlayers;
        this.readableByVotingTeam = readableByVotingTeam;
    }

    public boolean isReadableByPlayers() {
        return readableByPlayers;
    }

    public boolean isReadableByVotingTeam() {
        return readableByVotingTeam;
    }
}

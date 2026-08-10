package de.simplepvparena.team;

import de.simplepvparena.input.ChatInputValues;

import java.util.Optional;

public final class TeamCreationRequest {

    public static final int REQUIRED_VALUE_COUNT = 3;
    public static final String USAGE = "<teamName> <firstPlayer> <secondPlayer>";

    private final String teamName;
    private final String firstPlayerName;
    private final String secondPlayerName;
    private final String rejectionMessage;

    private TeamCreationRequest(String teamName, String firstPlayerName, String secondPlayerName,
                                String rejectionMessage) {
        this.teamName = teamName;
        this.firstPlayerName = firstPlayerName;
        this.secondPlayerName = secondPlayerName;
        this.rejectionMessage = rejectionMessage;
    }

    public static TeamCreationRequest parse(String line) {
        ChatInputValues values = ChatInputValues.of(line);
        Optional<String> countRejection = values.findCountRejection(REQUIRED_VALUE_COUNT, USAGE);
        if (countRejection.isPresent()) {
            return new TeamCreationRequest(null, null, null, countRejection.get());
        }
        String teamName = values.get(0);
        String firstPlayerName = values.get(1);
        String secondPlayerName = values.get(2);
        if (firstPlayerName.equalsIgnoreCase(secondPlayerName)) {
            return new TeamCreationRequest(null, null, null, "A team needs two different players, "
                    + firstPlayerName + " was named twice");
        }
        return new TeamCreationRequest(teamName, firstPlayerName, secondPlayerName, null);
    }

    public boolean isAccepted() {
        return rejectionMessage == null;
    }

    public String getTeamName() {
        return requireAccepted(teamName);
    }

    public String getFirstPlayerName() {
        return requireAccepted(firstPlayerName);
    }

    public String getSecondPlayerName() {
        return requireAccepted(secondPlayerName);
    }

    public Optional<String> getRejectionMessage() {
        return Optional.ofNullable(rejectionMessage);
    }

    private String requireAccepted(String value) {
        if (!isAccepted()) {
            throw new IllegalStateException("a rejected team line has no values");
        }
        return value;
    }
}

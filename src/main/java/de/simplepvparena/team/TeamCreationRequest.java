package de.simplepvparena.team;

import de.simplepvparena.input.ChatInputValues;

import java.util.List;
import java.util.Optional;

public final class TeamCreationRequest {

    public static final int MINIMUM_VALUE_COUNT = 1 + Team.MINIMUM_MEMBER_COUNT;
    public static final String USAGE = "<teamName> <player> [<player> ...]";

    private final String teamName;
    private final List<String> playerNames;
    private final String rejectionMessage;

    private TeamCreationRequest(String teamName, List<String> playerNames, String rejectionMessage) {
        this.teamName = teamName;
        this.playerNames = playerNames;
        this.rejectionMessage = rejectionMessage;
    }

    public static TeamCreationRequest parse(String line, int teamSize) {
        if (!TeamSizeSetting.isAllowed(teamSize)) {
            throw new IllegalArgumentException(TeamSizeSetting.ALLOWED_RANGE_REJECTION + ", not " + teamSize);
        }
        ChatInputValues values = ChatInputValues.of(line);
        Optional<String> countRejection = values.findCountRejection(MINIMUM_VALUE_COUNT, 1 + teamSize, USAGE);
        if (countRejection.isPresent()) {
            return rejected(countRejection.get());
        }
        String teamName = values.get(0);
        List<String> playerNames = List.copyOf(values.values().subList(1, values.count()));
        Optional<String> playerNamedTwice = findPlayerNamedTwice(playerNames);
        if (playerNamedTwice.isPresent()) {
            return rejected("A team needs " + Team.MINIMUM_MEMBER_COUNT + " to " + teamSize
                    + " different players, " + playerNamedTwice.get() + " was named twice");
        }
        return new TeamCreationRequest(teamName, playerNames, null);
    }

    public boolean isAccepted() {
        return rejectionMessage == null;
    }

    public String getTeamName() {
        return requireAccepted(teamName);
    }

    public List<String> getPlayerNames() {
        return requireAccepted(playerNames);
    }

    public Optional<String> getRejectionMessage() {
        return Optional.ofNullable(rejectionMessage);
    }

    private static TeamCreationRequest rejected(String rejectionMessage) {
        return new TeamCreationRequest(null, null, rejectionMessage);
    }

    private static Optional<String> findPlayerNamedTwice(List<String> playerNames) {
        for (int laterIndex = 1; laterIndex < playerNames.size(); laterIndex++) {
            for (int earlierIndex = 0; earlierIndex < laterIndex; earlierIndex++) {
                if (playerNames.get(earlierIndex).equalsIgnoreCase(playerNames.get(laterIndex))) {
                    return Optional.of(playerNames.get(earlierIndex));
                }
            }
        }
        return Optional.empty();
    }

    private <T> T requireAccepted(T value) {
        if (!isAccepted()) {
            throw new IllegalStateException("a rejected team line has no values");
        }
        return value;
    }
}

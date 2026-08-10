package de.simplepvparena.match;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RoundParticipants {

    private final String firstTeamName;
    private final String secondTeamName;
    private final Set<UUID> firstTeamActivePlayers;
    private final Set<UUID> secondTeamActivePlayers;

    public RoundParticipants(String firstTeamName,
                             Collection<UUID> firstTeamPlayers,
                             String secondTeamName,
                             Collection<UUID> secondTeamPlayers) {
        this.firstTeamName = Objects.requireNonNull(firstTeamName, "firstTeamName");
        this.secondTeamName = Objects.requireNonNull(secondTeamName, "secondTeamName");
        if (this.firstTeamName.equals(this.secondTeamName)) {
            throw new IllegalArgumentException("a round needs two different teams");
        }
        this.firstTeamActivePlayers = new LinkedHashSet<>(Objects.requireNonNull(firstTeamPlayers, "firstTeamPlayers"));
        this.secondTeamActivePlayers = new LinkedHashSet<>(Objects.requireNonNull(secondTeamPlayers, "secondTeamPlayers"));
    }

    public boolean eliminate(UUID playerUniqueId) {
        return firstTeamActivePlayers.remove(playerUniqueId) || secondTeamActivePlayers.remove(playerUniqueId);
    }

    public boolean isActive(UUID playerUniqueId) {
        return firstTeamActivePlayers.contains(playerUniqueId) || secondTeamActivePlayers.contains(playerUniqueId);
    }

    public Set<UUID> getActivePlayers(String teamName) {
        return Set.copyOf(selectActivePlayers(teamName));
    }

    public Set<UUID> getAllActivePlayers() {
        Set<UUID> allActivePlayers = new LinkedHashSet<>(firstTeamActivePlayers);
        allActivePlayers.addAll(secondTeamActivePlayers);
        return allActivePlayers;
    }

    public boolean isTeamDefeated(String teamName) {
        return selectActivePlayers(teamName).isEmpty();
    }

    public boolean isSameActiveTeam(UUID firstPlayerUniqueId, UUID secondPlayerUniqueId) {
        if (firstPlayerUniqueId == null || secondPlayerUniqueId == null
                || firstPlayerUniqueId.equals(secondPlayerUniqueId)) {
            return false;
        }
        Optional<String> firstPlayerTeamName = findTeamNameOfPlayer(firstPlayerUniqueId);
        Optional<String> secondPlayerTeamName = findTeamNameOfPlayer(secondPlayerUniqueId);
        return firstPlayerTeamName.isPresent() && firstPlayerTeamName.equals(secondPlayerTeamName);
    }

    public Optional<String> findTeamNameOfPlayer(UUID playerUniqueId) {
        if (firstTeamActivePlayers.contains(playerUniqueId)) {
            return Optional.of(firstTeamName);
        }
        if (secondTeamActivePlayers.contains(playerUniqueId)) {
            return Optional.of(secondTeamName);
        }
        return Optional.empty();
    }

    public Optional<RoundOutcome> determineOutcome() {
        boolean firstTeamDefeated = firstTeamActivePlayers.isEmpty();
        boolean secondTeamDefeated = secondTeamActivePlayers.isEmpty();
        if (firstTeamDefeated && secondTeamDefeated) {
            return Optional.of(RoundOutcome.draw());
        }
        if (firstTeamDefeated) {
            return Optional.of(RoundOutcome.victory(secondTeamName));
        }
        if (secondTeamDefeated) {
            return Optional.of(RoundOutcome.victory(firstTeamName));
        }
        return Optional.empty();
    }

    public String getFirstTeamName() {
        return firstTeamName;
    }

    public String getSecondTeamName() {
        return secondTeamName;
    }

    private Set<UUID> selectActivePlayers(String teamName) {
        if (firstTeamName.equals(teamName)) {
            return firstTeamActivePlayers;
        }
        if (secondTeamName.equals(teamName)) {
            return secondTeamActivePlayers;
        }
        throw new IllegalArgumentException(teamName + " does not take part in this round");
    }
}

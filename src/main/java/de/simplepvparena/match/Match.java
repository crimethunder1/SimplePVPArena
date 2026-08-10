package de.simplepvparena.match;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.team.Team;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Match {

    private final String matchId;
    private final Team firstTeam;
    private final Team secondTeam;
    private final Arena arena;
    private final MatchScore score;

    private MatchState state = MatchState.PREPARING;
    private RoundParticipants roundParticipants;
    private int roundNumber;

    public Match(String matchId, Team firstTeam, Team secondTeam, Arena arena) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.firstTeam = Objects.requireNonNull(firstTeam, "firstTeam");
        this.secondTeam = Objects.requireNonNull(secondTeam, "secondTeam");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.score = new MatchScore(firstTeam.getTeamName(), secondTeam.getTeamName());
    }

    public String getMatchId() {
        return matchId;
    }

    public Team getFirstTeam() {
        return firstTeam;
    }

    public Team getSecondTeam() {
        return secondTeam;
    }

    public List<Team> getTeams() {
        return List.of(firstTeam, secondTeam);
    }

    public Arena getArena() {
        return arena;
    }

    public MatchScore getScore() {
        return score;
    }

    public MatchState getState() {
        return state;
    }

    public void setState(MatchState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void beginRound(Collection<UUID> firstTeamActivePlayers, Collection<UUID> secondTeamActivePlayers) {
        roundParticipants = new RoundParticipants(firstTeam.getTeamName(), firstTeamActivePlayers,
                secondTeam.getTeamName(), secondTeamActivePlayers);
        roundNumber++;
        state = MatchState.RUNNING;
    }

    public Optional<RoundParticipants> getRoundParticipants() {
        return Optional.ofNullable(roundParticipants);
    }

    public boolean containsPlayer(UUID playerUniqueId) {
        return firstTeam.containsPlayer(playerUniqueId) || secondTeam.containsPlayer(playerUniqueId);
    }

    public Optional<Team> findTeamOfPlayer(UUID playerUniqueId) {
        if (firstTeam.containsPlayer(playerUniqueId)) {
            return Optional.of(firstTeam);
        }
        if (secondTeam.containsPlayer(playerUniqueId)) {
            return Optional.of(secondTeam);
        }
        return Optional.empty();
    }

    public Optional<Team> findTeamByName(String teamName) {
        if (firstTeam.getTeamName().equalsIgnoreCase(teamName)) {
            return Optional.of(firstTeam);
        }
        if (secondTeam.getTeamName().equalsIgnoreCase(teamName)) {
            return Optional.of(secondTeam);
        }
        return Optional.empty();
    }

    public Team getOpponentOf(Team team) {
        return team.equals(firstTeam) ? secondTeam : firstTeam;
    }

    public boolean isFinished() {
        return state == MatchState.FINISHED || state == MatchState.CANCELLED;
    }

    public String describe() {
        return matchId + " in arena " + arena.getArenaName() + " (" + score.describe() + ")";
    }
}

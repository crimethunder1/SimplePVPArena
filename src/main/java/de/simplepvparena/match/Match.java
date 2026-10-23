package de.simplepvparena.match;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamRegistry;

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
    private final boolean finalMatch;
    private final boolean autoMatch;

    private MatchState state = MatchState.PREPARING;
    private RoundParticipants roundParticipants;
    private int roundNumber;
    private Team teamWithChosenFighters;
    private List<UUID> chosenFighterUniqueIds = List.of();

    private Match(String matchId, Team firstTeam, Team secondTeam, Arena arena, boolean finalMatch,
                  boolean autoMatch) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.firstTeam = Objects.requireNonNull(firstTeam, "firstTeam");
        this.secondTeam = Objects.requireNonNull(secondTeam, "secondTeam");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.score = new MatchScore(firstTeam.getTeamName(), secondTeam.getTeamName());
        this.finalMatch = finalMatch;
        this.autoMatch = autoMatch;
    }

    public static Match regularMatch(String matchId, Team firstTeam, Team secondTeam, Arena arena) {
        return new Match(matchId, firstTeam, secondTeam, arena, false, false);
    }

    public static Match finalMatch(String matchId, Team firstTeam, Team secondTeam, Arena arena) {
        return new Match(matchId, firstTeam, secondTeam, arena, true, false);
    }

    public static Match autoMatch(String matchId, Team firstTeam, Team secondTeam, Arena arena) {
        return new Match(matchId, firstTeam, secondTeam, arena, false, true);
    }

    public String getMatchId() {
        return matchId;
    }

    public boolean isFinalMatch() {
        return finalMatch;
    }

    public boolean isAutoMatch() {
        return autoMatch;
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

    public void chooseFighters(Team team, Collection<UUID> fighterUniqueIds) {
        Optional<Team> teamOfThisMatch = findTeamByName(team.getTeamName());
        if (teamOfThisMatch.isEmpty()) {
            throw new IllegalArgumentException(team.getTeamName() + " does not take part in match " + matchId);
        }
        if (fighterUniqueIds.isEmpty()) {
            teamWithChosenFighters = null;
            chosenFighterUniqueIds = List.of();
            return;
        }
        teamWithChosenFighters = teamOfThisMatch.get();
        chosenFighterUniqueIds = List.copyOf(fighterUniqueIds);
    }

    public Optional<List<UUID>> findChosenFighters(Team team) {
        if (teamWithChosenFighters == null
                || !TeamRegistry.isSameTeamName(teamWithChosenFighters.getTeamName(), team.getTeamName())) {
            return Optional.empty();
        }
        return Optional.of(chosenFighterUniqueIds);
    }

    public Optional<Team> findTeamWithChosenFighters() {
        return Optional.ofNullable(teamWithChosenFighters);
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
        if (TeamRegistry.isSameTeamName(firstTeam.getTeamName(), teamName)) {
            return Optional.of(firstTeam);
        }
        if (TeamRegistry.isSameTeamName(secondTeam.getTeamName(), teamName)) {
            return Optional.of(secondTeam);
        }
        return Optional.empty();
    }

    public Team getOpponentOf(Team team) {
        return TeamRegistry.isSameTeamName(team.getTeamName(), firstTeam.getTeamName()) ? secondTeam : firstTeam;
    }

    public boolean isFinished() {
        return state == MatchState.FINISHED || state == MatchState.CANCELLED;
    }

    public String describe() {
        return matchId + " (" + score.describe() + ")";
    }
}

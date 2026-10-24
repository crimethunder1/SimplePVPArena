package de.simplepvparena.placement;

import de.simplepvparena.match.Match;
import de.simplepvparena.team.Team;
import de.simplepvparena.tournament.Bracket;
import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.TournamentState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PlacementService {

    private static final int CHAMPION_PLACE = 1;

    private final Supplier<TournamentState> tournamentStateSource;
    private final Supplier<Optional<Bracket>> bracketSource;
    private final Supplier<Optional<Match>> activeMatchSource;
    private final Function<String, Optional<Team>> teamLookup;

    public PlacementService(Supplier<TournamentState> tournamentStateSource,
                            Supplier<Optional<Bracket>> bracketSource,
                            Supplier<Optional<Match>> activeMatchSource,
                            Function<String, Optional<Team>> teamLookup) {
        this.tournamentStateSource = Objects.requireNonNull(tournamentStateSource, "tournamentStateSource");
        this.bracketSource = Objects.requireNonNull(bracketSource, "bracketSource");
        this.activeMatchSource = Objects.requireNonNull(activeMatchSource, "activeMatchSource");
        this.teamLookup = Objects.requireNonNull(teamLookup, "teamLookup");
    }

    public PlacementStandings findStandings() {
        TournamentState state = tournamentStateSource.get();
        Optional<Bracket> bracket = bracketSource.get();
        if (bracket.isPresent() && state == TournamentState.RUNNING) {
            return describeTournament(PlacementPhase.TOURNAMENT_RUNNING, bracket.get());
        }
        if (bracket.isPresent() && state == TournamentState.PAUSED) {
            return describeTournament(PlacementPhase.TOURNAMENT_PAUSED, bracket.get());
        }
        if (bracket.isPresent() && state == TournamentState.FINISHED) {
            return describeTournament(PlacementPhase.TOURNAMENT_OVER, bracket.get());
        }
        return PlacementStandings.none();
    }

    private static int placeOfTheLosersOf(List<BracketMatch> round) {
        return round.size() + 1;
    }

    private PlacementStandings describeTournament(PlacementPhase phase, Bracket bracket) {
        List<List<BracketMatch>> rounds = bracket.getRounds();
        Optional<Match> tournamentMatch = activeMatchSource.get().filter(match -> !match.isAutoMatch());
        int roundIndex = tournamentMatch.flatMap(match -> bracket.findMatch(match.getMatchId()))
                .map(BracketMatch::getRoundIndex)
                .or(() -> findFirstUndecidedRoundIndex(rounds))
                .orElse(rounds.size() - 1);
        Set<String> losers = new HashSet<>();
        List<PlacementElimination> eliminations = new ArrayList<>();
        for (int index = 0; index < rounds.size(); index++) {
            List<String> roundLosers = findLosers(rounds.get(index));
            losers.addAll(roundLosers);
            if (!roundLosers.isEmpty()) {
                eliminations.add(new PlacementElimination(index + 1, toTeams(roundLosers)));
            }
        }
        List<String> remainingTeamNames = bracket.getParticipatingTeamNames().stream()
                .filter(teamName -> !losers.contains(teamName))
                .toList();
        return PlacementStandings.tournament(phase, roundIndex + 1, rounds.size(),
                tournamentMatch.map(PlacementMatch::of), toTeams(remainingTeamNames), eliminations,
                bracket.isComplete() ? rank(bracket, rounds) : List.of());
    }

    private List<PlacementRank> rank(Bracket bracket, List<List<BracketMatch>> rounds) {
        List<PlacementRank> ranks = new ArrayList<>();
        bracket.getChampionTeamName().ifPresent(champion -> ranks.add(new PlacementRank(CHAMPION_PLACE,
                toTeam(champion))));
        for (int index = rounds.size() - 1; index >= 0; index--) {
            List<BracketMatch> round = rounds.get(index);
            int place = placeOfTheLosersOf(round);
            for (String loser : findLosers(round)) {
                ranks.add(new PlacementRank(place, toTeam(loser)));
            }
        }
        return ranks;
    }

    private static Optional<Integer> findFirstUndecidedRoundIndex(List<List<BracketMatch>> rounds) {
        for (int index = 0; index < rounds.size(); index++) {
            if (rounds.get(index).stream().anyMatch(match -> !match.isDecided())) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    private static List<String> findLosers(List<BracketMatch> round) {
        List<String> losers = new ArrayList<>();
        for (BracketMatch match : round) {
            if (match.isDecided() && match.hasBothParticipants()) {
                match.getOpponentOf(match.getWinnerTeamName().orElseThrow()).ifPresent(losers::add);
            }
        }
        return losers;
    }

    private List<PlacementTeam> toTeams(List<String> teamNames) {
        return teamNames.stream().map(this::toTeam).toList();
    }

    private PlacementTeam toTeam(String teamName) {
        return PlacementTeam.of(teamName, teamLookup.apply(teamName));
    }
}

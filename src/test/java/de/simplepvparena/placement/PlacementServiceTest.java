package de.simplepvparena.placement;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.match.Match;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.Bracket;
import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.TournamentState;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementServiceTest {

    private final TeamRegistry teamRegistry = new TeamRegistry();

    private TournamentState tournamentState = TournamentState.IDLE;
    private Bracket bracket;
    private Match activeMatch;

    private final PlacementService placementService = new PlacementService(() -> tournamentState,
            () -> Optional.ofNullable(bracket), () -> Optional.ofNullable(activeMatch), teamRegistry::findTeam);

    @Test
    void showsNothingWithoutATournament() {
        assertEquals(PlacementStandings.none(), placementService.findStandings());
        assertFalse(placementService.findStandings().hasContent());

        tournamentState = TournamentState.CANCELLED;
        assertFalse(placementService.findStandings().hasContent());
    }

    @Test
    void describesTheFirstRoundOfARunningTournamentWithTheRunningMatchInTheTeamColours() {
        registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        startTournament("Alpha", "Bravo", "Charlie", "Delta");
        BracketMatch firstMatch = bracket.getRounds().get(0).get(0);
        activeMatch = runningMatchOf(firstMatch);
        activeMatch.getScore().awardPoint(activeMatch.getFirstTeam().getTeamName());

        PlacementStandings standings = placementService.findStandings();

        assertEquals(PlacementPhase.TOURNAMENT_RUNNING, standings.phase());
        assertEquals(1, standings.roundNumber());
        assertEquals(2, standings.roundCount());
        assertEquals(Optional.of(new PlacementMatch(new PlacementTeam("Alpha", NamedTextColor.RED), 1,
                new PlacementTeam("Delta", NamedTextColor.WHITE), 0)), standings.runningMatch());
        assertEquals(List.of("Alpha", "Bravo", "Charlie", "Delta"), namesOf(standings.remainingTeams()));
        assertEquals(List.of(), standings.eliminations());
        assertEquals(List.of(), standings.ranks());
    }

    @Test
    void namesWhoIsOutInWhichRoundAndWhoIsStillIn() {
        registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        startTournament("Alpha", "Bravo", "Charlie", "Delta");
        playRoundWithTheEarlierNameWinning(0);

        PlacementStandings standings = placementService.findStandings();

        assertEquals(2, standings.roundNumber(), "the final is the next round");
        assertEquals(Optional.empty(), standings.runningMatch());
        assertEquals(List.of("Alpha", "Bravo"), namesOf(standings.remainingTeams()));
        assertEquals(List.of(new PlacementElimination(1, List.of(new PlacementTeam("Delta", NamedTextColor.WHITE),
                new PlacementTeam("Charlie", NamedTextColor.AQUA)))), standings.eliminations());
    }

    @Test
    void aByeIsNoDefeat() {
        registerTeams("T01", "T02", "T03", "T04", "T05", "T06");
        startTournament("T01", "T02", "T03", "T04", "T05", "T06");

        PlacementStandings standings = placementService.findStandings();

        assertEquals(6, standings.remainingTeams().size());
        assertEquals(List.of(), standings.eliminations());
        assertEquals(1, standings.roundNumber());
        assertEquals(3, standings.roundCount());
    }

    @Test
    void marksAPausedTournament() {
        registerTeams("Alpha", "Bravo");
        startTournament("Alpha", "Bravo");
        tournamentState = TournamentState.PAUSED;

        assertEquals(PlacementPhase.TOURNAMENT_PAUSED, placementService.findStandings().phase());
    }

    @Test
    void ranksFourTeams() {
        assertEquals(List.of("1 T01", "2 T02", "3 T04", "3 T03"), placesOfACompletedTournament(4));
    }

    @Test
    void ranksSixTeamsAndCountsTheTeamsThatSurvivedTheFirstRound() {
        assertEquals(List.of("1 T01", "2 T02", "3 T04", "3 T03", "5 T05", "5 T06"), placesOfACompletedTournament(6));
    }

    @Test
    void ranksEightTeams() {
        assertEquals(List.of("1 T01", "2 T02", "3 T04", "3 T03", "5 T08", "5 T05", "5 T07", "5 T06"),
                placesOfACompletedTournament(8));
    }

    @Test
    void ranksTwelveTeams() {
        assertEquals(List.of("1 T01", "2 T02", "3 T04", "3 T03", "5 T08", "5 T05", "5 T07", "5 T06",
                        "9 T09", "9 T12", "9 T10", "9 T11"),
                placesOfACompletedTournament(12));
    }

    @Test
    void theOverTournamentKeepsItsEliminationsAndItsLastRound() {
        placesOfACompletedTournament(4);

        PlacementStandings standings = placementService.findStandings();

        assertEquals(PlacementPhase.TOURNAMENT_OVER, standings.phase());
        assertEquals(2, standings.roundNumber());
        assertEquals(List.of("T01"), namesOf(standings.remainingTeams()));
        assertEquals(List.of(1, 2), standings.eliminations().stream().map(PlacementElimination::roundNumber)
                .toList());
    }

    @Test
    void showsNothingWithoutATournamentNotEvenARunningAutoMatch() {
        registerTeams("Alpha", "Bravo");
        activeMatch = Match.autoMatch("auto-1", teamRegistry.findTeam("Alpha").orElseThrow(),
                teamRegistry.findTeam("Bravo").orElseThrow(), new Arena());
        activeMatch.getScore().awardPoint("Bravo");

        assertEquals(PlacementStandings.none(), placementService.findStandings());
    }

    @Test
    void aRunningAPausedAndAnOverTournamentAreShown() {
        registerTeams("Alpha", "Bravo");
        startTournament("Alpha", "Bravo");

        assertEquals(PlacementPhase.TOURNAMENT_RUNNING, placementService.findStandings().phase());
        tournamentState = TournamentState.PAUSED;
        assertEquals(PlacementPhase.TOURNAMENT_PAUSED, placementService.findStandings().phase());

        bracket.recordWinner(bracket.getFinalMatch().getMatchId(), "Alpha");
        tournamentState = TournamentState.FINISHED;
        assertEquals(PlacementPhase.TOURNAMENT_OVER, placementService.findStandings().phase());
    }

    @Test
    void anOverTournamentStaysWhileAnAutoMatchRunsAndLeavesTheAutoMatchOut() {
        registerTeams("Alpha", "Bravo");
        startTournament("Alpha", "Bravo");
        bracket.recordWinner(bracket.getFinalMatch().getMatchId(), "Alpha");
        tournamentState = TournamentState.FINISHED;
        activeMatch = Match.autoMatch("auto-1", teamRegistry.findTeam("Alpha").orElseThrow(),
                teamRegistry.findTeam("Bravo").orElseThrow(), new Arena());

        PlacementStandings standings = placementService.findStandings();

        assertEquals(PlacementPhase.TOURNAMENT_OVER, standings.phase());
        assertEquals(Optional.empty(), standings.runningMatch());
        assertEquals(List.of("1 Alpha", "2 Bravo"), standings.ranks().stream()
                .map(rank -> rank.place() + " " + rank.team().name())
                .toList());
    }

    @Test
    void aTeamThatIsNoLongerRegisteredIsShownInWhite() {
        startTournament("Ghost", "Bravo");
        registerTeams("Alpha", "Bravo");

        List<PlacementTeam> remainingTeams = placementService.findStandings().remainingTeams();

        assertEquals(List.of(new PlacementTeam("Ghost", NamedTextColor.WHITE),
                new PlacementTeam("Bravo", NamedTextColor.DARK_GREEN)), remainingTeams);
    }

    @Test
    void theStandingsCarryContentInEveryPhaseButNone() {
        for (PlacementPhase phase : PlacementPhase.values()) {
            PlacementStandings standings = phase.isTournament()
                    ? PlacementStandings.tournament(phase, 1, 1, Optional.empty(), List.of(), List.of(), List.of())
                    : PlacementStandings.none();
            assertEquals(phase != PlacementPhase.NONE, standings.hasContent(), phase.name());
        }
        assertTrue(PlacementPhase.TOURNAMENT_OVER.isTournament());
        assertFalse(PlacementPhase.NONE.isTournament());
        assertEquals(4, PlacementPhase.values().length, "the auto match mode has no phase of its own");
    }

    private List<String> placesOfACompletedTournament(int teamCount) {
        List<String> teamNames = new ArrayList<>();
        for (int number = 1; number <= teamCount; number++) {
            teamNames.add(String.format("T%02d", number));
        }
        registerTeams(teamNames.toArray(String[]::new));
        startTournament(teamNames.toArray(String[]::new));
        for (int roundIndex = 0; roundIndex < bracket.getRoundCount(); roundIndex++) {
            assertTrue(placementService.findStandings().ranks().isEmpty(), "no place before the end");
            playRoundWithTheEarlierNameWinning(roundIndex);
        }
        tournamentState = TournamentState.FINISHED;
        return placementService.findStandings().ranks().stream()
                .map(rank -> rank.place() + " " + rank.team().name())
                .toList();
    }

    private void playRoundWithTheEarlierNameWinning(int roundIndex) {
        for (BracketMatch match : bracket.getRounds().get(roundIndex)) {
            if (match.isPlayable()) {
                String firstTeamName = match.getFirstTeamName().orElseThrow();
                String secondTeamName = match.getSecondTeamName().orElseThrow();
                bracket.recordWinner(match.getMatchId(),
                        firstTeamName.compareTo(secondTeamName) < 0 ? firstTeamName : secondTeamName);
            }
        }
    }

    private void startTournament(String... teamNames) {
        bracket = Bracket.createSingleElimination(List.of(teamNames));
        tournamentState = TournamentState.RUNNING;
    }

    private Match runningMatchOf(BracketMatch bracketMatch) {
        return Match.regularMatch(bracketMatch.getMatchId(),
                teamRegistry.findTeam(bracketMatch.getFirstTeamName().orElseThrow()).orElseThrow(),
                teamRegistry.findTeam(bracketMatch.getSecondTeamName().orElseThrow()).orElseThrow(), new Arena());
    }

    private void registerTeams(String... teamNames) {
        List<TeamColour> colours = List.of(TeamColour.RED, TeamColour.GREEN, TeamColour.LIGHT_BLUE);
        for (int index = 0; index < teamNames.length; index++) {
            List<TeamMember> members = List.of(new TeamMember(UUID.randomUUID(), teamNames[index] + "Player"));
            if (index < colours.size()) {
                teamRegistry.registerTeam(teamNames[index], members, colours.get(index));
            } else {
                teamRegistry.registerTeam(teamNames[index], members);
            }
        }
    }

    private static List<String> namesOf(List<PlacementTeam> teams) {
        return teams.stream().map(PlacementTeam::name).toList();
    }
}

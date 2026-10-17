package de.simplepvparena.tournament;

import de.simplepvparena.match.Match;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerTeamEditTest {

    private static final List<String> FOUR_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta");
    private static final String RENAME_REJECTION = "A team cannot be renamed while a tournament is in progress, "
            + "the bracket keeps its name";

    @Test
    void allowsToRenameAndChangeEveryTeamWithoutATournament() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);

        for (String teamName : FOUR_TEAMS) {
            assertEquals(Optional.empty(), fixture.tournamentManager().findTeamRenameRejection(teamName), teamName);
            assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection(teamName), teamName);
        }
    }

    @Test
    void refusesToRenameAnyTeamWhileATournamentRuns() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();

        for (String teamName : FOUR_TEAMS) {
            assertEquals(Optional.of(RENAME_REJECTION), fixture.tournamentManager().findTeamRenameRejection(teamName),
                    teamName);
        }
    }

    @Test
    void refusesToRenameAnyTeamWhileATournamentIsPaused() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        fixture.endRunningMatchAsPlayedByTheFirstTeam();
        assertTrue(fixture.tournamentManager().togglePause().successful());
        String teamWithoutMatch = fixture.bracketMatch("round1-match1").getSecondTeamName().orElseThrow();

        assertEquals(Optional.of(RENAME_REJECTION),
                fixture.tournamentManager().findTeamRenameRejection(teamWithoutMatch));
        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection(teamWithoutMatch));
    }

    @Test
    void allowsToRenameAgainOnceTheTournamentIsCancelled() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();

        assertTrue(fixture.tournamentManager().cancelTournament().successful());

        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamRenameRejection("Alpha"));
        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection("Alpha"));
    }

    @Test
    void refusesToChangeBothTeamsOfTheRunningMatchInAnyCase() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        Match runningMatch = fixture.matchService().getActiveMatch().orElseThrow();

        for (String teamName : List.of(runningMatch.getFirstTeam().getTeamName(),
                runningMatch.getSecondTeam().getTeamName())) {
            assertEquals(Optional.of("Team " + teamName + " is in match round1-match1, it cannot be changed before "
                            + "the match is over"),
                    fixture.tournamentManager().findTeamChangeRejection(teamName.toUpperCase(Locale.ROOT)), teamName);
        }
    }

    @Test
    void allowsToChangeTheTeamsWithoutAMatchDuringTheTournament() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        BracketMatch waitingMatch = fixture.bracketMatch("round1-match2");

        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection(
                waitingMatch.getFirstTeamName().orElseThrow()));
        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection(
                waitingMatch.getSecondTeamName().orElseThrow()));
    }

    @Test
    void allowsToChangeTheTeamsOfAMatchOnceItIsOver() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        BracketMatch firstMatch = fixture.bracketMatch("round1-match1");

        String winnerTeamName = fixture.endRunningMatchAsPlayedByTheFirstTeam();

        assertEquals(Optional.of("round1-match2"), fixture.runningMatchId());
        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection(winnerTeamName));
        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection(
                firstMatch.getOpponentOf(winnerTeamName).orElseThrow()));
    }

    @Test
    void refusesToChangeOrRenameTheTeamsOfAMatchOutsideOfATournament() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startMatchOutsideTheTournament("Alpha", "Bravo");

        assertEquals(Optional.of("Team Alpha is in match dry-run, it cannot be changed before the match is over"),
                fixture.tournamentManager().findTeamChangeRejection("Alpha"));
        assertEquals(Optional.of("Team Bravo is in match dry-run, it cannot be changed before the match is over"),
                fixture.tournamentManager().findTeamRenameRejection("bravo"));
        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamRenameRejection("Charlie"));
    }

    @Test
    void refusesToChangeBothTeamsOfTheMatchWhoseKitIsChosen() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        BracketMatch choosingMatch = fixture.bracketMatch("round1-match1");
        BracketMatch waitingMatch = fixture.bracketMatch("round1-match2");

        for (String teamName : List.of(choosingMatch.getFirstTeamName().orElseThrow(),
                choosingMatch.getSecondTeamName().orElseThrow())) {
            assertEquals(Optional.of("Team " + teamName + " is waiting for the kit choice of match round1-match1, "
                            + "it cannot be changed before the match is over"),
                    fixture.tournamentManager().findTeamChangeRejection(teamName.toLowerCase(Locale.ROOT)), teamName);
            assertEquals(Optional.of(RENAME_REJECTION), fixture.tournamentManager().findTeamRenameRejection(teamName));
        }
        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection(
                waitingMatch.getFirstTeamName().orElseThrow()));
    }

    @Test
    void allowsToChangeTheTeamsOfTheChosenMatchOnlyAfterItIsOver() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        String teamName = fixture.bracketMatch("round1-match1").getFirstTeamName().orElseThrow();
        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));

        assertEquals(Optional.of("Team " + teamName + " is in match round1-match1, it cannot be changed before the "
                + "match is over"), fixture.tournamentManager().findTeamChangeRejection(teamName));

        fixture.endRunningMatchAsPlayedByTheFirstTeam();

        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection(teamName));
    }

    @Test
    void removingATeamWithoutAMatchStillEndsItsNextMatchByWalkover() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        BracketMatch waitingMatch = fixture.bracketMatch("round1-match2");
        String removedTeamName = waitingMatch.getFirstTeamName().orElseThrow();
        String remainingTeamName = waitingMatch.getSecondTeamName().orElseThrow();
        assertEquals(Optional.empty(), fixture.tournamentManager().findTeamChangeRejection(removedTeamName));

        assertTrue(fixture.teamRegistry().removeTeam(removedTeamName));
        fixture.endRunningMatchAsPlayedByTheFirstTeam();

        assertEquals(Optional.of(remainingTeamName), fixture.bracketMatch("round1-match2").getWinnerTeamName());
        assertEquals(Optional.empty(), fixture.runningMatchId());
    }

    @Test
    void theRunningMatchKeepsTheTeamOfItsStartWhileTheNextMatchTakesTheChangedTeam() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        Match runningMatch = fixture.matchService().getActiveMatch().orElseThrow();
        String runningTeamName = runningMatch.getFirstTeam().getTeamName();
        fixture.teamRegistry().removeMember(runningTeamName,
                fixture.teamRegistry().findTeam(runningTeamName).orElseThrow().getMembers().getFirst().uniqueId());
        BracketMatch waitingMatch = fixture.bracketMatch("round1-match2");
        String changedTeamName = waitingMatch.getFirstTeamName().orElseThrow();
        fixture.teamRegistry().removeMember(changedTeamName,
                fixture.teamRegistry().findTeam(changedTeamName).orElseThrow().getMembers().getFirst().uniqueId());

        assertEquals(runningTeamName + "-first, " + runningTeamName + "-second",
                runningMatch.getFirstTeam().getMemberNamesJoined());

        fixture.endRunningMatchAsPlayedByTheFirstTeam();

        Match nextMatch = fixture.matchService().getActiveMatch().orElseThrow();
        assertEquals("round1-match2", nextMatch.getMatchId());
        assertEquals(changedTeamName + "-second", nextMatch.findTeamByName(changedTeamName).orElseThrow()
                .getMemberNamesJoined());
    }
}

package de.simplepvparena.tournament;

import de.simplepvparena.match.UnequalTeamSettings;
import de.simplepvparena.match.UnequalTeamsMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerTeamSizeTest {

    private final TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(List.of());

    @Test
    void startsWithoutANoticeWhenEveryTeamHasTheTeamSize() {
        fixture.setTeamSize(3);
        for (String teamName : List.of("Alpha", "Bravo", "Charlie", "Delta")) {
            fixture.registerTeamOf(teamName, 3);
        }

        fixture.startTournament();

        assertEquals(List.of(), fixture.adminMessages());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
    }

    @Test
    void startsAnywayAndNamesTheTeamsWithFewerPlayersThanTheTeamSize() {
        fixture.setTeamSize(3);
        fixture.registerTeamOf("Alpha", 3);
        fixture.registerTeamOf("Bravo", 2);
        fixture.registerTeamOf("Charlie", 1);
        fixture.registerTeamOf("Delta", 3);

        fixture.startTournament();

        assertEquals(List.of("2 teams have fewer than 3 players: Bravo (2) and Charlie (1). The tournament starts "
                + "anyway, a missing player counts as absent"), fixture.adminMessages());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertTrue(fixture.runningMatchId().isPresent());
    }

    @Test
    void startsAnywayAndNamesATeamWithMorePlayersThanTheTeamSize() {
        fixture.registerTeamOf("Alpha", 2);
        fixture.registerTeamOf("Bravo", 3);
        fixture.registerTeamOf("Charlie", 2);
        fixture.registerTeamOf("Delta", 2);

        fixture.startTournament();

        assertEquals(List.of("1 team has more than 2 players: Bravo (3). The tournament starts anyway, the larger "
                + "team votes who fights"), fixture.adminMessages());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
    }

    @Test
    void saysThatEveryOnlinePlayerOfTheLargerTeamFightsInTheModeAll() {
        fixture.matchService().setUnequalTeamSettings(new UnequalTeamSettings(UnequalTeamsMode.ALL,
                UnequalTeamSettings.DEFAULT_VOTE_SECONDS));
        fixture.registerTeamOf("Alpha", 2);
        fixture.registerTeamOf("Bravo", 3);
        fixture.registerTeamOf("Charlie", 2);
        fixture.registerTeamOf("Delta", 2);

        fixture.startTournament();

        assertEquals(List.of("1 team has more than 2 players: Bravo (3). The tournament starts anyway, every "
                + "online player of this team fights"), fixture.adminMessages());
    }

    @Test
    void namesTheSmallerAndTheLargerTeamsInTwoNotices() {
        fixture.setTeamSize(2);
        fixture.registerTeamOf("Alpha", 1);
        fixture.registerTeamOf("Bravo", 2);
        fixture.registerTeamOf("Charlie", 4);
        fixture.registerTeamOf("Delta", 2);

        fixture.startTournament();

        assertEquals(List.of(
                "1 team has fewer than 2 players: Alpha (1). The tournament starts anyway, a missing player counts "
                        + "as absent",
                "1 team has more than 2 players: Charlie (4). The tournament starts anyway, the larger team votes "
                        + "who fights"), fixture.adminMessages());
    }

    @Test
    void namesFiveSmallerTeamsAndCountsTheOthers() {
        fixture.setTeamSize(8);
        for (String teamName : List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel")) {
            fixture.registerTeamOf(teamName, 2);
        }

        fixture.startTournament();

        assertEquals(List.of("8 teams have fewer than 8 players: Alpha (2), Bravo (2), Charlie (2), Delta (2), "
                + "Echo (2) and 3 more. The tournament starts anyway, a missing player counts as absent"),
                fixture.adminMessages());
    }

    @Test
    void namesTheNoticeOnlyOnceAndNotAgainWhenThePausedTournamentContinues() {
        fixture.setTeamSize(3);
        fixture.registerTeamOf("Alpha", 3);
        fixture.registerTeamOf("Bravo", 2);
        fixture.startTournament();

        assertTrue(fixture.tournamentManager().togglePause().successful());
        assertTrue(fixture.tournamentManager().togglePause().successful());

        assertEquals(1, fixture.adminMessagesContaining("fewer than 3 players").size());
    }

    @Test
    void stillRefusesAStartWithASingleTeamAndSaysNothingAboutItsSize() {
        fixture.setTeamSize(4);
        fixture.registerTeamOf("Alpha", 1);

        TournamentOperationResult start = fixture.tournamentManager().startTournament(true);

        assertFalse(start.successful());
        assertEquals("At least 2 teams are required", start.message());
        assertEquals(List.of(), fixture.adminMessages());
    }

    @Test
    void knowsWhenATournamentIsInProgress() {
        fixture.registerTeamOf("Alpha", 2);
        fixture.registerTeamOf("Bravo", 2);
        assertFalse(fixture.tournamentManager().isTournamentInProgress());

        fixture.startTournament();
        assertTrue(fixture.tournamentManager().isTournamentInProgress());

        fixture.tournamentManager().togglePause();
        assertTrue(fixture.tournamentManager().isTournamentInProgress());

        fixture.tournamentManager().cancelTournament();
        assertFalse(fixture.tournamentManager().isTournamentInProgress());
    }
}

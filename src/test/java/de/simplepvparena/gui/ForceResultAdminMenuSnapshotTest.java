package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForceResultAdminMenuSnapshotTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MATCH_FORCE_RESULT_SLOT = 16;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsBothTeamsOfTheRunningMatch() {
        openTheForceResultMenuOfARunningTournament();

        assertEquals("""
                Result of round1-match1 | 27
                4 | BOOK | Force a result | Match round1-match1 (%1$s 0 : 0 %2$s) / Round 1 / \
                A click decides the match for that team
                11 | PLAYER_HEAD | %1$s | %1$s-first / %1$s-second / Decides the match for this team
                15 | PLAYER_HEAD | %2$s | %2$s-first / %2$s-second / Decides the match for this team
                22 | OAK_DOOR | Back | Returns to the match menu
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName()),
                fixture.describeLastMenu());
    }

    @Test
    void showsThatTheMatchIsNotRunningAnyMoreWhenTheMenuIsFilledAfterItWasDecided() {
        openTheForceResultMenuOfARunningTournament();
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());

        fixture.refillLastMenu();

        assertEquals("""
                Result of round1-match1 | 27
                4 | BOOK | Force a result | Match round1-match1 is not running any more
                22 | OAK_DOOR | Back | Returns to the match menu
                """, fixture.describeLastMenu());
    }

    private void openTheForceResultMenuOfARunningTournament() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_FORCE_RESULT_SLOT);
    }
}

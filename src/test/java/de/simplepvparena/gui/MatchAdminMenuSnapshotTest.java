package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchAdminMenuSnapshotTest {

    private static final int MAIN_MATCH_SLOT = 12;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheMatchMenuBeforeTheTournamentWithoutTeams() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match and tournament | 27
                4 | BOOK | Tournament | Tournament state: IDLE / Running match: none / Round timer: running
                10 | LIME_WOOL | Start the tournament | Needs an even number of teams
                11 | YELLOW_WOOL | Force start | Starts the tournament with an uneven number of teams
                12 | GRAY_DYE | Pause the tournament | No tournament is in progress
                13 | GRAY_DYE | Cancel the tournament | No tournament is in progress
                15 | GRAY_DYE | Start the final | The final is not ready, its stage is PENDING
                16 | GRAY_DYE | Force a result | No match is running
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMatchMenuBeforeTheTournamentWithEnoughTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match and tournament | 27
                4 | BOOK | Tournament | Tournament state: IDLE / Running match: none / Round timer: running
                10 | LIME_WOOL | Start the tournament | Needs an even number of teams
                11 | YELLOW_WOOL | Force start | Starts the tournament with an uneven number of teams
                12 | GRAY_DYE | Pause the tournament | No tournament is in progress
                13 | GRAY_DYE | Cancel the tournament | No tournament is in progress
                15 | GRAY_DYE | Start the final | The final is not ready, its stage is PENDING
                16 | GRAY_DYE | Force a result | No match is running
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMatchMenuWhileTheTournamentRunsAMatch() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match and tournament | 27
                4 | BOOK | Tournament | Tournament state: RUNNING | rounds: 2 | running match: round1-match1 | final: \
                PENDING / Running match: round1-match1 (%1$s 0 : 0 %2$s) / Round timer: running
                10 | GRAY_DYE | Start the tournament | A tournament is already in progress
                11 | GRAY_DYE | Force start | A tournament is already in progress
                12 | CLOCK | Pause the tournament | No further match is started until it continues / \
                The round timer keeps its remaining time
                13 | RED_WOOL | Cancel the tournament | Stops the running match and the bracket / \
                Asks for a confirmation first
                15 | GRAY_DYE | Start the final | The final is not ready, its stage is PENDING
                16 | ANVIL | Force a result | Decides the running match for one of its two teams
                22 | OAK_DOOR | Back | Returns to the main menu
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName()),
                fixture.describeLastMenu());
    }

    @Test
    void showsTheMatchMenuWhileTheTournamentIsPaused() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.pauseTournament();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match and tournament | 27
                4 | BOOK | Tournament | Tournament state: PAUSED | rounds: 2 | running match: round1-match1 | final: \
                PENDING / Running match: round1-match1 (%1$s 0 : 0 %2$s) / Round timer: paused
                10 | GRAY_DYE | Start the tournament | A tournament is already in progress
                11 | GRAY_DYE | Force start | A tournament is already in progress
                12 | CLOCK | Continue the tournament | The round timer continues with its remaining time
                13 | RED_WOOL | Cancel the tournament | Stops the running match and the bracket / \
                Asks for a confirmation first
                15 | GRAY_DYE | Start the final | The final is not ready, its stage is PENDING
                16 | ANVIL | Force a result | Decides the running match for one of its two teams
                22 | OAK_DOOR | Back | Returns to the main menu
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName()),
                fixture.describeLastMenu());
    }

    @Test
    void showsTheMatchMenuWhenTheFinalIsReady() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match and tournament | 27
                4 | BOOK | Tournament | Tournament state: RUNNING | rounds: 1 | running match: none | final: READY / \
                Running match: none / Round timer: running
                10 | GRAY_DYE | Start the tournament | A tournament is already in progress
                11 | GRAY_DYE | Force start | A tournament is already in progress
                12 | CLOCK | Pause the tournament | No further match is started until it continues / \
                The round timer keeps its remaining time
                13 | RED_WOOL | Cancel the tournament | Stops the running match and the bracket / \
                Asks for a confirmation first
                15 | NETHER_STAR | Start the final | Both finalists are known / Starts the final on the arena
                16 | GRAY_DYE | Force a result | No match is running
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMatchMenuAfterTheTournamentIsFinished() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.startFinal();
        fixture.decideRunningMatchFor("Alpha");

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match and tournament | 27
                4 | BOOK | Tournament | Tournament state: FINISHED | rounds: 1 | running match: none | final: FINISHED \
                | champion: Alpha / Running match: none / Round timer: running
                10 | LIME_WOOL | Start the tournament | Needs an even number of teams
                11 | YELLOW_WOOL | Force start | Starts the tournament with an uneven number of teams
                12 | GRAY_DYE | Pause the tournament | No tournament is in progress
                13 | GRAY_DYE | Cancel the tournament | No tournament is in progress
                15 | GRAY_DYE | Start the final | The final is over
                16 | GRAY_DYE | Force a result | No match is running
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMatchMenuWhileTheFinalIsRunning() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.startFinal();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match and tournament | 27
                4 | BOOK | Tournament | Tournament state: RUNNING | rounds: 1 | running match: round1-match1 | final: \
                RUNNING / Running match: round1-match1 (%1$s 0 : 0 %2$s) / Round timer: running
                10 | GRAY_DYE | Start the tournament | A tournament is already in progress
                11 | GRAY_DYE | Force start | A tournament is already in progress
                12 | CLOCK | Pause the tournament | No further match is started until it continues / \
                The round timer keeps its remaining time
                13 | RED_WOOL | Cancel the tournament | Stops the running match and the bracket / \
                Asks for a confirmation first
                15 | GRAY_DYE | Start the final | The final is running
                16 | ANVIL | Force a result | Decides the running match for one of its two teams
                22 | OAK_DOOR | Back | Returns to the main menu
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName()),
                fixture.describeLastMenu());
    }
}

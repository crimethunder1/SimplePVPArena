package de.simplepvparena.gui;

import de.simplepvparena.team.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminMenuChatInputTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int MAIN_TEAMS_SLOT = 11;

    private static final int ARENA_DIAGNOSE_SLOT = 11;
    private static final int ARENA_START_GATE_SLOT = 20;
    private static final int TEAMS_ADD_SLOT = 51;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;

    private static final String NOT_ALLOWED = "You are not allowed to use the tournament menu";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void closesTheTeamsMenuRegistersOneRequestAndAsksForTheTeamToAdd() {
        fixture.setTeamSize(Team.MAXIMUM_MEMBER_COUNT);
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(TEAMS_ADD_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.pendingChatInputCount());
        assertEquals(List.of(
                "Type: <teamName> <player> [<player> ...] in chat",
                "A team has 1 to 8 players, a team name has 1 to 16 letters, digits, hyphens or underscores",
                "Example: Alpha Steve Alex",
                "Example: Bravo Notch Jeb_ Dinnerbone",
                "Every player has to be online or known to the server",
                "Type cancel to abort, the request expires in 60 seconds"), fixture.messages());
        assertEquals(0, fixture.scheduledTaskCount());
    }

    @Test
    void registersAndSavesTheTypedTeamOnlyOnTheNextTickAndReopensTheTeamList() {
        askForTheTeamToAdd();

        assertTrue(fixture.typeInChat("Bravo Steve Alex"));

        assertNothingDoneBeforeTheNextTick();
        assertTrue(fixture.teamRegistry().findTeam("Bravo").isEmpty());
        assertEquals(0, fixture.teamConfigurationSaveCount());

        fixture.runNextTick();

        assertEquals(List.of("Team Bravo registered with Steve, Alex"), fixture.messages());
        assertEquals("Steve, Alex", fixture.teamRegistry().findTeam("Bravo").orElseThrow().getMemberNamesJoined());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertTeamsReopened(0, "Teams (page 1)");
        assertEquals("Bravo", fixture.lastMenu().findTile(1).orElseThrow().displayName());
    }

    @Test
    void reopensTheTeamListOnThePageTheTeamWasAskedFrom() {
        fixture.makePlayersKnown("Steve", "Alex");
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_ADD_SLOT);
        fixture.messages().clear();

        fixture.typeInChat("Bravo Steve Alex");
        fixture.runNextTick();

        assertEquals(List.of("Team Bravo registered with Steve, Alex"), fixture.messages());
        assertTeamsReopened(1, "Teams (page 2)");
        assertEquals("Bravo", fixture.lastMenu().findTile(1).orElseThrow().displayName());
    }

    @Test
    void rejectsATeamLineWithTooFewValuesAndReopensTheTeamList() {
        fixture.setTeamSize(Team.MAXIMUM_MEMBER_COUNT);
        askForTheTeamToAdd();

        fixture.typeInChat("Bravo");
        fixture.runNextTick();

        assertEquals(List.of("Expected 2 to 9 values separated by spaces (<teamName> <player> [<player> ...]), "
                + "but got 1: Bravo"), fixture.messages());
        assertNoTeamAdded();
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void rejectsATeamLineThatNamesOnePlayerTwiceAndReopensTheTeamList() {
        fixture.setTeamSize(Team.MAXIMUM_MEMBER_COUNT);
        askForTheTeamToAdd();

        fixture.typeInChat("Bravo Steve steve");
        fixture.runNextTick();

        assertEquals(List.of("A team needs 1 to 8 different players, Steve was named twice"), fixture.messages());
        assertNoTeamAdded();
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void rejectsATeamNameThatIsAlreadyRegisteredAndReopensTheTeamList() {
        askForTheTeamToAdd();

        fixture.typeInChat("alpha Steve Alex");
        fixture.runNextTick();

        assertEquals(List.of("Team alpha could not be registered\nA team named alpha already exists"),
                fixture.messages());
        assertNoTeamAdded();
        assertEquals("Alpha-first, Alpha-second",
                fixture.teamRegistry().findTeam("Alpha").orElseThrow().getMemberNamesJoined());
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void rejectsAPlayerThatIsAlreadyInAnotherTeamAndNamesTheTypedTeam() {
        fixture.makePlayersKnown("Notch");
        askForTheTeamToAdd();
        fixture.typeInChat("Bravo Steve Alex");
        fixture.runNextTick();
        fixture.click(TEAMS_ADD_SLOT);
        fixture.messages().clear();

        fixture.typeInChat("Charlie Steve Notch");
        fixture.runNextTick();

        assertEquals(List.of("Team Charlie could not be registered\nSteve is already registered in team Bravo"),
                fixture.messages());
        assertEquals(List.of("Alpha", "Bravo"), fixture.teamRegistry().getTeamNames());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void rejectsAFirstPlayerThatTheLookupDoesNotFindAndReopensTheTeamList() {
        askForTheTeamToAdd();

        fixture.typeInChat("Bravo Herobrine Alex");
        fixture.runNextTick();

        assertEquals(List.of("Unknown player Herobrine\nThe player has to be online or known to the server"),
                fixture.messages());
        assertNoTeamAdded();
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void rejectsASecondPlayerThatTheLookupDoesNotFindAndReopensTheTeamList() {
        askForTheTeamToAdd();

        fixture.typeInChat("Bravo Steve Herobrine");
        fixture.runNextTick();

        assertEquals(List.of("Unknown player Herobrine\nThe player has to be online or known to the server"),
                fixture.messages());
        assertNoTeamAdded();
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void registersTypedTeamsOfOneToEightPlayers() {
        fixture.setTeamSize(Team.MAXIMUM_MEMBER_COUNT);
        askForTheTeamToAdd();
        List<String> expectedTeamNames = new ArrayList<>(List.of("Alpha"));

        for (int playerCount = 1; playerCount <= 8; playerCount++) {
            List<String> playerNames = new ArrayList<>();
            for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
                playerNames.add("T" + playerCount + "Player" + playerNumber);
            }
            fixture.makePlayersKnown(playerNames.toArray(String[]::new));
            fixture.messages().clear();

            fixture.typeInChat("Team" + playerCount + " " + String.join(" ", playerNames));
            fixture.runNextTick();

            assertEquals(List.of("Team Team" + playerCount + " registered with " + String.join(", ", playerNames)),
                    fixture.messages());
            assertEquals(String.join(", ", playerNames),
                    fixture.teamRegistry().findTeam("Team" + playerCount).orElseThrow().getMemberNamesJoined());
            assertTeamsReopened(0, "Teams (page 1)");
            expectedTeamNames.add("Team" + playerCount);
            fixture.click(TEAMS_ADD_SLOT);
        }
        assertEquals(expectedTeamNames, fixture.teamRegistry().getTeamNames());
        assertEquals(8, fixture.teamConfigurationSaveCount());
    }

    @Test
    void rejectsATeamLineWithNinePlayersAndReopensTheTeamList() {
        fixture.setTeamSize(Team.MAXIMUM_MEMBER_COUNT);
        askForTheTeamToAdd();

        fixture.typeInChat("Bravo P1 P2 P3 P4 P5 P6 P7 P8 P9");
        fixture.runNextTick();

        assertEquals(List.of("Expected 2 to 9 values separated by spaces (<teamName> <player> [<player> ...]), "
                + "but got 10: Bravo P1 P2 P3 P4 P5 P6 P7 P8 P9"), fixture.messages());
        assertNoTeamAdded();
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void rejectsATeamLineThatNamesAPlayerTwiceApartAndReopensTheTeamList() {
        fixture.setTeamSize(Team.MAXIMUM_MEMBER_COUNT);
        askForTheTeamToAdd();
        fixture.makePlayersKnown("Notch", "Jeb_");

        fixture.typeInChat("Bravo Alex Notch Steve Jeb_ ALEX");
        fixture.runNextTick();

        assertEquals(List.of("A team needs 1 to 8 different players, Alex was named twice"), fixture.messages());
        assertNoTeamAdded();
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void rejectsAPlayerThatTheLookupDoesNotFindAtAnyPositionAndReopensTheTeamList() {
        fixture.setTeamSize(Team.MAXIMUM_MEMBER_COUNT);
        List<String> knownPlayerNames = List.of("Steve", "Alex", "Notch", "Jeb_");
        askForTheTeamToAdd();
        fixture.makePlayersKnown("Notch", "Jeb_");

        for (int unknownPosition = 0; unknownPosition <= knownPlayerNames.size(); unknownPosition++) {
            List<String> playerNames = new ArrayList<>(knownPlayerNames);
            playerNames.add(unknownPosition, "Herobrine");
            fixture.click(TEAMS_ADD_SLOT);
            fixture.messages().clear();

            fixture.typeInChat("Bravo " + String.join(" ", playerNames));
            fixture.runNextTick();

            assertEquals(List.of("Unknown player Herobrine\nThe player has to be online or known to the server"),
                    fixture.messages(), "position " + unknownPosition);
            assertNoTeamAdded();
            assertTeamsReopened(0, "Teams (page 1)");
        }
    }

    @Test
    void rejectsATeamNameOutsideTheNameRuleAndReopensTheTeamList() {
        askForTheTeamToAdd();

        fixture.typeInChat("Team.One Steve Alex");
        fixture.runNextTick();

        assertEquals(List.of("Team Team.One could not be registered\nThe team name Team.One is not allowed, "
                + "a team name has 1 to 16 letters, digits, hyphens or underscores"), fixture.messages());
        assertNoTeamAdded();
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void rejectsATeamNameOfSeventeenCharactersAndReopensTheTeamList() {
        askForTheTeamToAdd();

        fixture.typeInChat("ABCDEFGHIJKLMNOPQ Steve");
        fixture.runNextTick();

        assertEquals(List.of("Team ABCDEFGHIJKLMNOPQ could not be registered\nThe team name ABCDEFGHIJKLMNOPQ is "
                + "not allowed, a team name has 1 to 16 letters, digits, hyphens or underscores"), fixture.messages());
        assertNoTeamAdded();
    }

    @Test
    void registersATeamNameWithAnUmlautAndRejectsItInOtherCase() {
        askForTheTeamToAdd();

        fixture.typeInChat("Büffel Steve");
        fixture.runNextTick();
        fixture.click(TEAMS_ADD_SLOT);
        fixture.messages().clear();
        fixture.typeInChat("BÜFFEL Alex");
        fixture.runNextTick();

        assertEquals(List.of("Team BÜFFEL could not be registered\nA team named BÜFFEL already exists"),
                fixture.messages());
        assertEquals(List.of("Alpha", "Büffel"), fixture.teamRegistry().getTeamNames());
        assertEquals("Steve", fixture.teamRegistry().findTeam("büffel").orElseThrow().getMemberNamesJoined());
        assertEquals(1, fixture.teamConfigurationSaveCount());
    }

    @Test
    void cancelsTheTeamInputOnTheNextTickWithoutReopeningTheMenu() {
        askForTheTeamToAdd();

        assertTrue(fixture.typeInChat("cancel"));

        assertNothingDoneBeforeTheNextTick();

        fixture.runNextTick();

        assertEquals(List.of("The input for the team to add was cancelled, nothing was changed"), fixture.messages());
        assertNoTeamAdded();
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void refusesTheTypedTeamWhenThePermissionWasRevokedBeforeTheAnswer() {
        askForTheTeamToAdd();
        fixture.revokeAdminPermission();

        fixture.typeInChat("Bravo Steve Alex");
        fixture.runNextTick();

        assertEquals(List.of(NOT_ALLOWED), fixture.messages());
        assertNoTeamAdded();
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void closesTheArenaMenuRegistersOneRequestAndAsksForTheDiagnosisCellLimit() {
        openTheArenaMenuOfAReadyArena();

        fixture.click(ARENA_DIAGNOSE_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.pendingChatInputCount());
        assertEquals(List.of(
                "Type: <cells> in chat",
                "A number between 1 and 5000000",
                "Stand inside the border wall, the calculation runs from where you stand",
                "config.yml is not changed by it",
                "Type cancel to abort, the request expires in 60 seconds"), fixture.messages());
        assertEquals(0, fixture.scheduledTaskCount());
    }

    @Test
    void runsTheDiagnosisWithTheTypedCellLimitOnlyOnTheNextTick() {
        askForTheDiagnosisCellLimit();

        assertTrue(fixture.typeInChat("60000"));

        assertNothingDoneBeforeTheNextTick();

        fixture.runNextTick();

        assertEquals(List.of(
                "Calculating once with a limit of 60000 cells\n"
                        + "instead of the 50000 of arena.shape-maximum-cells, config.yml is not changed",
                "The fight area was calculated from the border blocks\n"
                        + "admin-menu with 25 cells between 1/1 and 5/5, from y 64 to y 67, calculated at y 64",
                "Reading the 375 blocks from y 53 to y 67, the fight area and the layers under its floor, as the "
                        + "baseline \"Clean the arena\" in the arena menu of /tournament restores\n"
                        + "In batches of 16384 per tick, the arena cannot be cleaned until that is done.\n"
                        + "The baseline is gone when the server restarts",
                "The recognized wall is shown for a few seconds\n96 border blocks connected by 184 lines"),
                fixture.messages());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void rejectsACellLimitThatIsNoNumberAndReopensTheArenaMenu() {
        askForTheDiagnosisCellLimit();

        fixture.typeInChat("many");
        fixture.runNextTick();

        assertEquals(List.of("The cell limit many is not a number"), fixture.messages());
        assertArenaReopened();
    }

    @Test
    void rejectsACellLimitOutsideOfTheAllowedRangeAndReopensTheArenaMenu() {
        askForTheDiagnosisCellLimit();

        fixture.typeInChat("0");
        fixture.runNextTick();

        assertEquals(List.of("The cell limit has to be between 1 and 5000000"), fixture.messages());
        assertArenaReopened();
    }

    @Test
    void cancelsTheDiagnosisInputOnTheNextTickWithoutReopeningTheMenu() {
        askForTheDiagnosisCellLimit();

        assertTrue(fixture.typeInChat("CANCEL"));

        assertNothingDoneBeforeTheNextTick();

        fixture.runNextTick();

        assertEquals(List.of("The input for the diagnosis cell limit was cancelled, nothing was changed"),
                fixture.messages());
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void refusesTheDiagnosisWhenThePermissionWasRevokedBeforeTheAnswer() {
        askForTheDiagnosisCellLimit();
        fixture.revokeAdminPermission();

        fixture.typeInChat("10");
        fixture.runNextTick();

        assertEquals(List.of(NOT_ALLOWED), fixture.messages());
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void closesTheArenaMenuRegistersOneRequestAndAsksForTheStartGateBlocks() {
        openTheArenaMenuOfAReadyArena();

        fixture.click(ARENA_START_GATE_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.pendingChatInputCount());
        assertEquals(List.of(
                "Type: <material> <amount> in chat",
                "Example: barrier 32",
                "The amount has to be between 1 and 64",
                "Type cancel to abort, the request expires in 60 seconds"), fixture.messages());
        assertEquals(0, fixture.scheduledTaskCount());
    }

    @Test
    void rejectsAnUnknownStartGateMaterialOnlyOnTheNextTickAndReopensTheArenaMenu() {
        askForTheStartGateBlocks();

        assertTrue(fixture.typeInChat("unobtainium 32"));

        assertNothingDoneBeforeTheNextTick();

        fixture.runNextTick();

        assertEquals(List.of("Unknown material unobtainium"), fixture.messages());
        assertTrue(fixture.handedOutItems().isEmpty());
        assertArenaReopened();
    }

    @Test
    void rejectsAStartGateLineWithoutAnAmountAndReopensTheArenaMenu() {
        askForTheStartGateBlocks();

        fixture.typeInChat("barrier");
        fixture.runNextTick();

        assertEquals(List.of("Expected 2 values separated by spaces (<material> <amount>), but got 1: barrier"),
                fixture.messages());
        assertTrue(fixture.handedOutItems().isEmpty());
        assertArenaReopened();
    }

    @Test
    void cancelsTheStartGateInputOnTheNextTickWithoutReopeningTheMenu() {
        askForTheStartGateBlocks();

        assertTrue(fixture.typeInChat(" cancel "));

        assertNothingDoneBeforeTheNextTick();

        fixture.runNextTick();

        assertEquals(List.of("The input for the start gate blocks was cancelled, nothing was changed"),
                fixture.messages());
        assertTrue(fixture.handedOutItems().isEmpty());
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void refusesTheStartGateBlocksWhenThePermissionWasRevokedBeforeTheAnswer() {
        askForTheStartGateBlocks();
        fixture.revokeAdminPermission();

        fixture.typeInChat("barrier 32");
        fixture.runNextTick();

        assertEquals(List.of(NOT_ALLOWED), fixture.messages());
        assertTrue(fixture.handedOutItems().isEmpty());
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void asksForATeamOfTheCurrentTeamSize() {
        List<String> expectedSizeLines = List.of(
                "A team has 1 player, a team name has 1 to 16 letters, digits, hyphens or underscores",
                "A team has 1 to 2 players, a team name has 1 to 16 letters, digits, hyphens or underscores",
                "A team has 1 to 3 players, a team name has 1 to 16 letters, digits, hyphens or underscores");

        for (int teamSize = 1; teamSize <= 3; teamSize++) {
            fixture.setTeamSize(teamSize);
            fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

            fixture.click(TEAMS_ADD_SLOT);

            assertEquals(expectedSizeLines.get(teamSize - 1), fixture.messages().get(1), "a team size of " + teamSize);
            fixture.typeInChat("cancel");
            fixture.runNextTick();
        }
    }

    @Test
    void registersTypedTeamsUpToTheTeamSizeAndRejectsATeamOfOnePlayerMore() {
        askForTheTeamToAdd();
        List<String> expectedTeamNames = new ArrayList<>(List.of("Alpha"));
        List<String> expectedRejections = List.of(
                "Expected 2 values separated by spaces (<teamName> <player> [<player> ...]), but got 3: "
                        + "Large1 L1Player1 L1Player2",
                "Expected 2 to 3 values separated by spaces (<teamName> <player> [<player> ...]), but got 4: "
                        + "Large2 L2Player1 L2Player2 L2Player3",
                "Expected 2 to 4 values separated by spaces (<teamName> <player> [<player> ...]), but got 5: "
                        + "Large3 L3Player1 L3Player2 L3Player3 L3Player4");

        for (int teamSize = 1; teamSize <= 3; teamSize++) {
            fixture.setTeamSize(teamSize);
            for (int playerCount = 1; playerCount <= teamSize; playerCount++) {
                String teamName = "S" + teamSize + "Team" + playerCount;
                List<String> playerNames = createKnownPlayerNames(teamName, playerCount);
                fixture.messages().clear();

                fixture.typeInChat(teamName + " " + String.join(" ", playerNames));
                fixture.runNextTick();

                assertEquals(List.of("Team " + teamName + " registered with " + String.join(", ", playerNames)),
                        fixture.messages(), playerCount + " players at a team size of " + teamSize);
                expectedTeamNames.add(teamName);
                fixture.click(TEAMS_ADD_SLOT);
            }
            List<String> tooManyPlayerNames = createKnownPlayerNames("L" + teamSize, teamSize + 1);
            fixture.messages().clear();

            fixture.typeInChat("Large" + teamSize + " " + String.join(" ", tooManyPlayerNames));
            fixture.runNextTick();

            assertEquals(List.of(expectedRejections.get(teamSize - 1)), fixture.messages());
            assertEquals(expectedTeamNames, fixture.teamRegistry().getTeamNames());
            assertTeamsReopened(0, "Teams (page 1)");
            fixture.click(TEAMS_ADD_SLOT);
        }
        assertEquals(6, fixture.teamConfigurationSaveCount());
    }

    @Test
    void namesTheCurrentTeamSizeWhenATypedTeamNamesAPlayerTwice() {
        fixture.setTeamSize(3);
        askForTheTeamToAdd();

        fixture.typeInChat("Bravo Alex Steve ALEX");
        fixture.runNextTick();

        assertEquals(List.of("A team needs 1 to 3 different players, Alex was named twice"), fixture.messages());
        assertNoTeamAdded();
        assertTeamsReopened(0, "Teams (page 1)");
    }

    @Test
    void checksTheTypedTeamAgainstTheTeamSizeOfTheMomentTheLineArrives() {
        fixture.makePlayersKnown("Notch");
        fixture.setTeamSize(3);
        askForTheTeamToAdd();

        fixture.setTeamSize(2);
        fixture.typeInChat("Bravo Steve Alex Notch");
        fixture.runNextTick();

        assertEquals(List.of("Expected 2 to 3 values separated by spaces (<teamName> <player> [<player> ...]), "
                + "but got 4: Bravo Steve Alex Notch"), fixture.messages());
        assertNoTeamAdded();
        assertTeamsReopened(0, "Teams (page 1)");
    }

    private List<String> createKnownPlayerNames(String prefix, int playerCount) {
        List<String> playerNames = new ArrayList<>();
        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            playerNames.add(prefix + "Player" + playerNumber);
        }
        fixture.makePlayersKnown(playerNames.toArray(String[]::new));
        return playerNames;
    }

    private void askForTheTeamToAdd() {
        fixture.makePlayersKnown("Steve", "Alex");
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_ADD_SLOT);
        fixture.messages().clear();
    }

    private void askForTheDiagnosisCellLimit() {
        openTheArenaMenuOfAReadyArena();
        fixture.standInsideTheFightArea();
        fixture.click(ARENA_DIAGNOSE_SLOT);
        fixture.messages().clear();
    }

    private void askForTheStartGateBlocks() {
        openTheArenaMenuOfAReadyArena();
        fixture.click(ARENA_START_GATE_SLOT);
        fixture.messages().clear();
    }

    private void openTheArenaMenuOfAReadyArena() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
    }

    private void assertNothingDoneBeforeTheNextTick() {
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(0, fixture.pendingChatInputCount());
        assertEquals(1, fixture.scheduledTaskCount());
        assertTrue(fixture.isMenuClosed());
    }

    private void assertNoTeamAdded() {
        assertEquals(List.of("Alpha"), fixture.teamRegistry().getTeamNames());
        assertEquals(0, fixture.teamConfigurationSaveCount());
    }

    private void assertTeamsReopened(int pageIndex, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAMS, menu.holder().getMenuType());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals(title, menu.title());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.pendingChatInputCount());
    }

    private void assertArenaReopened() {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.ARENA, menu.holder().getMenuType());
        assertEquals(Optional.empty(), menu.holder().getSubject());
        assertEquals("Arena", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.pendingChatInputCount());
    }
}

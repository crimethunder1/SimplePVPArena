package de.simplepvparena.gui;

import de.simplepvparena.team.TeamMember;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamEditChatInputTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_FIRST_TEAM_SLOT = 0;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;
    private static final int RENAME_SLOT = 10;
    private static final int ADD_PLAYER_SLOT = 11;
    private static final int BACK_SLOT = 22;

    private static final String NOT_ALLOWED = "You are not allowed to use the tournament menu";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void closesTheOptionsRegistersOneRequestAndAsksForTheNewName() {
        fixture.registerTeams("Alpha", "Bravo", "Charlie");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(1);

        fixture.click(RENAME_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.pendingChatInputCount());
        assertEquals(List.of(
                "Type: <newName> in chat",
                "A team name has 1 to 16 letters, digits, hyphens or underscores",
                "Example: Phoenix",
                "The team keeps its players and its place in the team list",
                "Type cancel to abort, the request expires in 60 seconds"), fixture.messages());
        assertEquals(0, fixture.scheduledTaskCount());
    }

    @Test
    void renamesTheTeamOnTheNextTickSavesAndOpensItsOptionsWithTheNewName() {
        askForTheNewNameOfBravo();

        assertTrue(fixture.typeInChat("Delta"));

        assertNothingDoneBeforeTheNextTick();
        assertEquals(List.of("Alpha", "Bravo", "Charlie"), fixture.teamRegistry().getTeamNames());

        fixture.runNextTick();

        assertEquals(List.of("Team Bravo is now called Delta"), fixture.messages());
        assertEquals(List.of("Alpha", "Delta", "Charlie"), fixture.teamRegistry().getTeamNames());
        assertEquals("Bravo-first, Bravo-second",
                fixture.teamRegistry().findTeam("Delta").orElseThrow().getMemberNamesJoined());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertTeamActionsOpened("Delta", 0);

        fixture.click(BACK_SLOT);

        assertEquals("Teams (page 1)", fixture.lastMenu().title());
        assertEquals("Delta", fixture.lastMenu().findTile(1).orElseThrow().displayName());
    }

    @Test
    void keepsTheSecondPageOfTheTeamListAndThePlaceOfTheRenamedTeam() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
        fixture.click(RENAME_SLOT);
        fixture.messages().clear();

        fixture.typeInChat("Phoenix");
        fixture.runNextTick();

        assertEquals(List.of("Team Team46 is now called Phoenix"), fixture.messages());
        assertTeamActionsOpened("Phoenix", 1);

        fixture.click(BACK_SLOT);

        assertEquals("Teams (page 2)", fixture.lastMenu().title());
        assertEquals("Phoenix", fixture.lastMenu().findTile(TEAMS_FIRST_TEAM_SLOT).orElseThrow().displayName());
    }

    @Test
    void renamesTheTeamToAnotherCaseOfItsOwnName() {
        askForTheNewNameOfBravo();

        fixture.typeInChat("BRAVO");
        fixture.runNextTick();

        assertEquals(List.of("Team Bravo is now called BRAVO"), fixture.messages());
        assertEquals(List.of("Alpha", "BRAVO", "Charlie"), fixture.teamRegistry().getTeamNames());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertTeamActionsOpened("BRAVO", 0);
    }

    @Test
    void rejectsANewNameOutsideTheNameRuleAndOpensTheOptionsAgain() {
        askForTheNewNameOfBravo();

        fixture.typeInChat("Bravo Team");
        fixture.runNextTick();

        assertEquals(List.of("Team Bravo could not be renamed\nThe team name Bravo Team is not allowed, a team name "
                + "has 1 to 16 letters, digits, hyphens or underscores"), fixture.messages());
        assertNoTeamRenamed();
        assertTeamActionsOpened("Bravo", 0);
    }

    @Test
    void rejectsTheNameOfAnotherTeamInAnyCaseAndOpensTheOptionsAgain() {
        askForTheNewNameOfBravo();

        fixture.typeInChat("alpha");
        fixture.runNextTick();

        assertEquals(List.of("Team Bravo could not be renamed\nA team named alpha already exists"),
                fixture.messages());
        assertNoTeamRenamed();
        assertTeamActionsOpened("Bravo", 0);
    }

    @Test
    void cancelsTheNewNameOnTheNextTickWithoutReopeningTheMenu() {
        askForTheNewNameOfBravo();

        assertTrue(fixture.typeInChat("cancel"));
        assertNothingDoneBeforeTheNextTick();
        fixture.runNextTick();

        assertEquals(List.of("The input for the new name of team Bravo was cancelled, nothing was changed"),
                fixture.messages());
        assertNoTeamRenamed();
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void refusesTheNewNameWhenThePermissionWasRevokedBeforeTheAnswer() {
        askForTheNewNameOfBravo();
        fixture.revokeAdminPermission();

        fixture.typeInChat("Delta");
        fixture.runNextTick();

        assertEquals(List.of(NOT_ALLOWED), fixture.messages());
        assertNoTeamRenamed();
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void refusesTheNewNameWhenATournamentStartedBetweenTheClickAndTheAnswer() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(1);
        fixture.click(RENAME_SLOT);
        fixture.startTournament();
        fixture.messages().clear();

        fixture.typeInChat("Phoenix");
        fixture.runNextTick();

        assertEquals(List.of("A team cannot be renamed while a tournament is in progress, the bracket keeps its "
                + "name"), fixture.messages());
        assertEquals(List.of("Alpha", "Bravo", "Charlie", "Delta"), fixture.teamRegistry().getTeamNames());
        assertEquals(0, fixture.teamConfigurationSaveCount());
        assertTeamActionsOpened("Bravo", 0);
    }

    @Test
    void refusesTheNewNameWhenTheTeamWasRemovedBetweenTheClickAndTheAnswer() {
        askForTheNewNameOfBravo();
        assertTrue(fixture.teamRegistry().removeTeam("Bravo"));

        fixture.typeInChat("Delta");
        fixture.runNextTick();

        assertEquals(List.of("This team is not registered any more"), fixture.messages());
        assertEquals(List.of("Alpha", "Charlie"), fixture.teamRegistry().getTeamNames());
        assertEquals(AdminMenuType.TEAMS, fixture.lastMenu().holder().getMenuType());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
    }

    @Test
    void closesTheOptionsRegistersOneRequestAndAsksForThePlayerToAdd() {
        fixture.registerTeamOf("Alpha", "Alex");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);

        fixture.click(ADD_PLAYER_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.pendingChatInputCount());
        assertEquals(List.of(
                "Type: <player> in chat",
                "Team Alpha has 1 of 2 players",
                "Example: Steve",
                "Every player has to be online or known to the server",
                "Type cancel to abort, the request expires in 60 seconds"), fixture.messages());
        assertEquals(0, fixture.scheduledTaskCount());
    }

    @Test
    void addsTheTypedPlayerOnTheNextTickSavesAndOpensTheOptionsAgain() {
        askForThePlayerToAddToAlpha();

        assertTrue(fixture.typeInChat("steve"));

        assertNothingDoneBeforeTheNextTick();
        assertEquals("Alex", alphaMembers());

        fixture.runNextTick();

        assertEquals(List.of("Steve joined team Alpha"), fixture.messages());
        assertEquals("Alex, Steve", alphaMembers());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertTeamActionsOpened("Alpha", 0);
        assertEquals(MenuItems.UNAVAILABLE_MATERIAL, fixture.lastMenu().findTile(ADD_PLAYER_SLOT).orElseThrow()
                .material(), "the team reached the team size");
    }

    @Test
    void rejectsAPlayerThatTheLookupDoesNotFindAndOpensTheOptionsAgain() {
        askForThePlayerToAddToAlpha();

        fixture.typeInChat("Herobrine");
        fixture.runNextTick();

        assertEquals(List.of("Unknown player Herobrine\nThe player has to be online or known to the server"),
                fixture.messages());
        assertNoPlayerAdded();
        assertTeamActionsOpened("Alpha", 0);
    }

    @Test
    void rejectsAPlayerOfAnotherTeamAndNamesThatTeam() {
        askForThePlayerToAddToAlpha();
        fixture.teamRegistry().registerTeam("Bravo", List.of(fixture.knownPlayer("Steve")));

        fixture.typeInChat("Steve");
        fixture.runNextTick();

        assertEquals(List.of("Steve could not join team Alpha\nSteve is already registered in team Bravo"),
                fixture.messages());
        assertNoPlayerAdded();
        assertTeamActionsOpened("Alpha", 0);
    }

    @Test
    void rejectsAPlayerThatIsAlreadyInTheTeam() {
        fixture.makePlayersKnown("Steve");
        fixture.teamRegistry().registerTeam("Alpha", List.of(fixture.knownPlayer("Steve")));
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
        fixture.click(ADD_PLAYER_SLOT);
        fixture.messages().clear();

        fixture.typeInChat("Steve");
        fixture.runNextTick();

        assertEquals(List.of("Steve could not join team Alpha\nSteve is already registered in team Alpha"),
                fixture.messages());
        assertEquals("Steve", alphaMembers());
        assertEquals(0, fixture.teamConfigurationSaveCount());
    }

    @Test
    void rejectsThePlayerWhenTheTeamReachedTheTeamSizeBetweenTheClickAndTheAnswer() {
        askForThePlayerToAddToAlpha();
        fixture.teamRegistry().addMember("Alpha", new TeamMember(UUID.randomUUID(), "Jeb_"));

        fixture.typeInChat("Steve");
        fixture.runNextTick();

        assertEquals(List.of("Team Alpha already has 2 players, the team size is 2"), fixture.messages());
        assertEquals("Alex, Jeb_", alphaMembers());
        assertEquals(0, fixture.teamConfigurationSaveCount());
        assertTeamActionsOpened("Alpha", 0);
    }

    @Test
    void rejectsThePlayerWhenTheTeamStartedAMatchBetweenTheClickAndTheAnswer() {
        askForThePlayerToAddToAlpha();
        fixture.registerTeams("Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.typeInChat("Steve");
        fixture.runNextTick();

        assertEquals(List.of("Team Alpha is in match M1, it cannot be changed before the match is over"),
                fixture.messages());
        assertNoPlayerAdded();
        assertTeamActionsOpened("Alpha", 0);
    }

    @Test
    void addsAPlayerToATeamWithoutAMatchDuringATournament() {
        fixture.makePlayersKnown("Steve");
        fixture.configureArena();
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        String teamName = fixture.teamRegistry().getTeamNames().stream()
                .filter(name -> fixture.runningMatch().findTeamByName(name).isEmpty())
                .findFirst()
                .orElseThrow();
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(fixture.teamRegistry().getTeamNames().indexOf(teamName));
        fixture.click(ADD_PLAYER_SLOT);
        fixture.messages().clear();

        fixture.typeInChat("Steve");
        fixture.runNextTick();

        assertEquals(List.of("Steve joined team " + teamName), fixture.messages());
        assertEquals(teamName + "-first, " + teamName + "-second, Steve",
                fixture.teamRegistry().findTeam(teamName).orElseThrow().getMemberNamesJoined());
    }

    @Test
    void cancelsThePlayerToAddOnTheNextTickWithoutReopeningTheMenu() {
        askForThePlayerToAddToAlpha();

        assertTrue(fixture.typeInChat("cancel"));
        assertNothingDoneBeforeTheNextTick();
        fixture.runNextTick();

        assertEquals(List.of("The input for the player to add to team Alpha was cancelled, nothing was changed"),
                fixture.messages());
        assertNoPlayerAdded();
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void refusesThePlayerToAddWhenThePermissionWasRevokedBeforeTheAnswer() {
        askForThePlayerToAddToAlpha();
        fixture.revokeAdminPermission();

        fixture.typeInChat("Steve");
        fixture.runNextTick();

        assertEquals(List.of(NOT_ALLOWED), fixture.messages());
        assertNoPlayerAdded();
        assertTrue(fixture.isMenuClosed());
    }

    private void askForTheNewNameOfBravo() {
        fixture.registerTeams("Alpha", "Bravo", "Charlie");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(1);
        fixture.click(RENAME_SLOT);
        fixture.messages().clear();
    }

    private void askForThePlayerToAddToAlpha() {
        fixture.makePlayersKnown("Steve");
        fixture.registerTeamOf("Alpha", "Alex");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
        fixture.click(ADD_PLAYER_SLOT);
        fixture.messages().clear();
    }

    private String alphaMembers() {
        return fixture.teamRegistry().findTeam("Alpha").orElseThrow().getMemberNamesJoined();
    }

    private void assertNothingDoneBeforeTheNextTick() {
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(0, fixture.pendingChatInputCount());
        assertEquals(1, fixture.scheduledTaskCount());
        assertTrue(fixture.isMenuClosed());
        assertEquals(0, fixture.teamConfigurationSaveCount());
    }

    private void assertNoTeamRenamed() {
        assertEquals(List.of("Alpha", "Bravo", "Charlie"), fixture.teamRegistry().getTeamNames());
        assertEquals(0, fixture.teamConfigurationSaveCount());
    }

    private void assertNoPlayerAdded() {
        assertEquals("Alex", alphaMembers());
        assertEquals(0, fixture.teamConfigurationSaveCount());
    }

    private void assertTeamActionsOpened(String teamName, int pageIndex) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAM_ACTIONS, menu.holder().getMenuType());
        assertEquals(Optional.of(teamName), menu.holder().getSubject());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals("Team " + teamName, menu.title());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.pendingChatInputCount());
    }
}

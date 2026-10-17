package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamActionsAdminMenuClickTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_FIRST_TEAM_SLOT = 0;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;

    private static final int EMPTY_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int RENAME_SLOT = 10;
    private static final int ADD_PLAYER_SLOT = 11;
    private static final int REMOVE_PLAYER_SLOT = 12;
    private static final int REMOVE_SLOT = 13;
    private static final int BACK_SLOT = 22;
    private static final int CONFIRMATION_YES_SLOT = 11;

    private static final String RENAME_IN_TOURNAMENT_REJECTION = "A team cannot be renamed while a tournament is "
            + "in progress, the bracket keeps its name";
    private static final String ALPHA_IN_MATCH_REJECTION = "Team Alpha is in match M1, it cannot be changed before "
            + "the match is over";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        openTheOptionsOfAlpha();

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClicked() {
        openTheOptionsOfAlpha();

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void asksForTheConfirmationOfTheRemovalWithTheTeamNameAndTheFirstPage() {
        openTheOptionsOfAlpha();

        fixture.click(REMOVE_SLOT);

        assertRemovalConfirmationOpened("Alpha", 0);
        assertTrue(fixture.teamRegistry().findTeam("Alpha").isPresent(), "nothing is removed before the confirmation");
    }

    @Test
    void asksForTheConfirmationOfTheRemovalWithTheTeamNameAndTheSecondPage() {
        openTheOptionsOfTheOnlyTeamOfTheSecondPage();

        fixture.click(REMOVE_SLOT);

        assertRemovalConfirmationOpened("Team46", 1);
        assertEquals(46, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void returnsToTheFirstPageOfTheTeamList() {
        openTheOptionsOfAlpha();

        fixture.click(BACK_SLOT);

        assertTeamsOpened(0, "Teams (page 1)");
    }

    @Test
    void returnsToTheSecondPageOfTheTeamList() {
        openTheOptionsOfTheOnlyTeamOfTheSecondPage();

        fixture.click(BACK_SLOT);

        assertTeamsOpened(1, "Teams (page 2)");
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        openTheOptionsOfAlpha();
        AdminMenuHolder teamActionsHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(REMOVE_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(teamActionsHolder, fixture.lastMenu().holder());
    }

    @Test
    void closesTheMenuAndWaitsForTheNewNameOnAClickOnRename() {
        openTheOptionsOfAlpha();

        fixture.click(RENAME_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.pendingChatInputCount());
        assertEquals("Type: <newName> in chat", fixture.messages().getFirst());
    }

    @Test
    void refusesTheRenameDuringATournamentAndKeepsTheMenuOpen() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        openTheOptionsOf(findTeamWithoutTheRunningMatch());

        assertRefusedOnAClickOn(RENAME_SLOT, RENAME_IN_TOURNAMENT_REJECTION);
    }

    @Test
    void refusesTheRenameOfATeamOfTheRunningMatch() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        openTheOptionsOf("Alpha");

        assertRefusedOnAClickOn(RENAME_SLOT, ALPHA_IN_MATCH_REJECTION);
    }

    @Test
    void closesTheMenuAndWaitsForThePlayerOnAClickOnAddAPlayer() {
        fixture.setTeamSize(3);
        openTheOptionsOfAlpha();

        fixture.click(ADD_PLAYER_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.pendingChatInputCount());
        assertEquals("Type: <player> in chat", fixture.messages().getFirst());
    }

    @Test
    void refusesToAddAPlayerToAFullTeam() {
        openTheOptionsOfAlpha();

        assertRefusedOnAClickOn(ADD_PLAYER_SLOT, "Team Alpha already has 2 players, the team size is 2");
    }

    @Test
    void refusesToAddAPlayerToATeamOfTheRunningMatch() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        openTheOptionsOf("Alpha");

        assertRefusedOnAClickOn(ADD_PLAYER_SLOT, ALPHA_IN_MATCH_REJECTION);
    }

    @Test
    void opensThePlayersOfTheTeamWithTheFirstPage() {
        openTheOptionsOfAlpha();

        fixture.click(REMOVE_PLAYER_SLOT);

        assertTeamMembersOpened("Alpha", 0);
    }

    @Test
    void opensThePlayersOfTheTeamWithTheSecondPage() {
        openTheOptionsOfTheOnlyTeamOfTheSecondPage();

        fixture.click(REMOVE_PLAYER_SLOT);

        assertTeamMembersOpened("Team46", 1);
    }

    @Test
    void refusesToOpenThePlayersOfATeamOfOnePlayer() {
        fixture.registerTeamOf("Solo", "Steve");
        openTheOptionsOf("Solo");

        assertRefusedOnAClickOn(REMOVE_PLAYER_SLOT, "A team keeps at least 1 player, remove the team instead");
    }

    @Test
    void refusesToOpenThePlayersOfATeamOfTheRunningMatch() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        openTheOptionsOf("Alpha");

        assertRefusedOnAClickOn(REMOVE_PLAYER_SLOT, ALPHA_IN_MATCH_REJECTION);
    }

    @Test
    void refusesTheRemovalOfATeamOfTheRunningMatchWithoutAskingForAConfirmation() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        openTheOptionsOf("Alpha");

        assertRefusedOnAClickOn(REMOVE_SLOT, ALPHA_IN_MATCH_REJECTION);
        assertEquals(List.of("Alpha", "Bravo"), fixture.teamRegistry().getTeamNames());
    }

    @Test
    void refusesTheRemovalOfATeamWhoseMatchWaitsForTheKitChoice() {
        AdminMenuFixture kitChoiceFixture = AdminMenuFixture.withKitChoice();
        kitChoiceFixture.configureArena();
        kitChoiceFixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        kitChoiceFixture.createKit("Archer");
        kitChoiceFixture.createKit("Knight");
        kitChoiceFixture.startTournament();
        String teamName = kitChoiceFixture.tournamentManager().getBracket().orElseThrow()
                .findMatch("round1-match1").orElseThrow().getSecondTeamName().orElseThrow();
        kitChoiceFixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        kitChoiceFixture.click(kitChoiceFixture.teamRegistry().getTeamNames().indexOf(teamName));
        AdminMenuHolder teamActionsHolder = kitChoiceFixture.lastMenu().holder();

        kitChoiceFixture.click(REMOVE_SLOT);

        assertEquals(List.of("Team " + teamName + " is waiting for the kit choice of match round1-match1, it "
                + "cannot be changed before the match is over"), kitChoiceFixture.messages());
        assertSame(teamActionsHolder, kitChoiceFixture.lastMenu().holder());
        assertEquals(4, kitChoiceFixture.teamRegistry().getTeamCount());
        assertTrue(kitChoiceFixture.kitChoiceService().isPending());
    }

    @Test
    void refusesTheConfirmedRemovalWhenTheTeamStartedAMatchMeanwhile() {
        openTheOptionsOfAlpha();
        fixture.click(REMOVE_SLOT);
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.click(CONFIRMATION_YES_SLOT);

        assertEquals(List.of(ALPHA_IN_MATCH_REJECTION), fixture.messages());
        assertEquals(List.of("Alpha", "Bravo"), fixture.teamRegistry().getTeamNames());
        assertEquals(0, fixture.teamConfigurationSaveCount());
        assertEquals(AdminMenuType.TEAMS, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void removesATeamWithoutAMatchDuringATournamentAfterTheConfirmation() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        String teamName = findTeamWithoutTheRunningMatch();
        openTheOptionsOf(teamName);
        fixture.click(REMOVE_SLOT);

        fixture.click(CONFIRMATION_YES_SLOT);

        assertEquals(List.of("Team " + teamName + " removed"), fixture.messages());
        assertEquals(3, fixture.teamRegistry().getTeamCount());
        assertEquals(1, fixture.teamConfigurationSaveCount());
    }

    private void openTheOptionsOf(String teamName) {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(fixture.teamRegistry().getTeamNames().indexOf(teamName));
    }

    private String findTeamWithoutTheRunningMatch() {
        return fixture.teamRegistry().getTeamNames().stream()
                .filter(teamName -> fixture.runningMatch().findTeamByName(teamName).isEmpty())
                .findFirst()
                .orElseThrow();
    }

    private void assertRefusedOnAClickOn(int slot, String reason) {
        AdminMenuHolder teamActionsHolder = fixture.lastMenu().holder();
        fixture.messages().clear();

        fixture.click(slot);

        assertEquals(List.of(reason), fixture.messages());
        assertSame(teamActionsHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.pendingChatInputCount());
        assertEquals(0, fixture.teamConfigurationSaveCount());
    }

    private void assertTeamMembersOpened(String teamName, int pageIndex) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAM_MEMBERS, menu.holder().getMenuType());
        assertEquals(Optional.of(teamName), menu.holder().getSubject());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals("Players of " + teamName, menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private void openTheOptionsOfAlpha() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
    }

    private void openTheOptionsOfTheOnlyTeamOfTheSecondPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
    }

    private void assertRemovalConfirmationOpened(String teamName, int pageIndex) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of(ConfirmationRequest.REMOVE_TEAM), menu.holder().getConfirmationRequest());
        assertEquals(Optional.of(teamName), menu.holder().getSubject());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals("Remove the team?", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private void assertTeamsOpened(int pageIndex, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAMS, menu.holder().getMenuType());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals(title, menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder teamActionsHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(teamActionsHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }
}

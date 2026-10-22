package de.simplepvparena.gui;

import de.simplepvparena.registration.RegistrationEntry;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationsAdminMenuClickTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_REGISTRATIONS_SLOT = 52;
    private static final int FIRST_PLAYER_SLOT = 0;
    private static final int TOOL_SLOT = 46;
    private static final int INFO_SLOT = 47;
    private static final int FORM_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int UNUSED_SLOT = 50;
    private static final int CLEAR_SLOT = 51;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void opensTheRegistrationsFromTheTeamsMenu() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(TEAMS_REGISTRATIONS_SLOT);

        assertEquals(AdminMenuType.REGISTRATIONS, fixture.lastMenu().holder().getMenuType());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
    }

    @Test
    void returnsToTheTeamsMenu() {
        openTheRegistrations();

        fixture.click(BACK_SLOT);

        assertEquals(AdminMenuType.TEAMS, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileOrAnEmptySlotIsClicked() {
        fixture.registerPlayers("Steve");
        openTheRegistrations();
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();

        for (int slot : List.of(INFO_SLOT, UNUSED_SLOT, 1, 44, NEXT_PAGE_SLOT)) {
            fixture.click(slot);
        }

        assertEquals(menu, fixture.lastMenu());
        assertEquals(1, fixture.registrationList().getCount());
        assertEquals(List.of(), fixture.messages());
    }

    @Test
    void removesARegistrationWithoutAConfirmation() {
        fixture.registerPlayers("Steve", "Alex");
        openTheRegistrations();

        fixture.click(FIRST_PLAYER_SLOT);

        assertEquals(List.of("Alex"), playerNames());
        assertEquals(List.of("The registration of Steve was removed"), fixture.messages());
        assertEquals(AdminMenuType.REGISTRATIONS, fixture.lastMenu().holder().getMenuType());
        assertEquals("0 | PLAYER_HEAD | Alex | Click to remove the registration",
                fixture.describeLastMenu().lines().skip(1).findFirst().orElseThrow());
        assertEquals(1, fixture.registrationSaveCount());
    }

    @Test
    void returnsToTheFirstPageWhenTheLastPlayerOfTheSecondPageIsRemoved() {
        fixture.registerPlayers(numberedPlayerNames(46));
        openTheRegistrations();
        fixture.click(NEXT_PAGE_SLOT);

        fixture.click(FIRST_PLAYER_SLOT);

        assertEquals(45, fixture.registrationList().getCount());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
        assertEquals("Registrations (page 1)", fixture.lastMenu().title());
    }

    @Test
    void handsOutTheStationTool() {
        openTheRegistrations();

        fixture.click(TOOL_SLOT);

        assertEquals(1, fixture.handedOutItems().size());
        assertTrue(fixture.registrationStationService().isTool(fixture.handedOutItems().get(0)));
        assertEquals(List.of("The registration station tool is in your inventory"), fixture.messages());
    }

    @Test
    void reportsAFullInventoryWhenTheToolDoesNotFit() {
        openTheRegistrations();
        fixture.fillAdminInventory();

        fixture.click(TOOL_SLOT);

        assertEquals(List.of(), fixture.handedOutItems());
        assertEquals(List.of("Your inventory is full, the registration station tool could not be handed out"),
                fixture.messages());
    }

    @Test
    void formsRandomTeamsAfterTheConfirmation() {
        fixture.registerTeams("Old");
        fixture.registerPlayers("P1", "P2", "P3", "P4", "P5");
        AdminMenuFixture.OnlinePlayer steve = fixture.letAnotherPlayerJoin("Steve", false, true);
        fixture.registrationList().register(steve.player().getUniqueId(), "Steve");
        openTheRegistrations();
        fixture.click(FORM_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Red", "Blue", "Green"), fixture.teamRegistry().getTeamNames());
        Team green = fixture.teamRegistry().findTeam("Green").orElseThrow();
        assertEquals(List.of("P5", "Steve"), green.getMembers().stream().map(TeamMember::playerName).toList());
        assertEquals(Optional.of(TeamColour.GREEN), green.getColour());
        assertEquals(List.of("3 teams were formed from 6 registered players"), fixture.messages());
        assertEquals(List.of("You play in team Green with P5"), steve.messages());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertEquals(6, fixture.registrationList().getCount());
        assertEquals(AdminMenuType.REGISTRATIONS, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void keepsTheTeamsWhenTheFormingIsRefused() {
        fixture.registerTeams("Old");
        fixture.registerPlayers("P1", "P2", "P3", "P4");
        openTheRegistrations();
        fixture.click(FORM_SLOT);

        fixture.click(CONFIRM_NO_SLOT);

        assertEquals(List.of("Old"), fixture.teamRegistry().getTeamNames());
        assertEquals(AdminMenuType.REGISTRATIONS, fixture.lastMenu().holder().getMenuType());
        assertEquals(0, fixture.teamConfigurationSaveCount());
    }

    @Test
    void refusesToFormTeamsFromTooFewPlayers() {
        fixture.registerPlayers("P1", "P2");
        openTheRegistrations();

        fixture.click(FORM_SLOT);

        assertEquals(AdminMenuType.REGISTRATIONS, fixture.lastMenu().holder().getMenuType());
        assertEquals(List.of("At least 2 teams are needed, 2 players are registered"), fixture.messages());
    }

    @Test
    void refusesToFormTeamsWhileATournamentIsInProgress() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.registerPlayers("P1", "P2", "P3", "P4");
        openTheRegistrations();
        fixture.click(FORM_SLOT);
        fixture.startTournament();
        fixture.messages().clear();

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Alpha", "Bravo"), fixture.teamRegistry().getTeamNames());
        assertEquals(List.of("Teams cannot be formed while a tournament is in progress"), fixture.messages());
    }

    @Test
    void clearsTheRegistrationsAfterTheConfirmation() {
        fixture.registerTeams("Alpha");
        fixture.registerPlayers("P1", "P2", "P3");
        openTheRegistrations();
        fixture.click(CLEAR_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(0, fixture.registrationList().getCount());
        assertEquals(List.of("Alpha"), fixture.teamRegistry().getTeamNames());
        assertEquals(List.of("The registrations were cleared"), fixture.messages());
        assertEquals(AdminMenuType.REGISTRATIONS, fixture.lastMenu().holder().getMenuType());
        assertEquals(1, fixture.registrationSaveCount());
    }

    @Test
    void keepsTheRegistrationsWhenTheClearingIsRefused() {
        fixture.registerPlayers("P1", "P2", "P3");
        openTheRegistrations();
        fixture.click(CLEAR_SLOT);

        fixture.click(CONFIRM_NO_SLOT);

        assertEquals(3, fixture.registrationList().getCount());
        assertEquals(0, fixture.registrationSaveCount());
    }

    @Test
    void refusesToClearAnEmptyList() {
        openTheRegistrations();

        fixture.click(CLEAR_SLOT);

        assertEquals(AdminMenuType.REGISTRATIONS, fixture.lastMenu().holder().getMenuType());
        assertEquals(List.of("No player is registered"), fixture.messages());
    }

    @Test
    void closesTheMenuForAPlayerWhoIsNoAdminAnyMore() {
        fixture.registerPlayers("Steve");
        openTheRegistrations();
        fixture.revokeAdminPermission();

        fixture.click(FIRST_PLAYER_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.registrationList().getCount());
        assertFalse(fixture.messages().isEmpty());
    }

    private void openTheRegistrations() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_REGISTRATIONS_SLOT);
        fixture.messages().clear();
    }

    private List<String> playerNames() {
        return fixture.registrationList().getEntries().stream().map(RegistrationEntry::playerName).toList();
    }

    private static String[] numberedPlayerNames(int playerCount) {
        String[] playerNames = new String[playerCount];
        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            playerNames[playerNumber - 1] = "Player" + playerNumber;
        }
        return playerNames;
    }
}

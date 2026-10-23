package de.simplepvparena.gui;

import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.registration.TeamMode;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamChoiceUnregistrationTest {

    private static final String[] LEAVE = {"leave"};
    private static final int RED_SLOT = 0;
    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_REGISTRATIONS_SLOT = 52;
    private static final int CLEAR_SLOT = 51;
    private static final int CONFIRM_YES_SLOT = 11;

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final TournamentCommand command = fixture.createTournamentCommand();
    private final AdminMenuFixture.OnlinePlayer steve = fixture.letAnotherPlayerJoin("Steve", false, true);
    private final AdminMenuFixture.OnlinePlayer alex = fixture.letAnotherPlayerJoin("Alex", false, true);

    @Test
    void leavesTheColouredTeamWhenThePlayerUnregisters() {
        chooseRed(steve);
        chooseRed(alex);

        command.onCommand(steve.player(), null, "tournament", LEAVE);

        assertEquals("Alex", fixture.teamRegistry().findTeam("Red").orElseThrow().getMemberNamesJoined());
        assertEquals("You are no longer registered", steve.messages().getLast());
    }

    @Test
    void removesTheColouredTeamThatTheUnregisteringPlayerLeavesEmpty() {
        chooseRed(steve);
        int saveCount = fixture.teamConfigurationSaveCount();

        command.onCommand(steve.player(), null, "tournament", LEAVE);

        assertEquals(0, fixture.teamRegistry().getTeamCount());
        assertEquals(saveCount + 1, fixture.teamConfigurationSaveCount());
    }

    @Test
    void leavesTheColouredTeamWhenAnAdminRemovesTheRegistration() {
        chooseRed(steve);
        openTheRegistrations();

        fixture.click(0);

        assertEquals(List.of("The registration of Steve was removed"), fixture.messages());
        assertEquals(0, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void keepsATeamWithoutAColourWhenThePlayerUnregisters() {
        fixture.registerTeamOfOnlinePlayers("Admins", steve, alex);
        register(steve);

        command.onCommand(steve.player(), null, "tournament", LEAVE);

        assertEquals("Steve, Alex", fixture.teamRegistry().findTeam("Admins").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void keepsTheTeamsUnchangedWhenTheTeamsAreFormedAtRandom() {
        chooseRed(steve);
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);

        command.onCommand(steve.player(), null, "tournament", LEAVE);

        assertEquals("Steve", fixture.teamRegistry().findTeam("Red").orElseThrow().getMemberNamesJoined());
        assertTrue(fixture.registrationList().getEntries().isEmpty());
    }

    @Test
    void refusesTheUnregistrationOfAPlayerWhoseColouredTeamIsInAMatch() {
        fixture.registerTeams("Alpha");
        fixture.teamRegistry().registerTeam("Red", List.of(new TeamMember(UUID.randomUUID(), "Herobrine")),
                TeamColour.RED);
        fixture.startMatchWithoutATournament("Red", "Alpha");
        fixture.teamRegistry().addMember("Red", new TeamMember(steve.player().getUniqueId(), "Steve"));
        register(steve);

        command.onCommand(steve.player(), null, "tournament", LEAVE);

        assertEquals(List.of("Team Red is in match M1, it cannot be changed before the match is over"),
                steve.messages());
        assertEquals("Herobrine, Steve", fixture.teamRegistry().findTeam("Red").orElseThrow()
                .getMemberNamesJoined());
        assertTrue(fixture.registrationList().isRegistered(steve.player().getUniqueId()));
    }

    @Test
    void namesTheRemovedColouredTeamsWhenTheRegistrationsAreCleared() {
        chooseRed(steve);
        chooseBlue(alex);
        fixture.registerTeamOf("Admins", "Herobrine");
        openTheRegistrations();

        fixture.click(CLEAR_SLOT);
        assertEquals(List.of("2 registrations are removed",
                "Every team with a colour is removed, the teams without a colour stay"),
                fixture.lastMenu().findTile(AdminMenuLayout.INFO_SLOT).orElseThrow().loreLines());
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Admins"), fixture.teamRegistry().getTeamNames());
        assertEquals(List.of("The registrations were cleared", "2 teams with a colour were removed"),
                fixture.messages());
        assertEquals(0, fixture.registrationList().getCount());
    }

    @Test
    void removesTheTeamsFormedAtRandomAsWellBecauseTheyCarryAColour() {
        fixture.registerPlayers("P1", "P2", "P3", "P4");
        openTheRegistrations();
        fixture.click(48);
        fixture.click(CONFIRM_YES_SLOT);
        assertEquals(List.of("Red", "Blue"), fixture.teamRegistry().getTeamNames());
        openTheRegistrations();

        fixture.click(CLEAR_SLOT);
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(0, fixture.teamRegistry().getTeamCount());
        assertEquals(List.of("The registrations were cleared", "2 teams with a colour were removed"),
                fixture.messages());
    }

    @Test
    void keepsTheColouredTeamOfARunningMatchWhenTheRegistrationsAreCleared() {
        fixture.registerTeams("Alpha");
        fixture.teamRegistry().registerTeam("Red", List.of(new TeamMember(UUID.randomUUID(), "Herobrine")),
                TeamColour.RED);
        fixture.startMatchWithoutATournament("Red", "Alpha");
        chooseBlue(steve);
        openTheRegistrations();

        fixture.click(CLEAR_SLOT);
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Alpha", "Red"), fixture.teamRegistry().getTeamNames());
        assertEquals(List.of("The registrations were cleared", "1 team with a colour was removed"),
                fixture.messages());
    }

    @Test
    void keepsEveryTeamWhenTheRegistrationsAreClearedAndTheTeamsAreFormedAtRandom() {
        chooseRed(steve);
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);
        openTheRegistrations();

        assertEquals(List.of("Removes every registration, the teams stay as they are",
                "Asks for a confirmation first"), fixture.lastMenu().findTile(CLEAR_SLOT).orElseThrow().loreLines());
        fixture.click(CLEAR_SLOT);
        assertEquals("The teams stay as they are",
                fixture.lastMenu().findTile(AdminMenuLayout.INFO_SLOT).orElseThrow().loreLines().get(1));
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Red"), fixture.teamRegistry().getTeamNames());
        assertEquals(List.of("The registrations were cleared"), fixture.messages());
    }

    @Test
    void keepsEveryTeamWhenTheRegistrationsAreClearedDuringATournament() {
        fixture.teamRegistry().registerTeam("Red", List.of(new TeamMember(UUID.randomUUID(), "Herobrine")),
                TeamColour.RED);
        fixture.teamRegistry().registerTeam("Blue", List.of(new TeamMember(UUID.randomUUID(), "Jeb_")),
                TeamColour.BLUE);
        register(steve);
        fixture.configureArena();
        fixture.startTournament();
        openTheRegistrations();

        fixture.click(CLEAR_SLOT);
        assertEquals("The teams stay as they are",
                fixture.lastMenu().findTile(AdminMenuLayout.INFO_SLOT).orElseThrow().loreLines().get(1));
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Red", "Blue"), fixture.teamRegistry().getTeamNames());
        assertEquals(List.of("The registrations were cleared"), fixture.messages());
    }

    @Test
    @UseLanguage("de")
    void namesTheRemovedColouredTeamsInGerman() {
        chooseRed(steve);
        openTheRegistrations();

        fixture.click(CLEAR_SLOT);
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Die Anmeldungen wurden geleert", "1 Team mit einer Farbe wurde entfernt"),
                fixture.messages());
    }

    private void register(AdminMenuFixture.OnlinePlayer player) {
        fixture.registrationList().register(player.player().getUniqueId(), player.player().getName());
    }

    private void chooseRed(AdminMenuFixture.OnlinePlayer player) {
        choose(player, RED_SLOT);
    }

    private void chooseBlue(AdminMenuFixture.OnlinePlayer player) {
        choose(player, 1);
    }

    private void choose(AdminMenuFixture.OnlinePlayer player, int slot) {
        register(player);
        fixture.openTeamChoiceFor(player.player());
        fixture.clickAs(player.player(), slot);
        player.messages().clear();
    }

    private void openTheRegistrations() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_REGISTRATIONS_SLOT);
        fixture.messages().clear();
    }
}

package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamChoiceJoinTest {

    private static final int RED_SLOT = 0;
    private static final int BLUE_SLOT = 1;
    private static final int MAIN_TEAMS_SLOT = 11;

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final AdminMenuFixture.OnlinePlayer steve = fixture.letAnotherPlayerJoin("Steve", false, true);
    private final AdminMenuFixture.OnlinePlayer alex = fixture.letAnotherPlayerJoin("Alex", false, true);
    private final AdminMenuFixture.OnlinePlayer notch = fixture.letAnotherPlayerJoin("Notch", false, true);

    @Test
    void createsTheTeamOfTheColourWithTheFirstPlayerWhoChoosesIt() {
        registerAndOpen(steve);

        fixture.clickAs(steve.player(), RED_SLOT);

        Team red = fixture.teamRegistry().findTeam("Red").orElseThrow();
        assertEquals(Optional.of(TeamColour.RED), red.getColour());
        assertEquals(List.of(new TeamMember(steve.player().getUniqueId(), "Steve")), red.getMembers());
        assertEquals(List.of("You joined team Red"), steve.messages());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertEquals(List.of("Steve", "1 of 2 places", "Your team"), tileOf(steve, RED_SLOT).loreLines());
    }

    @Test
    void addsTheNextPlayerToTheTeamOfTheColourAndTellsTheOtherMembers() {
        registerAndOpen(steve);
        registerAndOpen(alex);
        fixture.clickAs(steve.player(), RED_SLOT);

        fixture.clickAs(alex.player(), RED_SLOT);

        assertEquals("Steve, Alex", fixture.teamRegistry().findTeam("Red").orElseThrow().getMemberNamesJoined());
        assertEquals(1, fixture.teamRegistry().getTeamCount());
        assertEquals(List.of("You joined team Red"), alex.messages());
        assertEquals(List.of("You joined team Red", "Alex joined your team"), steve.messages());
    }

    @Test
    void switchesToAnotherColourAndRemovesTheTeamLeftEmpty() {
        registerAndOpen(steve);
        fixture.clickAs(steve.player(), RED_SLOT);

        fixture.clickAs(steve.player(), BLUE_SLOT);

        assertTrue(fixture.teamRegistry().findTeam("Red").isEmpty());
        assertEquals(List.of("Blue"), fixture.teamRegistry().getTeamNames());
        assertEquals(List.of("You joined team Red", "You joined team Blue"), steve.messages());
    }

    @Test
    void switchesToAnotherColourAndKeepsTheOldTeamForTheOthers() {
        registerAndOpen(steve);
        registerAndOpen(alex);
        fixture.clickAs(steve.player(), RED_SLOT);
        fixture.clickAs(alex.player(), RED_SLOT);

        fixture.clickAs(steve.player(), BLUE_SLOT);

        assertEquals("Alex", fixture.teamRegistry().findTeam("Red").orElseThrow().getMemberNamesJoined());
        assertEquals("Steve", fixture.teamRegistry().findTeam("Blue").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void leavesAManuallyCreatedTeamWithoutAColourToJoinAColour() {
        fixture.registerTeamOfOnlinePlayers("Admins", steve, alex);
        registerAndOpen(steve);

        fixture.clickAs(steve.player(), BLUE_SLOT);

        assertEquals("Alex", fixture.teamRegistry().findTeam("Admins").orElseThrow().getMemberNamesJoined());
        assertEquals("Steve", fixture.teamRegistry().findTeam("Blue").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void refusesAFullTeamAndChangesNothing() {
        registerAndOpen(steve);
        registerAndOpen(alex);
        registerAndOpen(notch);
        fixture.clickAs(steve.player(), RED_SLOT);
        fixture.clickAs(alex.player(), RED_SLOT);
        fixture.clickAs(notch.player(), BLUE_SLOT);
        notch.messages().clear();

        fixture.clickAs(notch.player(), RED_SLOT);

        assertEquals(List.of("This team is full"), notch.messages());
        assertEquals("Steve, Alex", fixture.teamRegistry().findTeam("Red").orElseThrow().getMemberNamesJoined());
        assertEquals("Notch", fixture.teamRegistry().findTeam("Blue").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void doesNothingWhenThePlayerClicksTheOwnTeam() {
        registerAndOpen(steve);
        fixture.clickAs(steve.player(), RED_SLOT);

        fixture.clickAs(steve.player(), RED_SLOT);

        assertEquals(List.of("You joined team Red"), steve.messages());
        assertEquals(1, fixture.teamConfigurationSaveCount());
    }

    @Test
    void findsTheTeamOfABedByItsColourNotByItsName() {
        fixture.teamRegistry().registerTeam("Crimson", List.of(new TeamMember(UUID.randomUUID(), "Herobrine")),
                TeamColour.RED);
        registerAndOpen(steve);

        fixture.clickAs(steve.player(), RED_SLOT);

        assertEquals("Herobrine, Steve", fixture.teamRegistry().findTeam("Crimson").orElseThrow()
                .getMemberNamesJoined());
        assertEquals(List.of("You joined team Crimson"), steve.messages());
    }

    @Test
    void appendsAFreeNumberWhenTheColourNameBelongsToATeamWithoutThatColour() {
        fixture.registerTeamOf("Red", "Herobrine");
        fixture.registerTeamOf("Red-2", "Jeb_");
        registerAndOpen(steve);

        fixture.clickAs(steve.player(), RED_SLOT);

        Team team = fixture.teamRegistry().findTeamByPlayer(steve.player().getUniqueId()).orElseThrow();
        assertEquals("Red-3", team.getTeamName());
        assertEquals(Optional.of(TeamColour.RED), team.getColour());
        assertEquals(List.of("You joined team Red-3"), steve.messages());
    }

    @Test
    @UseLanguage("de")
    void namesTheNewTeamInTheLanguageOfTheServer() {
        fixture.registerTeamOf("Rot", "Herobrine");
        registerAndOpen(steve);
        registerAndOpen(alex);

        fixture.clickAs(steve.player(), RED_SLOT);
        fixture.clickAs(alex.player(), BLUE_SLOT);
        fixture.clickAs(alex.player(), RED_SLOT);
        fixture.clickAs(steve.player(), 26);

        assertEquals(List.of("Du bist Team Rot-2 beigetreten", "Alex ist deinem Team beigetreten",
                "Du hast Team Rot-2 verlassen"), steve.messages());
        assertEquals(List.of("Du bist Team Blau beigetreten", "Du bist Team Rot-2 beigetreten"), alex.messages());
    }

    @Test
    void ignoresTheSlotOfAColourThatIsNotOffered() {
        registerAndOpen(steve);

        fixture.clickAs(steve.player(), 10);
        fixture.clickAs(steve.player(), 20);

        assertEquals(0, fixture.teamRegistry().getTeamCount());
        assertEquals(List.of(), steve.messages());
    }

    @Test
    void leavesTheTeamAndRemovesItOnceEmpty() {
        registerAndOpen(steve);
        fixture.clickAs(steve.player(), RED_SLOT);

        fixture.clickAs(steve.player(), TeamChoiceMenu.LEAVE_SLOT);

        assertEquals(0, fixture.teamRegistry().getTeamCount());
        assertEquals(List.of("You joined team Red", "You left team Red"), steve.messages());
        assertFalse(fixture.findLastMenuOf(steve.player()).orElseThrow().findTile(TeamChoiceMenu.LEAVE_SLOT)
                .isPresent());
        assertEquals(2, fixture.teamConfigurationSaveCount());
    }

    @Test
    void leavesTheTeamAndKeepsItForTheOthers() {
        registerAndOpen(steve);
        registerAndOpen(alex);
        fixture.clickAs(steve.player(), RED_SLOT);
        fixture.clickAs(alex.player(), RED_SLOT);

        fixture.clickAs(steve.player(), TeamChoiceMenu.LEAVE_SLOT);

        assertEquals("Alex", fixture.teamRegistry().findTeam("Red").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void ignoresTheLeaveSlotForAPlayerWithoutATeam() {
        registerAndOpen(steve);

        fixture.clickAs(steve.player(), TeamChoiceMenu.LEAVE_SLOT);

        assertEquals(List.of(), steve.messages());
    }

    @Test
    void refusesToJoinATeamThatIsInARunningMatch() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha", "Bravo");
        fixture.teamRegistry().registerTeam("Red", members("Red-first", "Red-second"), TeamColour.RED);
        fixture.startMatchWithoutATournament("Red", "Alpha");
        registerAndOpen(steve);

        fixture.clickAs(steve.player(), RED_SLOT);

        assertEquals(List.of("Team Red is in match M1, it cannot be changed before the match is over"),
                steve.messages());
        assertEquals("Red-first, Red-second", fixture.teamRegistry().findTeam("Red").orElseThrow()
                .getMemberNamesJoined());
    }

    @Test
    void refusesToSwitchAwayFromATeamThatIsInARunningMatch() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha");
        fixture.teamRegistry().registerTeam("Red", members("Red-first"), TeamColour.RED);
        fixture.startMatchWithoutATournament("Red", "Alpha");
        fixture.teamRegistry().addMember("Red", new TeamMember(steve.player().getUniqueId(), "Steve"));
        registerAndOpen(steve);

        fixture.clickAs(steve.player(), BLUE_SLOT);
        fixture.clickAs(steve.player(), TeamChoiceMenu.LEAVE_SLOT);

        assertEquals(List.of("Team Red is in match M1, it cannot be changed before the match is over",
                "Team Red is in match M1, it cannot be changed before the match is over"), steve.messages());
        assertEquals("Red-first, Steve", fixture.teamRegistry().findTeam("Red").orElseThrow()
                .getMemberNamesJoined());
        assertTrue(fixture.teamRegistry().findTeam("Blue").isEmpty());
    }

    @Test
    void refreshesTheTeamChoiceOfEveryoneWhoHasItOpen() {
        registerAndOpen(steve);
        registerAndOpen(alex);
        RecordingAdminMenuWindows.RecordedMenu menuOfAlex = fixture.findLastMenuOf(alex.player()).orElseThrow();

        fixture.clickAs(steve.player(), RED_SLOT);

        assertEquals(List.of("Steve", "1 of 2 places", "Click to join"),
                menuOfAlex.findTile(RED_SLOT).orElseThrow().loreLines());
        assertTrue(menuOfAlex.refreshCount() > 0);
    }

    @Test
    void refreshesNoTeamChoiceThatWasClosedMeanwhile() {
        registerAndOpen(steve);
        registerAndOpen(alex);
        RecordingAdminMenuWindows.RecordedMenu menuOfAlex = fixture.findLastMenuOf(alex.player()).orElseThrow();
        fixture.openBracketViewFor(alex.player());

        fixture.clickAs(steve.player(), RED_SLOT);

        assertEquals(0, menuOfAlex.refreshCount());
    }

    @Test
    void showsTheChosenTeamsInTheTeamsMenuOfTheAdmin() {
        registerAndOpen(steve);
        registerAndOpen(alex);
        fixture.clickAs(steve.player(), RED_SLOT);
        fixture.clickAs(alex.player(), BLUE_SLOT);

        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        String teamsMenu = fixture.describeLastMenu();
        assertTrue(teamsMenu.contains("| Red |"), teamsMenu);
        assertTrue(teamsMenu.contains("| Blue |"), teamsMenu);
    }

    private void registerAndOpen(AdminMenuFixture.OnlinePlayer player) {
        fixture.registrationList().register(player.player().getUniqueId(), player.player().getName());
        fixture.openTeamChoiceFor(player.player());
    }

    private MenuTile tileOf(AdminMenuFixture.OnlinePlayer player, int slot) {
        return fixture.findLastMenuOf(player.player()).orElseThrow().findTile(slot).orElseThrow();
    }

    private static List<TeamMember> members(String... playerNames) {
        List<TeamMember> members = new ArrayList<>();
        for (String playerName : playerNames) {
            members.add(new TeamMember(UUID.randomUUID(), playerName));
        }
        return members;
    }
}

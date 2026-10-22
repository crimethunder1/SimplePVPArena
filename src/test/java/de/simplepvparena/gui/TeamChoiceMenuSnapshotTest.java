package de.simplepvparena.gui;

import de.simplepvparena.registration.TeamChoiceService;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamChoiceMenuSnapshotTest {

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final AdminMenuFixture.OnlinePlayer steve = fixture.letAnotherPlayerJoin("Steve", false, true);
    private final AdminMenuFixture.OnlinePlayer alex = fixture.letAnotherPlayerJoin("Alex", false, true);
    private final AdminMenuFixture.OnlinePlayer notch = fixture.letAnotherPlayerJoin("Notch", false, true);

    @Test
    void offersTwoBedsWhileNobodyIsRegistered() {
        RecordingAdminMenuWindows windows = new RecordingAdminMenuWindows();
        AdminMenuNavigator navigator = new AdminMenuNavigator(windows);
        navigator.register(AdminMenuType.TEAM_CHOICE, new TeamChoiceMenu(navigator, fixture.teamChoiceService()));

        navigator.open(steve.player(), AdminMenuHolder.teamChoice());

        assertEquals("""
                Choose your team | 27
                0 | RED_BED | Team Red | No one yet / 0 of 2 places / Click to join
                1 | BLUE_BED | Team Blue | No one yet / 0 of 2 places / Click to join
                22 | BOOK | Team choice | 0 players are registered / A team has room for 2 players / \
                Choose a bed to join its team
                """, describe(windows.lastOpenedMenu(steve.player())));
    }

    @Test
    void offersTwoBedsForThreeRegisteredPlayersAtTheTeamSizeTwo() {
        register(steve, alex, notch);

        fixture.openTeamChoiceFor(steve.player());

        assertEquals("""
                Choose your team | 27
                0 | RED_BED | Team Red | No one yet / 0 of 2 places / Click to join
                1 | BLUE_BED | Team Blue | No one yet / 0 of 2 places / Click to join
                22 | BOOK | Team choice | 3 players are registered / A team has room for 2 players / \
                Choose a bed to join its team
                """, fixture.describeLastMenuOf(steve.player()));
    }

    @Test
    void offersFiveBedsForNineRegisteredPlayersAtTheTeamSizeTwo() {
        register(steve);
        fixture.registerPlayers("P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9");

        fixture.openTeamChoiceFor(steve.player());

        assertEquals("""
                Choose your team | 27
                0 | RED_BED | Team Red | No one yet / 0 of 2 places / Click to join
                1 | BLUE_BED | Team Blue | No one yet / 0 of 2 places / Click to join
                2 | GREEN_BED | Team Green | No one yet / 0 of 2 places / Click to join
                3 | YELLOW_BED | Team Yellow | No one yet / 0 of 2 places / Click to join
                4 | ORANGE_BED | Team Orange | No one yet / 0 of 2 places / Click to join
                22 | BOOK | Team choice | 9 players are registered / A team has room for 2 players / \
                Choose a bed to join its team
                """, fixture.describeLastMenuOf(steve.player()));
    }

    @Test
    void greysAFullTeamAndShowsItsMembers() {
        register(steve, alex, notch);
        registerColouredTeam("Red", TeamColour.RED, alex, notch);

        fixture.openTeamChoiceFor(steve.player());

        assertEquals("""
                Choose your team | 27
                0 | GRAY_DYE | Team Red | Alex / Notch / 2 of 2 places / This team is full
                1 | BLUE_BED | Team Blue | No one yet / 0 of 2 places / Click to join
                22 | BOOK | Team choice | 3 players are registered / A team has room for 2 players / \
                Choose a bed to join its team
                """, fixture.describeLastMenuOf(steve.player()));
    }

    @Test
    void marksTheOwnTeamAndOffersToLeaveIt() {
        register(steve, alex, notch);
        registerColouredTeam("Blue", TeamColour.BLUE, steve);
        registerColouredTeam("Red", TeamColour.RED, alex);

        fixture.openTeamChoiceFor(steve.player());

        assertEquals("""
                Choose your team | 27
                0 | RED_BED | Team Red | Alex / 1 of 2 places / Click to join
                1 | BLUE_BED | Team Blue | Steve / 1 of 2 places / Your team
                22 | BOOK | Team choice | 3 players are registered / A team has room for 2 players / \
                Choose a bed to join its team
                26 | BARRIER | Leave your team | Click to leave team Blue
                """, fixture.describeLastMenuOf(steve.player()));
    }

    @Test
    void marksAFullOwnTeamAsTheOwnTeam() {
        register(steve, alex);
        registerColouredTeam("Red", TeamColour.RED, steve, alex);

        fixture.openTeamChoiceFor(steve.player());

        MenuTile ownTeam = fixture.findLastMenuOf(steve.player()).orElseThrow().findTile(0).orElseThrow();
        assertEquals(Material.RED_BED, ownTeam.material());
        assertEquals(List.of("Steve", "Alex", "2 of 2 places", "Your team"), ownTeam.loreLines());
    }

    @Test
    void addsTheBedOfEveryColourThatAlreadyHasATeamAtItsFixedPlace() {
        register(steve, alex);
        registerColouredTeam("Cyan", TeamColour.CYAN, alex);

        fixture.openTeamChoiceFor(steve.player());

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.findLastMenuOf(steve.player()).orElseThrow();
        assertEquals("CYAN_BED Team Cyan [Alex, 1 of 2 places, Click to join]", menu.describeTiles().get(12));
        assertEquals(List.of(0, 1, 12, 22), List.copyOf(menu.describeTiles().keySet()));
        assertEquals(List.of(TeamColour.RED, TeamColour.BLUE, TeamColour.CYAN),
                fixture.teamChoiceService().findOfferedColours());
    }

    @Test
    void namesTheSingularOfOnePlaceAndOnePlayer() {
        fixture.setTeamSize(1);
        register(steve);

        fixture.openTeamChoiceFor(steve.player());

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.findLastMenuOf(steve.player()).orElseThrow();
        assertEquals(List.of("No one yet", "0 of 1 place", "Click to join"), menu.findTile(0).orElseThrow()
                .loreLines());
        assertEquals(List.of("1 player is registered", "A team has room for 1 player",
                "Choose a bed to join its team"), menu.findTile(TeamChoiceMenu.INFO_SLOT).orElseThrow().loreLines());
    }

    @Test
    void countsTheNeededBedsRoundedUpBetweenTwoAndSixteen() {
        assertEquals(2, TeamChoiceService.countNeededColours(0, 2));
        assertEquals(2, TeamChoiceService.countNeededColours(3, 2));
        assertEquals(3, TeamChoiceService.countNeededColours(5, 2));
        assertEquals(5, TeamChoiceService.countNeededColours(9, 2));
        assertEquals(4, TeamChoiceService.countNeededColours(10, 3));
        assertEquals(16, TeamChoiceService.countNeededColours(40, 1));
    }

    @Test
    void usesTheBedOfTheColourOfEveryTeamColour() {
        for (TeamColour colour : TeamColour.values()) {
            assertEquals(colour.getDyeColour().name() + "_BED", TeamChoiceMenu.bedOf(colour).name());
            assertEquals(colour, TeamChoiceMenu.colourOf(TeamChoiceMenu.slotOf(colour)).orElseThrow());
        }
    }

    private void register(AdminMenuFixture.OnlinePlayer... players) {
        for (AdminMenuFixture.OnlinePlayer player : players) {
            fixture.registrationList().register(player.player().getUniqueId(), player.player().getName());
        }
    }

    private void registerColouredTeam(String teamName, TeamColour colour, AdminMenuFixture.OnlinePlayer... players) {
        List<TeamMember> members = new ArrayList<>();
        for (AdminMenuFixture.OnlinePlayer player : players) {
            members.add(new TeamMember(player.player().getUniqueId(), player.player().getName()));
        }
        fixture.teamRegistry().registerTeam(teamName, members, colour);
    }

    private static String describe(RecordingAdminMenuWindows.RecordedMenu menu) {
        StringBuilder description = new StringBuilder(menu.title()).append(" | ").append(menu.size()).append('\n');
        for (int slot = 0; slot < menu.size(); slot++) {
            int describedSlot = slot;
            menu.findTile(slot).ifPresent(tile -> description.append(describedSlot).append(" | ")
                    .append(tile.material()).append(" | ").append(tile.displayName()).append(" | ")
                    .append(String.join(" / ", tile.loreLines())).append('\n'));
        }
        return description.toString();
    }
}

package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessage;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamChoiceServiceTest {

    private final StationTestServer server = new StationTestServer();
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final TeamModeSetting teamModeSetting = new TeamModeSetting();
    private final TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
    private final Map<String, String> rejectionsByTeamName = new HashMap<>();
    private final Map<UUID, Player> onlinePlayers = new HashMap<>();
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final StationTestServer.TestPlayer admin = join("Admin", true);
    private final StationTestServer.TestPlayer steve = join("Steve", false);
    private final StationTestServer.TestPlayer alex = join("Alex", false);
    private final Block button = server.placeBlock(10, 64, 20, Material.STONE_BUTTON);

    private boolean tournamentInProgress;
    private int saveCount;

    private final TeamChoiceService teamChoiceService = new TeamChoiceService(server.registrationService(),
            teamRegistry, teamSizeSetting, teamModeSetting, () -> tournamentInProgress,
            teamName -> Optional.ofNullable(rejectionsByTeamName.get(teamName)), () -> saveCount++,
            onlinePlayers::get);

    TeamChoiceServiceTest() {
        teamChoiceService.setPresenter(presenter);
        server.registrationService().addObserver(teamChoiceService);
        server.stationService().useTool(admin.player(), button);
    }

    @Test
    void showsTheTeamChoiceAfterARegistrationAtTheStation() {
        server.stationService().useStation(steve.player(), button);

        assertEquals(List.of(steve.player()), presenter.shownPlayers);
        assertEquals(1, presenter.refreshCount);
        assertEquals(List.of("You are registered for the next event"), steve.messages());
    }

    @Test
    void showsTheTeamChoiceOnlyAfterTheFirstRegistration() {
        server.stationService().useStation(steve.player(), button);

        server.stationService().useStation(steve.player(), button);

        assertEquals(List.of(steve.player()), presenter.shownPlayers);
    }

    @Test
    void showsNoTeamChoiceWhenTheTeamsAreFormedAtRandom() {
        teamModeSetting.setTeamMode(TeamMode.RANDOM);

        server.stationService().useStation(steve.player(), button);

        assertEquals(List.of(), presenter.shownPlayers);
        assertTrue(server.registrationList().isRegistered(steve.uniqueId()));
    }

    @Test
    void leavesTheColouredTeamWhenThePlayerSneaksAndClicksTheStation() {
        server.stationService().useStation(steve.player(), button);
        teamChoiceService.join(steve.player(), TeamColour.RED);
        steve.setSneaking(true);

        server.stationService().useStation(steve.player(), button);

        assertEquals(0, teamRegistry.getTeamCount());
        assertEquals(List.of("You are registered for the next event", "You joined team Red",
                "You are no longer registered"), steve.messages());
        assertEquals(2, saveCount);
    }

    @Test
    void refusesToJoinATeamThatIsAboutToPlayAnAutoMatch() {
        register(steve);
        teamRegistry.registerTeam("Red", List.of(new TeamMember(alex.uniqueId(), "Alex")), TeamColour.RED);
        rejectionsByTeamName.put("Red", aboutToPlay("Red"));

        teamChoiceService.join(steve.player(), TeamColour.RED);

        assertEquals(List.of(aboutToPlay("Red")), steve.messages());
        assertEquals("Alex", teamRegistry.findTeam("Red").orElseThrow().getMemberNamesJoined());
        assertEquals(0, saveCount);
    }

    @Test
    void refusesToSwitchAwayFromOrLeaveATeamThatIsAboutToPlayAnAutoMatch() {
        register(steve);
        teamChoiceService.join(steve.player(), TeamColour.RED);
        rejectionsByTeamName.put("Red", aboutToPlay("Red"));

        teamChoiceService.join(steve.player(), TeamColour.BLUE);
        teamChoiceService.leave(steve.player());

        assertEquals(List.of("You joined team Red", aboutToPlay("Red"), aboutToPlay("Red")), steve.messages());
        assertEquals(List.of("Red"), teamRegistry.getTeamNames());
    }

    @Test
    void refusesTheUnregistrationOfAPlayerWhoseColouredTeamIsAboutToPlay() {
        register(steve);
        teamChoiceService.join(steve.player(), TeamColour.RED);
        rejectionsByTeamName.put("Red", aboutToPlay("Red"));
        steve.setSneaking(true);

        server.stationService().useStation(steve.player(), button);

        assertEquals(List.of("You joined team Red", aboutToPlay("Red")), steve.messages());
        assertEquals("Steve", teamRegistry.findTeam("Red").orElseThrow().getMemberNamesJoined());
        assertTrue(server.registrationList().isRegistered(steve.uniqueId()));
    }

    @Test
    void unregistersInTheRandomModeEvenWhileTheTeamIsAboutToPlay() {
        register(steve);
        teamChoiceService.join(steve.player(), TeamColour.RED);
        teamModeSetting.setTeamMode(TeamMode.RANDOM);
        rejectionsByTeamName.put("Red", aboutToPlay("Red"));
        steve.setSneaking(true);

        server.stationService().useStation(steve.player(), button);

        assertEquals(List.of("You joined team Red", "You are no longer registered"), steve.messages());
        assertFalse(server.registrationList().isRegistered(steve.uniqueId()));
        assertEquals("Steve", teamRegistry.findTeam("Red").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void anAdminStillRemovesTheRegistrationOfAPlayerWhoseTeamIsAboutToPlay() {
        register(steve);
        teamChoiceService.join(steve.player(), TeamColour.RED);
        rejectionsByTeamName.put("Red", aboutToPlay("Red"));

        assertTrue(server.registrationService().removeRegistration(steve.uniqueId()));

        assertFalse(server.registrationList().isRegistered(steve.uniqueId()));
        assertEquals("Steve", teamRegistry.findTeam("Red").orElseThrow().getMemberNamesJoined());
        assertEquals(List.of("You joined team Red", aboutToPlay("Red")), steve.messages());
    }

    @Test
    void refusesEveryTeamChangeWhileATournamentIsInProgress() {
        register(steve);
        teamChoiceService.join(steve.player(), TeamColour.RED);
        tournamentInProgress = true;

        teamChoiceService.join(steve.player(), TeamColour.BLUE);
        teamChoiceService.leave(steve.player());
        server.registrationService().removeRegistration(steve.uniqueId());

        String closed = "The team choice is closed while a tournament is in progress";
        assertEquals(List.of("You joined team Red", closed, closed, closed), steve.messages());
        assertEquals("Steve", teamRegistry.findTeam("Red").orElseThrow().getMemberNamesJoined());
        assertEquals(0, teamChoiceService.removeColouredTeams());
    }

    @Test
    void tellsOnlyTheOtherMembersWhoAreOnline() {
        register(steve);
        teamRegistry.registerTeam("Red", List.of(new TeamMember(alex.uniqueId(), "Alex"),
                new TeamMember(UUID.randomUUID(), "Herobrine")), TeamColour.RED);
        teamSizeSetting.setTeamSize(3);
        onlinePlayers.remove(alex.uniqueId());

        teamChoiceService.join(steve.player(), TeamColour.RED);

        assertEquals(List.of("You joined team Red"), steve.messages());
        assertEquals(List.of(), alex.messages());
        assertEquals("Alex, Herobrine, Steve", teamRegistry.findTeam("Red").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void tellsTheOtherOnlineMembersAboutTheNewMember() {
        register(steve);
        teamRegistry.registerTeam("Red", List.of(new TeamMember(alex.uniqueId(), "Alex")), TeamColour.RED);

        teamChoiceService.join(steve.player(), TeamColour.RED);

        assertEquals(List.of("Steve joined your team"), alex.messages());
        assertEquals(1, presenter.refreshCount);
    }

    private StationTestServer.TestPlayer join(String playerName, boolean adminPermission) {
        StationTestServer.TestPlayer player = server.joinPlayer(playerName, adminPermission);
        onlinePlayers.put(player.uniqueId(), player.player());
        return player;
    }

    private void register(StationTestServer.TestPlayer player) {
        server.registrationList().register(player.uniqueId(), player.player().getName());
    }

    private static String aboutToPlay(String teamName) {
        return Messages.text(TournamentMessage.TEAM_ABOUT_TO_PLAY_AUTO_MATCH, Placeholder.of("team", teamName));
    }

    private static final class RecordingPresenter implements TeamChoicePresenter {

        private final List<Player> shownPlayers = new ArrayList<>();
        private int refreshCount;

        @Override
        public void show(Player player) {
            shownPlayers.add(player);
        }

        @Override
        public void refresh() {
            refreshCount++;
        }
    }
}

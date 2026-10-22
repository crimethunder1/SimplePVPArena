package de.simplepvparena.registration;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomTeamFormationTest {

    private static final RandomGenerator KEEPING_THE_ORDER = new FixedRandomGenerator(true);
    private static final RandomGenerator ROTATING_THE_ORDER = new FixedRandomGenerator(false);

    private final RegistrationList registrationList = new RegistrationList();
    private final RegistrationService registrationService = new RegistrationService(registrationList,
            () -> false, () -> { });
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
    private final Map<UUID, Player> onlinePlayers = new HashMap<>();
    private final Map<String, List<String>> messagesByPlayer = new HashMap<>();
    private final List<String> adminMessages = new ArrayList<>();
    private final Player admin = createPlayer("Admin", adminMessages);

    private boolean tournamentInProgress;
    private boolean autoMatchAnnouncedOrRunning;
    private int teamSaveCount;

    @Test
    void countsTheTeamsOfAListWithASmallerLastTeam() {
        assertEquals(0, RandomTeamFormation.countTeams(0, 2));
        assertEquals(1, RandomTeamFormation.countTeams(1, 2));
        assertEquals(1, RandomTeamFormation.countTeams(2, 2));
        assertEquals(2, RandomTeamFormation.countTeams(3, 2));
        assertEquals(4, RandomTeamFormation.countTeams(7, 2));
        assertEquals(3, RandomTeamFormation.countTeams(3, 1));
        assertEquals(2, RandomTeamFormation.countTeams(9, 8));
    }

    @Test
    void splitsTheListIntoTeamsOfTheTeamSizeWithASmallerLastTeam() {
        registerPlayers("P1", "P2", "P3", "P4", "P5", "P6", "P7");

        assertTrue(formation(KEEPING_THE_ORDER).form(admin));

        assertEquals(List.of("Red", "Blue", "Green", "Yellow"), teamRegistry.getTeamNames());
        assertEquals(List.of(List.of("P1", "P2"), List.of("P3", "P4"), List.of("P5", "P6"), List.of("P7")),
                memberNamesOfEveryTeam());
    }

    @Test
    void shufflesTheListWithTheRandomSource() {
        registerPlayers("P1", "P2", "P3", "P4");

        formation(ROTATING_THE_ORDER).form(admin);

        assertEquals(List.of(List.of("P2", "P3"), List.of("P4", "P1")), memberNamesOfEveryTeam());
    }

    @Test
    void placesEveryRegisteredPlayerInExactlyOneTeam() {
        registerPlayers("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10", "P11");
        teamSizeSetting.setTeamSize(3);

        formation(new Random(42)).form(admin);

        List<String> placedPlayers = memberNamesOfEveryTeam().stream().flatMap(List::stream).toList();
        assertEquals(11, placedPlayers.size());
        assertEquals(Set.of("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10", "P11"),
                new HashSet<>(placedPlayers));
        assertEquals(List.of(3, 3, 3, 2), teamRegistry.getTeams().stream().map(team -> team.getMembers().size())
                .toList());
    }

    @Test
    void replacesEveryExistingTeamAndWritesTheTeamsOnce() {
        teamRegistry.registerTeam("Old", List.of(new TeamMember(UUID.randomUUID(), "Notch")));
        teamRegistry.registerTeam("Older", List.of(new TeamMember(UUID.randomUUID(), "Jeb")));
        registerPlayers("P1", "P2", "P3", "P4");

        formation(KEEPING_THE_ORDER).form(admin);

        assertEquals(List.of("Red", "Blue"), teamRegistry.getTeamNames());
        assertEquals(1, teamSaveCount);
    }

    @Test
    void keepsTheRegistrationsSoTheTeamsCanBeFormedAgain() {
        registerPlayers("P1", "P2", "P3", "P4");

        formation(KEEPING_THE_ORDER).form(admin);
        formation(ROTATING_THE_ORDER).form(admin);

        assertEquals(4, registrationList.getCount());
        assertEquals(List.of(List.of("P2", "P3"), List.of("P4", "P1")), memberNamesOfEveryTeam());
        assertEquals(2, teamSaveCount);
    }

    @Test
    void givesTheFirstSixteenTeamsTheColoursInTheirOrderAndNumbersTheRest() {
        teamSizeSetting.setTeamSize(1);
        for (int playerNumber = 1; playerNumber <= 18; playerNumber++) {
            registrationList.register(UUID.randomUUID(), "P" + playerNumber);
        }

        formation(KEEPING_THE_ORDER).form(admin);

        assertEquals(List.of("Red", "Blue", "Green", "Yellow", "Orange", "Purple", "LightBlue", "Pink", "White",
                "Gray", "Black", "Brown", "Cyan", "Lime", "Magenta", "LightGray", "Team-17", "Team-18"),
                teamRegistry.getTeamNames());
        List<Optional<TeamColour>> colours = teamRegistry.getTeams().stream().map(Team::getColour).toList();
        for (int teamIndex = 0; teamIndex < 16; teamIndex++) {
            assertEquals(Optional.of(TeamColour.values()[teamIndex]), colours.get(teamIndex));
        }
        assertEquals(Optional.empty(), colours.get(16));
        assertEquals(Optional.empty(), colours.get(17));
    }

    @Test
    @UseLanguage("de")
    void namesTheTeamsInTheLanguageOfTheServer() {
        registerPlayers("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10", "P11", "P12", "P13", "P14");

        formation(KEEPING_THE_ORDER).form(admin);

        assertEquals(List.of("Rot", "Blau", "Grün", "Gelb", "Orange", "Lila", "Hellblau"),
                teamRegistry.getTeamNames());
        assertEquals(List.of("Aus 14 angemeldeten Spielern wurden 7 Teams gebildet"), adminMessages);
        assertEquals(List.of("Du spielst in Team Rot mit P2"), messagesOf("P1"));
    }

    @Test
    void tellsTheAdminHowManyTeamsWereFormed() {
        registerPlayers("P1", "P2", "P3", "P4", "P5", "P6", "P7");

        formation(KEEPING_THE_ORDER).form(admin);

        assertEquals(List.of("4 teams were formed from 7 registered players"), adminMessages);
    }

    @Test
    void tellsEveryOnlinePlayerTheTeamAndTheMates() {
        teamSizeSetting.setTeamSize(3);
        registerPlayers("Alex", "Steve", "Notch", "Jeb");
        registrationList.register(UUID.randomUUID(), "Offline");

        formation(KEEPING_THE_ORDER).form(admin);

        assertEquals(List.of("You play in team Red with Steve and Notch"), messagesOf("Alex"));
        assertEquals(List.of("You play in team Red with Alex and Notch"), messagesOf("Steve"));
        assertEquals(List.of("You play in team Blue with Offline"), messagesOf("Jeb"));
    }

    @Test
    void tellsAPlayerWhoPlaysAlone() {
        registerPlayers("Alex", "Steve", "Notch");

        formation(KEEPING_THE_ORDER).form(admin);

        assertEquals(List.of("You play alone as team Blue"), messagesOf("Notch"));
    }

    @Test
    void refusesWhileATournamentIsInProgress() {
        registerPlayers("P1", "P2", "P3", "P4");
        teamRegistry.registerTeam("Old", List.of(new TeamMember(UUID.randomUUID(), "Notch")));
        tournamentInProgress = true;

        assertFalse(formation(KEEPING_THE_ORDER).form(admin));

        assertEquals(List.of("Old"), teamRegistry.getTeamNames());
        assertEquals(List.of("Teams cannot be formed while a tournament is in progress"), adminMessages);
        assertEquals(0, teamSaveCount);
        assertEquals(List.of(), messagesOf("P1"));
    }

    @Test
    void refusesWhileAnAutoMatchIsAnnouncedOrRunning() {
        registerPlayers("P1", "P2", "P3", "P4");
        autoMatchAnnouncedOrRunning = true;

        assertFalse(formation(KEEPING_THE_ORDER).form(admin));

        assertEquals(0, teamRegistry.getTeamCount());
        assertEquals(List.of("Teams cannot be formed while an auto match is announced or running"), adminMessages);
    }

    @Test
    void refusesWhenFewerThanTwoTeamsWouldBeFormed() {
        RandomTeamFormation formation = formation(KEEPING_THE_ORDER);
        assertEquals(Optional.of("At least 2 teams are needed, 0 players are registered"), formation.findRejection());
        registerPlayers("P1");
        assertEquals(Optional.of("At least 2 teams are needed, 1 player is registered"), formation.findRejection());
        registerPlayers("P2");

        assertFalse(formation.form(admin));

        assertEquals(List.of("At least 2 teams are needed, 2 players are registered"), adminMessages);
        registerPlayers("P3");
        assertEquals(Optional.empty(), formation.findRejection());
    }

    @Test
    void formsTwoTeamsOfOnePlayerFromTwoRegistrations() {
        teamSizeSetting.setTeamSize(1);
        registerPlayers("P1", "P2");

        assertTrue(formation(KEEPING_THE_ORDER).form(admin));

        assertEquals(List.of(List.of("P1"), List.of("P2")), memberNamesOfEveryTeam());
        assertEquals(List.of("2 teams were formed from 2 registered players"), adminMessages);
    }

    private RandomTeamFormation formation(RandomGenerator randomGenerator) {
        return new RandomTeamFormation(registrationService, teamRegistry, teamSizeSetting,
                () -> tournamentInProgress, () -> autoMatchAnnouncedOrRunning, () -> teamSaveCount++,
                onlinePlayers::get, randomGenerator);
    }

    private void registerPlayers(String... playerNames) {
        for (String playerName : playerNames) {
            List<String> messages = new ArrayList<>();
            messagesByPlayer.put(playerName, messages);
            Player player = createPlayer(playerName, messages);
            onlinePlayers.put(player.getUniqueId(), player);
            registrationList.register(player.getUniqueId(), playerName);
        }
    }

    private List<String> messagesOf(String playerName) {
        return messagesByPlayer.get(playerName);
    }

    private List<List<String>> memberNamesOfEveryTeam() {
        return teamRegistry.getTeams().stream()
                .map(team -> team.getMembers().stream().map(TeamMember::playerName).toList())
                .toList();
    }

    private static Player createPlayer(String playerName, List<String> messages) {
        UUID uniqueId = UUID.randomUUID();
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> uniqueId;
            case "getName" -> playerName;
            case "sendMessage" -> {
                messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }

    private record FixedRandomGenerator(boolean keepingTheOrder) implements RandomGenerator {

        @Override
        public int nextInt(int bound) {
            return keepingTheOrder ? bound - 1 : 0;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("only bounded integers are drawn");
        }
    }
}

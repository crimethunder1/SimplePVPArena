package de.simplepvparena.arena;

import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaAudienceTest {

    private static final String ARENA_WORLD_NAME = "audience-arena";
    private static final String LOBBY_WORLD_NAME = "audience-lobby";
    private static final int WALL_MINIMUM = 0;
    private static final int WALL_MAXIMUM = 6;
    private static final int GROUND_Y = 64;
    private static final int WALL_TOP_Y = GROUND_Y + 3;
    private static final int EFFECT_MARGIN = 10;
    private static final double FAR_AWAY = 500.5D;

    private final World arenaWorld = createWorld(ARENA_WORLD_NAME);
    private final World lobbyWorld = createWorld(LOBBY_WORLD_NAME);
    private final List<Player> onlinePlayers = new ArrayList<>();
    private final ArenaRegistry arenaRegistry = new ArenaRegistry();
    private final ArenaAudience audience = new ArenaAudience(() -> onlinePlayers, arenaRegistry);

    @BeforeEach
    void setUp() {
        arenaRegistry.setEffectMargin(EFFECT_MARGIN);
        arenaRegistry.getArena().setShape(buildShape());
    }

    @Test
    void aPlayerInsideOfTheAreaBelongsToTheAudience() {
        Player inside = addOnlinePlayer("Inside", new Location(arenaWorld, 3.5D, GROUND_Y, 3.5D));
        Player atTheTribune = addOnlinePlayer("Tribune", new Location(arenaWorld, WALL_MAXIMUM + 5.5D, GROUND_Y,
                WALL_MAXIMUM + 5.5D));

        assertEquals(List.of(inside, atTheTribune), audience.findPlayersInTheArea());
    }

    @Test
    void aPlayerFarOutsideOfTheAreaIsLeftOut() {
        Player inside = addOnlinePlayer("Inside", new Location(arenaWorld, 3.5D, GROUND_Y, 3.5D));
        addOnlinePlayer("Outside", new Location(arenaWorld, FAR_AWAY, GROUND_Y, FAR_AWAY));

        assertEquals(List.of(inside), audience.findPlayersInTheArea());
    }

    @Test
    void aPlayerInAnotherWorldIsLeftOutEvenAtTheSameCoordinates() {
        addOnlinePlayer("Lobby", new Location(lobbyWorld, 3.5D, GROUND_Y, 3.5D));

        assertEquals(List.of(), audience.findPlayersInTheArea());
    }

    @Test
    void withoutAnAreaNobodyStandsInIt() {
        arenaRegistry.clearArenaConfiguration();
        addOnlinePlayer("Inside", new Location(arenaWorld, 3.5D, GROUND_Y, 3.5D));

        assertEquals(List.of(), audience.findPlayersInTheArea());
    }

    @Test
    void theMembersOfTheMatchTeamsBelongToTheMatchAudienceOutsideOfTheArea() {
        Player firstMember = addOnlinePlayer("First", new Location(lobbyWorld, FAR_AWAY, GROUND_Y, FAR_AWAY));
        Player secondMember = addOnlinePlayer("Second", new Location(arenaWorld, FAR_AWAY, GROUND_Y, FAR_AWAY));
        Player bystander = addOnlinePlayer("Bystander", new Location(arenaWorld, 3.5D, GROUND_Y, 3.5D));
        addOnlinePlayer("Stranger", new Location(arenaWorld, FAR_AWAY, GROUND_Y, FAR_AWAY));

        List<Player> matchAudience = audience.findMatchAudience(List.of(teamOf("Alpha", firstMember),
                teamOf("Bravo", secondMember)));

        assertEquals(List.of(firstMember, secondMember, bystander), matchAudience);
    }

    @Test
    void aMemberInsideOfTheAreaIsListedOnlyOnce() {
        Player member = addOnlinePlayer("Member", new Location(arenaWorld, 3.5D, GROUND_Y, 3.5D));
        Player bystander = addOnlinePlayer("Bystander", new Location(arenaWorld, 4.5D, GROUND_Y, 4.5D));

        List<Player> matchAudience = audience.findMatchAudience(List.of(teamOf("Alpha", member)));

        assertEquals(List.of(member, bystander), matchAudience);
    }

    @Test
    void anOfflineMemberIsMissing() {
        Player onlineMember = addOnlinePlayer("Online", new Location(lobbyWorld, FAR_AWAY, GROUND_Y, FAR_AWAY));
        Team team = new Team("Alpha", List.of(new TeamMember(onlineMember.getUniqueId(), "Online"),
                new TeamMember(UUID.randomUUID(), "Offline")));

        assertEquals(List.of(onlineMember), audience.findMatchAudience(List.of(team)));
    }

    @Test
    void withoutAnAreaTheMatchAudienceIsMadeOfTheMembers() {
        arenaRegistry.clearArenaConfiguration();
        Player member = addOnlinePlayer("Member", new Location(arenaWorld, 3.5D, GROUND_Y, 3.5D));
        addOnlinePlayer("Bystander", new Location(arenaWorld, 4.5D, GROUND_Y, 4.5D));

        assertEquals(List.of(member), audience.findMatchAudience(List.of(teamOf("Alpha", member))));
    }

    private Team teamOf(String teamName, Player member) {
        return new Team(teamName, List.of(new TeamMember(member.getUniqueId(), member.getName())));
    }

    private Player addOnlinePlayer(String playerName, Location location) {
        UUID uniqueId = UUID.randomUUID();
        Player player = SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> uniqueId;
            case "getName" -> playerName;
            case "getLocation" -> location;
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
        onlinePlayers.add(player);
        return player;
    }

    private static World createWorld(String worldName) {
        return SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> worldName;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
    }

    private static ArenaShape buildShape() {
        BorderMarkIndex borderMarks = new BorderMarkIndex();
        for (int coordinate = WALL_MINIMUM; coordinate <= WALL_MAXIMUM; coordinate++) {
            for (int wallY = GROUND_Y; wallY <= WALL_TOP_Y; wallY++) {
                borderMarks.mark(ARENA_WORLD_NAME, coordinate, wallY, WALL_MINIMUM);
                borderMarks.mark(ARENA_WORLD_NAME, coordinate, wallY, WALL_MAXIMUM);
                borderMarks.mark(ARENA_WORLD_NAME, WALL_MINIMUM, wallY, coordinate);
                borderMarks.mark(ARENA_WORLD_NAME, WALL_MAXIMUM, wallY, coordinate);
            }
        }
        return ArenaShapeFloodFill.calculate(ARENA_WORLD_NAME, 3, GROUND_Y, 3,
                        ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT,
                        borderMarks.createColumnReader(ARENA_WORLD_NAME))
                .getShape().orElseThrow();
    }
}

package de.simplepvparena.arena;

import de.simplepvparena.border.BorderConnectivity;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.FakeSpectatorItems;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.tournament.AutomaticArenaCleanup;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleanBetweenRoundsTest {

    private static final String WORLD_NAME = "event";
    private static final int FLOOR_Y = 63;
    private static final int WALL_BOTTOM_Y = 64;
    private static final int WALL_TOP_Y = 66;
    private static final int MAXIMUM_CLEANING_TICKS = 600;

    @Test
    void cleansTheArenaWhileTheMatchIsStillRunningOnTheSameWayAsAfterAMatch() {
        Fixture fixture = new Fixture();
        fixture.calculate();
        fixture.startMatch();
        fixture.harness.setBlock(0, WALL_BOTTOM_Y, 0, Material.COBWEB);

        assertEquals(Optional.of("A match is running, the arena can only be cleaned between matches"),
                fixture.setupService.startAutomaticCleaning(fixture.outcomes::add),
                "the way after a match stays closed while a match runs");
        assertEquals(Optional.empty(), fixture.arenaCleanup.startCleaningBetweenRounds(fixture.outcomes::add));
        assertTrue(fixture.arenaCleanup.isCleaning());
        fixture.runUntilTheCleaningIsOver();

        assertEquals(Material.AIR, fixture.harness.getBlock(0, WALL_BOTTOM_Y, 0), "the placed block is gone");
        assertEquals(1, fixture.outcomes.size());
        assertTrue(fixture.outcomes.get(0).startsWith("The arena is clean"), fixture.outcomes.get(0));
        assertTrue(fixture.matchService.hasActiveMatch());
    }

    @Test
    void refusesASecondRunAndKeepsTheLockForEveryOtherStepOfTheArena() {
        Fixture fixture = new Fixture();
        fixture.calculate();
        fixture.startMatch();
        assertEquals(Optional.empty(), fixture.arenaCleanup.startCleaningBetweenRounds(fixture.outcomes::add));

        assertEquals(Optional.of("The arena is already being cleaned"),
                fixture.arenaCleanup.startCleaningBetweenRounds(fixture.outcomes::add));
        assertTrue(fixture.setupService.findResetRejection().isPresent());
        assertTrue(fixture.setupService.findCleaningRejection().isPresent());

        fixture.runUntilTheCleaningIsOver();
        assertEquals(1, fixture.outcomes.size());
    }

    @Test
    void refusesWithoutABaselineAndWithoutAFightArea() {
        Fixture withoutFightArea = new Fixture();
        assertTrue(withoutFightArea.arenaCleanup.startCleaningBetweenRounds(withoutFightArea.outcomes::add)
                .orElseThrow().contains("not calculated"));

        Fixture withoutBaseline = new Fixture();
        withoutBaseline.calculate();
        withoutBaseline.baselineService.discardSnapshot();
        withoutBaseline.startMatch();

        assertFalse(withoutBaseline.arenaCleanup.hasBaseline());
        assertTrue(withoutBaseline.arenaCleanup.startCleaningBetweenRounds(withoutBaseline.outcomes::add)
                .orElseThrow().contains("no baseline of the fight area is stored"));
        assertFalse(withoutBaseline.arenaCleanup.isCleaning());
        assertTrue(withoutBaseline.outcomes.isEmpty());
    }

    private static final class Fixture {

        private final FakeServerHarness harness = new FakeServerHarness(WORLD_NAME);
        private final ArenaRegistry arenaRegistry = new ArenaRegistry();
        private final ArenaBaselineService baselineService =
                new ArenaBaselineService(harness.getPlugin(), new ArenaBaselineRegistry());
        private final ArenaCleaningService cleaningService =
                new ArenaCleaningService(harness.getPlugin(), (world, shape) -> 0);
        private final MatchService matchService = new MatchService(harness.getPlugin(), new KitService(),
                new SpectatorService(arenaRegistry, new FakeSpectatorItems()),
                SharedBukkitServer.proxy(FighterColours.class, (method, arguments) -> null), StartGates.NONE);
        private final List<String> messages = new ArrayList<>();
        private final List<String> outcomes = new ArrayList<>();
        private final ArenaSetupService setupService;
        private final AutomaticArenaCleanup arenaCleanup;

        private Fixture() {
            SharedBukkitServer.install();
            BorderMarkIndex borderMarks = new BorderMarkIndex();
            for (int blockX = -2; blockX <= 2; blockX++) {
                for (int blockZ = -2; blockZ <= 2; blockZ++) {
                    harness.setBlock(blockX, FLOOR_Y, blockZ, Material.STONE);
                    if (Math.abs(blockX) != 2 && Math.abs(blockZ) != 2) {
                        continue;
                    }
                    for (int blockY = WALL_BOTTOM_Y; blockY <= WALL_TOP_Y; blockY++) {
                        borderMarks.mark(WORLD_NAME, blockX, blockY, blockZ);
                    }
                }
            }
            setupService = new ArenaSetupService(new ActiveArenaState(arenaRegistry, new ArenaHeightMarkers()),
                    matchService, new SpectatorBorderService(borderMarks, () -> { }),
                    new BorderWallPreview(harness.getPlugin(), new SilentVisualEffect()),
                    new ArenaGapGuide(harness.getPlugin()), cleaningService, baselineService,
                    new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape), () -> { });
            arenaCleanup = new AutomaticArenaCleanup(setupService, cleaningService);
        }

        private void calculate() {
            setupService.calculateShape(fakePlayer(messages, harness.world(), WALL_BOTTOM_Y));
            harness.runTicks(20);
            assertTrue(setupService.getShape().isPresent(), "the calculation has to succeed: " + messages);
            assertTrue(setupService.hasBaseline(), "and it has to store a baseline: " + messages);
            messages.clear();
        }

        private void startMatch() {
            Team firstTeam = new Team("Alpha", List.of(new TeamMember(UUID.randomUUID(), "AlphaFighter")));
            Team secondTeam = new Team("Beta", List.of(new TeamMember(UUID.randomUUID(), "BetaFighter")));
            matchService.startMatch(Match.regularMatch("M1", firstTeam, secondTeam, arenaRegistry.getArena()));
            assertTrue(matchService.hasActiveMatch(), "the match holds the arena");
        }

        private void runUntilTheCleaningIsOver() {
            int tickCount = 0;
            while (cleaningService.isCleaning() && tickCount < MAXIMUM_CLEANING_TICKS) {
                harness.runTick();
                tickCount++;
            }
            assertFalse(cleaningService.isCleaning(), "the cleaning has to finish");
        }
    }

    private static final class SilentVisualEffect implements BorderVisualEffect {

        @Override
        public void showAt(Player player, Location borderBlockCenter, BorderConnectivity connectivity) {
        }

        @Override
        public void showConfirmedAt(Player player, Location borderBlockCenter) {
        }

        @Override
        public void applyConfirmedParticleSettings(ConfirmedBorderParticleSettings settings,
                                                   Consumer<String> problemReporter) {
        }

        @Override
        public void showColumnConnection(Player player, Location pointBetweenBorderColumns) {
        }

        @Override
        public void showConnection(Player player, Location pointBetweenBorderBlocks) {
        }
    }

    private static Player fakePlayer(List<String> messages, World world, int blockY) {
        UUID playerId = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
                (proxyInstance, method, arguments) -> switch (method.getName()) {
                    case "getLocation" -> new Location(world, 0.5D, blockY, 0.5D);
                    case "sendMessage" -> {
                        messages.add(arguments[0] instanceof Component component
                                ? TournamentMessages.toPlainBody(component) : String.valueOf(arguments[0]));
                        yield null;
                    }
                    case "getUniqueId" -> playerId;
                    case "getName" -> "admin";
                    case "isOnline" -> true;
                    case "getWorld" -> world;
                    case "hasPermission" -> true;
                    case "toString" -> "fake player";
                    case "hashCode" -> System.identityHashCode(proxyInstance);
                    case "equals" -> proxyInstance == arguments[0];
                    default -> throw new UnsupportedOperationException("Player." + method.getName());
                });
    }
}

package de.simplepvparena.arena;

import de.simplepvparena.border.BorderConnectivity;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleaningTemporaryEntityTest {

    private static final String WORLD_NAME = "event";
    private static final int FLOOR_Y = 63;
    private static final int WALL_BOTTOM_Y = 64;
    private static final int WALL_TOP_Y = 66;

    private static final int MAXIMUM_CLEANING_TICKS = 600;

    private static final int PROBE_X = 0;
    private static final int PROBE_Z = 0;

    @Test
    void removesTheTemporaryEntitiesOnceInTheFirstTickOfTheCleaningAfterAMatch() {
        Fixture fixture = new Fixture();
        fixture.calculate();
        fixture.cleaningService.setAnimationDurationTicks(0L);
        int probeY = fixture.placeCobwebInTheFirstCleanedLayer();

        assertTrue(fixture.setupService.startAutomaticCleaning(outcome -> { }).isEmpty());
        assertTrue(fixture.removals.isEmpty(), "nothing is removed before the run has its first tick");
        fixture.harness.runTick();

        assertEquals(1, fixture.removals.size());
        assertEquals(Material.COBWEB, fixture.removals.get(0).probedMaterial(),
                "no block was written before the removal");
        assertEquals(Material.AIR, fixture.harness.getBlock(PROBE_X, probeY, PROBE_Z),
                "the same tick wrote the first layer after it");
        fixture.runUntilTheCleaningIsOver();
        assertEquals(1, fixture.removals.size(), "the run removes them only once");
        fixture.assertTheFightAreaWasHandedOver(fixture.removals.get(0));
    }

    @Test
    void removesTheTemporaryEntitiesOnceInTheFirstTickOfACleaningStartedFromTheMenu() {
        Fixture fixture = new Fixture();
        fixture.calculate();
        fixture.cleaningService.setAnimationDurationTicks(0L);
        int probeY = fixture.placeCobwebInTheFirstCleanedLayer();

        assertTrue(fixture.setupService.startCleaning(fakeSender()));
        assertTrue(fixture.removals.isEmpty(), "nothing is removed before the run has its first tick");
        fixture.harness.runTick();

        assertEquals(1, fixture.removals.size());
        assertEquals(Material.COBWEB, fixture.removals.get(0).probedMaterial(),
                "no block was written before the removal");
        assertEquals(Material.AIR, fixture.harness.getBlock(PROBE_X, probeY, PROBE_Z),
                "the same tick wrote the first layer after it");
        fixture.runUntilTheCleaningIsOver();
        assertEquals(1, fixture.removals.size(), "the run removes them only once");
        fixture.assertTheFightAreaWasHandedOver(fixture.removals.get(0));
    }

    @Test
    void removesThemBeforeTheFirstFloodedLayerIsShown() {
        Fixture fixture = new Fixture();
        fixture.calculate();

        assertTrue(fixture.setupService.startAutomaticCleaning(outcome -> { }).isEmpty());
        fixture.harness.runTick();

        assertEquals(List.of("remove temporary entities", "show " + ArenaCleaningPhase.FLOOD),
                fixture.steps);
        fixture.runUntilTheCleaningIsOver();
        assertEquals(1, fixture.removals.size(), "the animated run removes them only once as well");
    }

    @Test
    void removesThemOnceForEveryRun() {
        Fixture fixture = new Fixture();
        fixture.calculate();
        fixture.cleaningService.setAnimationDurationTicks(0L);

        assertTrue(fixture.setupService.startAutomaticCleaning(outcome -> { }).isEmpty());
        fixture.runUntilTheCleaningIsOver();
        assertTrue(fixture.setupService.startCleaning(fakeSender()));
        fixture.runUntilTheCleaningIsOver();

        assertEquals(2, fixture.removals.size());
    }

    @Test
    void removesNothingWhenTheWorldIsNotLoaded() {
        Fixture fixture = new Fixture();
        fixture.calculate();
        assertTrue(fixture.setupService.startAutomaticCleaning(outcome -> { }).isEmpty());
        fixture.harness.unloadWorld();

        fixture.harness.runTick();

        assertFalse(fixture.cleaningService.isCleaning(), "the run stops without a world");
        assertTrue(fixture.removals.isEmpty());
    }

    @Test
    void restoresEveryBlockAndEndsSuccessfullyWhenTheRemovalFails() {
        Fixture fixture = new Fixture();
        fixture.calculate();
        fixture.cleaningService.setAnimationDurationTicks(0L);
        RuntimeException failure = new IllegalStateException("the entities could not be looked up");
        fixture.removalFailure = failure;
        ArenaShape cleanedVolume = fixture.setupService.findCleanedVolume().orElseThrow();
        fixture.harness.setBlock(PROBE_X, cleanedVolume.getFloorY(), PROBE_Z, Material.COBWEB);
        fixture.harness.setBlock(PROBE_X, FLOOR_Y, PROBE_Z, Material.COBWEB);
        fixture.harness.setBlock(PROBE_X, WALL_TOP_Y, PROBE_Z, Material.COBWEB);
        List<String> outcomes = new ArrayList<>();
        List<LogRecord> logRecords = new ArrayList<>();
        Handler recordingHandler = recordingHandler(logRecords);
        Logger logger = fixture.harness.getPlugin().getLogger();
        logger.addHandler(recordingHandler);
        try {
            assertTrue(fixture.setupService.startAutomaticCleaning(outcomes::add).isEmpty());
            fixture.runUntilTheCleaningIsOver();
        } finally {
            logger.removeHandler(recordingHandler);
        }

        assertEquals(1, fixture.removals.size(), "the failed removal is not tried again in the following ticks");
        assertEquals(1, outcomes.size());
        assertTrue(outcomes.get(0).startsWith("The arena is clean"), "the run ends successfully: " + outcomes);
        assertTrue(outcomes.get(0).contains("3 of " + cleanedVolume.getBlockCount() + " checked blocks"),
                "every block of the cleaned volume was walked: " + outcomes);
        assertEquals(Material.AIR, fixture.harness.getBlock(PROBE_X, cleanedVolume.getFloorY(), PROBE_Z),
                "the deepest secured layer is restored");
        assertEquals(Material.STONE, fixture.harness.getBlock(PROBE_X, FLOOR_Y, PROBE_Z),
                "the floor is restored");
        assertEquals(Material.AIR, fixture.harness.getBlock(PROBE_X, WALL_TOP_Y, PROBE_Z),
                "the top layer is restored");
        assertTrue(logRecords.stream().anyMatch(logRecord -> logRecord.getLevel() == Level.WARNING
                        && logRecord.getMessage().contains("temporary entities of the arena could not be removed")
                        && logRecord.getThrown() == failure),
                "the failure is logged as a warning with the exception");
    }

    private static Handler recordingHandler(List<LogRecord> logRecords) {
        return new Handler() {

            @Override
            public void publish(LogRecord logRecord) {
                logRecords.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }

    private static final class Fixture {

        private final FakeServerHarness harness = new FakeServerHarness(WORLD_NAME);
        private final ArenaRegistry arenaRegistry = new ArenaRegistry();
        private final ArenaBaselineService baselineService =
                new ArenaBaselineService(harness.getPlugin(), new ArenaBaselineRegistry());
        private final List<String> steps = new ArrayList<>();
        private final List<Removal> removals = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();
        private final ArenaSetupService setupService;

        private int probeY = WALL_BOTTOM_Y;
        private RuntimeException removalFailure;

        private final ArenaCleaningService cleaningService = new ArenaCleaningService(harness.getPlugin(),
                (world, shape) -> 0, new RecordingVisualEffect(), ArenaCleaningExclusion.NONE,
                (world, shape) -> {
                    steps.add("remove temporary entities");
                    removals.add(new Removal(world, shape, harness.getBlock(PROBE_X, probeY, PROBE_Z)));
                    if (removalFailure != null) {
                        throw removalFailure;
                    }
                    return 0;
                });

        private Fixture() {
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
            setupService = new ArenaSetupService(
                    new ActiveArenaState(arenaRegistry, new ArenaHeightMarkers()),
                    new MatchService(harness.getPlugin(), null, null, null, StartGates.NONE),
                    new SpectatorBorderService(harness.getPlugin(), borderMarks, () -> { }),
                    new BorderWallPreview(harness.getPlugin(), new SilentVisualEffect()),
                    new ArenaGapGuide(harness.getPlugin()), cleaningService, baselineService,
                    new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape),
                    () -> { });
        }

        private void calculate() {
            setupService.calculateShape(fakePlayer(messages, harness.world(), WALL_BOTTOM_Y));
            harness.runTicks(20);
            assertTrue(setupService.getShape().isPresent(), "the calculation has to succeed: " + messages);
            assertTrue(setupService.hasBaseline(), "and it has to store a baseline: " + messages);
            messages.clear();
        }

        private int placeCobwebInTheFirstCleanedLayer() {
            probeY = setupService.findCleanedVolume().orElseThrow().getFloorY();
            harness.setBlock(PROBE_X, probeY, PROBE_Z, Material.COBWEB);
            return probeY;
        }

        private void runUntilTheCleaningIsOver() {
            int tickCount = 0;
            while (cleaningService.isCleaning() && tickCount < MAXIMUM_CLEANING_TICKS) {
                harness.runTick();
                tickCount++;
            }
            assertFalse(cleaningService.isCleaning(), "the cleaning has to finish");
        }

        private void assertTheFightAreaWasHandedOver(Removal removal) {
            ArenaShape fightArea = setupService.getShape().orElseThrow();
            assertSame(harness.world(), removal.world());
            assertTrue(fightArea.hasSameVolume(removal.shape()),
                    "the fight area is handed over: " + removal.shape().describe());
            assertEquals(WALL_BOTTOM_Y, removal.shape().getFloorY(),
                    "from its own floor, not from the secured depth under it");
            assertTrue(setupService.findCleanedVolume().orElseThrow().getFloorY() < WALL_BOTTOM_Y,
                    "while the cleaning itself reaches below the floor");
        }

        private final class RecordingVisualEffect implements ArenaCleaningVisualEffect {

            @Override
            public void showFloodedLayer(World world, ArenaShape shape, ArenaCleaningPhase phase,
                                         int layerY) {
                if (steps.size() < 2) {
                    steps.add("show " + phase);
                }
            }

            @Override
            public void applyParticleSettings(ArenaCleaningParticleSettings settings,
                                              Consumer<String> problemReporter) {
            }
        }
    }

    private record Removal(World world, ArenaShape shape, Material probedMaterial) {
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

    private static CommandSender fakeSender() {
        return SharedBukkitServer.proxy(CommandSender.class, (method, arguments) -> switch (method.getName()) {
            case "sendMessage" -> null;
            default -> throw new UnsupportedOperationException("CommandSender." + method.getName());
        });
    }

    private static Player fakePlayer(List<String> messages, World world, int blockY) {
        UUID playerId = UUID.randomUUID();
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
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
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }
}

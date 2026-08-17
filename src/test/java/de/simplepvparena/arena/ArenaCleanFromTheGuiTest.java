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
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleanFromTheGuiTest {

    private static final String WORLD_NAME = "event";
    private static final int FLOOR_Y = 63;
    private static final int WALL_BOTTOM_Y = 64;
    private static final int WALL_TOP_Y = 66;

    private static final int MAXIMUM_CLEANING_TICKS = 600;

    private static final int PROBE_X = 0;
    private static final int PROBE_Y = WALL_BOTTOM_Y;
    private static final int PROBE_Z = 0;

    @Test
    void cleansTheArenaAfterACalculationRunFromAboveTheWall() {
        Fixture fixture = new Fixture();

        fixture.calculateFrom(WALL_TOP_Y + 6);
        fixture.harness.setBlock(0, WALL_BOTTOM_Y, 0, Material.COBWEB);
        fixture.clean();

        assertEquals(Material.AIR, fixture.harness.getBlock(0, WALL_BOTTOM_Y, 0),
                "the block placed after the calculation has to be gone");
        assertEquals(Material.STONE, fixture.harness.getBlock(0, FLOOR_Y, 0),
                "the floor of the baseline stays");
    }

    @Test
    void coversTheSameBlocksNoMatterWhichHeightTheCalculationRanAt() {
        Fixture standing = new Fixture();
        standing.calculateFrom(WALL_BOTTOM_Y);
        Fixture flying = new Fixture();
        flying.calculateFrom(WALL_TOP_Y + 6);

        ArenaShape standingShape = standing.setupService.getShape().orElseThrow();
        ArenaShape flyingShape = flying.setupService.getShape().orElseThrow();

        assertEquals(WALL_BOTTOM_Y, standingShape.getFloorY());
        assertEquals(WALL_BOTTOM_Y, flyingShape.getFloorY(),
                "the floor of the volume is the bottom of the wall, not the height of the admin");
        assertEquals(standingShape.getBlockCount(), flyingShape.getBlockCount());
        assertEquals(WALL_TOP_Y + 6, flyingShape.getReferenceY(),
                "the calculation height itself is kept, the wall recognition still runs on it");
    }

    @Test
    void emptiesTheSameVolumeWhenTheBaselineIsGone() {
        Fixture fixture = new Fixture();
        fixture.calculateFrom(WALL_TOP_Y + 6);
        fixture.baselineService.discardSnapshot();
        fixture.harness.setBlock(0, WALL_BOTTOM_Y, 0, Material.COBWEB);

        assertFalse(fixture.setupService.hasBaseline());
        fixture.clean();

        assertEquals(Material.AIR, fixture.harness.getBlock(0, WALL_BOTTOM_Y, 0));
        assertTrue(fixture.messages.stream().anyMatch(message -> message.contains("set to air")),
                "the admin reads why the arena was emptied instead of restored: " + fixture.messages);
    }

    @Test
    void saysSoWhenNotASingleBlockHadToBeChanged() {
        Fixture fixture = new Fixture();
        fixture.calculateFrom(WALL_BOTTOM_Y);

        fixture.clean();

        assertTrue(fixture.messages.stream().anyMatch(
                        message -> message.contains("not a single block had to be changed")),
                "the report has to explain the arena that did not move: " + fixture.messages);
    }

    @Test
    void namesTheCoveredHeightsBeforeAndWhileItCleans() {
        Fixture fixture = new Fixture();
        fixture.calculateFrom(WALL_TOP_Y + 6);

        assertTrue(fixture.setupService.describeCleaningEffect().stream().anyMatch(
                        line -> line.contains("y " + WALL_BOTTOM_Y) && line.contains("y " + WALL_TOP_Y)),
                "the tile and its confirmation show the covered heights: "
                        + fixture.setupService.describeCleaningEffect());

        fixture.clean();

        assertTrue(fixture.messages.stream().anyMatch(
                        message -> message.startsWith("Cleaning the arena")
                                && message.contains("between y " + WALL_BOTTOM_Y + " and y " + WALL_TOP_Y)),
                "the running cleaning names them too: " + fixture.messages);
    }

    @Test
    void staysUsableWhenTheHandlerAfterTheLastBlockThrows() {
        Fixture fixture = new Fixture();
        fixture.calculateFrom(WALL_BOTTOM_Y);
        fixture.cleanedArenaHandler = (world, shape) -> {
            throw new IllegalStateException("the border marks could not be cleared");
        };
        fixture.harness.setBlock(0, WALL_BOTTOM_Y, 0, Material.COBWEB);

        fixture.clean();

        assertFalse(fixture.cleaningService.isCleaning(),
                "the failed handler may not leave the cleaning running");
        assertEquals(0, fixture.harness.getScheduledTaskCount(),
                "the task may not keep throwing every tick");
        assertEquals(Material.AIR, fixture.harness.getBlock(0, WALL_BOTTOM_Y, 0),
                "the blocks were written before the handler ran and stay written");
        assertTrue(fixture.messages.stream().anyMatch(
                        message -> message.startsWith("The arena cleaning was stopped")
                                && message.contains("the border marks could not be cleared")),
                "the admin reads why the cleaning was stopped: " + fixture.messages);
        assertTrue(fixture.messages.stream().noneMatch(message -> message.startsWith("The arena is clean")),
                "a stopped cleaning is never announced as a finished one: " + fixture.messages);

        fixture.cleanedArenaHandler = (world, shape) -> 1;
        fixture.messages.clear();
        fixture.clean();

        assertTrue(fixture.messages.stream().anyMatch(message -> message.startsWith("The arena is clean")),
                "the second cleaning has to finish normally: " + fixture.messages);
    }

    @Test
    void saysSoWhenTheBaselineCaptureCannotReadTheFightArea() {
        Fixture fixture = new Fixture();

        fixture.startCalculationFrom(WALL_BOTTOM_Y);
        fixture.harness.failBlockAccessWith(new IllegalStateException("the chunk is no longer loaded"));
        fixture.harness.runTicks(20);
        fixture.harness.stopFailingBlockAccess();

        assertTrue(fixture.setupService.getShape().isPresent(), "the calculation itself succeeded");
        assertFalse(fixture.baselineService.isCapturing(),
                "the failed capture may not leave itself running: " + fixture.messages);
        assertFalse(fixture.setupService.hasBaseline(), "a half read baseline is never stored");
        assertTrue(fixture.setupService.findCleaningRejection().isEmpty(),
                "a hanging capture may not hold the next cleaning back: "
                        + fixture.setupService.findCleaningRejection());
        assertTrue(fixture.messages.stream().anyMatch(
                        message -> message.contains("could not be taken")
                                && message.contains("the chunk is no longer loaded")
                                && message.contains("sets every block to air")),
                "the admin reads that the next cleaning falls back to air: " + fixture.messages);

        fixture.messages.clear();
        fixture.calculateFrom(WALL_BOTTOM_Y);

        assertTrue(fixture.setupService.hasBaseline(), "the repeated calculation has to store a baseline");
    }

    @Test
    void climbsTheFightAreaInRedFirstAndClearsItInGreenAfterwards() {
        Fixture fixture = new Fixture();
        fixture.calculateFrom(WALL_BOTTOM_Y);
        fixture.harness.setBlock(PROBE_X, PROBE_Y, PROBE_Z, Material.COBWEB);

        fixture.clean();

        assertEquals(List.of(WALL_BOTTOM_Y, WALL_BOTTOM_Y + 1, WALL_TOP_Y),
                fixture.layersOf(ArenaCleaningPhase.FLOOD),
                "the red flood climbs every layer of the volume, from the floor up");
        assertEquals(List.of(WALL_BOTTOM_Y, WALL_BOTTOM_Y + 1, WALL_TOP_Y),
                fixture.layersOf(ArenaCleaningPhase.CLEAR),
                "and the green one walks the very same layers again");
        assertEquals(List.of(ArenaCleaningPhase.FLOOD, ArenaCleaningPhase.CLEAR),
                fixture.shownLayers.stream().map(ShownLayer::phase).distinct().toList(),
                "never the other way round and never mixed: " + fixture.shownLayers);
        assertTrue(fixture.shownLayers.stream()
                        .filter(shownLayer -> shownLayer.phase() == ArenaCleaningPhase.FLOOD)
                        .allMatch(shownLayer -> shownLayer.probedMaterial() == Material.COBWEB),
                "the announcing phase may not have written a single block: " + fixture.shownLayers);
        assertEquals(Material.AIR, fixture.harness.getBlock(PROBE_X, PROBE_Y, PROBE_Z),
                "and the green phase behind it still cleans");
    }

    @Test
    void takesTheConfiguredTimeForBothPhasesAndNoneWithoutThem() {
        Fixture animated = new Fixture();
        animated.calculateFrom(WALL_BOTTOM_Y);
        animated.cleaningService.setAnimationDurationTicks(60L);
        animated.harness.setBlock(PROBE_X, PROBE_Y, PROBE_Z, Material.COBWEB);

        assertEquals(60, animated.clean(), "30 ticks of red and 30 of green over three layers");
        assertEquals(Material.AIR, animated.harness.getBlock(PROBE_X, PROBE_Y, PROBE_Z));

        Fixture switchedOff = new Fixture();
        switchedOff.calculateFrom(WALL_BOTTOM_Y);
        switchedOff.cleaningService.setAnimationDurationTicks(0L);
        switchedOff.harness.setBlock(PROBE_X, PROBE_Y, PROBE_Z, Material.COBWEB);

        assertEquals(3, switchedOff.clean(), "one tick per layer and no phase that is held back");
        assertEquals(Material.AIR, switchedOff.harness.getBlock(PROBE_X, PROBE_Y, PROBE_Z));
        assertTrue(switchedOff.layersOf(ArenaCleaningPhase.FLOOD).isEmpty(),
                "there is nothing to announce when nothing is drawn: " + switchedOff.shownLayers);
    }

    @Test
    void clearsWhatWasAddedSinceTheCalculationAndNothingElse() {
        Fixture fixture = new Fixture();
        fixture.harness.setBlock(1, WALL_BOTTOM_Y, 1, Material.STONE);
        fixture.harness.setBlock(-1, WALL_BOTTOM_Y, -1, Material.STONE);
        fixture.calculateFrom(WALL_BOTTOM_Y);

        fixture.harness.setBlock(PROBE_X, PROBE_Y, PROBE_Z, Material.COBWEB);
        fixture.harness.setBlock(0, WALL_TOP_Y, 1, Material.WATER);
        fixture.harness.setBlock(1, WALL_BOTTOM_Y + 1, 0, Material.LAVA);
        fixture.harness.setBlock(-1, WALL_BOTTOM_Y, -1, Material.AIR);

        fixture.clean();

        assertEquals(Material.AIR, fixture.harness.getBlock(PROBE_X, PROBE_Y, PROBE_Z),
                "a block placed after the calculation is gone");
        assertEquals(Material.AIR, fixture.harness.getBlock(0, WALL_TOP_Y, 1),
                "water that was poured into the arena is gone with it");
        assertEquals(Material.AIR, fixture.harness.getBlock(1, WALL_BOTTOM_Y + 1, 0), "and so is lava");
        assertEquals(Material.STONE, fixture.harness.getBlock(1, WALL_BOTTOM_Y, 1),
                "a block the baseline holds is never touched");
        assertEquals(Material.STONE, fixture.harness.getBlock(-1, WALL_BOTTOM_Y, -1),
                "and one that was broken out of the arena comes back");
        assertTrue(fixture.messages.stream().anyMatch(message -> message.contains("4 of 27 checked blocks")),
                "every block of the volume is checked and only the four are written: " + fixture.messages);
    }

    @Test
    void cleansTheWholeArenaWhenTheParticlesThrowInTheAnnouncingPhase() {
        Fixture fixture = new Fixture();
        fixture.calculateFrom(WALL_BOTTOM_Y);
        fixture.visualEffectFailure = new IllegalStateException("the particle could not be spawned");
        fixture.harness.setBlock(PROBE_X, PROBE_Y, PROBE_Z, Material.COBWEB);

        fixture.clean();

        assertEquals(Material.AIR, fixture.harness.getBlock(PROBE_X, PROBE_Y, PROBE_Z),
                "the blocks are written although the effect failed in the phase before them");
        assertEquals(Material.STONE, fixture.harness.getBlock(0, FLOOR_Y, 0));
        assertFalse(fixture.cleaningService.isCleaning(), "the cleaning has to finish normally");
        assertTrue(fixture.messages.stream().anyMatch(message -> message.startsWith("The arena is clean")),
                "a failed effect is nothing the admin is told about: " + fixture.messages);
        assertTrue(fixture.messages.stream().noneMatch(message -> message.contains("particle")),
                "the failure belongs into the log, not into the chat: " + fixture.messages);
    }

    @Test
    void stopsAndReportsWhenTheWorldBreaksInTheMiddleOfTheClearingPhase() {
        Fixture fixture = new Fixture();
        fixture.calculateFrom(WALL_BOTTOM_Y);
        fixture.harness.setBlock(PROBE_X, PROBE_Y, PROBE_Z, Material.COBWEB);
        fixture.onShownLayer = shownLayer -> {
            if (shownLayer.phase() == ArenaCleaningPhase.CLEAR && shownLayer.layerY() > WALL_BOTTOM_Y) {
                fixture.harness.failBlockAccessWith(new IllegalStateException("the chunk is no longer loaded"));
            }
        };

        fixture.clean();
        fixture.harness.stopFailingBlockAccess();

        assertFalse(fixture.cleaningService.isCleaning(), "the broken world may not leave the run behind");
        assertEquals(0, fixture.harness.getScheduledTaskCount(),
                "the task may not keep throwing every tick");
        assertEquals(Material.AIR, fixture.harness.getBlock(PROBE_X, PROBE_Y, PROBE_Z),
                "the layers that were written before the failure stay written");
        assertTrue(fixture.messages.stream().anyMatch(
                        message -> message.startsWith("The arena cleaning was stopped")
                                && message.contains("the chunk is no longer loaded")),
                "the admin reads why the cleaning was stopped: " + fixture.messages);

        fixture.messages.clear();
        fixture.onShownLayer = shownLayer -> { };
        fixture.clean();

        assertTrue(fixture.messages.stream().anyMatch(message -> message.startsWith("The arena is clean")),
                "the second cleaning has to finish normally: " + fixture.messages);
    }

    private static final class Fixture {

        private final FakeServerHarness harness = new FakeServerHarness(WORLD_NAME);
        private final ArenaRegistry arenaRegistry = new ArenaRegistry();
        private final ArenaBaselineService baselineService =
                new ArenaBaselineService(harness.getPlugin(), new ArenaBaselineRegistry());
        private final List<String> messages = new ArrayList<>();
        private final ArenaSetupService setupService;

        private CleanedArenaHandler cleanedArenaHandler = (world, shape) -> 0;

        private final List<ShownLayer> shownLayers = new ArrayList<>();

        private RuntimeException visualEffectFailure;

        private Consumer<ShownLayer> onShownLayer = shownLayer -> { };

        private final ArenaCleaningService cleaningService = new ArenaCleaningService(harness.getPlugin(),
                (world, shape) -> cleanedArenaHandler.onArenaCleaned(world, shape),
                new RecordingVisualEffect());

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

        private List<String> calculateFrom(int blockY) {
            startCalculationFrom(blockY);
            harness.runTicks(20);
            assertTrue(setupService.getShape().isPresent(), "the calculation has to succeed: " + messages);
            assertFalse(baselineService.isCapturing(), "the baseline has to be complete: " + messages);
            List<String> calculationMessages = List.copyOf(messages);
            messages.clear();
            return calculationMessages;
        }

        private void startCalculationFrom(int blockY) {
            setupService.calculateShape(fakePlayer(messages, harness.world(), blockY));
        }

        private final class RecordingVisualEffect implements ArenaCleaningVisualEffect {

            @Override
            public void showFloodedLayer(World world, ArenaShape shape, ArenaCleaningPhase phase,
                                         int layerY) {
                if (visualEffectFailure != null) {
                    throw visualEffectFailure;
                }
                ShownLayer shownLayer = new ShownLayer(phase, layerY,
                        harness.getBlock(PROBE_X, PROBE_Y, PROBE_Z));
                shownLayers.add(shownLayer);
                onShownLayer.accept(shownLayer);
            }

            @Override
            public void applyParticleSettings(ArenaCleaningParticleSettings settings,
                                              Consumer<String> problemReporter) {
            }
        }

        private int clean() {
            assertTrue(setupService.findCleaningRejection().isEmpty(),
                    "nothing may stand in the way of the cleaning: " + setupService.findCleaningRejection());
            assertTrue(setupService.startCleaning(fakeSender(messages)));
            int tickCount = 0;
            while (cleaningService.isCleaning() && tickCount < MAXIMUM_CLEANING_TICKS) {
                harness.runTick();
                tickCount++;
            }
            assertFalse(cleaningService.isCleaning(), "the cleaning has to finish");
            return tickCount;
        }

        private List<Integer> layersOf(ArenaCleaningPhase phase) {
            List<Integer> layers = new ArrayList<>();
            for (ShownLayer shownLayer : shownLayers) {
                if (shownLayer.phase() == phase && (layers.isEmpty()
                        || layers.get(layers.size() - 1) != shownLayer.layerY())) {
                    layers.add(shownLayer.layerY());
                }
            }
            return layers;
        }
    }

    private record ShownLayer(ArenaCleaningPhase phase, int layerY, Material probedMaterial) {
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

    private static String recordedText(Object message) {
        if (message instanceof Component component) {
            return TournamentMessages.toPlainBody(component);
        }
        return String.valueOf(message);
    }

    private static Player fakePlayer(List<String> messages, World world, int blockY) {
        UUID playerId = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
                (proxyInstance, method, arguments) -> switch (method.getName()) {
                    case "getLocation" -> new Location(world, 0.5D, blockY, 0.5D);
                    case "sendMessage" -> {
                        messages.add(recordedText(arguments[0]));
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

    private static CommandSender fakeSender(List<String> messages) {
        return (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxyInstance, method, arguments) -> switch (method.getName()) {
                    case "sendMessage" -> {
                        messages.add(recordedText(arguments[0]));
                        yield null;
                    }
                    case "toString" -> "fake sender";
                    case "hashCode" -> System.identityHashCode(proxyInstance);
                    case "equals" -> proxyInstance == arguments[0];
                    default -> throw new UnsupportedOperationException("CommandSender." + method.getName());
                });
    }
}

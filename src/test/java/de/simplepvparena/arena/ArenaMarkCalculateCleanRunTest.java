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
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.FakeSpectatorItems;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

class ArenaMarkCalculateCleanRunTest {

    private static final String WORLD_NAME = "event";
    private static final int FLOOR_Y = 63;
    private static final int WALL_BOTTOM_Y = 64;
    private static final int WALL_TOP_Y = 66;

    private static final int MARKED_LOWEST_Y = FLOOR_Y - 2;
    private static final int MARKED_HIGHEST_Y = WALL_BOTTOM_Y;

    private static final int MAXIMUM_CLEANING_TICKS = 600;

    @Test
    void cleansTheMarkedVolumeAfterOneMarkingOneCalculationAndOneClean() {
        Fixture fixture = new Fixture();

        fixture.placeMarkerAt(MARKED_LOWEST_Y);
        fixture.placeMarkerAt(MARKED_HIGHEST_Y);

        fixture.clickCalculate(WALL_BOTTOM_Y);

        ArenaShape volume = fixture.setupService.getShape().orElseThrow();
        assertEquals(MARKED_LOWEST_Y, volume.getFloorY(),
                "the volume that is cleaned starts at the marked floor, not at the border wall");
        assertEquals(MARKED_HIGHEST_Y, volume.getWallTopY(),
                "and it stops at the marked ceiling, although the wall stands above it");

        fixture.harness.setBlock(0, MARKED_LOWEST_Y, 0, Material.COBWEB);
        fixture.harness.setBlock(0, WALL_TOP_Y, 0, Material.COBWEB);
        fixture.clickClean();

        assertEquals(Material.AIR, fixture.harness.getBlock(0, MARKED_LOWEST_Y, 0),
                "the block below the wall is inside the marked volume and is cleaned");
        assertEquals(Material.COBWEB, fixture.harness.getBlock(0, WALL_TOP_Y, 0),
                "the block at the top of the wall lies above the marked ceiling and is left alone");
    }

    @Test
    void climbsFromTheMarkedFloorToTheMarkedCeilingAndNoHigher() {
        Fixture fixture = new Fixture();
        fixture.placeMarkerAt(MARKED_LOWEST_Y);
        fixture.placeMarkerAt(MARKED_HIGHEST_Y);
        fixture.clickCalculate(WALL_BOTTOM_Y);

        fixture.clickClean();

        List<Integer> clearedLayers = fixture.layersOf(ArenaCleaningPhase.CLEAR);
        assertEquals(List.of(MARKED_LOWEST_Y, MARKED_LOWEST_Y + 1, FLOOR_Y, MARKED_HIGHEST_Y), clearedLayers,
                "the cleaning walks exactly the marked layers, from the marked floor up");
        assertEquals(clearedLayers, fixture.layersOf(ArenaCleaningPhase.FLOOD),
                "and the red phase announced the very same ones");
    }

    @Test
    void readsTheBaselineOfTheSameVolumeItCleans() {
        Fixture fixture = new Fixture();
        fixture.placeMarkerAt(MARKED_LOWEST_Y);
        fixture.placeMarkerAt(MARKED_HIGHEST_Y);
        fixture.harness.setBlock(0, MARKED_LOWEST_Y, 0, Material.STONE);

        fixture.clickCalculate(WALL_BOTTOM_Y);

        ArenaShape volume = fixture.setupService.getShape().orElseThrow();
        ArenaBaselineSnapshot baseline = fixture.baselineService.findSnapshot(volume).orElseThrow();
        assertEquals(MARKED_LOWEST_Y, baseline.getShape().getFloorY());
        assertEquals(MARKED_HIGHEST_Y, baseline.getShape().getWallTopY());
        assertTrue(baseline.covers(volume), "the baseline and the cleaned volume are the same blocks");
        assertTrue(fixture.setupService.findCleaningRejection().isEmpty(),
                "and nothing about that pair is refused: " + fixture.setupService.findCleaningRejection());

        fixture.harness.setBlock(0, MARKED_LOWEST_Y, 0, Material.AIR);
        fixture.clickClean();

        assertEquals(Material.STONE, fixture.harness.getBlock(0, MARKED_LOWEST_Y, 0));
    }

    @Test
    void showsTheMarkedHeightsInTheTileTheAdminConfirms() {
        Fixture fixture = new Fixture();
        fixture.placeMarkerAt(MARKED_LOWEST_Y);
        fixture.placeMarkerAt(MARKED_HIGHEST_Y);
        fixture.clickCalculate(WALL_BOTTOM_Y);

        List<String> cleaningLore = fixture.setupService.describeCleaningEffect();

        assertTrue(cleaningLore.stream().anyMatch(line -> line.contains("y " + MARKED_LOWEST_Y)
                        && line.contains("y " + MARKED_HIGHEST_Y)),
                "the confirmation names the marked heights: " + cleaningLore);
        assertTrue(cleaningLore.stream().noneMatch(line -> line.contains("y " + WALL_TOP_Y)),
                "and not the top of the border wall: " + cleaningLore);
    }

    @Test
    void leavesTheRunWithoutAnyMarkExactlyAsItWas() {
        Fixture fixture = new Fixture();

        fixture.clickCalculate(WALL_BOTTOM_Y);

        ArenaShape volume = fixture.setupService.getShape().orElseThrow();
        assertEquals(WALL_BOTTOM_Y, volume.getFloorY(), "the bottom of the border wall");
        assertEquals(WALL_TOP_Y, volume.getWallTopY(), "and its top");
        assertFalse(volume.hasMarkedHeights());
        assertTrue(fixture.messages.stream().noneMatch(message -> message.contains("mark")),
                "nothing is reported about marks that were never made: " + fixture.messages);

        fixture.harness.setBlock(0, WALL_BOTTOM_Y, 0, Material.COBWEB);
        fixture.clickClean();

        assertEquals(Material.AIR, fixture.harness.getBlock(0, WALL_BOTTOM_Y, 0));
        assertEquals(Material.STONE, fixture.harness.getBlock(0, FLOOR_Y, 0),
                "the floor below the wall is outside the volume and is not touched");
    }

    @Test
    void refusesToCleanWhenTheStoredBaselineBelongsToAnotherVolume() {
        Fixture fixture = new Fixture();
        fixture.placeMarkerAt(MARKED_LOWEST_Y);
        fixture.placeMarkerAt(MARKED_HIGHEST_Y);
        fixture.clickCalculate(WALL_BOTTOM_Y);
        ArenaShape markedVolume = fixture.setupService.getShape().orElseThrow();

        fixture.arenaRegistry.getArena().setShape(
                markedVolume.withVerticalExtent(WALL_BOTTOM_Y, WALL_TOP_Y));
        fixture.harness.setBlock(0, WALL_BOTTOM_Y, 0, Material.COBWEB);

        String rejection = fixture.setupService.findCleaningRejection().orElseThrow();
        assertTrue(rejection.contains("does not belong to the fight area"), rejection);
        assertTrue(rejection.contains(Arena.describeCalculateAction()),
                "the admin reads the one thing that fixes it: " + rejection);
        assertFalse(fixture.setupService.startCleaning(fakePlayer(fixture.messages, fixture.harness.world(),
                WALL_BOTTOM_Y)));
        assertEquals(Material.COBWEB, fixture.harness.getBlock(0, WALL_BOTTOM_Y, 0),
                "a refused cleaning writes no block at all");
    }

    private static final class Fixture {

        private final FakeServerHarness harness = new FakeServerHarness(WORLD_NAME);
        private final ArenaRegistry arenaRegistry = new ArenaRegistry();
        private final ArenaHeightMarkers markers = new ArenaHeightMarkers();
        private final ActiveArenaState activeArena = new ActiveArenaState(arenaRegistry, markers);
        private final ArenaHeightMarkerService heightMarkerService =
                new ArenaHeightMarkerService(harness.getPlugin(), markers);
        private final ArenaBaselineService baselineService =
                new ArenaBaselineService(harness.getPlugin(), new ArenaBaselineRegistry());
        private final List<String> messages = new ArrayList<>();
        private final List<ShownLayer> shownLayers = new ArrayList<>();
        private final ArenaCleaningService cleaningService = new ArenaCleaningService(harness.getPlugin(),
                (world, shape) -> 0, new RecordingVisualEffect());
        private final ArenaSetupService setupService;

        private Fixture() {
            baselineService.setDepthBelowFloor(0);
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
            setupService = new ArenaSetupService(activeArena,
                    new MatchService(harness.getPlugin(), new KitService(),
                            new SpectatorService(arenaRegistry, new FakeSpectatorItems()),
                            SharedBukkitServer.proxy(FighterColours.class, (method, arguments) -> null),
                            StartGates.NONE),
                    new SpectatorBorderService(borderMarks, () -> { }),
                    new BorderWallPreview(harness.getPlugin(), new SilentVisualEffect()),
                    new ArenaGapGuide(harness.getPlugin()), cleaningService, baselineService,
                    new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape), () -> { });
        }

        private void placeMarkerAt(int blockY) {
            ArenaHeightMarkResult result = heightMarkerService.markHeight(
                    fakePlayer(messages, harness.world(), blockY), blockY);
            assertTrue(result.isTaken(), "the placement has to be taken: " + result.getRejectionMessage());
            harness.runTicks(2);
        }

        private void clickCalculate(int adminY) {
            setupService.calculateShape(fakePlayer(messages, harness.world(), adminY));
            harness.runTicks(40);
            assertTrue(setupService.getShape().isPresent(), "the calculation has to succeed: " + messages);
            assertFalse(baselineService.isCapturing(), "the baseline has to be complete: " + messages);
        }

        private void clickClean() {
            assertTrue(setupService.findCleaningRejection().isEmpty(),
                    "the tile would not even open the confirmation: " + setupService.findCleaningRejection());
            assertTrue(setupService.startCleaning(fakePlayer(messages, harness.world(), WALL_BOTTOM_Y)));
            int tickCount = 0;
            while (cleaningService.isCleaning() && tickCount < MAXIMUM_CLEANING_TICKS) {
                harness.runTick();
                tickCount++;
            }
            assertFalse(cleaningService.isCleaning(), "the cleaning has to finish: " + messages);
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

        private final class RecordingVisualEffect implements ArenaCleaningVisualEffect {

            @Override
            public void showFloodedLayer(World world, ArenaShape shape, ArenaCleaningPhase phase,
                                         int layerY) {
                shownLayers.add(new ShownLayer(phase, layerY));
            }

            @Override
            public void applyParticleSettings(ArenaCleaningParticleSettings settings,
                                              Consumer<String> problemReporter) {
            }
        }
    }

    private record ShownLayer(ArenaCleaningPhase phase, int layerY) {
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
}

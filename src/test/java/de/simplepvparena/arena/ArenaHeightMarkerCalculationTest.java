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

class ArenaHeightMarkerCalculationTest {

    private static final String WORLD_NAME = "event";
    private static final int FLOOR_Y = 63;
    private static final int WALL_BOTTOM_Y = 64;
    private static final int WALL_TOP_Y = 66;
    private static final int MARKED_LOWEST_Y = 40;
    private static final int MARKED_HIGHEST_Y = 90;

    private static final int MAXIMUM_CLEANING_TICKS = 600;

    @Test
    void takesTheTwoMarkedHeightsOverAsTheCleanedVolume() {
        Fixture fixture = new Fixture();
        fixture.markHeights(MARKED_LOWEST_Y, MARKED_HIGHEST_Y);

        fixture.calculateFrom(WALL_BOTTOM_Y);

        ArenaShape shape = fixture.setupService.getShape().orElseThrow();
        assertEquals(MARKED_LOWEST_Y, shape.getFloorY());
        assertEquals(MARKED_HIGHEST_Y, shape.getWallTopY());
        assertTrue(fixture.messages.stream().anyMatch(message -> message.contains("marked heights were taken over")
                        && message.contains("y " + MARKED_LOWEST_Y) && message.contains("y " + MARKED_HIGHEST_Y)),
                "the admin reads which heights the area got: " + fixture.messages);
    }

    @Test
    void leavesTheHorizontalAreaOfTheCalculationUntouched() {
        Fixture withoutMarkers = new Fixture();
        withoutMarkers.calculateFrom(WALL_BOTTOM_Y);
        Fixture withMarkers = new Fixture();
        withMarkers.markHeights(MARKED_LOWEST_Y, MARKED_HIGHEST_Y);
        withMarkers.calculateFrom(WALL_BOTTOM_Y);

        ArenaShape plainShape = withoutMarkers.setupService.getShape().orElseThrow();
        ArenaShape markedShape = withMarkers.setupService.getShape().orElseThrow();

        assertEquals(plainShape.getCellCount(), markedShape.getCellCount());
        assertArrayEqualsAsCells(plainShape, markedShape);
        assertEquals(plainShape.getReferenceY(), markedShape.getReferenceY(),
                "the height the calculation ran at is not a marked one");
        assertEquals(WALL_BOTTOM_Y, plainShape.getFloorY(), "the wall still decides for the area without marks");
        assertEquals(WALL_TOP_Y, plainShape.getWallTopY());
    }

    @Test
    void usesThePairUpSoThatTheNextCalculationCannotBeReshapedByIt() {
        Fixture fixture = new Fixture();
        fixture.markHeights(MARKED_LOWEST_Y, MARKED_HIGHEST_Y);

        fixture.calculateFrom(WALL_BOTTOM_Y);

        assertTrue(fixture.markers.isEmpty(), "the taken over marks are gone");
    }

    @Test
    void keepsTheMarkedVolumeWhenTheSameFightAreaIsCalculatedAgain() {
        Fixture fixture = new Fixture();
        fixture.markHeights(FLOOR_Y - 2, WALL_BOTTOM_Y);
        fixture.harness.setBlock(0, FLOOR_Y - 1, 0, Material.COBWEB);
        fixture.calculateFrom(WALL_BOTTOM_Y);

        fixture.messages.clear();
        fixture.calculateFrom(WALL_BOTTOM_Y);

        ArenaShape shape = fixture.setupService.getShape().orElseThrow();
        assertEquals(FLOOR_Y - 2, shape.getFloorY(), "the second calculation keeps the marked floor");
        assertEquals(WALL_BOTTOM_Y, shape.getWallTopY());
        assertTrue(shape.hasMarkedHeights(), "and it stays a volume an admin described");
        ArenaBaselineSnapshot baseline = fixture.baselineService.findSnapshot(shape).orElseThrow();
        assertEquals(FLOOR_Y - 2, baseline.getShape().getFloorY(),
                "the baseline of the second calculation was read of the marked volume");
        assertEquals(WALL_BOTTOM_Y, baseline.getShape().getWallTopY());
        assertTrue(fixture.messages.stream().anyMatch(message -> message.contains("keeps the")
                        && message.contains("heights that were marked for it")),
                "the admin reads why the area is not on its wall: " + fixture.messages);

        fixture.harness.setBlock(0, FLOOR_Y - 1, 0, Material.STONE);
        fixture.harness.setBlock(0, WALL_TOP_Y, 0, Material.COBWEB);
        fixture.clean();

        assertEquals(Material.COBWEB, fixture.harness.getBlock(0, FLOOR_Y - 1, 0),
                "the cleaning reaches below the wall, where the marked volume is");
        assertEquals(Material.COBWEB, fixture.harness.getBlock(0, WALL_TOP_Y, 0),
                "and it stops at the marked ceiling, although the wall stands above it");
    }

    @Test
    void dropsTheMarkedVolumeAsSoonAsTheCalculationFindsAnotherArea() {
        Fixture fixture = new Fixture();
        fixture.markHeights(MARKED_LOWEST_Y, MARKED_HIGHEST_Y);
        fixture.calculateFrom(WALL_BOTTOM_Y);

        fixture.narrowTheWall();
        fixture.messages.clear();
        fixture.calculateFrom(WALL_BOTTOM_Y);

        ArenaShape shape = fixture.setupService.getShape().orElseThrow();
        assertEquals(WALL_BOTTOM_Y, shape.getFloorY(), "another area is calculated from its own wall");
        assertEquals(WALL_TOP_Y, shape.getWallTopY());
        assertFalse(shape.hasMarkedHeights());
        assertTrue(fixture.messages.stream().anyMatch(message -> message.contains("not the fight area")
                        && message.contains("y " + MARKED_LOWEST_Y) && message.contains("they are gone")),
                "the admin reads that the marked heights did not survive the move: " + fixture.messages);
    }

    @Test
    void leavesACalculationWithoutAnyMarkAlone() {
        Fixture fixture = new Fixture();

        fixture.calculateFrom(WALL_BOTTOM_Y);
        fixture.messages.clear();
        fixture.calculateFrom(WALL_TOP_Y + 6);

        ArenaShape shape = fixture.setupService.getShape().orElseThrow();
        assertEquals(WALL_BOTTOM_Y, shape.getFloorY());
        assertEquals(WALL_TOP_Y, shape.getWallTopY());
        assertFalse(shape.hasMarkedHeights());
        assertTrue(fixture.messages.stream().noneMatch(message -> message.contains("mark")),
                "nothing is reported about marks that were never made: " + fixture.messages);
    }

    @Test
    void keepsTheHeightOfTheWallWhenOnlyOneEndIsMarked() {
        Fixture fixture = new Fixture();
        assertTrue(fixture.markers.mark(MARKED_LOWEST_Y).isTaken());

        fixture.calculateFrom(WALL_BOTTOM_Y);

        ArenaShape shape = fixture.setupService.getShape().orElseThrow();
        assertEquals(WALL_BOTTOM_Y, shape.getFloorY(), "half a pair never moves the floor");
        assertEquals(WALL_TOP_Y, shape.getWallTopY());
        assertTrue(fixture.messages.stream().anyMatch(message -> message.contains("Only the lowest point")
                        && message.contains("keeps the height of the border wall")),
                "the admin reads that the single mark was not used: " + fixture.messages);
        assertEquals(MARKED_LOWEST_Y, fixture.markers.getLowestY().orElseThrow(),
                "the single mark waits for the second one instead of being thrown away");
    }

    @Test
    void cleansTheMarkedVolumeBelowTheBorderWall() {
        Fixture fixture = new Fixture();
        fixture.markHeights(FLOOR_Y - 2, WALL_BOTTOM_Y);
        fixture.harness.setBlock(0, FLOOR_Y - 1, 0, Material.COBWEB);
        fixture.calculateFrom(WALL_BOTTOM_Y);

        fixture.harness.setBlock(0, FLOOR_Y - 1, 0, Material.STONE);
        fixture.harness.setBlock(0, WALL_TOP_Y, 0, Material.COBWEB);
        fixture.clean();

        assertEquals(Material.COBWEB, fixture.harness.getBlock(0, FLOOR_Y - 1, 0),
                "the baseline of the marked volume reaches below the wall and is restored");
        assertEquals(Material.COBWEB, fixture.harness.getBlock(0, WALL_TOP_Y, 0),
                "above the marked ceiling nothing is cleaned, although the wall stands there");
    }

    private static void assertArrayEqualsAsCells(ArenaShape expectedShape, ArenaShape actualShape) {
        long[] expectedCells = expectedShape.getCells();
        long[] actualCells = actualShape.getCells();
        assertEquals(expectedCells.length, actualCells.length);
        for (int cellIndex = 0; cellIndex < expectedCells.length; cellIndex++) {
            assertEquals(expectedCells[cellIndex], actualCells[cellIndex],
                    "cell " + cellIndex + " of the calculated area");
        }
    }

    private static final class Fixture {

        private final FakeServerHarness harness = new FakeServerHarness(WORLD_NAME);
        private final ArenaRegistry arenaRegistry = new ArenaRegistry();
        private final ArenaBaselineService baselineService =
                new ArenaBaselineService(harness.getPlugin(), new ArenaBaselineRegistry());
        private final ArenaCleaningService cleaningService =
                new ArenaCleaningService(harness.getPlugin(), (world, shape) -> 0);
        private final ArenaHeightMarkers markers = new ArenaHeightMarkers();
        private final List<String> messages = new ArrayList<>();
        private final BorderMarkIndex borderMarks = new BorderMarkIndex();
        private final ArenaSetupService setupService;

        private Fixture() {
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
            setupService = new ArenaSetupService(new ActiveArenaState(arenaRegistry, markers),
                    new MatchService(harness.getPlugin(), new KitService(),
                            new SpectatorService(arenaRegistry, new FakeSpectatorItems()),
                            SharedBukkitServer.proxy(FighterColours.class, (method, arguments) -> null),
                            StartGates.NONE),
                    new SpectatorBorderService(borderMarks, () -> { }),
                    new BorderWallPreview(harness.getPlugin(), new SilentVisualEffect()),
                    new ArenaGapGuide(harness.getPlugin()), cleaningService, baselineService,
                    new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape), () -> { });
        }

        private void markHeights(int lowestY, int highestY) {
            assertTrue(markers.mark(lowestY).isTaken(), "the floor mark has to be taken");
            assertTrue(markers.mark(highestY).isTaken(), "the ceiling mark has to be taken");
            assertTrue(markers.isComplete(), "two placements have to leave a complete pair");
        }

        private void narrowTheWall() {
            for (int blockZ = -1; blockZ <= 1; blockZ++) {
                for (int blockY = WALL_BOTTOM_Y; blockY <= WALL_TOP_Y; blockY++) {
                    borderMarks.mark(WORLD_NAME, 1, blockY, blockZ);
                }
            }
        }

        private void calculateFrom(int blockY) {
            setupService.calculateShape(fakePlayer(messages, harness.world(), blockY));
            harness.runTicks(40);
            assertTrue(setupService.getShape().isPresent(), "the calculation has to succeed: " + messages);
            assertFalse(baselineService.isCapturing(), "the baseline has to be complete: " + messages);
        }

        private void clean() {
            assertTrue(setupService.startCleaning(fakePlayer(messages, harness.world(), WALL_BOTTOM_Y)));
            int tickCount = 0;
            while (cleaningService.isCleaning() && tickCount < MAXIMUM_CLEANING_TICKS) {
                harness.runTick();
                tickCount++;
            }
            assertFalse(cleaningService.isCleaning(), "the cleaning has to finish: " + messages);
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

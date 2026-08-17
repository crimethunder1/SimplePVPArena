package de.simplepvparena.arena;

import de.simplepvparena.gate.StartGateMarks;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.capture;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.position;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.shape;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleaningGateExclusionTest {

    private static final String WORLD_NAME = "event";
    private static final String OTHER_WORLD_NAME = "lobby";

    private static final int FLOOR_Y = 64;
    private static final int GATE_Y = 65;

    private static final int GATE_X = 1;
    private static final int ARENA_X = 0;
    private static final int BLOCK_Z = 0;

    private static final int MAXIMUM_CLEANING_TICKS = 200;

    @Test
    void leavesAGateBlockThatTheCountdownAlreadyTookAwayAsItIs() {
        Fixture fixture = new Fixture();
        fixture.buildArenaWithGate();
        fixture.markGateBlock(WORLD_NAME);

        fixture.harness.setBlock(GATE_X, GATE_Y, BLOCK_Z, Material.AIR);
        fixture.harness.setBlock(ARENA_X, FLOOR_Y, BLOCK_Z, Material.AIR);

        ArenaCleaningReport report = fixture.clean();

        assertEquals(Material.AIR, fixture.harness.getBlock(GATE_X, GATE_Y, BLOCK_Z),
                "the gate position is skipped although the baseline holds the gate material there");
        assertEquals(Material.STONE, fixture.harness.getBlock(ARENA_X, FLOOR_Y, BLOCK_Z),
                "the block next to it is restored exactly as it always was");
        assertEquals(1L, report.changedBlockCount(), "only the block that was not skipped was written");
        assertTrue(report.isSuccessful(), report.describeOutcome());
    }

    @Test
    void writesThatVeryBlockWhenNoGateIsMarkedThere() {
        Fixture fixture = new Fixture();
        fixture.buildArenaWithGate();

        fixture.harness.setBlock(GATE_X, GATE_Y, BLOCK_Z, Material.AIR);

        ArenaCleaningReport report = fixture.clean();

        assertEquals(Material.GLASS, fixture.harness.getBlock(GATE_X, GATE_Y, BLOCK_Z),
                "without a mark it is an ordinary block of the fight area");
        assertEquals(1L, report.changedBlockCount());
    }

    @Test
    void leavesAStandingGateAloneWhileTheAreaAroundItIsEmptied() {
        Fixture fixture = new Fixture();
        fixture.buildArenaWithGate();
        fixture.markGateBlock(WORLD_NAME);

        ArenaCleaningReport report = fixture.cleanWithoutBaseline();

        assertEquals(Material.GLASS, fixture.harness.getBlock(GATE_X, GATE_Y, BLOCK_Z),
                "the standing gate is not torn open by a cleaning");
        assertEquals(Material.AIR, fixture.harness.getBlock(ARENA_X, FLOOR_Y, BLOCK_Z));
        assertEquals(Material.AIR, fixture.harness.getBlock(GATE_X, FLOOR_Y, BLOCK_Z));
        assertEquals(2L, report.changedBlockCount(), "the two floor blocks, and nothing of the gate");
        assertEquals(4L, report.checkedBlockCount(), "the whole volume was walked over all the same");
    }

    @Test
    void protectsNothingFromAGatePlacedInAnotherWorld() {
        Fixture fixture = new Fixture();
        fixture.buildArenaWithGate();
        fixture.markGateBlock(OTHER_WORLD_NAME);

        fixture.harness.setBlock(GATE_X, GATE_Y, BLOCK_Z, Material.AIR);

        fixture.clean();

        assertEquals(Material.GLASS, fixture.harness.getBlock(GATE_X, GATE_Y, BLOCK_Z));
    }

    private static final class Fixture {

        private final FakeServerHarness harness = new FakeServerHarness(WORLD_NAME);
        private final StartGateMarks gateMarks = new StartGateMarks();
        private final Map<Long, Material> capturedWorld = new HashMap<>();
        private final ArenaShape volume = shape(FLOOR_Y, GATE_Y, ARENA_X, BLOCK_Z, GATE_X, BLOCK_Z);
        private final ArenaCleaningService cleaningService = new ArenaCleaningService(harness.getPlugin(),
                (world, shape) -> 0, ArenaCleaningVisualEffect.NONE, gateMarks::isMarked);

        private Fixture() {
            cleaningService.setAnimationDurationTicks(0L);
        }

        private void buildArenaWithGate() {
            placeBlock(ARENA_X, FLOOR_Y, BLOCK_Z, Material.STONE);
            placeBlock(GATE_X, FLOOR_Y, BLOCK_Z, Material.STONE);
            placeBlock(GATE_X, GATE_Y, BLOCK_Z, Material.GLASS);
        }

        private void markGateBlock(String worldName) {
            assertTrue(gateMarks.mark(worldName, GATE_X, GATE_Y, BLOCK_Z, Material.GLASS));
        }

        private void placeBlock(int blockX, int blockY, int blockZ, Material material) {
            harness.setBlock(blockX, blockY, blockZ, material);
            capturedWorld.put(position(blockX, blockY, blockZ), material);
        }

        private ArenaCleaningReport clean() {
            return run(ArenaCleaningPlan.of(volume, Optional.of(capture(volume, capturedWorld, 4))));
        }

        private ArenaCleaningReport cleanWithoutBaseline() {
            return run(ArenaCleaningPlan.of(volume, Optional.empty()));
        }

        private ArenaCleaningReport run(ArenaCleaningPlan plan) {
            AtomicReference<ArenaCleaningReport> report = new AtomicReference<>();
            assertTrue(cleaningService.startCleaning(plan, report::set));
            int tickCount = 0;
            while (cleaningService.isCleaning() && tickCount < MAXIMUM_CLEANING_TICKS) {
                harness.runTick();
                tickCount++;
            }
            assertFalse(cleaningService.isCleaning(), "the cleaning has to finish");
            return report.get();
        }
    }
}

package de.simplepvparena.gate;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGateItemServiceTest {

    private static final String WORLD = "event";

    private final GateServerHarness harness = new GateServerHarness(WORLD);
    private final StartGateMarks gateMarks = new StartGateMarks();
    private final AtomicInteger markChangeCount = new AtomicInteger();
    private final StartGateItemService itemService = new StartGateItemService(harness.getPlugin(),
            gateMarks, new StartGateDuration(), markChangeCount::incrementAndGet);

    @Test
    void makesAPlacedBlockPartOfTheGate() {
        harness.setBlock(10, 65, 20, Material.BARRIER);

        assertTrue(itemService.markPlacedBlock(harness.blockAt(10, 65, 20)));

        assertTrue(itemService.isGateBlock(harness.blockAt(10, 65, 20)));
        assertEquals(Optional.of(Material.BARRIER), gateMarks.findMaterial(WORLD, 10, 65, 20));
        assertEquals(1, itemService.getMarkedBlockCount());
        assertEquals(1, markChangeCount.get(), "the mark is written to startgates.yml right away");
    }

    @Test
    void writesDownWhateverMaterialWasPlaced() {
        harness.setBlock(10, 65, 20, Material.BARRIER);
        harness.setBlock(40, 65, 20, Material.IRON_BARS);

        itemService.markPlacedBlock(harness.blockAt(10, 65, 20));
        itemService.markPlacedBlock(harness.blockAt(40, 65, 20));

        assertEquals(Optional.of(Material.BARRIER), gateMarks.findMaterial(WORLD, 10, 65, 20));
        assertEquals(Optional.of(Material.IRON_BARS), gateMarks.findMaterial(WORLD, 40, 65, 20));
        assertEquals(2, itemService.getMarkedBlockCount());
    }

    @Test
    void takesABrokenBlockOutOfTheGateAgain() {
        harness.setBlock(10, 65, 20, Material.BARRIER);
        itemService.markPlacedBlock(harness.blockAt(10, 65, 20));

        assertTrue(itemService.clearBlockMark(harness.blockAt(10, 65, 20)));

        assertFalse(itemService.isGateBlock(harness.blockAt(10, 65, 20)));
        assertEquals(0, itemService.getMarkedBlockCount());
        assertEquals(2, markChangeCount.get(), "the file is written for the mark and for its removal");
    }

    @Test
    void ignoresTheBreakOfABlockThatIsNoPartOfTheGate() {
        harness.setBlock(10, 65, 20, Material.STONE);

        assertFalse(itemService.clearBlockMark(harness.blockAt(10, 65, 20)));

        assertEquals(0, markChangeCount.get());
    }

    @Test
    void keepsTheFirstMaterialWhenTheSameBlockIsPlacedAgain() {
        harness.setBlock(10, 65, 20, Material.BARRIER);
        itemService.markPlacedBlock(harness.blockAt(10, 65, 20));
        harness.setBlock(10, 65, 20, Material.GLASS);

        assertFalse(itemService.markPlacedBlock(harness.blockAt(10, 65, 20)));

        assertEquals(Optional.of(Material.BARRIER), gateMarks.findMaterial(WORLD, 10, 65, 20));
        assertEquals(1, markChangeCount.get());
    }

    @Test
    void refusesToMarkAir() {
        assertFalse(itemService.markPlacedBlock(harness.blockAt(10, 65, 20)));

        assertEquals(0, itemService.getMarkedBlockCount());
        assertEquals(0, markChangeCount.get());
    }

    @Test
    void describesTheGateWithItsBlockCountAndItsSeconds() {
        StartGateDuration duration = new StartGateDuration();
        StartGateItemService describingService = new StartGateItemService(harness.getPlugin(), gateMarks,
                duration, () -> { });

        assertEquals("The gate holds no block yet", describingService.describeGate().get(0));
        assertEquals("It comes down over 10 seconds", describingService.describeGate().get(1));

        harness.setBlock(10, 65, 20, Material.BARRIER);
        describingService.markPlacedBlock(harness.blockAt(10, 65, 20));
        duration.setSeconds(1);

        assertEquals("The gate holds 1 block", describingService.describeGate().get(0));
        assertEquals("It comes down over 1 second", describingService.describeGate().get(1));
    }
}

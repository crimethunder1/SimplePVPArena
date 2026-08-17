package de.simplepvparena.gate;

import de.simplepvparena.arena.BlockPositionCodec;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGateMarksTest {

    private static final String WORLD = "event";
    private static final String OTHER_WORLD = "event_backup";

    private final StartGateMarks gateMarks = new StartGateMarks();

    @Test
    void remembersTheMaterialABlockHeldWhenItWasMarked() {
        assertTrue(gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER), "the block was not marked before");

        assertTrue(gateMarks.isMarked(WORLD, 10, 65, 20));
        assertEquals(Optional.of(Material.BARRIER), gateMarks.findMaterial(WORLD, 10, 65, 20));
        assertEquals(1, gateMarks.getMarkedBlockCount());
        assertFalse(gateMarks.isEmpty());
    }

    @Test
    void keepsTheFirstMaterialWhenTheSameBlockIsMarkedAgain() {
        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);

        assertFalse(gateMarks.mark(WORLD, 10, 65, 20, Material.AIR), "the block was marked before");

        assertEquals(Optional.of(Material.BARRIER), gateMarks.findMaterial(WORLD, 10, 65, 20));
        assertEquals(1, gateMarks.getMarkedBlockCount());
    }

    @Test
    void takesAMarkAwayAgainAndAnswersWhetherThereWasOne() {
        gateMarks.mark(WORLD, 1, 64, 1, Material.GLASS);

        assertTrue(gateMarks.clearMark(WORLD, 1, 64, 1), "the block really was marked");
        assertFalse(gateMarks.clearMark(WORLD, 1, 64, 1), "and is not any more");

        assertFalse(gateMarks.isMarked(WORLD, 1, 64, 1));
        assertEquals(Optional.empty(), gateMarks.findMaterial(WORLD, 1, 64, 1));
        assertTrue(gateMarks.isEmpty());
        assertTrue(gateMarks.getWorldNames().isEmpty(), "a world without a mark is dropped with the last one");
    }

    @Test
    void holdsTheGateOfBothSpawnsInOneRegistry() {
        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 40, 65, 20, Material.GLASS);

        assertTrue(gateMarks.isMarked(WORLD, 10, 65, 20));
        assertTrue(gateMarks.isMarked(WORLD, 40, 65, 20));
        assertEquals(2, gateMarks.getMarkedBlockCount());
        assertEquals(Optional.of(Material.BARRIER), gateMarks.findMaterial(WORLD, 10, 65, 20));
        assertEquals(Optional.of(Material.GLASS), gateMarks.findMaterial(WORLD, 40, 65, 20),
                "every block keeps its own material, one registry is not one material");
    }

    @Test
    void keepsTheWorldsApart() {
        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);
        gateMarks.mark(OTHER_WORLD, 10, 65, 20, Material.GLASS);

        assertFalse(gateMarks.isMarked(OTHER_WORLD, 10, 65, 21));
        assertEquals(Optional.of(Material.BARRIER), gateMarks.findMaterial(WORLD, 10, 65, 20));
        assertEquals(Optional.of(Material.GLASS), gateMarks.findMaterial(OTHER_WORLD, 10, 65, 20));
        assertEquals(List.of(WORLD, OTHER_WORLD), new ArrayList<>(gateMarks.getWorldNames()));
        assertEquals(1, gateMarks.getMarkedBlockCount(WORLD));
        assertEquals(2, gateMarks.getMarkedBlockCount());
    }

    @Test
    void handsOutTheLayersOfTheGateFromItsTopDown() {
        gateMarks.mark(WORLD, 10, 64, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 11, 66, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 11, 65, 20, Material.BARRIER);

        long[][] layers = gateMarks.collectLayersTopDown(WORLD);

        assertEquals(3, layers.length);
        assertEquals(3, gateMarks.getLayerCount(WORLD));
        assertEquals(List.of(66), heightsOf(layers[0]));
        assertEquals(List.of(65, 65), heightsOf(layers[1]));
        assertEquals(List.of(64), heightsOf(layers[2]));
    }

    @Test
    void cutsTheLayersAcrossBothSpawnsAtOnce() {
        gateMarks.mark(WORLD, 10, 64, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 40, 64, 20, Material.GLASS);
        gateMarks.mark(WORLD, 40, 65, 20, Material.GLASS);

        long[][] layers = gateMarks.collectLayersTopDown(WORLD);

        assertEquals(2, layers.length, "two heights, not two boxes");
        assertEquals(List.of(65, 65), heightsOf(layers[0]));
        assertEquals(List.of(10, 40), xCoordinatesOf(layers[0]), "both spawns lose their top together");
        assertEquals(List.of(10, 40), xCoordinatesOf(layers[1]));
    }

    @Test
    void countsOnlyTheHeightsThatWereReallyMarked() {
        gateMarks.mark(WORLD, 10, 64, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 10, 70, 20, Material.BARRIER);

        long[][] layers = gateMarks.collectLayersTopDown(WORLD);

        assertEquals(2, layers.length);
        assertEquals(List.of(70), heightsOf(layers[0]));
        assertEquals(List.of(64), heightsOf(layers[1]));
        assertEquals(0, gateMarks.getLayerCount(OTHER_WORLD), "a world without marks has no layer");
    }

    @Test
    void handsOutEveryMarkOfAWorldInAStableOrder() {
        gateMarks.mark(WORLD, 11, 65, 20, Material.GLASS);
        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);

        Map<Long, Material> firstRead = gateMarks.getMarks(WORLD);
        Map<Long, Material> secondRead = gateMarks.getMarks(WORLD);

        assertEquals(2, firstRead.size());
        assertEquals(List.copyOf(firstRead.keySet()), List.copyOf(secondRead.keySet()));
        assertEquals(Material.BARRIER, firstRead.get(BlockPositionCodec.encode(10, 65, 20)));
        assertEquals(Material.GLASS, firstRead.get(BlockPositionCodec.encode(11, 65, 20)));
        assertTrue(gateMarks.getMarks(OTHER_WORLD).isEmpty());
    }

    @Test
    void replacesTheMarksOfAWorldAndClearsTheWholeGate() {
        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 40, 65, 20, Material.GLASS);

        gateMarks.replaceWorld(WORLD, Map.of(BlockPositionCodec.encode(1, 2, 3), Material.IRON_BARS));

        assertFalse(gateMarks.isMarked(WORLD, 10, 65, 20));
        assertEquals(Optional.of(Material.IRON_BARS), gateMarks.findMaterial(WORLD, 1, 2, 3));

        gateMarks.replaceWorld(WORLD, Map.of());
        assertTrue(gateMarks.isEmpty(), "an empty world is dropped");

        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);
        gateMarks.clear();
        assertEquals(0, gateMarks.getMarkedBlockCount());
    }

    private static List<Integer> heightsOf(long[] layer) {
        List<Integer> heights = new ArrayList<>(layer.length);
        for (long encodedPosition : layer) {
            heights.add(BlockPositionCodec.decodeY(encodedPosition));
        }
        return heights;
    }

    private static List<Integer> xCoordinatesOf(long[] layer) {
        List<Integer> xCoordinates = new ArrayList<>(layer.length);
        for (long encodedPosition : layer) {
            xCoordinates.add(BlockPositionCodec.decodeX(encodedPosition));
        }
        return xCoordinates;
    }
}

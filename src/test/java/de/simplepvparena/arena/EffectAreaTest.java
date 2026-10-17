package de.simplepvparena.arena;

import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectAreaTest {

    private static final String WORLD_NAME = "event";
    private static final String OTHER_WORLD_NAME = "lobby";
    private static final int GROUND_Y = 64;
    private static final double PRECISION = 0.000001D;

    @Test
    void placesTheCentreInTheMiddleOfTheFrameAndReachesTheCornerPlusTheMargin() {
        EffectArea area = EffectArea.around(rectangle(0, 5, 0, 7), 10);

        assertEquals(new EffectArea.Center(3.0D, 4.0D), area.getCenter(),
                "the cells 0 to 5 and 0 to 7 cover the blocks from 0 to 6 and from 0 to 8");
        assertEquals(15.0D, area.getRadius(), PRECISION, "five blocks to each corner and ten blocks of margin");
        assertEquals(WORLD_NAME, area.getWorldName());
    }

    @Test
    void letsTheCornerOfTheFrameDecideTheRadiusOfAnLShapedFightArea() {
        EffectArea area = EffectArea.around(lShape(), 0);

        assertEquals(new EffectArea.Center(5.0D, 5.0D), area.getCenter());
        assertEquals(Math.sqrt(50.0D), area.getRadius(), PRECISION);
        assertTrue(area.containsBlock(WORLD_NAME, 9, 9), "the empty corner of the frame lies inside");
        assertTrue(area.contains(WORLD_NAME, 10.0D, 10.0D), "up to the outer edge of that corner");
        assertFalse(area.contains(WORLD_NAME, 10.1D, 10.1D), "and not a step beyond it");
    }

    @Test
    void endsExactlyAtTheRadius() {
        EffectArea area = EffectArea.around(rectangle(0, 5, 0, 7), 10);

        assertTrue(area.contains(WORLD_NAME, 18.0D, 4.0D), "on the edge of the circle");
        assertFalse(area.contains(WORLD_NAME, 18.1D, 4.0D), "just beyond the edge");
        assertTrue(area.containsBlock(WORLD_NAME, 17, 3), "the last block inside the circle");
        assertFalse(area.containsBlock(WORLD_NAME, 18, 3), "the first block outside of it");
        assertTrue(area.containsBlock(WORLD_NAME, -12, 3), "on the other side as well");
        assertFalse(area.containsBlock(WORLD_NAME, -13, 3));
    }

    @Test
    void coversTheFightAreaItself() {
        ArenaShape fightArea = lShape();
        EffectArea area = EffectArea.around(fightArea, 0);

        for (long cell : fightArea.getCells()) {
            assertTrue(area.containsBlock(WORLD_NAME, HorizontalCellCodec.decodeX(cell),
                    HorizontalCellCodec.decodeZ(cell)), "every cell of the fight area");
        }
    }

    @Test
    void holdsOnlyInTheWorldOfTheArena() {
        EffectArea area = EffectArea.around(rectangle(0, 5, 0, 7), 10);

        assertFalse(area.contains(OTHER_WORLD_NAME, 3.0D, 4.0D));
        assertFalse(area.containsBlock(OTHER_WORLD_NAME, 3, 4));
        assertFalse(area.contains(new Location(world(OTHER_WORLD_NAME), 3.0D, GROUND_Y, 4.0D)));
        assertFalse(area.contains((Location) null));
        assertFalse(area.contains(new Location(null, 3.0D, GROUND_Y, 4.0D)));
    }

    @Test
    void holdsAtEveryHeight() {
        EffectArea area = EffectArea.around(rectangle(0, 5, 0, 7), 10);
        World world = world(WORLD_NAME);

        assertTrue(area.contains(new Location(world, 17.5D, -64.0D, 4.0D)), "deep under the arena");
        assertTrue(area.contains(new Location(world, 17.5D, GROUND_Y, 4.0D)));
        assertTrue(area.contains(new Location(world, 17.5D, 319.0D, 4.0D)), "and high above it");
        assertFalse(area.contains(new Location(world, 18.5D, 319.0D, 4.0D)));
    }

    @Test
    void describesTheCentreAndTheRadius() {
        assertEquals("centre 3/4, radius 15 blocks", EffectArea.around(rectangle(0, 5, 0, 7), 10).describe());
        assertEquals("centre 5/5, radius 7.1 blocks", EffectArea.around(lShape(), 0).describe());
        assertEquals("centre 3.5/3.5, radius 99.5 blocks", EffectArea.around(rectangle(1, 5, 1, 5), 96).describe());
    }

    @Test
    void describesTheDistanceOfAPosition() {
        EffectArea area = EffectArea.around(rectangle(0, 5, 0, 7), 10);

        assertEquals("27.5 blocks from its centre 3/4, radius 15 blocks",
                area.describeDistanceOf(WORLD_NAME, 30.5D, 4.0D));
        assertEquals("in the world lobby, while the area lies in event with its centre 3/4, radius 15 blocks",
                area.describeDistanceOf(OTHER_WORLD_NAME, 3.0D, 4.0D));
        assertEquals(27.5D, area.distanceToCenter(30.5D, 4.0D), PRECISION);
    }

    @Test
    void writesTheNumbersWithAPointInEveryLanguage() {
        Locale previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            assertEquals("centre 3.5/3.5, radius 99.5 blocks",
                    EffectArea.around(rectangle(1, 5, 1, 5), 96).describe());
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    static ArenaShape rectangle(int minimumX, int maximumX, int minimumZ, int maximumZ) {
        List<Long> cells = new ArrayList<>();
        for (int x = minimumX; x <= maximumX; x++) {
            for (int z = minimumZ; z <= maximumZ; z++) {
                cells.add(HorizontalCellCodec.encode(x, z));
            }
        }
        return shapeOf(cells);
    }

    private static ArenaShape lShape() {
        List<Long> cells = new ArrayList<>();
        for (int x = 0; x <= 9; x++) {
            for (int z = 0; z <= 1; z++) {
                cells.add(HorizontalCellCodec.encode(x, z));
            }
        }
        for (int x = 0; x <= 1; x++) {
            for (int z = 2; z <= 9; z++) {
                cells.add(HorizontalCellCodec.encode(x, z));
            }
        }
        return shapeOf(cells);
    }

    private static ArenaShape shapeOf(List<Long> cells) {
        return new ArenaShape(WORLD_NAME, GROUND_Y, GROUND_Y + 3,
                cells.stream().mapToLong(Long::longValue).toArray());
    }

    private static World world(String worldName) {
        return SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> worldName;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
    }
}

package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaSafetyZoneTest {

    private static final String WORLD_NAME = "event";
    private static final UUID SPECTATOR = UUID.randomUUID();
    private static final UUID FIGHTER = UUID.randomUUID();

    private final Set<UUID> activeFighters = new HashSet<>(Set.of(FIGHTER));
    private ArenaShape configuredShape = square(0, 0, 5);
    private final ArenaSafetyZone safetyZone = new ArenaSafetyZone(
            () -> Optional.ofNullable(configuredShape), activeFighters::contains);

    @Test
    void protectsASpectatorInsideTheRadius() {
        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, -0.5D, 2.5D), "right next to the fight area");
        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, 30.5D, 2.5D), "30 blocks away from it");
    }

    @Test
    void leavesThePlayersInsideTheFightAreaAlone() {
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 2.5D, 2.5D), "the middle of the fight area");
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 0.1D, 0.1D), "a cell of its outline");
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 4.9D, 4.9D), "the far corner of it");
    }

    @Test
    void neverProtectsAnActiveFighter() {
        assertFalse(safetyZone.protects(FIGHTER, WORLD_NAME, -0.5D, 2.5D), "next to the fight area");
        assertFalse(safetyZone.protects(FIGHTER, WORLD_NAME, 30.5D, 2.5D), "in the middle of the zone");
        assertFalse(safetyZone.protects(FIGHTER, WORLD_NAME, 2.5D, 2.5D), "inside the fight area");
        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, 30.5D, 2.5D),
                "and the very same position protects everybody who is not fighting");
    }

    @Test
    void protectsAFighterOfAFinishedRoundAgain() {
        assertFalse(safetyZone.protects(FIGHTER, WORLD_NAME, 30.5D, 2.5D));

        activeFighters.clear();

        assertTrue(safetyZone.protects(FIGHTER, WORLD_NAME, 30.5D, 2.5D));
    }

    @Test
    void endsAtTheConfiguredRadius() {
        assertEquals(ArenaSafetyZone.DEFAULT_RADIUS, safetyZone.getRadius(), "64 blocks are the shipped default");

        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, 67.5D, 2.5D), "63 blocks from the outline");
        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, 68.5D, 2.5D), "exactly 64 blocks from the outline");
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 69.5D, 2.5D), "65 blocks from the outline");
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 500.5D, 2.5D), "far out in the world");
    }

    @Test
    void followsAChangedRadius() {
        safetyZone.setRadius(10);

        assertEquals(10, safetyZone.getRadius());
        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, 14.5D, 2.5D), "exactly 10 blocks from the outline");
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 15.5D, 2.5D), "11 blocks from the outline");
    }

    @Test
    void protectsNobodyWithoutARadius() {
        safetyZone.setRadius(0);

        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, -0.5D, 2.5D), "not even right at the outline");

        safetyZone.setRadius(-5);

        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, -0.5D, 2.5D));
    }

    @Test
    void protectsNobodyWithoutACalculatedFightArea() {
        configuredShape = null;

        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 2.5D, 2.5D));
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 30.5D, 2.5D));
    }

    @Test
    void protectsNobodyInAnotherWorld() {
        assertFalse(safetyZone.protects(SPECTATOR, "lobby", 30.5D, 2.5D));
        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, 30.5D, 2.5D));
    }

    @Test
    void followsANewlyCalculatedFightArea() {
        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, 30.5D, 2.5D));

        configuredShape = square(1000, 1000, 5);

        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 30.5D, 2.5D), "the old zone is gone");
        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, 1030.5D, 1002.5D), "the new one holds");
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 1002.5D, 1002.5D), "inside the new fight area");
    }

    @Test
    void measuresTheDistanceToTheOutlineAndNotToTheBoundingBox() {
        configuredShape = shape(0, 100, 1, 100, 2, 100, 100, 0, 100, 1, 100, 2);

        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, 1.5D, 40.5D), "60 blocks below the first arm");
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, 50.5D, 50.5D),
                "the middle of the bounding box is far from both arms");
    }

    @Test
    void reportsTheDistanceToTheOutline() {
        assertEquals(Optional.of(0.0D), safetyZone.findDistanceToContour(WORLD_NAME, 0.5D, 0.5D));
        assertEquals(Optional.of(10.0D), safetyZone.findDistanceToContour(WORLD_NAME, 14.5D, 2.5D));
        assertEquals(Optional.empty(), safetyZone.findDistanceToContour("lobby", 14.5D, 2.5D));

        configuredShape = null;

        assertEquals(Optional.empty(), safetyZone.findDistanceToContour(WORLD_NAME, 14.5D, 2.5D));
    }

    @Test
    void readsThePositionAsTheBlockColumnItStandsIn() {
        configuredShape = square(-4, -4, 5);

        assertTrue(safetyZone.protects(SPECTATOR, WORLD_NAME, -4.9D, -0.5D), "the column -5 is next to the outline");
        assertFalse(safetyZone.protects(SPECTATOR, WORLD_NAME, -0.5D, -0.5D), "the column -1 is inside the fight area");
    }

    private static ArenaShape square(int originX, int originZ, int edgeLength) {
        List<Integer> coordinates = new ArrayList<>();
        for (int columnX = originX; columnX < originX + edgeLength; columnX++) {
            for (int columnZ = originZ; columnZ < originZ + edgeLength; columnZ++) {
                coordinates.add(columnX);
                coordinates.add(columnZ);
            }
        }
        int[] values = new int[coordinates.size()];
        for (int index = 0; index < values.length; index++) {
            values[index] = coordinates.get(index);
        }
        return shape(values);
    }

    private static ArenaShape shape(int... coordinatePairs) {
        long[] cells = new long[coordinatePairs.length / 2];
        for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
            cells[cellIndex] = HorizontalCellCodec.encode(coordinatePairs[cellIndex * 2],
                    coordinatePairs[cellIndex * 2 + 1]);
        }
        return new ArenaShape(WORLD_NAME, 64, 70, cells);
    }
}

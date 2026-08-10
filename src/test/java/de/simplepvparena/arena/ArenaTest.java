package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaTest {

    @Test
    void needsACalculatedFightAreaAndATribuneSpawn() {
        Arena arena = new Arena();

        assertFalse(arena.isFullyConfigured());
        assertEquals(2, arena.getMissingConfiguration().size());

        arena.setShape(shape());
        assertFalse(arena.isFullyConfigured());
        assertEquals(List.of("the tribune spawn (" + Arena.TRIBUNE_SPAWN_ACTION + ")"),
                arena.getMissingConfiguration());

        arena.setTribuneSpawn(position(50.0D, 50.0D));
        assertTrue(arena.isFullyConfigured());
        assertTrue(arena.getMissingConfiguration().isEmpty());
    }

    @Test
    void fallsBackToTheMiddleOfTheFightAreaForTeamSpawns() {
        Arena arena = new Arena();
        arena.setShape(shape());

        PositionSnapshot firstTeamSpawn = arena.getFirstTeamSpawn().orElseThrow();

        assertFalse(arena.contains(null));
        assertTrue(arena.getShape().orElseThrow()
                .contains("world", firstTeamSpawn.x(), firstTeamSpawn.z()));
        assertEquals(firstTeamSpawn, arena.getSecondTeamSpawn().orElseThrow());
        assertEquals(firstTeamSpawn, arena.getSpectatorFlightSpawn().orElseThrow());
    }

    @Test
    void warnsAboutSpawnsThatDoNotFitTheCalculatedFightArea() {
        Arena arena = new Arena();
        arena.setShape(shape());
        arena.setFirstTeamSpawn(position(1.5D, 1.5D));
        arena.setSecondTeamSpawn(position(50.5D, 50.5D));
        arena.setTribuneSpawn(position(2.5D, 2.5D));

        List<String> warnings = arena.findSpawnPlacementWarnings();

        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("The second team spawn"));
        assertTrue(warnings.get(0).contains("outside"));
        assertTrue(warnings.get(1).contains("The tribune spawn"));
        assertTrue(warnings.get(1).contains("inside"));
    }

    @Test
    void staysSilentAboutSpawnsAsLongAsNoFightAreaIsCalculated() {
        Arena arena = new Arena();
        arena.setFirstTeamSpawn(position(1.5D, 1.5D));

        assertTrue(arena.findSpawnPlacementWarnings().isEmpty());
    }

    private PositionSnapshot position(double x, double z) {
        return new PositionSnapshot("world", x, 64.0D, z, 0.0F, 0.0F);
    }

    private ArenaShape shape() {
        return new ArenaShape("world", 64, 70, new long[] {
                HorizontalCellCodec.encode(1, 1),
                HorizontalCellCodec.encode(2, 1),
                HorizontalCellCodec.encode(1, 2),
                HorizontalCellCodec.encode(2, 2)});
    }
}

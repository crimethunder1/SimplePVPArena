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
        assertEquals(List.of("the tribune spawn (" + Arena.describeTribuneSpawnAction() + ")"),
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
    void refusesATeamSpawnOutsideOfTheCalculatedFightArea() {
        Arena arena = new Arena();
        arena.setShape(shape());

        for (ArenaSpawnType spawnType : List.of(ArenaSpawnType.FIRST_TEAM, ArenaSpawnType.SECOND_TEAM)) {
            String rejection = arena.setSpawn(spawnType, position(50.5D, 50.5D)).orElseThrow();

            assertTrue(rejection.contains("The " + spawnType.getDisplayName() + " must lie inside"),
                    rejection);
            assertTrue(rejection.contains("lies outside of it"), rejection);
            assertTrue(rejection.contains(spawnType.getPlacementHint()), rejection);
            assertTrue(arena.getConfiguredSpawn(spawnType).isEmpty());
        }
    }

    @Test
    void acceptsATeamSpawnInsideOfTheCalculatedFightArea() {
        Arena arena = new Arena();
        arena.setShape(shape());

        for (ArenaSpawnType spawnType : List.of(ArenaSpawnType.FIRST_TEAM, ArenaSpawnType.SECOND_TEAM)) {
            PositionSnapshot spawn = position(1.5D, 1.5D);

            assertTrue(arena.setSpawn(spawnType, spawn).isEmpty());
            assertEquals(spawn, arena.getConfiguredSpawn(spawnType).orElseThrow());
        }
        assertTrue(arena.findSpawnPlacementWarnings().isEmpty());
    }

    @Test
    void refusesTheTribuneSpawnInsideOfTheCalculatedFightArea() {
        Arena arena = new Arena();
        arena.setShape(shape());

        String rejection = arena.setSpawn(ArenaSpawnType.TRIBUNE, position(2.5D, 2.5D)).orElseThrow();

        assertTrue(rejection.contains("The tribune spawn must lie outside of"), rejection);
        assertTrue(rejection.contains("lies inside it"), rejection);
        assertTrue(rejection.contains(ArenaSpawnType.TRIBUNE.getPlacementHint()), rejection);
        assertTrue(arena.getConfiguredTribuneSpawn().isEmpty());
        assertFalse(arena.isFullyConfigured());
    }

    @Test
    void acceptsTheTribuneSpawnOutsideOfTheCalculatedFightArea() {
        Arena arena = new Arena();
        arena.setShape(shape());
        PositionSnapshot spawn = position(50.5D, 50.5D);

        assertTrue(arena.setSpawn(ArenaSpawnType.TRIBUNE, spawn).isEmpty());

        assertEquals(spawn, arena.getConfiguredTribuneSpawn().orElseThrow());
        assertTrue(arena.isFullyConfigured());
    }

    @Test
    void acceptsEverySpawnAsLongAsNoFightAreaIsCalculated() {
        Arena arena = new Arena();
        PositionSnapshot insidePosition = position(1.5D, 1.5D);
        PositionSnapshot outsidePosition = position(50.5D, 50.5D);

        for (ArenaSpawnType spawnType : ArenaSpawnType.values()) {
            assertTrue(arena.setSpawn(spawnType, insidePosition).isEmpty());
            assertEquals(insidePosition, arena.getConfiguredSpawn(spawnType).orElseThrow());
            assertTrue(arena.setSpawn(spawnType, outsidePosition).isEmpty());
            assertEquals(outsidePosition, arena.getConfiguredSpawn(spawnType).orElseThrow());
        }
        assertTrue(arena.findSpawnPlacementWarnings().isEmpty());
    }

    @Test
    void warnsAboutStoredSpawnsThatDoNotFitTheCalculatedFightArea() {
        Arena arena = new Arena();
        arena.setFirstTeamSpawn(position(1.5D, 1.5D));
        arena.setSecondTeamSpawn(position(50.5D, 50.5D));
        arena.setTribuneSpawn(position(2.5D, 2.5D));

        arena.setShape(shape());
        List<String> warnings = arena.findSpawnPlacementWarnings();

        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("The second team spawn"));
        assertTrue(warnings.get(0).contains("lies outside of it"));
        assertTrue(warnings.get(0).contains(ArenaSpawnType.SECOND_TEAM.getPlacementHint()));
        assertTrue(warnings.get(1).contains("The tribune spawn"));
        assertTrue(warnings.get(1).contains("lies inside it"));
        assertTrue(warnings.get(1).contains(ArenaSpawnType.TRIBUNE.getPlacementHint()));
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

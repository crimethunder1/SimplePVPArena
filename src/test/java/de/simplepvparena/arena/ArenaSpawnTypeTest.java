package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaSpawnTypeTest {

    @Test
    void namesEverySpawnOfTheArena() {
        assertEquals("first team spawn", ArenaSpawnType.FIRST_TEAM.getDisplayName());
        assertEquals("second team spawn", ArenaSpawnType.SECOND_TEAM.getDisplayName());
        assertEquals("tribune spawn", ArenaSpawnType.TRIBUNE.getDisplayName());
    }

    @Test
    void keepsOnlyTheTribuneOutsideOfTheFightArea() {
        assertFalse(ArenaSpawnType.FIRST_TEAM.mustLieOutsideFightArea());
        assertFalse(ArenaSpawnType.SECOND_TEAM.mustLieOutsideFightArea());
        assertTrue(ArenaSpawnType.TRIBUNE.mustLieOutsideFightArea());
    }

    @Test
    void tellsWhereToStandForEverySpawn() {
        for (ArenaSpawnType spawnType : ArenaSpawnType.values()) {
            String hint = spawnType.getPlacementHint();
            assertFalse(hint.isBlank(), spawnType.name());
            assertTrue(hint.startsWith(spawnType.mustLieOutsideFightArea() ? "outside" : "inside"), hint);
        }
        assertTrue(ArenaSpawnType.FIRST_TEAM.getPlacementHint().contains("start box"));
    }

    @Test
    void coversTheThreeSpawnsOfTheArenaAndNothingElse() {
        assertEquals(3, ArenaSpawnType.values().length);
    }
}

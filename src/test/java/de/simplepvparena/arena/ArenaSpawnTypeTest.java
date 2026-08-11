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
    void putsBothTeamsIntoTheFightAreaAndTheTribuneOutsideOfIt() {
        assertTrue(ArenaSpawnType.FIRST_TEAM.belongsIntoFightArea());
        assertTrue(ArenaSpawnType.SECOND_TEAM.belongsIntoFightArea());
        assertFalse(ArenaSpawnType.TRIBUNE.belongsIntoFightArea());
    }

    @Test
    void tellsWhereToStandForEverySpawn() {
        for (ArenaSpawnType spawnType : ArenaSpawnType.values()) {
            String hint = spawnType.getPlacementHint();
            assertFalse(hint.isBlank(), spawnType.name());
            assertTrue(hint.startsWith(spawnType.belongsIntoFightArea() ? "inside" : "outside"), hint);
        }
    }

    @Test
    void coversTheThreeSpawnsOfTheArenaAndNothingElse() {
        assertEquals(3, ArenaSpawnType.values().length);
    }
}

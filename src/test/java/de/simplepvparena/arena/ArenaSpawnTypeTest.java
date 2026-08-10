package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaSpawnTypeTest {

    @Test
    void namesEverySpawnOfTheArena() {
        assertEquals("first team spawn", ArenaSpawnType.FIRST_TEAM.getDisplayName());
        assertEquals("second team spawn", ArenaSpawnType.SECOND_TEAM.getDisplayName());
        assertEquals("tribune spawn", ArenaSpawnType.TRIBUNE.getDisplayName());
    }

    @Test
    void coversTheThreeSpawnsOfTheArenaAndNothingElse() {
        assertEquals(3, ArenaSpawnType.values().length);
    }
}

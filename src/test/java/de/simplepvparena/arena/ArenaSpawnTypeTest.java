package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaSpawnTypeTest {

    @Test
    void resolvesTheThreeSpawnArguments() {
        assertEquals(ArenaSpawnType.FIRST_TEAM, ArenaSpawnType.fromArgument("team1").orElseThrow());
        assertEquals(ArenaSpawnType.SECOND_TEAM, ArenaSpawnType.fromArgument("team2").orElseThrow());
        assertEquals(ArenaSpawnType.TRIBUNE, ArenaSpawnType.fromArgument("tribune").orElseThrow());
    }

    @Test
    void resolvesAnArgumentWithoutLookingAtItsCase() {
        assertEquals(ArenaSpawnType.TRIBUNE, ArenaSpawnType.fromArgument("TrIbUnE").orElseThrow());
    }

    @Test
    void resolvesNothingForAnUnknownSpawnType() {
        assertTrue(ArenaSpawnType.fromArgument("team3").isEmpty());
        assertTrue(ArenaSpawnType.fromArgument("").isEmpty());
        assertTrue(ArenaSpawnType.fromArgument(null).isEmpty());
    }

    @Test
    void offersEveryArgumentToTheTabCompletion() {
        assertEquals(List.of("team1", "team2", "tribune"), ArenaSpawnType.getArgumentNames());
    }
}

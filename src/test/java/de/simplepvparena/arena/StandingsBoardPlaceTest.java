package de.simplepvparena.arena;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingsBoardPlaceTest {

    private static final PositionSnapshot BOARD = new PositionSnapshot("world", 12.5D, 70.0D, -3.5D, 90.0F, 0.0F);

    @Test
    void theArenaSectionOfConfigYmlKeepsThePlaceOfTheBoard() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.getArena().setStandingsBoard(BOARD);
        MemoryConfiguration configuration = new MemoryConfiguration();

        registry.saveToConfiguration(configuration);
        ArenaRegistry reloadedRegistry = new ArenaRegistry();
        reloadedRegistry.loadFromConfiguration(configuration);

        assertEquals("world", configuration.getString("arena.standings-board.world"));
        assertEquals(12.5D, configuration.getDouble("arena.standings-board.x"));
        assertEquals(90.0D, configuration.getDouble("arena.standings-board.yaw"));
        assertEquals(Optional.of(BOARD), reloadedRegistry.getArena().getStandingsBoard());
    }

    @Test
    void anArenaWithoutABoardWritesAnEmptySectionAndReadsNoBoard() {
        ArenaRegistry registry = new ArenaRegistry();
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("arena.standings-board.world", "world");

        registry.saveToConfiguration(configuration);
        registry.loadFromConfiguration(configuration);

        assertTrue(configuration.isConfigurationSection("arena.standings-board"));
        assertFalse(configuration.contains("arena.standings-board.world"));
        assertEquals(Optional.empty(), registry.getArena().getStandingsBoard());
    }

    @Test
    void theBoardIsNoPartOfAFullyConfiguredArenaAndGoesWithTheReset() {
        Arena arena = new Arena();
        arena.setStandingsBoard(BOARD);

        assertFalse(arena.isFullyConfigured());
        arena.clearConfiguration();

        assertEquals(Optional.empty(), arena.getStandingsBoard());
    }
}

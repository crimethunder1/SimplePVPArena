package de.simplepvparena.arena;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaRegistryEffectAreaTest {

    private static final String WORLD_NAME = "event";
    private static final double PRECISION = 0.000001D;

    private final ArenaRegistry arenaRegistry = new ArenaRegistry();

    @Test
    void hasNoAreaOfEffectWithoutACalculatedFightArea() {
        assertEquals(Optional.empty(), arenaRegistry.findEffectArea());

        arenaRegistry.getArena().setTribuneSpawn(new PositionSnapshot(WORLD_NAME, 500.5D, 64.0D, 500.5D, 0F, 0F));

        assertEquals(Optional.empty(), arenaRegistry.findTribuneOutsideOfTheEffectArea(),
                "without an area there is nothing a tribune could lie outside of");
    }

    @Test
    void startsWithTheShippedMarginAndTeleportOnJoin() {
        assertEquals(EffectArea.DEFAULT_MARGIN, arenaRegistry.getEffectMargin());
        assertEquals(96, EffectArea.DEFAULT_MARGIN);
        assertTrue(arenaRegistry.isTeleportOnJoin());
    }

    @Test
    void buildsTheAreaOfEffectFromTheFightAreaAndTheMargin() {
        arenaRegistry.getArena().setShape(EffectAreaTest.rectangle(0, 5, 0, 7));

        EffectArea area = arenaRegistry.findEffectArea().orElseThrow();

        assertEquals(new EffectArea.Center(3.0D, 4.0D), area.getCenter());
        assertEquals(5.0D + EffectArea.DEFAULT_MARGIN, area.getRadius(), PRECISION);
        assertSame(area, arenaRegistry.findEffectArea().orElseThrow(), "the area is kept while nothing changes");
    }

    @Test
    void followsANewMarginAndANewFightArea() {
        arenaRegistry.getArena().setShape(EffectAreaTest.rectangle(0, 5, 0, 7));

        arenaRegistry.setEffectMargin(10);

        assertEquals(15.0D, arenaRegistry.findEffectArea().orElseThrow().getRadius(), PRECISION);
        assertEquals(10, arenaRegistry.getEffectMargin());

        arenaRegistry.getArena().setShape(EffectAreaTest.rectangle(100, 105, 0, 7));

        assertEquals(new EffectArea.Center(103.0D, 4.0D), arenaRegistry.findEffectArea().orElseThrow().getCenter());

        arenaRegistry.getArena().setShape(null);

        assertEquals(Optional.empty(), arenaRegistry.findEffectArea());
    }

    @Test
    void namesATribuneOutsideOfTheAreaOfEffect() {
        arenaRegistry.getArena().setShape(EffectAreaTest.rectangle(0, 5, 0, 7));
        arenaRegistry.getArena().setTribuneSpawn(new PositionSnapshot(WORLD_NAME, 30.5D, 64.0D, 4.0D, 0F, 0F));
        arenaRegistry.setEffectMargin(10);

        assertEquals(Optional.of("The tribune spawn event 30.5/64.0/4.0 lies outside of the area of effect, "
                        + "27.5 blocks from its centre 3/4, radius 15 blocks"),
                arenaRegistry.findTribuneOutsideOfTheEffectArea());

        arenaRegistry.setEffectMargin(30);

        assertEquals(Optional.empty(), arenaRegistry.findTribuneOutsideOfTheEffectArea());
        assertTrue(arenaRegistry.getArena().getConfiguredTribuneSpawn().isPresent());
    }

    @Test
    void keepsTheTeleportOnJoinThatWasSet() {
        arenaRegistry.setTeleportOnJoin(false);

        assertFalse(arenaRegistry.isTeleportOnJoin());

        arenaRegistry.loadFromConfiguration(new MemoryConfiguration());

        assertFalse(arenaRegistry.isTeleportOnJoin(), "loading the arena does not touch the setting");
    }
}

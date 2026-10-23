package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.EffectMarginSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaInventoryMarginChangeTest {

    @TempDir
    Path dataFolder;

    private AreaInventoryFixture fixture;
    private ArenaRegistry arenaRegistry;
    private EffectMarginSetting effectMarginSetting;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
        arenaRegistry = fixture.harness().getArenaRegistry();
        effectMarginSetting = new EffectMarginSetting(arenaRegistry, arenaRegistry::setEffectMargin);
    }

    @Test
    void theRegistryUsesTheNewMarginAtOnce() {
        double radiusBefore = arenaRegistry.findEffectArea().orElseThrow().getRadius();

        effectMarginSetting.apply(effectMarginSetting.findLargerMargin(EffectMarginSetting.LARGE_STEP));

        assertEquals(radiusBefore + EffectMarginSetting.LARGE_STEP,
                arenaRegistry.findEffectArea().orElseThrow().getRadius());
    }

    @Test
    void aSmallerMarginGivesTheInventoryBackAtTheNextCheckAndALargerOneTakesItAgain() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("MarginWitness");
        player.moveTo(fixture.onTheTribune());
        fixture.service().reconcileOnlinePlayers();
        assertTrue(fixture.service().hasDeposited(player.player()));

        while (effectMarginSetting.canGetSmaller()) {
            effectMarginSetting.apply(effectMarginSetting.findSmallerMargin(EffectMarginSetting.LARGE_STEP));
        }
        fixture.service().reconcileOnlinePlayers();

        assertFalse(fixture.service().hasDeposited(player.player()));
        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());

        effectMarginSetting.apply(effectMarginSetting.findLargerMargin(EffectMarginSetting.LARGE_STEP));
        fixture.service().reconcileOnlinePlayers();

        assertTrue(fixture.service().hasDeposited(player.player()));
        assertArrayEquals(AreaInventoryFixture.emptyContents(), player.contents());
    }
}

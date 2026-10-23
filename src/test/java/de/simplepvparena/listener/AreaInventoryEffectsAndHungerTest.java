package de.simplepvparena.listener;

import de.simplepvparena.arena.AreaPlayerCondition;
import de.simplepvparena.arena.SavedPotionEffect;
import de.simplepvparena.kit.KitItemCodec;
import de.simplepvparena.match.Match;
import de.simplepvparena.support.FakeItemStackCodec;
import de.simplepvparena.support.FakePlayerPotionEffects;
import org.bukkit.GameMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaInventoryEffectsAndHungerTest {

    private static final SavedPotionEffect SPEED = new SavedPotionEffect("minecraft:speed", 1, 1200, false, true,
            true);
    private static final SavedPotionEffect ENDLESS_NIGHT_VISION = new SavedPotionEffect("minecraft:night_vision", 0,
            SavedPotionEffect.INFINITE_DURATION_TICKS, true, false, false);
    private static final SavedPotionEffect EXPIRED_HASTE = new SavedPotionEffect("minecraft:haste", 0, 0, false,
            true, true);
    private static final SavedPotionEffect STRENGTH_GAINED_INSIDE = new SavedPotionEffect("minecraft:strength", 0,
            600, false, true, true);

    @TempDir
    Path dataFolder;

    private AreaInventoryFixture fixture;
    private ArenaMatchHarness harness;
    private FakePlayerPotionEffects potionEffects;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
        harness = fixture.harness();
        potionEffects = fixture.potionEffects();
    }

    @Test
    void enteringSavesTheEffectsAndTheHungerAndOnlyThenRemovesTheEffectsAndFillsTheHunger() {
        ArenaMatchHarness.FakePlayer player = addPlayerWithEffectsAndHalfAHunger("Runner");
        List<String> stateAtTheSave = new ArrayList<>();
        fixture.observeSave(() -> stateAtTheSave.add(potionEffects.effectsOf(player.uniqueId()).size() + " effects, "
                + "food " + player.foodLevel()));

        fixture.service().reconcile(player.player(), fixture.inside());

        assertEquals(List.of("2 effects, food 10"), stateAtTheSave);
        assertEquals(new AreaPlayerCondition(List.of(SPEED, ENDLESS_NIGHT_VISION), 10, 2.5F, 1.5F),
                fixture.findSavedInventory(player).condition().orElseThrow());
        assertEquals(List.of(), potionEffects.effectsOf(player.uniqueId()));
        assertEquals(20, player.foodLevel());
        assertEquals(20.0F, player.saturation());
        assertEquals(0.0F, player.exhaustion());
        assertEquals(17, player.level(), "the level stays");
        assertEquals(20.0D, player.health(), "the health stays");
        assertEquals(GameMode.SURVIVAL, player.gameMode(), "the game mode stays");
    }

    @Test
    void leavingRemovesTheEffectsGainedInsideAndGivesBackTheSavedEffectsAndTheHunger() {
        ArenaMatchHarness.FakePlayer player = addPlayerWithEffectsAndHalfAHunger("Runner");
        fixture.service().reconcile(player.player(), fixture.inside());
        potionEffects.give(player.uniqueId(), STRENGTH_GAINED_INSIDE);
        player.setHunger(3, 0.0F, 2.0F);

        fixture.service().reconcile(player.player(), fixture.outside());

        assertEquals(List.of(SPEED, ENDLESS_NIGHT_VISION), potionEffects.effectsOf(player.uniqueId()));
        assertHunger(player, 10, 2.5F, 1.5F);
        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
    }

    @Test
    void anEffectWithoutRemainingTimeIsNotGivenBackAndAnEndlessOneStaysEndless() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Runner");
        potionEffects.give(player.uniqueId(), EXPIRED_HASTE, ENDLESS_NIGHT_VISION, SPEED);

        fixture.service().reconcile(player.player(), fixture.inside());
        fixture.service().reconcile(player.player(), fixture.outside());

        assertEquals(List.of(ENDLESS_NIGHT_VISION, SPEED), potionEffects.effectsOf(player.uniqueId()));
        assertEquals(SavedPotionEffect.INFINITE_DURATION_TICKS,
                potionEffects.effectsOf(player.uniqueId()).get(0).durationTicks());
        assertEquals(1200, potionEffects.effectsOf(player.uniqueId()).get(1).durationTicks(),
                "the remaining time of the moment of entering");
    }

    @Test
    void aFileThatCannotBeWrittenLeavesTheEffectsAndTheHungerAsTheyAre() {
        ArenaMatchHarness.FakePlayer player = addPlayerWithEffectsAndHalfAHunger("Runner");
        fixture.failWritesWith(new IOException("disk full"));

        fixture.service().reconcile(player.player(), fixture.inside());

        assertFalse(fixture.service().hasDeposited(player.player()));
        assertEquals(List.of(SPEED, ENDLESS_NIGHT_VISION), potionEffects.effectsOf(player.uniqueId()));
        assertHunger(player, 10, 2.5F, 1.5F);
        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
    }

    @Test
    void aFileOfAnOlderFormatGivesTheInventoryBackAndLeavesEffectsAndHungerAlone() throws IOException {
        ArenaMatchHarness.FakePlayer player = addPlayerWithEffectsAndHalfAHunger("Runner");
        fixture.service().reconcile(player.player(), fixture.inside());
        String encodedContents = KitItemCodec.encode(new FakeItemStackCodec().encode(
                AreaInventoryFixture.ownContents()));
        Files.writeString(fixture.inventoryFileOf(player),
                "format-version: 2\nsaved-at: '2026-10-01T10:00:00Z'\ncontents: " + encodedContents + "\n",
                StandardCharsets.UTF_8);
        potionEffects.give(player.uniqueId(), STRENGTH_GAINED_INSIDE);
        player.setHunger(4, 1.0F, 0.5F);

        fixture.service().reconcile(player.player(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
        assertEquals(List.of(STRENGTH_GAINED_INSIDE), potionEffects.effectsOf(player.uniqueId()));
        assertHunger(player, 4, 1.0F, 0.5F);
        assertEquals(List.of(), fixture.warnings());
    }

    @Test
    void afterACrashTheNextStartGivesEffectsAndHungerBackToAPlayerWhoIsOutside() {
        ArenaMatchHarness.FakePlayer player = addPlayerWithEffectsAndHalfAHunger("Runner");
        fixture.service().reconcile(player.player(), fixture.inside());

        fixture.restartService();
        player.moveTo(fixture.outside());
        fixture.service().reconcileOnlinePlayers();

        assertEquals(List.of(SPEED, ENDLESS_NIGHT_VISION), potionEffects.effectsOf(player.uniqueId()));
        assertHunger(player, 10, 2.5F, 1.5F);
        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
    }

    @Test
    void theShutdownGivesEffectsAndHungerBackEvenInsideTheArea() {
        ArenaMatchHarness.FakePlayer player = addPlayerWithEffectsAndHalfAHunger("Runner");
        player.moveTo(fixture.inside());
        fixture.service().reconcileOnlinePlayers();

        fixture.service().giveEveryInventoryBackQuietly();

        assertEquals(List.of(SPEED, ENDLESS_NIGHT_VISION), potionEffects.effectsOf(player.uniqueId()));
        assertHunger(player, 10, 2.5F, 1.5F);
    }

    @Test
    void aMatchInsideTheAreaGivesBackTheStateOfTheAreaAndLeavingTheStateFromBeforeEntering() {
        ArenaMatchHarness.FakePlayer fighter = addPlayerWithEffectsAndHalfAHunger("Fighter");
        ArenaMatchHarness.FakePlayer mate = fixture.addPlayerOutsideWithItems("Mate");
        ArenaMatchHarness.FakePlayer opponent = fixture.addPlayerOutsideWithItems("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = fixture.addPlayerOutsideWithItems("OpponentMate");
        for (ArenaMatchHarness.FakePlayer player : List.of(fighter, mate, opponent, opponentMate)) {
            player.moveTo(fixture.onTheTribune());
        }
        fixture.service().reconcileOnlinePlayers();
        assertHunger(fighter, 20, 20.0F, 0.0F);

        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);
        fighter.setHunger(6, 1.0F, 3.0F);
        potionEffects.give(fighter.uniqueId(), STRENGTH_GAINED_INSIDE);
        assertTrue(harness.getMatchService().forceResult(match.getMatchId(), match.getFirstTeam().getTeamName()));

        assertEquals(20, fighter.foodLevel(), "the match gives back the hunger of the area");
        assertEquals(20.0F, fighter.saturation());
        assertTrue(fixture.service().hasDeposited(fighter.player()));

        fighter.moveTo(fixture.outside());
        fixture.service().reconcile(fighter.player());

        assertEquals(List.of(SPEED, ENDLESS_NIGHT_VISION), potionEffects.effectsOf(fighter.uniqueId()));
        assertHunger(fighter, 10, 2.5F, 1.5F);
        assertArrayEquals(AreaInventoryFixture.ownContents(), fighter.contents());
        assertEquals(List.of(), fixture.warnings());
    }

    @Test
    void anAdminInCreativeModeHandsInEffectsAndHungerAsWell() {
        ArenaMatchHarness.FakePlayer admin = addPlayerWithEffectsAndHalfAHunger("CreativeAdmin");
        admin.makeOperator();
        admin.grantAdminPermission();
        admin.player().setGameMode(GameMode.CREATIVE);

        fixture.service().reconcile(admin.player(), fixture.inside());

        assertEquals(List.of(), potionEffects.effectsOf(admin.uniqueId()));
        assertHunger(admin, 20, 20.0F, 0.0F);

        fixture.service().reconcile(admin.player(), fixture.outside());

        assertEquals(List.of(SPEED, ENDLESS_NIGHT_VISION), potionEffects.effectsOf(admin.uniqueId()));
        assertHunger(admin, 10, 2.5F, 1.5F);
        assertEquals(GameMode.CREATIVE, admin.gameMode());
    }

    private ArenaMatchHarness.FakePlayer addPlayerWithEffectsAndHalfAHunger(String playerName) {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems(playerName);
        potionEffects.give(player.uniqueId(), SPEED, ENDLESS_NIGHT_VISION);
        player.setHunger(10, 2.5F, 1.5F);
        return player;
    }

    private static void assertHunger(ArenaMatchHarness.FakePlayer player, int foodLevel, float saturation,
                                     float exhaustion) {
        assertEquals(foodLevel, player.foodLevel(), "food level");
        assertEquals(saturation, player.saturation(), "saturation");
        assertEquals(exhaustion, player.exhaustion(), "exhaustion");
    }
}

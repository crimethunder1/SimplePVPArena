package de.simplepvparena.arena;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.capture;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.position;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.shape;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBreakRuleTest {

    private static final boolean ACTIVE_FIGHTER = true;
    private static final boolean NOT_FIGHTING = false;
    private static final boolean ADMIN = true;
    private static final boolean REGULAR_PLAYER = false;
    private static final boolean INSIDE = true;
    private static final boolean OUTSIDE = false;
    private static final boolean ORIGINAL_ARENA = true;
    private static final boolean PLACED_DURING_THE_MATCH = false;

    @Test
    void letsAnActiveFighterBreakWhatWasPlacedDuringTheMatch() {
        assertTrue(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, INSIDE,
                PLACED_DURING_THE_MATCH));
    }

    @Test
    void keepsAnActiveFighterFromBreakingTheArenaItself() {
        assertFalse(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, INSIDE, ORIGINAL_ARENA));
    }

    @Test
    void keepsEverybodyWhoIsNotFightingOutOfTheFightArea() {
        assertFalse(ArenaBreakRule.isBreakingAllowed(NOT_FIGHTING, REGULAR_PLAYER, INSIDE, ORIGINAL_ARENA));
        assertFalse(ArenaBreakRule.isBreakingAllowed(NOT_FIGHTING, REGULAR_PLAYER, INSIDE,
                PLACED_DURING_THE_MATCH));
    }

    @Test
    void leavesEveryBreakOutsideTheFightAreaAlone() {
        assertTrue(ArenaBreakRule.isBreakingAllowed(NOT_FIGHTING, REGULAR_PLAYER, OUTSIDE, ORIGINAL_ARENA));
        assertTrue(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, OUTSIDE, ORIGINAL_ARENA));
    }

    @Test
    void letsAnAdminBreakEverything() {
        assertTrue(ArenaBreakRule.isBreakingAllowed(NOT_FIGHTING, ADMIN, INSIDE, ORIGINAL_ARENA));
        assertTrue(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, ADMIN, INSIDE, ORIGINAL_ARENA));
        assertTrue(ArenaBreakRule.isBreakingAllowed(NOT_FIGHTING, ADMIN, OUTSIDE, ORIGINAL_ARENA));
    }

    @Test
    void readsTheArenaItselfOutOfTheBaseline() {
        Optional<ArenaBaselineSnapshot> baseline = baselineOfAFloorWithOneHole();

        assertTrue(ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 64, 0, Material.STONE),
                "the floor the arena was calculated with");
        assertFalse(ArenaBreakRule.isPartOfTheOriginalArena(baseline, 1, 64, 0, Material.OAK_PLANKS),
                "a block placed into the hole of that floor during the match");
        assertFalse(ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 65, 0, Material.WATER),
                "and a bucket emptied over it, where the baseline holds air");
    }

    @Test
    void treatsABlockThatReplacedOneOfTheArenaAsPlacedDuringTheMatch() {
        Optional<ArenaBaselineSnapshot> baseline = baselineOfAFloorWithOneHole();

        assertFalse(ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 64, 0, Material.SAND));
        assertTrue(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 64, 0, Material.SAND)));
    }

    @Test
    void treatsABlockAboveTheCapturedVolumeAsPlacedDuringTheMatch() {
        assertFalse(ArenaBreakRule.isPartOfTheOriginalArena(baselineOfAFloorWithOneHole(), 0, 66, 0,
                Material.STONE));
    }

    @Test
    void treatsABlockBelowTheCapturedVolumeAsTheArenaItself() {
        Optional<ArenaBaselineSnapshot> baseline = baselineOfAFloorWithOneHole();

        assertTrue(ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 63, 0, Material.STONE),
                "the floor the border wall was laid on");
        assertTrue(ArenaBreakRule.isPartOfTheOriginalArena(baseline, 1, 60, 0, Material.DIRT),
                "and the ground under that floor, whatever it is made of");
        assertFalse(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 63, 0, Material.STONE)),
                "so no fighter digs the arena out from under the opponent");
        assertTrue(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, ADMIN, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 63, 0, Material.STONE)),
                "and the admin who built it still takes it apart");
    }

    @Test
    void treatsEveryBlockAsTheArenaItselfWithoutABaseline() {
        assertTrue(ArenaBreakRule.isPartOfTheOriginalArena(Optional.empty(), 0, 64, 0, Material.OAK_PLANKS));
        assertFalse(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(Optional.empty(), 0, 64, 0, Material.OAK_PLANKS)));
        assertTrue(ArenaBreakRule.isBreakingAllowed(NOT_FIGHTING, ADMIN, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(Optional.empty(), 0, 64, 0, Material.OAK_PLANKS)),
                "the admin who calculates the area again is never locked out of it");
    }

    @Test
    void keepsTheRuleForABaselineThatReachesUnderTheFloor() {
        Optional<ArenaBaselineSnapshot> baseline = Optional.of(capture(shape(64, 65, 0, 0, 1, 0),
                Map.of(position(0, 59, 0), Material.STONE, position(0, 64, 0), Material.STONE), 4, 10));

        assertTrue(ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 59, 0, Material.STONE),
                "the ground the baseline now covers is still the arena");
        assertFalse(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 64, 0, Material.STONE)),
                "the floor stays protected from the fighters");
        assertTrue(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(baseline, 1, 65, 0, Material.OAK_PLANKS)),
                "a block placed during the match comes down again");
        assertTrue(ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 53, 0, Material.DIRT),
                "the first block under the captured volume counts as the arena");
        assertFalse(ArenaBreakRule.isBreakingAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 53, 0, Material.DIRT)));
        assertFalse(ArenaBreakRule.isBreakingAllowed(NOT_FIGHTING, REGULAR_PLAYER, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 53, 0, Material.DIRT)));
        assertTrue(ArenaBreakRule.isBreakingAllowed(NOT_FIGHTING, ADMIN, INSIDE,
                ArenaBreakRule.isPartOfTheOriginalArena(baseline, 0, 53, 0, Material.DIRT)),
                "while the admin still takes it apart");
    }

    private static Optional<ArenaBaselineSnapshot> baselineOfAFloorWithOneHole() {
        return Optional.of(capture(shape(64, 65, 0, 0, 1, 0),
                Map.of(position(0, 64, 0), Material.STONE), 4));
    }
}

package de.simplepvparena.arena;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDamageRuleTest {

    private static final List<DamageCause> REQUESTED_CAUSES = List.of(
            DamageCause.CUSTOM, DamageCause.VOID, DamageCause.KILL, DamageCause.SUICIDE, DamageCause.FALL,
            DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA, DamageCause.DROWNING,
            DamageCause.SUFFOCATION, DamageCause.STARVATION, DamageCause.POISON, DamageCause.WITHER,
            DamageCause.ENTITY_ATTACK, DamageCause.PROJECTILE, DamageCause.MAGIC, DamageCause.THORNS,
            DamageCause.ENTITY_EXPLOSION, DamageCause.BLOCK_EXPLOSION, DamageCause.LIGHTNING);

    private static final Set<DamageCause> ALWAYS_ALLOWED_CAUSES = EnumSet.of(
            DamageCause.VOID, DamageCause.KILL, DamageCause.SUICIDE);

    private static final Set<DamageCause> FIGHT_CAUSES = EnumSet.of(
            DamageCause.LAVA, DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.HOT_FLOOR, DamageCause.CAMPFIRE,
            DamageCause.CONTACT, DamageCause.FALL, DamageCause.DROWNING, DamageCause.SUFFOCATION,
            DamageCause.FLY_INTO_WALL, DamageCause.FALLING_BLOCK, DamageCause.BLOCK_EXPLOSION,
            DamageCause.ENTITY_EXPLOSION, DamageCause.MAGIC, DamageCause.POISON, DamageCause.WITHER,
            DamageCause.THORNS, DamageCause.DRAGON_BREATH, DamageCause.SONIC_BOOM, DamageCause.STARVATION,
            DamageCause.FREEZE, DamageCause.LIGHTNING, DamageCause.ENTITY_SWEEP_ATTACK, DamageCause.ENTITY_ATTACK,
            DamageCause.PROJECTILE);

    private static final Set<ArenaDamageCauser> CAUSERS_WITHOUT_PLAYER_OR_MOB = EnumSet.of(
            ArenaDamageCauser.NONE, ArenaDamageCauser.NON_LIVING_ENTITY);

    private static final Set<ArenaDamageCauser> REFUSED_FIGHTER_CAUSERS = EnumSet.of(
            ArenaDamageCauser.TEAM_MATE, ArenaDamageCauser.OTHER_PLAYER, ArenaDamageCauser.MOB);

    private static final Set<ArenaDamageCauser> ALLOWED_FIGHTER_CAUSERS = EnumSet.of(
            ArenaDamageCauser.OPPONENT, ArenaDamageCauser.ADMIN, ArenaDamageCauser.SELF);

    @Test
    void coversEveryRequestedCauseInTheMatrix() {
        assertEquals(20, REQUESTED_CAUSES.size());
        assertTrue(EnumSet.allOf(DamageCause.class).containsAll(REQUESTED_CAUSES));
    }

    @Test
    void neverCancelsTheDamageOfAPlayerWithoutARoleInTheArena() {
        for (DamageCause cause : DamageCause.values()) {
            for (ArenaDamageCauser causer : ArenaDamageCauser.values()) {
                assertFalse(isCancelled(ArenaDamageVictimRole.NONE, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void letsOnlyVoidKillAndSuicideHurtASpectator() {
        for (DamageCause cause : ALWAYS_ALLOWED_CAUSES) {
            for (ArenaDamageCauser causer : ArenaDamageCauser.values()) {
                assertFalse(isCancelled(ArenaDamageVictimRole.SPECTATOR, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void cancelsEveryOtherDamageOfASpectatorWhoeverCausesIt() {
        for (DamageCause cause : DamageCause.values()) {
            if (ALWAYS_ALLOWED_CAUSES.contains(cause)) {
                continue;
            }
            for (ArenaDamageCauser causer : ArenaDamageCauser.values()) {
                assertTrue(isCancelled(ArenaDamageVictimRole.SPECTATOR, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void letsSuddenDeathVoidKillAndSuicideHurtAFighterWhoeverCausesIt() {
        Set<DamageCause> causes = EnumSet.copyOf(ALWAYS_ALLOWED_CAUSES);
        causes.add(DamageCause.CUSTOM);
        for (DamageCause cause : causes) {
            for (ArenaDamageCauser causer : ArenaDamageCauser.values()) {
                assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void coversEveryCauseOfAFightInTheArena() {
        assertEquals(25, FIGHT_CAUSES.size());
        assertEquals(EnumSet.allOf(ArenaDamageCauser.class), union(CAUSERS_WITHOUT_PLAYER_OR_MOB,
                REFUSED_FIGHTER_CAUSERS, ALLOWED_FIGHTER_CAUSERS));
    }

    @Test
    void letsEveryCauseOfAFightHurtAFighterWhenNoPlayerAndNoMobIsBehindIt() {
        for (DamageCause cause : FIGHT_CAUSES) {
            for (ArenaDamageCauser causer : CAUSERS_WITHOUT_PLAYER_OR_MOB) {
                assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void letsAnOpponentAnAdminAndTheFighterCauseEveryDamageToAFighter() {
        for (DamageCause cause : DamageCause.values()) {
            for (ArenaDamageCauser causer : ALLOWED_FIGHTER_CAUSERS) {
                assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void keepsTeamMatesPlayersOutsideTheMatchAndMobsAwayFromAFighter() {
        for (DamageCause cause : DamageCause.values()) {
            if (cause == DamageCause.CUSTOM || ALWAYS_ALLOWED_CAUSES.contains(cause)) {
                continue;
            }
            for (ArenaDamageCauser causer : REFUSED_FIGHTER_CAUSERS) {
                assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void keepsEveryOtherCauseAwayFromAFighterWhenNoOpponentAdminOrTheFighterIsBehindIt() {
        for (DamageCause cause : DamageCause.values()) {
            if (cause == DamageCause.CUSTOM || ALWAYS_ALLOWED_CAUSES.contains(cause) || FIGHT_CAUSES.contains(cause)) {
                continue;
            }
            for (ArenaDamageCauser causer : CAUSERS_WITHOUT_PLAYER_OR_MOB) {
                assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void letsLavaFireAndTheExplosionsOfAnOpponentHurtAFighter() {
        for (DamageCause cause : List.of(DamageCause.LAVA, DamageCause.FIRE, DamageCause.FIRE_TICK,
                DamageCause.FALL, DamageCause.POISON, DamageCause.LIGHTNING, DamageCause.BLOCK_EXPLOSION)) {
            assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, ArenaDamageCauser.NONE), cause.name());
        }
        assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_EXPLOSION,
                ArenaDamageCauser.OPPONENT), "the TNT of an opponent");
        assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_EXPLOSION,
                ArenaDamageCauser.NON_LIVING_ENTITY), "an end crystal");
        assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.FALLING_BLOCK,
                ArenaDamageCauser.NON_LIVING_ENTITY), "a falling anvil");
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_EXPLOSION,
                ArenaDamageCauser.MOB), "a creeper");
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_ATTACK,
                ArenaDamageCauser.MOB), "a mob");
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_ATTACK,
                ArenaDamageCauser.TEAM_MATE), "a team mate");
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_ATTACK,
                ArenaDamageCauser.OTHER_PLAYER), "a player outside the match");
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.CRAMMING,
                ArenaDamageCauser.NONE), "cramming");
    }

    @Test
    void letsOpponentsAdminsAndTheFightersOwnProjectileThrough() {
        for (DamageCause cause : List.of(DamageCause.ENTITY_ATTACK, DamageCause.PROJECTILE, DamageCause.MAGIC,
                DamageCause.THORNS)) {
            assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, ArenaDamageCauser.OPPONENT),
                    "an opponent: " + cause.name());
            assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, ArenaDamageCauser.ADMIN),
                    "an admin: " + cause.name());
        }
        assertFalse(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.PROJECTILE, ArenaDamageCauser.SELF),
                "the own projectile of the fighter");
    }

    @SafeVarargs
    private static Set<ArenaDamageCauser> union(Set<ArenaDamageCauser>... causerSets) {
        Set<ArenaDamageCauser> causers = EnumSet.noneOf(ArenaDamageCauser.class);
        for (Set<ArenaDamageCauser> causerSet : causerSets) {
            causers.addAll(causerSet);
        }
        return causers;
    }

    private static boolean isCancelled(ArenaDamageVictimRole victimRole, DamageCause cause,
                                       ArenaDamageCauser causer) {
        return ArenaDamageRule.isDamageCancelled(victimRole, cause, causer);
    }

    private static String describe(DamageCause cause, ArenaDamageCauser causer) {
        return cause.name() + " caused by " + causer.name();
    }
}

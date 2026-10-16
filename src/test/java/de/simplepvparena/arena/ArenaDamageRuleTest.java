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

    private static final Set<DamageCause> EXPLOSION_CAUSES = EnumSet.of(
            DamageCause.ENTITY_EXPLOSION, DamageCause.BLOCK_EXPLOSION);

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
    void treatsEveryExplosionAsEnvironmentalDamageOfAFighter() {
        for (DamageCause cause : EXPLOSION_CAUSES) {
            for (ArenaDamageCauser causer : ArenaDamageCauser.values()) {
                assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void letsOnlyAnOpponentAnAdminOrTheFighterCauseAnyOtherDamageToAFighter() {
        for (DamageCause cause : DamageCause.values()) {
            if (cause == DamageCause.CUSTOM || ALWAYS_ALLOWED_CAUSES.contains(cause)
                    || EXPLOSION_CAUSES.contains(cause)) {
                continue;
            }
            for (ArenaDamageCauser causer : ArenaDamageCauser.values()) {
                assertEquals(!ALLOWED_FIGHTER_CAUSERS.contains(causer),
                        isCancelled(ArenaDamageVictimRole.FIGHTER, cause, causer), describe(cause, causer));
            }
        }
    }

    @Test
    void keepsTheEnvironmentTeamMatesAndMobsAwayFromAFighter() {
        for (DamageCause cause : List.of(DamageCause.FALL, DamageCause.FIRE, DamageCause.FIRE_TICK,
                DamageCause.LAVA, DamageCause.DROWNING, DamageCause.SUFFOCATION, DamageCause.STARVATION,
                DamageCause.POISON, DamageCause.WITHER, DamageCause.LIGHTNING)) {
            assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, cause, ArenaDamageCauser.NONE), cause.name());
        }
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_ATTACK,
                ArenaDamageCauser.TEAM_MATE), "a team mate");
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_ATTACK,
                ArenaDamageCauser.NON_PLAYER), "a mob");
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.LIGHTNING,
                ArenaDamageCauser.NON_PLAYER), "a lightning strike");
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_EXPLOSION,
                ArenaDamageCauser.OPPONENT), "the TNT of an opponent");
        assertTrue(isCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_ATTACK,
                ArenaDamageCauser.OTHER_PLAYER), "a player outside the match");
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

    private static boolean isCancelled(ArenaDamageVictimRole victimRole, DamageCause cause,
                                       ArenaDamageCauser causer) {
        return ArenaDamageRule.isDamageCancelled(victimRole, cause, causer);
    }

    private static String describe(DamageCause cause, ArenaDamageCauser causer) {
        return cause.name() + " caused by " + causer.name();
    }
}

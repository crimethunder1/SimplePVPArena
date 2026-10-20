package de.simplepvparena.arena;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDamageRuleStartAnimationTest {

    private static final Set<DamageCause> ALWAYS_ALLOWED_CAUSES = EnumSet.of(
            DamageCause.VOID, DamageCause.KILL, DamageCause.SUICIDE);

    @Test
    void letsOnlyVoidKillAndSuicideHurtAFighterDuringTheStartAnimationWhoeverCausesIt() {
        for (DamageCause cause : DamageCause.values()) {
            for (ArenaDamageCauser causer : ArenaDamageCauser.values()) {
                assertTrue(ALWAYS_ALLOWED_CAUSES.contains(cause) != isCancelled(cause, causer),
                        cause.name() + " caused by " + causer.name());
            }
        }
    }

    @Test
    void keepsOpponentsAdminsTheFighterAndSuddenDeathAwayDuringTheStartAnimation() {
        for (DamageCause cause : new DamageCause[] {DamageCause.ENTITY_ATTACK, DamageCause.PROJECTILE,
                DamageCause.MAGIC, DamageCause.THORNS}) {
            assertTrue(isCancelled(cause, ArenaDamageCauser.OPPONENT), "an opponent: " + cause.name());
            assertTrue(isCancelled(cause, ArenaDamageCauser.ADMIN), "an admin: " + cause.name());
            assertTrue(isCancelled(cause, ArenaDamageCauser.SELF), "the fighter: " + cause.name());
        }
        assertTrue(isCancelled(DamageCause.CUSTOM, ArenaDamageCauser.NONE), "the damage of sudden death");
    }

    @Test
    void letsTheVoidAnAdminKillAndASuicideThroughDuringTheStartAnimation() {
        assertFalse(isCancelled(DamageCause.VOID, ArenaDamageCauser.NONE), "void");
        assertFalse(isCancelled(DamageCause.KILL, ArenaDamageCauser.NONE), "a kill command");
        assertFalse(isCancelled(DamageCause.SUICIDE, ArenaDamageCauser.SELF), "a suicide");
    }

    @Test
    void theFighterRoleOutsideOfTheStartAnimationKeepsItsRules() {
        assertFalse(ArenaDamageRule.isDamageCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_ATTACK,
                ArenaDamageCauser.OPPONENT), "an opponent");
        assertFalse(ArenaDamageRule.isDamageCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.ENTITY_ATTACK,
                ArenaDamageCauser.ADMIN), "an admin");
        assertFalse(ArenaDamageRule.isDamageCancelled(ArenaDamageVictimRole.FIGHTER, DamageCause.CUSTOM,
                ArenaDamageCauser.NONE), "sudden death");
    }

    private static boolean isCancelled(DamageCause cause, ArenaDamageCauser causer) {
        return ArenaDamageRule.isDamageCancelled(ArenaDamageVictimRole.FIGHTER_IN_START_ANIMATION, cause, causer);
    }
}

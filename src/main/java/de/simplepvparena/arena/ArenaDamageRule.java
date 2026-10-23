package de.simplepvparena.arena;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.EnumSet;
import java.util.Set;

public final class ArenaDamageRule {

    private static final Set<DamageCause> ALWAYS_ALLOWED_CAUSES = EnumSet.of(
            DamageCause.VOID,
            DamageCause.KILL,
            DamageCause.SUICIDE);

    private static final Set<DamageCause> FIGHT_CAUSES = EnumSet.of(
            DamageCause.LAVA,
            DamageCause.FIRE,
            DamageCause.FIRE_TICK,
            DamageCause.HOT_FLOOR,
            DamageCause.CAMPFIRE,
            DamageCause.CONTACT,
            DamageCause.FALL,
            DamageCause.DROWNING,
            DamageCause.SUFFOCATION,
            DamageCause.FLY_INTO_WALL,
            DamageCause.FALLING_BLOCK,
            DamageCause.BLOCK_EXPLOSION,
            DamageCause.ENTITY_EXPLOSION,
            DamageCause.MAGIC,
            DamageCause.POISON,
            DamageCause.WITHER,
            DamageCause.THORNS,
            DamageCause.DRAGON_BREATH,
            DamageCause.SONIC_BOOM,
            DamageCause.STARVATION,
            DamageCause.FREEZE,
            DamageCause.LIGHTNING,
            DamageCause.ENTITY_SWEEP_ATTACK,
            DamageCause.ENTITY_ATTACK,
            DamageCause.PROJECTILE);

    private static final Set<ArenaDamageCauser> ALLOWED_FIGHTER_CAUSERS = EnumSet.of(
            ArenaDamageCauser.OPPONENT,
            ArenaDamageCauser.ADMIN,
            ArenaDamageCauser.SELF);

    private static final Set<ArenaDamageCauser> CAUSERS_WITHOUT_PLAYER_OR_MOB = EnumSet.of(
            ArenaDamageCauser.NONE,
            ArenaDamageCauser.NON_LIVING_ENTITY);

    private ArenaDamageRule() {
    }

    public static boolean isDamageCancelled(ArenaDamageVictimRole victimRole, DamageCause cause,
                                            ArenaDamageCauser causer) {
        return switch (victimRole) {
            case NONE -> false;
            case SPECTATOR, FIGHTER_IN_START_ANIMATION -> !ALWAYS_ALLOWED_CAUSES.contains(cause);
            case FIGHTER -> !isFighterDamageAllowed(cause, causer);
        };
    }

    private static boolean isFighterDamageAllowed(DamageCause cause, ArenaDamageCauser causer) {
        if (cause == DamageCause.CUSTOM || ALWAYS_ALLOWED_CAUSES.contains(cause)) {
            return true;
        }
        if (ALLOWED_FIGHTER_CAUSERS.contains(causer)) {
            return true;
        }
        return FIGHT_CAUSES.contains(cause) && CAUSERS_WITHOUT_PLAYER_OR_MOB.contains(causer);
    }
}

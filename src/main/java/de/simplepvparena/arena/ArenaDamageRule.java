package de.simplepvparena.arena;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.EnumSet;
import java.util.Set;

public final class ArenaDamageRule {

    private static final Set<DamageCause> ALWAYS_ALLOWED_CAUSES = EnumSet.of(
            DamageCause.VOID,
            DamageCause.KILL,
            DamageCause.SUICIDE);

    private static final Set<DamageCause> EXPLOSION_CAUSES = EnumSet.of(
            DamageCause.ENTITY_EXPLOSION,
            DamageCause.BLOCK_EXPLOSION);

    private static final Set<ArenaDamageCauser> ALLOWED_FIGHTER_CAUSERS = EnumSet.of(
            ArenaDamageCauser.OPPONENT,
            ArenaDamageCauser.ADMIN,
            ArenaDamageCauser.SELF);

    private ArenaDamageRule() {
    }

    public static boolean isDamageCancelled(ArenaDamageVictimRole victimRole, DamageCause cause,
                                            ArenaDamageCauser causer) {
        return switch (victimRole) {
            case NONE -> false;
            case SPECTATOR -> !ALWAYS_ALLOWED_CAUSES.contains(cause);
            case FIGHTER -> !isFighterDamageAllowed(cause, causer);
        };
    }

    private static boolean isFighterDamageAllowed(DamageCause cause, ArenaDamageCauser causer) {
        if (cause == DamageCause.CUSTOM || ALWAYS_ALLOWED_CAUSES.contains(cause)) {
            return true;
        }
        if (EXPLOSION_CAUSES.contains(cause)) {
            return false;
        }
        return ALLOWED_FIGHTER_CAUSERS.contains(causer);
    }
}

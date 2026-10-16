package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaSafetyZone;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaSafetyZoneListenerTest {

    private static final Set<DamageCause> CANCELLED_CAUSES = EnumSet.of(
            DamageCause.FALL,
            DamageCause.DROWNING,
            DamageCause.SUFFOCATION,
            DamageCause.FIRE,
            DamageCause.FIRE_TICK,
            DamageCause.LAVA,
            DamageCause.STARVATION);

    private ArenaMatchHarness.FakePlayer spectator;
    private ArenaSafetyZoneListener safetyZoneListener;

    @BeforeEach
    void setUp() {
        ArenaMatchHarness harness = new ArenaMatchHarness();
        spectator = harness.addPlayer("Spectator");
        ArenaSafetyZone safetyZone = new ArenaSafetyZone(() -> harness.getArenaRegistry().getArena().getShape(),
                harness.getMatchService()::isActiveFighter);
        safetyZoneListener = new ArenaSafetyZoneListener(safetyZone);
    }

    @Test
    void letsAPlayerInTheSafetyZoneFallIntoTheVoid() {
        assertFalse(isCancelled(spectator, DamageCause.VOID));
    }

    @Test
    void keepsProtectingAPlayerInTheSafetyZoneFromTheOtherCausesOfTheWorld() {
        for (DamageCause cause : CANCELLED_CAUSES) {
            assertTrue(isCancelled(spectator, cause), cause.name());
        }
    }

    private boolean isCancelled(ArenaMatchHarness.FakePlayer victim, DamageCause cause) {
        EntityDamageEvent event = new EntityDamageEvent(victim.player(), cause, createDamageSource(), 1.0D);
        safetyZoneListener.onEntityDamage(event);
        return event.isCancelled();
    }

    private static DamageSource createDamageSource() {
        return ArenaMatchHarness.proxy(DamageSource.class, (method, arguments) -> {
            throw new UnsupportedOperationException("DamageSource." + method.getName());
        });
    }
}

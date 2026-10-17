package de.simplepvparena.listener;

import com.google.common.base.Functions;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalPvpLockListenerTest {

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer admin;
    private ArenaMatchHarness.FakePlayer bystander;
    private GlobalPvpLockListener pvpLockListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        admin = harness.addPlayer("Admin");
        admin.grantAdminPermission();
        bystander = harness.addPlayer("Bystander");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        pvpLockListener = new GlobalPvpLockListener(harness.getMatchService(), harness.getArenaRegistry(),
                harness.getPlugin().getServer());
    }

    @Test
    void letsAnAdminHitAnActiveFighterInMeleeWithAnArrowAPotionAndACloud() {
        assertFalse(isCancelled(fighter, admin.player(), DamageCause.ENTITY_ATTACK), "melee");
        assertFalse(isCancelled(fighter, arrowShotBy(admin), DamageCause.PROJECTILE), "arrow");
        assertFalse(isCancelled(fighter, potionThrownBy(admin), DamageCause.MAGIC), "splash potion");
        assertFalse(isCancelled(fighter, cloudOf(admin), DamageCause.MAGIC), "lingering cloud");
    }

    @Test
    void keepsTheEffectOfAPotionOrACloudOfAnAdminOnAnActiveFighter() {
        assertFalse(pvpLockListener.isLockedTarget(admin.uniqueId(), fighter.player()));
        assertFalse(pvpLockListener.isLockedTarget(admin.uniqueId(), opponent.player()));
    }

    @Test
    void stillLocksAnAdminAgainstAPlayerOutsideTheMatch() {
        assertTrue(isCancelled(bystander, admin.player(), DamageCause.ENTITY_ATTACK), "melee");
        assertTrue(isCancelled(bystander, arrowShotBy(admin), DamageCause.PROJECTILE), "arrow");
        assertTrue(pvpLockListener.isLockedTarget(admin.uniqueId(), bystander.player()), "potion or cloud");
    }

    @Test
    void stillLocksAPlayerOutsideTheMatchAgainstAnActiveFighter() {
        assertTrue(isCancelled(fighter, bystander.player(), DamageCause.ENTITY_ATTACK), "melee");
        assertTrue(isCancelled(fighter, arrowShotBy(bystander), DamageCause.PROJECTILE), "arrow");
        assertTrue(isCancelled(fighter, potionThrownBy(bystander), DamageCause.MAGIC), "splash potion");
        assertTrue(pvpLockListener.isLockedTarget(bystander.uniqueId(), fighter.player()), "potion or cloud");
    }

    @Test
    void keepsLettingTwoOpposingFightersHitEachOther() {
        assertFalse(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK));
        assertFalse(pvpLockListener.isLockedTarget(opponent.uniqueId(), fighter.player()));
    }

    @Test
    void doesNotLetTheFriendlyFireCheckStopAnAdminOutsideTheTeamOfTheFighter() {
        FriendlyFireListener friendlyFireListener = new FriendlyFireListener(harness.getMatchService());
        EntityDamageByEntityEvent event = createEvent(fighter, admin.player(), DamageCause.ENTITY_ATTACK);

        pvpLockListener.onEntityDamageByEntity(event);
        friendlyFireListener.onEntityDamageByEntity(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void decidesTheAdminExceptionFromTheAttackerAndTheVictimAlone() {
        assertTrue(GlobalPvpLockListener.isAdminAgainstActiveFighter(true, true));
        assertFalse(GlobalPvpLockListener.isAdminAgainstActiveFighter(true, false), "a victim outside the round");
        assertFalse(GlobalPvpLockListener.isAdminAgainstActiveFighter(false, true), "an attacker without rights");
        assertFalse(GlobalPvpLockListener.isAdminAgainstActiveFighter(false, false));
    }

    private boolean isCancelled(ArenaMatchHarness.FakePlayer victim, Entity damager, DamageCause cause) {
        EntityDamageByEntityEvent event = createEvent(victim, damager, cause);
        pvpLockListener.onEntityDamageByEntity(event);
        return event.isCancelled();
    }

    private static EntityDamageByEntityEvent createEvent(ArenaMatchHarness.FakePlayer victim, Entity damager,
                                                         DamageCause cause) {
        return new EntityDamageByEntityEvent(damager, victim.player(), cause,
                createDamageSource(), new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, 1.0D)),
                new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(-0.0D))), false);
    }

    private static Arrow arrowShotBy(ArenaMatchHarness.FakePlayer shooter) {
        Player shootingPlayer = shooter.player();
        return ArenaMatchHarness.proxy(Arrow.class, (method, arguments) -> switch (method.getName()) {
            case "getShooter" -> shootingPlayer;
            default -> throw new UnsupportedOperationException("Arrow." + method.getName());
        });
    }

    private static ThrownPotion potionThrownBy(ArenaMatchHarness.FakePlayer thrower) {
        Player throwingPlayer = thrower.player();
        return ArenaMatchHarness.proxy(ThrownPotion.class, (method, arguments) -> switch (method.getName()) {
            case "getShooter" -> throwingPlayer;
            default -> throw new UnsupportedOperationException("ThrownPotion." + method.getName());
        });
    }

    private static AreaEffectCloud cloudOf(ArenaMatchHarness.FakePlayer owner) {
        Player owningPlayer = owner.player();
        return ArenaMatchHarness.proxy(AreaEffectCloud.class, (method, arguments) -> switch (method.getName()) {
            case "getSource" -> owningPlayer;
            default -> throw new UnsupportedOperationException("AreaEffectCloud." + method.getName());
        });
    }

    private static DamageSource createDamageSource() {
        return ArenaMatchHarness.proxy(DamageSource.class, (method, arguments) -> {
            throw new UnsupportedOperationException("DamageSource." + method.getName());
        });
    }
}

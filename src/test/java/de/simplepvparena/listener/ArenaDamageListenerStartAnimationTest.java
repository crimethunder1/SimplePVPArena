package de.simplepvparena.listener;

import com.google.common.base.Functions;
import de.simplepvparena.support.TestDamageEvents;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDamageListenerStartAnimationTest {

    private static final long SECOND = ArenaMatchHarness.TICKS_PER_SECOND;
    private static final double INSIDE_X = 5.5D;
    private static final double INSIDE_Z = 5.5D;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;
    private ArenaMatchHarness.FakePlayer admin;
    private ArenaDamageListener damageListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        harness.getMatchService().setCountdownSeconds(3);
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
        admin = harness.addPlayer("Admin");
        admin.grantAdminPermission();
        damageListener = new ArenaDamageListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), harness.getPlugin().getServer());
    }

    @Test
    void duringTheCountdownNoOpponentAdminOrOwnArrowHurtsAFighter() {
        start();

        assertTrue(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK), "melee of an opponent");
        assertTrue(isCancelled(fighter, arrowShotBy(opponent), DamageCause.PROJECTILE), "arrow of an opponent");
        assertTrue(isCancelled(fighter, admin.player(), DamageCause.ENTITY_ATTACK), "an admin");
        assertTrue(isCancelled(fighter, arrowShotBy(fighter), DamageCause.PROJECTILE), "the own arrow");
        assertTrue(isCancelled(opponent, fighter.player(), DamageCause.ENTITY_ATTACK), "the other way round");
        assertTrue(isCancelled(fighter, DamageCause.FALL), "fall");
        assertTrue(isCancelled(fighter, DamageCause.CUSTOM), "custom damage");
    }

    @Test
    void duringTheCountdownTheVoidAndAKillStillHurtAFighter() {
        start();

        assertFalse(isCancelled(fighter, DamageCause.VOID), "void");
        assertFalse(isCancelled(fighter, DamageCause.KILL), "kill");
        assertFalse(isCancelled(fighter, DamageCause.SUICIDE), "suicide");
    }

    @Test
    void untilTheLastTickOfTheCountdownNoFighterIsHurtAndFromFightOnTheRulesAreAsBefore() {
        start();
        harness.advanceTicks(3 * SECOND - 1);

        assertTrue(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK));
        assertTrue(isCancelled(fighter, admin.player(), DamageCause.ENTITY_ATTACK));

        harness.advanceTicks(1);

        assertFalse(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK), "an opponent");
        assertFalse(isCancelled(fighter, admin.player(), DamageCause.ENTITY_ATTACK), "an admin");
        assertFalse(isCancelled(fighter, DamageCause.CUSTOM), "sudden death");
        assertTrue(isCancelled(fighter, mate.player(), DamageCause.ENTITY_ATTACK), "a team mate as before");
    }

    @Test
    void withAGateOnlyTheLastThreeSecondsOfTheGateProtectTheFighters() {
        harness.setBlock(2, ArenaMatchHarness.GROUND_Y + 1, 3, Material.BARRIER);
        harness.getGateMarks().mark(ArenaMatchHarness.WORLD_NAME, 2, ArenaMatchHarness.GROUND_Y + 1, 3,
                Material.BARRIER);
        start();

        assertFalse(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK), "the gate is up as before");

        harness.advanceTicksWithRepeatingTasks(7 * SECOND);

        assertTrue(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK), "the last three seconds");

        harness.advanceTicksWithRepeatingTasks(3 * SECOND);

        assertFalse(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK), "fight");
    }

    @Test
    void withoutACountdownAFighterIsHurtFromTheStartAsBefore() {
        harness.getMatchService().setCountdownSeconds(0);
        start();

        assertFalse(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK));
        assertFalse(isCancelled(fighter, admin.player(), DamageCause.ENTITY_ATTACK));
    }

    @Test
    void aFighterWhoFellDuringTheCountdownIsTreatedAsASpectator() {
        start();
        fighter.moveTo(harness.at(INSIDE_X, INSIDE_Z));

        harness.getMatchService().handlePlayerEliminated(fighter.uniqueId());

        assertTrue(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK));
        assertFalse(isCancelled(fighter, DamageCause.VOID));
    }

    private void start() {
        harness.startMatch(fighter, mate, opponent, opponentMate);
    }

    private boolean isCancelled(ArenaMatchHarness.FakePlayer victim, DamageCause cause) {
        EntityDamageEvent event = TestDamageEvents.damage(victim.player(), cause, createDamageSource(), 1.0D);
        damageListener.onEntityDamage(event);
        return event.isCancelled();
    }

    private boolean isCancelled(ArenaMatchHarness.FakePlayer victim, Entity damager, DamageCause cause) {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(damager, victim.player(), cause,
                createDamageSource(), new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, 1.0D)),
                new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(-0.0D))), false);
        damageListener.onEntityDamage(event);
        return event.isCancelled();
    }

    private static Arrow arrowShotBy(ArenaMatchHarness.FakePlayer shooter) {
        Player shootingPlayer = shooter.player();
        return ArenaMatchHarness.proxy(Arrow.class, (method, arguments) -> switch (method.getName()) {
            case "getShooter" -> shootingPlayer;
            default -> throw new UnsupportedOperationException("Arrow." + method.getName());
        });
    }

    private static DamageSource createDamageSource() {
        return ArenaMatchHarness.proxy(DamageSource.class, (method, arguments) -> {
            throw new UnsupportedOperationException("DamageSource." + method.getName());
        });
    }
}

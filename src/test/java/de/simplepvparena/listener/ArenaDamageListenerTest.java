package de.simplepvparena.listener;

import com.google.common.base.Functions;
import de.simplepvparena.support.TestDamageEvents;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDamageListenerTest {

    private static final double INSIDE_X = 5.5D;
    private static final double INSIDE_Z = 5.5D;
    private static final double TRIBUNE_X = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;
    private static final double TRIBUNE_Z = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;
    private static final double OUTSIDE_X = ArenaMatchHarness.WALL_MAXIMUM + 20.5D;
    private static final double OUTSIDE_Z = ArenaMatchHarness.WALL_MAXIMUM + 20.5D;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer admin;
    private ArenaMatchHarness.FakePlayer spectator;
    private ArenaMatchHarness.FakePlayer bystander;
    private ArenaDamageListener damageListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        admin = harness.addPlayer("Admin");
        admin.grantAdminPermission();
        spectator = harness.addPlayer("Spectator");
        bystander = harness.addPlayer("Bystander");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        assignSpectatorOnTheTribune(spectator);
        damageListener = new ArenaDamageListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), harness.getPlugin().getServer());
    }

    @Test
    void protectsASpectatorOnTheTribuneFromEverythingButVoidAndKill() {
        assertTrue(isCancelled(spectator, DamageCause.FALL), "fall");
        assertTrue(isCancelled(spectator, DamageCause.FIRE), "fire");
        assertTrue(isCancelled(spectator, opponent.player(), DamageCause.ENTITY_ATTACK), "an opponent");
        assertTrue(isCancelled(spectator, admin.player(), DamageCause.ENTITY_ATTACK), "an admin");
        assertTrue(isCancelled(spectator, arrowShotBy(spectator), DamageCause.PROJECTILE), "the own arrow");

        assertFalse(isCancelled(spectator, DamageCause.VOID), "void");
        assertFalse(isCancelled(spectator, DamageCause.KILL), "kill");
    }

    @Test
    void treatsAPlayerWithoutAssignmentInsideTheFightAreaAsASpectator() {
        bystander.moveTo(harness.at(INSIDE_X, INSIDE_Z));

        assertTrue(isCancelled(bystander, DamageCause.FALL), "fall");
        assertTrue(isCancelled(bystander, opponent.player(), DamageCause.ENTITY_ATTACK), "an opponent");
        assertFalse(isCancelled(bystander, DamageCause.VOID), "void");
    }

    @Test
    void leavesAPlayerOutsideTheFightAreaWithoutAssignmentAlone() {
        bystander.moveTo(harness.at(OUTSIDE_X, OUTSIDE_Z));

        assertFalse(isCancelled(bystander, DamageCause.FALL), "fall");
        assertFalse(isCancelled(bystander, DamageCause.LAVA), "lava");
        assertFalse(isCancelled(bystander, admin.player(), DamageCause.ENTITY_ATTACK), "an admin");
        assertFalse(isCancelled(bystander, createEntity(Zombie.class), DamageCause.ENTITY_ATTACK), "a mob");
    }

    @Test
    void letsAnActiveFighterBeHurtByAnOpponentAnAdminAndTheOwnProjectile() {
        assertFalse(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK), "melee of an opponent");
        assertFalse(isCancelled(fighter, arrowShotBy(opponent), DamageCause.PROJECTILE), "arrow of an opponent");
        assertFalse(isCancelled(fighter, admin.player(), DamageCause.ENTITY_ATTACK), "an admin");
        assertFalse(isCancelled(fighter, arrowShotBy(fighter), DamageCause.PROJECTILE), "the own arrow");
    }

    @Test
    void keepsTeamMatesMobsAndPlayersOutsideTheMatchAwayFromAnActiveFighter() {
        assertTrue(isCancelled(fighter, mate.player(), DamageCause.ENTITY_ATTACK), "a team mate");
        assertTrue(isCancelled(fighter, arrowShotBy(mate), DamageCause.PROJECTILE), "the arrow of a team mate");
        assertTrue(isCancelled(fighter, createEntity(Zombie.class), DamageCause.ENTITY_ATTACK), "a mob");
        assertTrue(isCancelled(fighter, bystander.player(), DamageCause.ENTITY_ATTACK),
                "a player outside the match");
    }

    @Test
    void letsTheEnvironmentAndTheTntOfAnOpponentHurtAnActiveFighter() {
        assertFalse(isCancelled(fighter, DamageCause.FALL), "fall");
        assertFalse(isCancelled(fighter, DamageCause.LAVA), "lava");
        assertFalse(isCancelled(fighter, DamageCause.POISON), "poison");
        assertFalse(isCancelled(fighter, tntIgnitedBy(opponent), DamageCause.ENTITY_EXPLOSION),
                "the TNT of an opponent");
    }

    @Test
    void letsSuddenDeathAndVoidHurtAnActiveFighter() {
        assertFalse(isCancelled(fighter, DamageCause.CUSTOM), "sudden death");
        assertFalse(isCancelled(fighter, DamageCause.VOID), "void");
    }

    @Test
    void treatsAnEliminatedFighterInsideTheFightAreaAsASpectator() {
        fighter.moveTo(harness.at(INSIDE_X, INSIDE_Z));
        harness.getMatchService().handlePlayerEliminated(fighter.uniqueId());
        assertFalse(harness.getMatchService().isActiveFighter(fighter.uniqueId()));

        assertTrue(isCancelled(fighter, DamageCause.FALL), "fall");
        assertTrue(isCancelled(fighter, opponent.player(), DamageCause.ENTITY_ATTACK), "an opponent");
        assertTrue(isCancelled(fighter, DamageCause.CUSTOM), "sudden death");
        assertFalse(isCancelled(fighter, DamageCause.VOID), "void");
    }

    @Test
    void treatsAnEliminatedFighterOnTheTribuneAsASpectator() {
        harness.getMatchService().handlePlayerEliminated(fighter.uniqueId());
        assignSpectatorOnTheTribune(fighter);

        assertTrue(isCancelled(fighter, DamageCause.FIRE), "fire");
        assertFalse(isCancelled(fighter, DamageCause.KILL), "kill");
    }

    @Test
    void leavesAnEliminatedFighterOutsideWithoutAssignmentAlone() {
        harness.getMatchService().handlePlayerEliminated(fighter.uniqueId());
        fighter.moveTo(harness.at(OUTSIDE_X, OUTSIDE_Z));

        assertFalse(isCancelled(fighter, DamageCause.FALL), "fall");
    }

    @Test
    void neverLiftsACancellationOfAnotherListener() {
        EntityDamageEvent event = TestDamageEvents.damage(fighter.player(), DamageCause.CUSTOM,
                createDamageSource(), 1.0D);
        event.setCancelled(true);

        damageListener.onEntityDamage(event);

        assertTrue(event.isCancelled());
    }

    private void assignSpectatorOnTheTribune(ArenaMatchHarness.FakePlayer player) {
        assertTrue(harness.getSpectatorService().enterArenaFlight(player.player()));
        player.moveTo(harness.at(TRIBUNE_X, TRIBUNE_Z));
        assertTrue(harness.getSpectatorService().findSpectatorMode(player.uniqueId()).isPresent());
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

    private static TNTPrimed tntIgnitedBy(ArenaMatchHarness.FakePlayer igniter) {
        Player ignitingPlayer = igniter.player();
        return ArenaMatchHarness.proxy(TNTPrimed.class, (method, arguments) -> switch (method.getName()) {
            case "getSource" -> ignitingPlayer;
            default -> throw new UnsupportedOperationException("TNTPrimed." + method.getName());
        });
    }

    private static DamageSource createDamageSource() {
        return ArenaMatchHarness.proxy(DamageSource.class, (method, arguments) -> {
            throw new UnsupportedOperationException("DamageSource." + method.getName());
        });
    }

    private static <T extends Entity> T createEntity(Class<T> type) {
        return ArenaMatchHarness.proxy(type, (method, arguments) -> {
            throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName());
        });
    }
}

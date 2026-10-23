package de.simplepvparena.listener;

import com.google.common.base.Functions;
import de.simplepvparena.support.TestDamageEvents;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDamageListenerFightCausesTest {

    private static final List<DamageCause> FIGHT_CAUSES = List.of(
            DamageCause.LAVA, DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.HOT_FLOOR, DamageCause.CAMPFIRE,
            DamageCause.CONTACT, DamageCause.FALL, DamageCause.DROWNING, DamageCause.SUFFOCATION,
            DamageCause.FLY_INTO_WALL, DamageCause.FALLING_BLOCK, DamageCause.BLOCK_EXPLOSION,
            DamageCause.ENTITY_EXPLOSION, DamageCause.MAGIC, DamageCause.POISON, DamageCause.WITHER,
            DamageCause.THORNS, DamageCause.DRAGON_BREATH, DamageCause.SONIC_BOOM, DamageCause.STARVATION,
            DamageCause.FREEZE, DamageCause.LIGHTNING, DamageCause.ENTITY_SWEEP_ATTACK, DamageCause.ENTITY_ATTACK,
            DamageCause.PROJECTILE);
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
    private ArenaMatchHarness.FakePlayer opponentMate;
    private ArenaMatchHarness.FakePlayer spectator;
    private ArenaDamageListener damageListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
        spectator = harness.addPlayer("Spectator");
        damageListener = new ArenaDamageListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), harness.getPlugin().getServer());
    }

    @Test
    void listsEveryCauseOfAFightInTheArena() {
        assertEquals(25, FIGHT_CAUSES.size());
    }

    @Test
    void everyCauseOfAFightHurtsAnActiveFighterOfTheRunningRound() {
        harness.startMatch(fighter, mate, opponent, opponentMate);

        for (DamageCause cause : FIGHT_CAUSES) {
            assertFalse(isCancelled(fighter, cause), cause.name());
        }
    }

    @Test
    void lavaFireAndExplosionsBehindAnOpponentOrWithoutAnyoneHurtAnActiveFighter() {
        harness.startMatch(fighter, mate, opponent, opponentMate);

        assertFalse(isCancelled(fighter, DamageCause.LAVA), "lava placed by an opponent");
        assertFalse(isCancelled(fighter, DamageCause.FIRE), "fire");
        assertFalse(isCancelled(fighter, DamageCause.FIRE_TICK), "burning");
        assertFalse(isCancelled(fighter, tntIgnitedBy(opponent.player()), DamageCause.ENTITY_EXPLOSION),
                "the TNT of an opponent");
        assertFalse(isCancelled(fighter, tntIgnitedBy(null), DamageCause.ENTITY_EXPLOSION),
                "TNT set off by redstone");
        assertFalse(isCancelled(fighter, createEntity(EnderCrystal.class), DamageCause.ENTITY_EXPLOSION),
                "an end crystal");
        assertFalse(isCancelled(fighter, createEntity(FallingBlock.class), DamageCause.FALLING_BLOCK),
                "a falling anvil");
        assertFalse(isCancelled(fighter, createEntity(LightningStrike.class), DamageCause.LIGHTNING),
                "a lightning strike");
    }

    @Test
    void mobsTeamMatesAndOtherCausesStayAwayFromAnActiveFighter() {
        harness.startMatch(fighter, mate, opponent, opponentMate);

        assertTrue(isCancelled(fighter, createEntity(Creeper.class), DamageCause.ENTITY_EXPLOSION), "a creeper");
        assertTrue(isCancelled(fighter, arrowShotBy(createEntity(Skeleton.class)), DamageCause.PROJECTILE),
                "the arrow of a skeleton");
        assertTrue(isCancelled(fighter, tntIgnitedBy(mate.player()), DamageCause.ENTITY_EXPLOSION),
                "the TNT of a team mate");
        assertTrue(isCancelled(fighter, DamageCause.CRAMMING), "cramming");
        assertTrue(isCancelled(fighter, DamageCause.WORLD_BORDER), "the world border");
    }

    @Test
    void noCauseOfAFightHurtsASpectatorOnTheTribune() {
        harness.startMatch(fighter, mate, opponent, opponentMate);
        assertTrue(harness.getSpectatorService().enterArenaFlight(spectator.player()));
        spectator.moveTo(harness.at(TRIBUNE_X, TRIBUNE_Z));

        for (DamageCause cause : FIGHT_CAUSES) {
            assertTrue(isCancelled(spectator, cause), cause.name());
        }
    }

    @Test
    void noCauseOfAFightHurtsAFighterDuringTheStartAnimation() {
        harness.getMatchService().setCountdownSeconds(3);
        harness.startMatch(fighter, mate, opponent, opponentMate);
        assertTrue(harness.getMatchService().isStartAnimationRunning());

        for (DamageCause cause : FIGHT_CAUSES) {
            assertTrue(isCancelled(fighter, cause), cause.name());
        }
    }

    @Test
    void withoutARunningRoundAPlayerInTheFightAreaCountsAsSpectatorAndAPlayerOutsideIsLeftAlone() {
        fighter.moveTo(harness.at(INSIDE_X, INSIDE_Z));
        opponent.moveTo(harness.at(OUTSIDE_X, OUTSIDE_Z));

        for (DamageCause cause : FIGHT_CAUSES) {
            assertTrue(isCancelled(fighter, cause), "inside the fight area: " + cause.name());
            assertFalse(isCancelled(opponent, cause), "outside the fight area: " + cause.name());
        }
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

    private static Arrow arrowShotBy(Entity shooter) {
        return ArenaMatchHarness.proxy(Arrow.class, (method, arguments) -> switch (method.getName()) {
            case "getShooter" -> shooter;
            default -> throw new UnsupportedOperationException("Arrow." + method.getName());
        });
    }

    private static TNTPrimed tntIgnitedBy(Player igniter) {
        return ArenaMatchHarness.proxy(TNTPrimed.class, (method, arguments) -> switch (method.getName()) {
            case "getSource" -> igniter;
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

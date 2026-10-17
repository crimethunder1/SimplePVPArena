package de.simplepvparena.listener;

import com.google.common.base.Functions;
import de.simplepvparena.arena.EffectArea;
import org.bukkit.Location;
import org.bukkit.World;
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

class GlobalPvpLockEffectAreaTest {

    private static final double FAR_X = 300.5D;
    private static final double FAR_Z = 5.5D;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer admin;
    private ArenaMatchHarness.FakePlayer bystander;
    private ArenaMatchHarness.FakePlayer otherBystander;
    private GlobalPvpLockListener pvpLockListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        admin = harness.addPlayer("Admin");
        admin.grantAdminPermission();
        bystander = harness.addPlayer("Bystander");
        otherBystander = harness.addPlayer("OtherBystander");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        pvpLockListener = new GlobalPvpLockListener(harness.getMatchService(), harness.getArenaRegistry(),
                harness.getPlugin().getServer());
    }

    @Test
    void locksAVictimInsideTheEffectAreaAsBefore() {
        assertTrue(harness.getArenaRegistry().findEffectArea().orElseThrow().contains(bystander.location()),
                "the harness lets every player join on its tribune");

        assertTrue(isCancelled(bystander, otherBystander.player(), DamageCause.ENTITY_ATTACK), "melee");
        assertTrue(isCancelled(bystander, arrowShotBy(otherBystander), DamageCause.PROJECTILE), "arrow");
        assertTrue(pvpLockListener.isLockedTarget(otherBystander.uniqueId(), bystander.player()), "potion or cloud");
        assertTrue(isCancelled(fighter, bystander.player(), DamageCause.ENTITY_ATTACK), "a fighter in the arena");
    }

    @Test
    void decidesByThePositionOfTheVictimAndNotOfTheAttacker() {
        otherBystander.moveTo(harness.at(FAR_X, FAR_Z));

        assertTrue(isCancelled(bystander, arrowShotBy(otherBystander), DamageCause.PROJECTILE),
                "an arrow from far outside onto the tribune");

        bystander.moveTo(harness.at(FAR_X + 1.0D, FAR_Z));
        otherBystander.moveTo(harness.at(15.5D, 15.5D));

        assertFalse(isCancelled(bystander, arrowShotBy(otherBystander), DamageCause.PROJECTILE),
                "and an arrow from the tribune onto a player far outside");
    }

    @Test
    void leavesAVictimOutsideOfTheEffectAreaAlone() {
        bystander.moveTo(harness.at(FAR_X, FAR_Z));
        otherBystander.moveTo(harness.at(FAR_X + 1.0D, FAR_Z));

        assertFalse(isCancelled(bystander, otherBystander.player(), DamageCause.ENTITY_ATTACK), "melee");
        assertFalse(isCancelled(bystander, arrowShotBy(otherBystander), DamageCause.PROJECTILE), "arrow");
        assertFalse(pvpLockListener.isLockedTarget(otherBystander.uniqueId(), bystander.player()),
                "potion or cloud");
        assertFalse(isCancelled(bystander, admin.player(), DamageCause.ENTITY_ATTACK), "an admin as well");
        assertFalse(isCancelled(bystander, fighter.player(), DamageCause.ENTITY_ATTACK),
                "and a fighter of the running round");
    }

    @Test
    void locksUpToTheEdgeOfTheEffectArea() {
        EffectArea effectArea = harness.getArenaRegistry().findEffectArea().orElseThrow();
        double edgeX = effectArea.getCenter().x() + effectArea.getRadius();
        double centreZ = effectArea.getCenter().z();

        bystander.moveTo(harness.at(edgeX - 0.1D, centreZ));
        assertTrue(isCancelled(bystander, otherBystander.player(), DamageCause.ENTITY_ATTACK), "just inside");

        bystander.moveTo(harness.at(edgeX + 0.1D, centreZ));
        assertFalse(isCancelled(bystander, otherBystander.player(), DamageCause.ENTITY_ATTACK), "just outside");
    }

    @Test
    void leavesAVictimInAnotherWorldAlone() {
        bystander.moveTo(new Location(lobbyWorld(), 5.5D, ArenaMatchHarness.GROUND_Y, 5.5D));

        assertFalse(isCancelled(bystander, otherBystander.player(), DamageCause.ENTITY_ATTACK));
        assertFalse(pvpLockListener.isLockedTarget(otherBystander.uniqueId(), bystander.player()));
    }

    @Test
    void locksNothingWhileNoFightAreaIsCalculated() {
        harness.getArenaRegistry().getArena().setShape(null);

        assertFalse(isCancelled(bystander, otherBystander.player(), DamageCause.ENTITY_ATTACK));
        assertFalse(pvpLockListener.isLockedTarget(otherBystander.uniqueId(), bystander.player()));
    }

    @Test
    void decidesEveryPlayerAPotionOrACloudReachesOnItsOwn() {
        ArenaMatchHarness.FakePlayer playerOutside = harness.addPlayer("Outside");
        playerOutside.moveTo(harness.at(FAR_X, FAR_Z));

        assertTrue(pvpLockListener.isLockedTarget(otherBystander.uniqueId(), bystander.player()),
                "the player on the tribune is spared");
        assertFalse(pvpLockListener.isLockedTarget(otherBystander.uniqueId(), playerOutside.player()),
                "while the player far outside takes the effect");
        assertFalse(pvpLockListener.isLockedTarget(admin.uniqueId(), fighter.player()),
                "and the admin still reaches an active fighter");
        assertTrue(pvpLockListener.isLockedTarget(admin.uniqueId(), bystander.player()),
                "but not a player outside the match inside the area");
    }

    @Test
    void keepsTheAdminExceptionInsideTheEffectArea() {
        assertFalse(isCancelled(fighter, admin.player(), DamageCause.ENTITY_ATTACK), "melee");
        assertFalse(isCancelled(fighter, arrowShotBy(admin), DamageCause.PROJECTILE), "arrow");
        assertTrue(isCancelled(bystander, admin.player(), DamageCause.ENTITY_ATTACK),
                "and no exception against a player outside the match");
    }

    private boolean isCancelled(ArenaMatchHarness.FakePlayer victim, Entity damager, DamageCause cause) {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(damager, victim.player(), cause,
                createDamageSource(), new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, 1.0D)),
                new EnumMap<>(Map.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(-0.0D))), false);
        pvpLockListener.onEntityDamageByEntity(event);
        return event.isCancelled();
    }

    private static Arrow arrowShotBy(ArenaMatchHarness.FakePlayer shooter) {
        Player shootingPlayer = shooter.player();
        return ArenaMatchHarness.proxy(Arrow.class, (method, arguments) -> switch (method.getName()) {
            case "getShooter" -> shootingPlayer;
            default -> throw new UnsupportedOperationException("Arrow." + method.getName());
        });
    }

    private static World lobbyWorld() {
        return ArenaMatchHarness.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> ArenaMatchHarness.LOBBY_WORLD_NAME;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
    }

    private static DamageSource createDamageSource() {
        return ArenaMatchHarness.proxy(DamageSource.class, (method, arguments) -> {
            throw new UnsupportedOperationException("DamageSource." + method.getName());
        });
    }
}

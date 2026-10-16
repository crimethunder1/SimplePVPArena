package de.simplepvparena.listener;

import de.simplepvparena.support.FakeBlockData;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaMobRuleTest {

    private static final int INSIDE_X = 5;
    private static final int INSIDE_Z = 5;
    private static final int TRIBUNE_X = ArenaMatchHarness.WALL_MAXIMUM + 5;
    private static final int TRIBUNE_Z = ArenaMatchHarness.WALL_MAXIMUM + 5;

    private ArenaMatchHarness harness;
    private ArenaMobRuleListener mobRuleListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        mobRuleListener = new ArenaMobRuleListener(harness.getArenaRegistry());
    }

    @Test
    void keepsEveryKindOfSpawnOutOfTheFightArea() {
        Location insideTheFightArea = harness.at(INSIDE_X + 0.5D, INSIDE_Z + 0.5D);

        for (CreatureSpawnEvent.SpawnReason spawnReason : List.of(CreatureSpawnEvent.SpawnReason.NATURAL,
                CreatureSpawnEvent.SpawnReason.SPAWNER_EGG, CreatureSpawnEvent.SpawnReason.SPAWNER,
                CreatureSpawnEvent.SpawnReason.BREEDING, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            assertTrue(isSpawnCancelled(insideTheFightArea, spawnReason), spawnReason.name());
        }
        assertTrue(isSpawnCancelled(new Location(harness.getWorld(), INSIDE_X + 0.5D,
                ArenaMatchHarness.WALL_TOP_Y + 20.5D, INSIDE_Z + 0.5D), CreatureSpawnEvent.SpawnReason.NATURAL),
                "twenty blocks above the wall top");
    }

    @Test
    void letsMobsSpawnOutsideTheFightArea() {
        assertFalse(isSpawnCancelled(harness.at(TRIBUNE_X + 0.5D, TRIBUNE_Z + 0.5D),
                CreatureSpawnEvent.SpawnReason.NATURAL), "on the tribune");
        assertFalse(isSpawnCancelled(new Location(harness.getWorld(), INSIDE_X + 0.5D,
                ArenaMatchHarness.GROUND_Y - 1.0D, INSIDE_Z + 0.5D), CreatureSpawnEvent.SpawnReason.NATURAL),
                "under the floor");
        assertFalse(isSpawnCancelled(new Location(harness.getWorld(), INSIDE_X + 0.5D,
                ArenaMatchHarness.WALL_TOP_Y + 21.0D, INSIDE_Z + 0.5D), CreatureSpawnEvent.SpawnReason.NATURAL),
                "above the searched height");
        World lobbyWorld = harness.createBlockInTheLobby(INSIDE_X, ArenaMatchHarness.GROUND_Y, INSIDE_Z).getWorld();
        assertFalse(isSpawnCancelled(new Location(lobbyWorld, INSIDE_X + 0.5D, ArenaMatchHarness.GROUND_Y,
                INSIDE_Z + 0.5D), CreatureSpawnEvent.SpawnReason.NATURAL), "in another world");
    }

    @Test
    void letsMobsSpawnAnywhereWhileNoFightAreaIsCalculated() {
        harness.getArenaRegistry().getArena().setShape(null);

        assertFalse(isSpawnCancelled(harness.at(INSIDE_X + 0.5D, INSIDE_Z + 0.5D),
                CreatureSpawnEvent.SpawnReason.SPAWNER_EGG));
    }

    @Test
    void keepsAMobFromChangingABlockInsideTheFightArea() {
        assertTrue(isChangeCancelled(createEntity(Enderman.class),
                harness.createBlock(INSIDE_X, ArenaMatchHarness.GROUND_Y, INSIDE_Z)));
    }

    @Test
    void leavesBlockChangesByOtherEntitiesAndOutsideTheFightAreaToTheOtherRules() {
        Block insideTheFightArea = harness.createBlock(INSIDE_X, ArenaMatchHarness.GROUND_Y + 1, INSIDE_Z);
        ArenaMatchHarness.FakePlayer player = harness.addPlayer("Player");

        assertFalse(isChangeCancelled(createEntity(FallingBlock.class), insideTheFightArea), "a falling block");
        assertFalse(isChangeCancelled(player.player(), insideTheFightArea), "a player");
        assertFalse(isChangeCancelled(createEntity(Enderman.class),
                harness.createBlock(TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z)),
                "the tribune is left to the environment protection");
    }

    @Test
    void takesEveryBlockFromTheExplosionOfAMobInsideTheFightArea() {
        EntityExplodeEvent event = explode(createEntity(Creeper.class), harness.at(INSIDE_X + 0.5D, INSIDE_Z + 0.5D));

        assertTrue(event.blockList().isEmpty(), "the creeper destroys no block: " + event.blockList());
        assertFalse(event.isCancelled(), "the explosion itself still goes off");
    }

    @Test
    void leavesTheBlocksOfOtherExplosionsAlone() {
        EntityExplodeEvent tntExplosion = explode(createEntity(TNTPrimed.class),
                harness.at(INSIDE_X + 0.5D, INSIDE_Z + 0.5D));
        EntityExplodeEvent creeperOnTheTribune = explode(createEntity(Creeper.class),
                harness.at(TRIBUNE_X + 0.5D, TRIBUNE_Z + 0.5D));

        assertEquals(2, tntExplosion.blockList().size(), "TNT inside the fight area");
        assertEquals(2, creeperOnTheTribune.blockList().size(), "a creeper outside the fight area");
    }

    @Test
    void letsAnArmorStandBePlacedInsideTheFightArea() {
        Location insideTheFightArea = harness.at(INSIDE_X + 0.5D, INSIDE_Z + 0.5D);

        for (CreatureSpawnEvent.SpawnReason spawnReason : List.of(CreatureSpawnEvent.SpawnReason.DEFAULT,
                CreatureSpawnEvent.SpawnReason.SPAWNER_EGG, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            assertFalse(isSpawnCancelled(ArmorStand.class, insideTheFightArea, spawnReason),
                    "an armor stand: " + spawnReason.name());
            assertTrue(isSpawnCancelled(Cow.class, insideTheFightArea, spawnReason),
                    "a cow: " + spawnReason.name());
        }
    }

    private boolean isSpawnCancelled(Location location, CreatureSpawnEvent.SpawnReason spawnReason) {
        return isSpawnCancelled(Cow.class, location, spawnReason);
    }

    private boolean isSpawnCancelled(Class<? extends LivingEntity> type, Location location,
                                     CreatureSpawnEvent.SpawnReason spawnReason) {
        LivingEntity spawnedEntity = ArenaMatchHarness.proxy(type, (method, arguments) -> switch (method.getName()) {
            case "getLocation" -> location.clone();
            default -> throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName());
        });
        CreatureSpawnEvent event = new CreatureSpawnEvent(spawnedEntity, spawnReason);
        mobRuleListener.onCreatureSpawn(event);
        return event.isCancelled();
    }

    private boolean isChangeCancelled(Entity entity, Block changedBlock) {
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(entity, changedBlock,
                FakeBlockData.of(Material.AIR));
        mobRuleListener.onEntityChangeBlock(event);
        return event.isCancelled();
    }

    private EntityExplodeEvent explode(Entity explodingEntity, Location location) {
        EntityExplodeEvent event = new EntityExplodeEvent(explodingEntity, location,
                new ArrayList<>(List.of(harness.createBlock(INSIDE_X, ArenaMatchHarness.GROUND_Y, INSIDE_Z),
                        harness.createBlock(INSIDE_X + 1, ArenaMatchHarness.GROUND_Y, INSIDE_Z))),
                1.0F, ExplosionResult.DESTROY);
        mobRuleListener.onEntityExplode(event);
        return event;
    }

    private static <T extends Entity> T createEntity(Class<T> type) {
        return ArenaMatchHarness.proxy(type, (method, arguments) -> {
            throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName());
        });
    }
}

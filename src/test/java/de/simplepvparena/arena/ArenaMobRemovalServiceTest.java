package de.simplepvparena.arena;

import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaMobRemovalServiceTest {

    private static final String WORLD_NAME = "event";
    private static final int FLOOR_Y = 64;
    private static final int WALL_TOP_Y = 66;
    private static final int HIGHEST_SEARCHED_Y = WALL_TOP_Y + 20;

    private final FakeServerHarness harness = new FakeServerHarness(WORLD_NAME);
    private final World world = harness.world();
    private final ArenaRegistry arenaRegistry = new ArenaRegistry();
    private final Set<Entity> removedEntities = new HashSet<>();
    private final ArenaMobRemovalService service = new ArenaMobRemovalService(harness.getPlugin(), arenaRegistry);

    private boolean locationLookupFailing;

    ArenaMobRemovalServiceTest() {
        arenaRegistry.getArena().setShape(new ArenaShape(WORLD_NAME, FLOOR_Y, FLOOR_Y, WALL_TOP_Y, new long[] {
                HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0), HorizontalCellCodec.encode(2, 0),
                HorizontalCellCodec.encode(0, 1), HorizontalCellCodec.encode(0, 2)}));
    }

    @Test
    void removesCowsAndZombiesInsideTheFightAreaAndCountsThem() {
        Cow cow = spawn(Cow.class, 0.5D, FLOOR_Y, 0.5D);
        Zombie zombie = spawn(Zombie.class, 2.5D, HIGHEST_SEARCHED_Y + 0.5D, 0.5D);

        int removedMobCount = service.removeMobsInTheFightArea();

        assertEquals(2, removedMobCount);
        assertEquals(Set.of(cow, zombie), removedEntities);
        assertEquals(1, harness.getSearchedBoxes().size());
        BoundingBox searchedBox = harness.getSearchedBoxes().get(0);
        assertEquals(new BoundingBox(0.0D, FLOOR_Y, 0.0D, 3.0D, HIGHEST_SEARCHED_Y + 1.0D, 3.0D), searchedBox,
                "only the frame of the fight area up to twenty blocks above the wall is searched");
    }

    @Test
    void leavesPlayersArmorStandsAndMobsOutsideTheFightAreaAlone() {
        spawn(Player.class, 0.5D, FLOOR_Y, 0.5D);
        spawn(ArmorStand.class, 1.5D, FLOOR_Y, 0.5D);
        spawn(Cow.class, 2.5D, FLOOR_Y, 2.5D);
        spawn(Zombie.class, 0.5D, FLOOR_Y - 0.5D, 0.5D);
        spawn(Cow.class, 0.5D, HIGHEST_SEARCHED_Y + 1.0D, 0.5D);

        int removedMobCount = service.removeMobsInTheFightArea();

        assertEquals(0, removedMobCount);
        assertTrue(removedEntities.isEmpty(), "nothing may be removed: " + removedEntities);
    }

    @Test
    void doesNothingWhenTheWorldIsNotLoaded() {
        spawn(Cow.class, 0.5D, FLOOR_Y, 0.5D);
        harness.unloadWorld();

        assertEquals(0, service.removeMobsInTheFightArea());
        assertTrue(removedEntities.isEmpty());
        assertTrue(harness.getSearchedBoxes().isEmpty(), "no world is searched");
    }

    @Test
    void doesNothingWhileNoFightAreaIsCalculated() {
        spawn(Cow.class, 0.5D, FLOOR_Y, 0.5D);
        arenaRegistry.getArena().setShape(null);

        assertEquals(0, service.removeMobsInTheFightArea());
        assertTrue(removedEntities.isEmpty());
        assertTrue(harness.getSearchedBoxes().isEmpty(), "no world is searched");
    }

    @Test
    void startPlansOneRepeatingTaskAndStopEndsIt() {
        Cow firstCow = spawn(Cow.class, 0.5D, FLOOR_Y, 0.5D);

        service.start();
        service.start();

        assertTrue(service.isRunning());
        assertEquals(1, harness.getScheduledTaskCount(), "a second start plans no second task");
        harness.runTick();
        assertEquals(Set.of(firstCow), removedEntities);

        Cow secondCow = spawn(Cow.class, 1.5D, FLOOR_Y, 0.5D);
        harness.runTick();
        assertTrue(removedEntities.contains(secondCow), "the task keeps running");

        service.stop();
        Cow thirdCow = spawn(Cow.class, 2.5D, FLOOR_Y, 0.5D);
        harness.runTick();

        assertFalse(service.isRunning());
        assertEquals(0, harness.getScheduledTaskCount(), "the task is cancelled");
        assertFalse(removedEntities.contains(thirdCow), "nothing is removed after the stop");
    }

    @Test
    void reportsAFailedRunAsAWarningAndKeepsRunning() {
        Zombie zombie = spawn(Zombie.class, 0.5D, FLOOR_Y, 0.5D);
        locationLookupFailing = true;
        List<LogRecord> logRecords = new ArrayList<>();
        Handler recordingHandler = recordingHandler(logRecords);
        Logger logger = harness.getPlugin().getLogger();
        logger.addHandler(recordingHandler);
        try {
            service.start();
            harness.runTick();

            assertTrue(logRecords.stream().anyMatch(logRecord -> logRecord.getLevel() == Level.WARNING
                            && logRecord.getMessage().contains("mobs in the fight area could not be removed")
                            && logRecord.getThrown() instanceof IllegalStateException),
                    "the failure is logged as a warning with the exception");
            assertTrue(service.isRunning());
            assertEquals(1, harness.getScheduledTaskCount(), "the task is still planned");

            locationLookupFailing = false;
            harness.runTick();

            assertEquals(Set.of(zombie), removedEntities, "the next run removes the mob");
        } finally {
            logger.removeHandler(recordingHandler);
            service.stop();
        }
    }

    private <T extends Entity> T spawn(Class<T> type, double x, double y, double z) {
        Location location = new Location(world, x, y, z);
        Entity[] self = new Entity[1];
        T entity = SharedBukkitServer.proxy(type, (method, arguments) -> switch (method.getName()) {
            case "getLocation" -> {
                if (locationLookupFailing) {
                    throw new IllegalStateException("the location could not be read");
                }
                yield location.clone();
            }
            case "remove" -> {
                removedEntities.add(self[0]);
                yield null;
            }
            default -> throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName());
        });
        self[0] = entity;
        harness.addNearbyEntity(entity);
        return entity;
    }

    private static Handler recordingHandler(List<LogRecord> logRecords) {
        return new Handler() {

            @Override
            public void publish(LogRecord logRecord) {
                logRecords.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }
}

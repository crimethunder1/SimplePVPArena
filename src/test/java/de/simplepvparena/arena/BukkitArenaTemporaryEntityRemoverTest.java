package de.simplepvparena.arena;

import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WindCharge;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitArenaTemporaryEntityRemoverTest {

    private static final String WORLD_NAME = "event";
    private static final int FLOOR_Y = 64;
    private static final int WALL_TOP_Y = 66;
    private static final int HIGHEST_SEARCHED_Y = WALL_TOP_Y + 20;

    private final ArenaShape shape = new ArenaShape(WORLD_NAME, FLOOR_Y, FLOOR_Y, WALL_TOP_Y, new long[] {
            HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0), HorizontalCellCodec.encode(2, 0),
            HorizontalCellCodec.encode(0, 1), HorizontalCellCodec.encode(0, 2)});
    private final List<Entity> entities = new ArrayList<>();
    private final Set<Entity> removedEntities = new HashSet<>();
    private final List<BoundingBox> searchedBoxes = new ArrayList<>();
    private final World world = SharedBukkitServer.proxy(World.class, (method, arguments) ->
            switch (method.getName()) {
                case "getName" -> WORLD_NAME;
                case "getNearbyEntities" -> {
                    searchedBoxes.add((BoundingBox) arguments[0]);
                    yield new ArrayList<>(entities);
                }
                default -> throw new UnsupportedOperationException("World." + method.getName());
            });
    private final BukkitArenaTemporaryEntityRemover remover = new BukkitArenaTemporaryEntityRemover();

    @Test
    void removesEveryKindOfTemporaryEntityInsideTheFightAreaAndCountsThem() {
        List<Entity> temporaryEntities = List.of(
                spawn(Item.class, 0.5D, FLOOR_Y, 0.5D),
                spawn(ExperienceOrb.class, 1.5D, FLOOR_Y + 0.25D, 0.5D),
                spawn(Arrow.class, 2.5D, FLOOR_Y + 1.0D, 0.5D),
                spawn(Trident.class, 0.5D, FLOOR_Y + 1.5D, 1.5D),
                spawn(Snowball.class, 0.5D, FLOOR_Y + 2.0D, 2.5D),
                spawn(EnderPearl.class, 0.5D, FLOOR_Y + 3.0D, 0.5D),
                spawn(Fireball.class, 1.5D, FLOOR_Y + 4.0D, 0.5D),
                spawn(ThrownPotion.class, 2.5D, FLOOR_Y + 5.0D, 0.5D),
                spawn(WindCharge.class, 0.5D, FLOOR_Y + 6.0D, 1.5D),
                spawn(FishHook.class, 0.5D, FLOOR_Y + 0.1D, 2.5D),
                spawn(Firework.class, 0.5D, HIGHEST_SEARCHED_Y - 1.0D, 0.5D),
                spawn(TNTPrimed.class, 1.5D, FLOOR_Y, 0.5D),
                spawn(FallingBlock.class, 2.5D, HIGHEST_SEARCHED_Y + 0.9D, 0.5D),
                spawn(EnderCrystal.class, 0.5D, FLOOR_Y, 1.5D),
                spawn(AreaEffectCloud.class, 0.5D, FLOOR_Y, 2.5D));

        int removedEntityCount = remover.removeTemporaryEntities(world, shape);

        assertEquals(temporaryEntities.size(), removedEntityCount);
        assertEquals(Set.copyOf(temporaryEntities), removedEntities);
    }

    @Test
    void searchesOnlyTheFrameOfTheFightAreaFromTheFloorToTwentyBlocksAboveTheWall() {
        remover.removeTemporaryEntities(world, shape);

        assertEquals(1, searchedBoxes.size());
        BoundingBox searchedBox = searchedBoxes.get(0);
        assertEquals(0.0D, searchedBox.getMinX());
        assertEquals(FLOOR_Y, searchedBox.getMinY());
        assertEquals(0.0D, searchedBox.getMinZ());
        assertEquals(3.0D, searchedBox.getMaxX());
        assertEquals(HIGHEST_SEARCHED_Y + 1.0D, searchedBox.getMaxY());
        assertEquals(3.0D, searchedBox.getMaxZ());
    }

    @Test
    void leavesPlayersDecorationsVehiclesAndAnimalsAndEverythingOutsideTheFightAreaAlone() {
        spawn(Player.class, 0.5D, FLOOR_Y, 0.5D);
        spawn(ArmorStand.class, 1.5D, FLOOR_Y, 0.5D);
        spawn(ItemFrame.class, 2.5D, FLOOR_Y + 1.0D, 0.5D);
        spawn(Painting.class, 0.5D, FLOOR_Y + 1.0D, 1.5D);
        spawn(Boat.class, 0.5D, FLOOR_Y, 2.5D);
        spawn(Minecart.class, 0.5D, FLOOR_Y, 1.5D);
        spawn(Cow.class, 1.5D, FLOOR_Y, 0.5D);
        spawn(Item.class, 2.5D, FLOOR_Y, 2.5D);
        spawn(Item.class, 0.5D, FLOOR_Y - 0.5D, 0.5D);
        spawn(Item.class, 0.5D, HIGHEST_SEARCHED_Y + 1.0D, 0.5D);

        int removedEntityCount = remover.removeTemporaryEntities(world, shape);

        assertEquals(0, removedEntityCount);
        assertTrue(removedEntities.isEmpty(), "nothing may be removed: " + removedEntities);
    }

    @Test
    void doesNothingWhenTheWorldIsNotLoaded() {
        spawn(Item.class, 0.5D, FLOOR_Y, 0.5D);

        int removedEntityCount = remover.removeTemporaryEntities(null, shape);

        assertEquals(0, removedEntityCount);
        assertTrue(removedEntities.isEmpty());
        assertTrue(searchedBoxes.isEmpty(), "no world is searched");
    }

    private <T extends Entity> T spawn(Class<T> type, double x, double y, double z) {
        Location location = new Location(world, x, y, z);
        Entity[] self = new Entity[1];
        T entity = SharedBukkitServer.proxy(type, (method, arguments) -> switch (method.getName()) {
            case "getLocation" -> location.clone();
            case "remove" -> {
                removedEntities.add(self[0]);
                yield null;
            }
            default -> throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName());
        });
        self[0] = entity;
        entities.add(entity);
        return entity;
    }
}

package de.simplepvparena.registration;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import de.simplepvparena.listener.RegistrationStationWorldListener;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationStationWorldListenerTest {

    private static final String WORLD_NAME = "event";
    private static final RegistrationStation STATION = new RegistrationStation(WORLD_NAME, 10, 64, 20);

    private final Map<List<Integer>, Material> materials = new HashMap<>();
    private final World world = SharedBukkitServer.proxy(World.class, (method, arguments) ->
            switch (method.getName()) {
                case "getName" -> WORLD_NAME;
                case "isChunkLoaded" -> true;
                case "getBlockAt" -> blockAt((Integer) arguments[0], (Integer) arguments[1], (Integer) arguments[2]);
                default -> throw new UnsupportedOperationException("World." + method.getName());
            });
    private final RegistrationStations stations = new RegistrationStations();
    private final List<String> displayCalls = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<Runnable> nextTickTasks = new ArrayList<>();
    private final ListenerFixture fixture = new ListenerFixture();

    @Test
    void loadingAChunkRemovesAStationWithoutButtonBeforeAnyDisplayIsShown() {
        addStation(Material.AIR);

        fixture.listener.onChunkLoad(new ChunkLoadEvent(chunk(), false));

        assertFalse(stations.isStation(STATION));
        assertEquals(List.of(), displayCalls);
        assertEquals(1, warnings.size());
    }

    @Test
    void loadingAChunkShowsTheDisplayOfAStationWithButtonAndUnloadingTakesItAway() {
        addStation(Material.STONE_BUTTON);

        fixture.listener.onChunkLoad(new ChunkLoadEvent(chunk(), false));
        fixture.listener.onChunkUnload(new ChunkUnloadEvent(chunk()));

        assertEquals(List.of("text at 10.5 65.7 20.5", "remove text at 10.5 65.7 20.5"), displayCalls);
        assertTrue(stations.isStation(STATION));
    }

    @Test
    void physicsBreakingAndDestroyingAtOrNextToAStationEachLeadToACheckOneTickLater() {
        addStation(Material.STONE_BUTTON);
        Block wall = blockAt(10, 64, 21);
        Block button = blockAt(10, 64, 20);

        fixture.listener.onBlockPhysics(new BlockPhysicsEvent(button, null));
        runNextTick();
        fixture.listener.onBlockBreak(new BlockBreakEvent(wall, null));
        runNextTick();
        fixture.listener.onBlockDestroy(new BlockDestroyEvent(button, null, null, 0, true));
        assertTrue(stations.isStation(STATION));

        materials.put(List.of(10, 64, 20), Material.AIR);
        runNextTick();

        assertFalse(stations.isStation(STATION));
        assertEquals(1, warnings.size());
    }

    private void addStation(Material material) {
        stations.add(STATION);
        materials.put(List.of(STATION.blockX(), STATION.blockY(), STATION.blockZ()), material);
    }

    private void runNextTick() {
        List<Runnable> tasks = List.copyOf(nextTickTasks);
        nextTickTasks.clear();
        tasks.forEach(Runnable::run);
    }

    private Block blockAt(int blockX, int blockY, int blockZ) {
        return SharedBukkitServer.proxy(Block.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getX" -> blockX;
            case "getY" -> blockY;
            case "getZ" -> blockZ;
            case "getType" -> materials.getOrDefault(List.of(blockX, blockY, blockZ), Material.AIR);
            default -> throw new UnsupportedOperationException("Block." + method.getName());
        });
    }

    private Chunk chunk() {
        return SharedBukkitServer.proxy(Chunk.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getX" -> 0;
            case "getZ" -> 1;
            case "getEntities" -> new Entity[0];
            default -> throw new UnsupportedOperationException("Chunk." + method.getName());
        });
    }

    private final class ListenerFixture implements StationDisplayEntities {

        private final RegistrationStationWorldListener listener;

        private ListenerFixture() {
            Plugin plugin = SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
                case "getName", "namespace" -> "simplepvparena";
                default -> throw new UnsupportedOperationException("Plugin." + method.getName());
            });
            RegistrationStationService stationService = new RegistrationStationService(plugin, stations,
                    new RegistrationService(new RegistrationList(), () -> false, () -> { }),
                    material -> material.name().endsWith("_BUTTON"), () -> { },
                    material -> new RecordingItemStack(material, 1));
            RegistrationStationDisplays displays = new RegistrationStationDisplays(stations,
                    worldName -> world, this, Optional::empty, () -> true);
            stationService.addObserver(displays);
            RegistrationStationButtonCheck buttonCheck = new RegistrationStationButtonCheck(stations, stationService,
                    worldName -> world, material -> material.name().endsWith("_BUTTON"), nextTickTasks::add,
                    warnings::add);
            listener = new RegistrationStationWorldListener(displays, buttonCheck);
        }

        @Override
        public Entity spawnItemDisplay(Location location, ItemStack itemStack) {
            throw new UnsupportedOperationException("no station item in this test");
        }

        @Override
        public Entity spawnTextDisplay(Location location, Component text) {
            String description = "text at " + location.getX() + " " + location.getY() + " " + location.getZ();
            displayCalls.add(description);
            return SharedBukkitServer.proxy(Entity.class, (method, arguments) -> switch (method.getName()) {
                case "remove" -> {
                    displayCalls.add("remove " + description);
                    yield null;
                }
                default -> throw new UnsupportedOperationException("Entity." + method.getName());
            });
        }

        @Override
        public boolean isStationDisplay(Entity entity) {
            return false;
        }
    }
}

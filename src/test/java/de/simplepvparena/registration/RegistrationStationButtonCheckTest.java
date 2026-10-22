package de.simplepvparena.registration;

import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationStationButtonCheckTest {

    private static final String WORLD_NAME = "event";
    private static final RegistrationStation STATION = new RegistrationStation(WORLD_NAME, 10, 64, 20);
    private static final RegistrationStation SECOND_STATION = new RegistrationStation(WORLD_NAME, 12, 64, 20);
    private static final RegistrationStation FAR_STATION = new RegistrationStation(WORLD_NAME, 100, 64, -40);

    private final Map<List<Integer>, Material> materials = new HashMap<>();
    private final Set<List<Integer>> loadedChunks = new HashSet<>();
    private final World world = SharedBukkitServer.proxy(World.class, (method, arguments) ->
            switch (method.getName()) {
                case "getName" -> WORLD_NAME;
                case "isChunkLoaded" -> loadedChunks.contains(List.of((Integer) arguments[0], (Integer) arguments[1]));
                case "getBlockAt" -> blockAt((Integer) arguments[0], (Integer) arguments[1], (Integer) arguments[2]);
                default -> throw new UnsupportedOperationException("World." + method.getName());
            });
    private final RegistrationStations stations = new RegistrationStations();
    private final List<String> stationChanges = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<Runnable> nextTickTasks = new ArrayList<>();
    private final RegistrationStationService stationService = createStationService();
    private final RegistrationStationButtonCheck buttonCheck = new RegistrationStationButtonCheck(stations,
            stationService, worldName -> WORLD_NAME.equals(worldName) ? world : null,
            material -> material.name().endsWith("_BUTTON"), nextTickTasks::add, warnings::add);

    private int saveCount;

    @Test
    void theStartRemovesAStationWithoutButtonInALoadedChunkAndWarnsWithItsPlace() {
        addStation(STATION, Material.AIR);
        addStation(SECOND_STATION, Material.OAK_BUTTON);
        loadChunk(0, 1);

        buttonCheck.checkLoadedStations();

        assertEquals(List.of(SECOND_STATION), stations.stations());
        assertEquals(1, saveCount);
        assertEquals(List.of("removed " + STATION), stationChanges);
        assertEquals(List.of("The registration station at 10 64 20 in world event has no button any more and was "
                + "removed"), warnings);
    }

    @Test
    void aStationReplacedByAnotherBlockIsRemovedToo() {
        addStation(STATION, Material.STONE);
        loadChunk(0, 1);

        buttonCheck.checkLoadedStations();

        assertFalse(stations.isStation(STATION));
        assertEquals(1, warnings.size());
    }

    @Test
    void aStationInAnUnloadedChunkOrAnUnknownWorldIsLeftAlone() {
        addStation(STATION, Material.AIR);
        RegistrationStation otherWorldStation = new RegistrationStation("nether", 10, 64, 20);
        stations.add(otherWorldStation);

        buttonCheck.checkLoadedStations();

        assertEquals(List.of(STATION, otherWorldStation), stations.stations());
        assertEquals(0, saveCount);
        assertEquals(List.of(), warnings);
    }

    @Test
    void loadingAChunkChecksTheStationsOfThatChunkOnly() {
        addStation(STATION, Material.AIR);
        addStation(FAR_STATION, Material.AIR);
        loadChunk(0, 1);
        loadChunk(6, -3);

        buttonCheck.checkChunk(chunk(0, 1));

        assertEquals(List.of(FAR_STATION), stations.stations());
        assertEquals(List.of("removed " + STATION), stationChanges);
    }

    @Test
    void aBlockChangeAtTheStationIsCheckedOneTickLater() {
        addStation(STATION, Material.STONE_BUTTON);
        loadChunk(0, 1);

        buttonCheck.checkSoonAround(blockAt(10, 64, 20));
        materials.put(List.of(10, 64, 20), Material.AIR);
        assertTrue(stations.isStation(STATION), "the change happens after the event");

        runNextTick();

        assertFalse(stations.isStation(STATION));
        assertEquals(List.of("removed " + STATION), stationChanges);
        assertEquals(1, warnings.size());
    }

    @Test
    void breakingTheBlockBehindTheStationRemovesItOnceTheButtonHasFallenOff() {
        addStation(STATION, Material.STONE_BUTTON);
        loadChunk(0, 1);

        buttonCheck.checkSoonAround(blockAt(10, 64, 21));
        materials.put(List.of(10, 64, 20), Material.AIR);
        runNextTick();

        assertFalse(stations.isStation(STATION));
    }

    @Test
    void everyNeighbourCountsButNotABlockFurtherAway() {
        addStation(STATION, Material.AIR);
        loadChunk(0, 1);

        for (int[] position : new int[][] {{12, 64, 20}, {10, 66, 20}, {11, 65, 20}, {10, 64, 22}}) {
            buttonCheck.checkSoonAround(blockAt(position[0], position[1], position[2]));
        }
        assertEquals(List.of(), nextTickTasks);

        for (int[] position : new int[][] {{11, 64, 20}, {9, 64, 20}, {10, 65, 20}, {10, 63, 20}, {10, 64, 21},
                {10, 64, 19}}) {
            addStation(STATION, Material.AIR);
            buttonCheck.checkSoonAround(blockAt(position[0], position[1], position[2]));
            runNextTick();
            assertFalse(stations.isStation(STATION), List.of(position[0], position[1], position[2]).toString());
        }
    }

    @Test
    void manyBlockChangesInOneTickAreCheckedTogetherOnce() {
        addStation(STATION, Material.STONE_BUTTON);
        addStation(SECOND_STATION, Material.STONE_BUTTON);
        loadChunk(0, 1);

        buttonCheck.checkSoonAround(blockAt(10, 64, 20));
        buttonCheck.checkSoonAround(blockAt(10, 64, 21));
        buttonCheck.checkSoonAround(blockAt(11, 64, 20));
        buttonCheck.checkSoonAround(blockAt(12, 64, 21));

        assertEquals(1, nextTickTasks.size());
        materials.put(List.of(10, 64, 20), Material.AIR);
        materials.put(List.of(12, 64, 20), Material.AIR);
        runNextTick();
        assertEquals(List.of(), stations.stations());
        assertEquals(2, warnings.size());

        addStation(STATION, Material.STONE_BUTTON);
        buttonCheck.checkSoonAround(blockAt(10, 64, 20));
        assertEquals(1, nextTickTasks.size(), "a later tick gets its own check");
    }

    @Test
    void aButtonThatIsStillThereKeepsItsStation() {
        addStation(STATION, Material.STONE_BUTTON);
        loadChunk(0, 1);

        buttonCheck.checkSoonAround(blockAt(10, 64, 21));
        runNextTick();
        buttonCheck.checkLoadedStations();

        assertTrue(stations.isStation(STATION));
        assertEquals(0, saveCount);
        assertEquals(List.of(), warnings);
    }

    @Test
    void aStationRemovedBeforeTheCheckIsNeitherSavedNorWarnedAboutAgain() {
        addStation(STATION, Material.STONE_BUTTON);
        loadChunk(0, 1);
        buttonCheck.checkSoonAround(blockAt(10, 64, 20));
        stations.remove(STATION);
        materials.put(List.of(10, 64, 20), Material.AIR);

        runNextTick();

        assertEquals(0, saveCount);
        assertEquals(List.of(), stationChanges);
        assertEquals(List.of(), warnings);
    }

    @Test
    void withoutAnyStationABlockChangeSchedulesNothing() {
        buttonCheck.checkSoonAround(blockAt(10, 64, 20));

        assertEquals(List.of(), nextTickTasks);
    }

    @Test
    void aStationWithoutButtonLosesItsDisplay() {
        RecordingDisplays displays = new RecordingDisplays();
        stationService.addObserver(displays);
        addStation(STATION, Material.AIR);
        loadChunk(0, 1);

        buttonCheck.checkLoadedStations();

        assertEquals(List.of("hide " + STATION), displays.calls);
    }

    @Test
    @UseLanguage("de")
    void warnsInGerman() {
        addStation(STATION, Material.AIR);
        loadChunk(0, 1);

        buttonCheck.checkLoadedStations();

        assertEquals(List.of("Die Anmelde-Station bei 10 64 20 in der Welt event hat keinen Knopf mehr und wurde "
                + "entfernt"), warnings);
    }

    private RegistrationStationService createStationService() {
        Plugin plugin = SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getName", "namespace" -> "simplepvparena";
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
        RegistrationService registrationService = new RegistrationService(new RegistrationList(), () -> false,
                () -> { });
        RegistrationStationService service = new RegistrationStationService(plugin, stations, registrationService,
                material -> material.name().endsWith("_BUTTON"), () -> saveCount++,
                material -> new RecordingItemStack(material, 1));
        service.addObserver(new RegistrationStationObserver() {

            @Override
            public void stationAdded(RegistrationStation station) {
                stationChanges.add("added " + station);
            }

            @Override
            public void stationRemoved(RegistrationStation station) {
                stationChanges.add("removed " + station);
            }
        });
        return service;
    }

    private void addStation(RegistrationStation station, Material material) {
        stations.add(station);
        materials.put(List.of(station.blockX(), station.blockY(), station.blockZ()), material);
    }

    private void loadChunk(int chunkX, int chunkZ) {
        loadedChunks.add(List.of(chunkX, chunkZ));
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

    private Chunk chunk(int chunkX, int chunkZ) {
        return SharedBukkitServer.proxy(Chunk.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getX" -> chunkX;
            case "getZ" -> chunkZ;
            default -> throw new UnsupportedOperationException("Chunk." + method.getName());
        });
    }

    private static final class RecordingDisplays implements RegistrationStationObserver {

        private final List<String> calls = new ArrayList<>();

        @Override
        public void stationAdded(RegistrationStation station) {
            calls.add("show " + station);
        }

        @Override
        public void stationRemoved(RegistrationStation station) {
            calls.add("hide " + station);
        }
    }
}

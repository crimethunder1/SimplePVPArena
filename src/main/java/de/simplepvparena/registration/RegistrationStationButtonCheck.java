package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.RegistrationMessage;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class RegistrationStationButtonCheck {

    private static final int CHUNK_SHIFT = 4;
    private static final int[][] SELF_AND_NEIGHBOUR_OFFSETS = {
            {0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private final RegistrationStations stations;
    private final RegistrationStationService stationService;
    private final Function<String, World> worldLookup;
    private final Predicate<Material> buttonMaterials;
    private final Consumer<Runnable> nextTickRunner;
    private final Consumer<String> warningLogger;
    private final Set<RegistrationStation> pendingStations = new LinkedHashSet<>();

    public RegistrationStationButtonCheck(RegistrationStations stations, RegistrationStationService stationService,
                                          Function<String, World> worldLookup, Predicate<Material> buttonMaterials,
                                          Consumer<Runnable> nextTickRunner, Consumer<String> warningLogger) {
        this.stations = Objects.requireNonNull(stations, "stations");
        this.stationService = Objects.requireNonNull(stationService, "stationService");
        this.worldLookup = Objects.requireNonNull(worldLookup, "worldLookup");
        this.buttonMaterials = Objects.requireNonNull(buttonMaterials, "buttonMaterials");
        this.nextTickRunner = Objects.requireNonNull(nextTickRunner, "nextTickRunner");
        this.warningLogger = Objects.requireNonNull(warningLogger, "warningLogger");
    }

    public void checkLoadedStations() {
        for (RegistrationStation station : stations.stations()) {
            checkIfLoaded(station);
        }
    }

    public void checkChunk(Chunk chunk) {
        World world = chunk.getWorld();
        String worldName = world.getName();
        for (RegistrationStation station : stations.stations()) {
            if (station.worldName().equals(worldName) && chunkCoordinate(station.blockX()) == chunk.getX()
                    && chunkCoordinate(station.blockZ()) == chunk.getZ()) {
                check(station, world);
            }
        }
    }

    public void checkSoonAround(Block block) {
        if (stations.isEmpty()) {
            return;
        }
        String worldName = block.getWorld().getName();
        for (int[] offset : SELF_AND_NEIGHBOUR_OFFSETS) {
            RegistrationStation station = new RegistrationStation(worldName, block.getX() + offset[0],
                    block.getY() + offset[1], block.getZ() + offset[2]);
            if (stations.isStation(station)) {
                schedule(station);
            }
        }
    }

    private void schedule(RegistrationStation station) {
        boolean firstPendingStation = pendingStations.isEmpty();
        if (pendingStations.add(station) && firstPendingStation) {
            nextTickRunner.accept(this::checkPendingStations);
        }
    }

    private void checkPendingStations() {
        List<RegistrationStation> stationsToCheck = List.copyOf(pendingStations);
        pendingStations.clear();
        for (RegistrationStation station : stationsToCheck) {
            if (stations.isStation(station)) {
                checkIfLoaded(station);
            }
        }
    }

    private void checkIfLoaded(RegistrationStation station) {
        World world = worldLookup.apply(station.worldName());
        if (world != null && world.isChunkLoaded(chunkCoordinate(station.blockX()),
                chunkCoordinate(station.blockZ()))) {
            check(station, world);
        }
    }

    private void check(RegistrationStation station, World world) {
        Material material = world.getBlockAt(station.blockX(), station.blockY(), station.blockZ()).getType();
        if (buttonMaterials.test(material) || !stationService.removeStationWithoutButton(station)) {
            return;
        }
        warningLogger.accept(Messages.text(RegistrationMessage.STATION_WITHOUT_BUTTON,
                Placeholder.of("world", station.worldName()), Placeholder.of("x", station.blockX()),
                Placeholder.of("y", station.blockY()), Placeholder.of("z", station.blockZ())));
    }

    private static int chunkCoordinate(int blockCoordinate) {
        return blockCoordinate >> CHUNK_SHIFT;
    }
}

package de.simplepvparena.arena;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.structure.Palette;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class ArenaBaselineFile {

    public static final String STRUCTURE_FILE_NAME = "arena-baseline.nbt";
    public static final String KEY_FILE_NAME = "arena-baseline.yml";

    private static final String KEY_PATH = "key";

    private final Server server;
    private final File structureFile;
    private final File keyFile;

    public ArenaBaselineFile(Server server, File dataFolder) {
        this.server = Objects.requireNonNull(server, "server");
        this.structureFile = new File(dataFolder, STRUCTURE_FILE_NAME);
        this.keyFile = new File(dataFolder, KEY_FILE_NAME);
    }

    public void save(ArenaBaselineSnapshot snapshot) throws IOException {
        ArenaShape volume = snapshot.getCapturedVolume();
        World world = server.getWorld(volume.getWorldName());
        if (world == null) {
            throw new IOException("the world " + volume.getWorldName() + " is not loaded");
        }
        RegionBoundary boundary = volume.getBoundingBox();
        StructureManager structureManager = server.getStructureManager();
        Structure structure = structureManager.createStructure();
        structure.fill(new Location(world, boundary.getMinimumX(), volume.getFloorY(), boundary.getMinimumZ()),
                new BlockVector(boundary.getMaximumX() - boundary.getMinimumX() + 1,
                        volume.getWallTopY() - volume.getFloorY() + 1,
                        boundary.getMaximumZ() - boundary.getMinimumZ() + 1), false);
        Files.createDirectories(structureFile.toPath().toAbsolutePath().getParent());
        structureManager.saveStructure(structureFile, structure);
        YamlConfiguration key = new YamlConfiguration();
        key.set(KEY_PATH, describeKey(snapshot.getShape(), snapshot.getDepthBelowFloor()));
        Files.writeString(keyFile.toPath(), key.saveToString(), StandardCharsets.UTF_8);
    }

    public Optional<ArenaBaselineSnapshot> load(ArenaShape shape, int depthBelowFloor) throws IOException {
        if (!structureFile.isFile() || !keyFile.isFile()) {
            return Optional.empty();
        }
        YamlConfiguration key = new YamlConfiguration();
        try {
            key.loadFromString(Files.readString(keyFile.toPath(), StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException failure) {
            throw new IOException(failure.getMessage(), failure);
        }
        if (!describeKey(shape, depthBelowFloor).equals(key.getString(KEY_PATH))) {
            return Optional.empty();
        }
        Structure structure = server.getStructureManager().loadStructure(structureFile);
        List<Palette> palettes = structure.getPalettes();
        if (palettes.isEmpty()) {
            return Optional.empty();
        }
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape, depthBelowFloor);
        ArenaShape volume = builder.getCapturedVolume();
        RegionBoundary boundary = volume.getBoundingBox();
        for (BlockState state : palettes.getFirst().getBlocks()) {
            int blockX = boundary.getMinimumX() + state.getX();
            int blockY = volume.getFloorY() + state.getY();
            int blockZ = boundary.getMinimumZ() + state.getZ();
            if (volume.findBlockIndex(blockX, blockY, blockZ) >= 0L) {
                builder.record(blockX, blockY, blockZ, state.getBlockData(), () -> state);
            }
        }
        return builder.isComplete() ? Optional.of(builder.build()) : Optional.empty();
    }

    static String describeKey(ArenaShape shape, int depthBelowFloor) {
        return shape.getWorldName() + "|" + shape.getFloorY() + "|" + shape.getWallTopY() + "|" + depthBelowFloor
                + "|" + shape.getCellCount() + "|" + Arrays.hashCode(shape.getCells());
    }
}

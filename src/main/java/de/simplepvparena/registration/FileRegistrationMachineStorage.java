package de.simplepvparena.registration;

import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FileRegistrationMachineStorage {

    public static final String FILE_NAME = "machines.yml";

    private static final String MACHINES_KEY = "machines";
    private static final String WORLD_KEY = "world";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String FACING_KEY = "facing";

    private final Path file;

    public FileRegistrationMachineStorage(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public List<RegistrationMachine> load() throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException failure) {
            throw new IOException(failure.getMessage(), failure);
        }
        List<RegistrationMachine> machines = new ArrayList<>();
        for (Map<?, ?> entry : configuration.getMapList(MACHINES_KEY)) {
            YamlConfiguration section = new YamlConfiguration();
            entry.forEach((key, value) -> section.set(String.valueOf(key), value));
            readMachine(section).ifPresent(machines::add);
        }
        return machines;
    }

    private static Optional<RegistrationMachine> readMachine(ConfigurationSection section) {
        String worldName = section.getString(WORLD_KEY);
        if (worldName == null) {
            return Optional.empty();
        }
        try {
            BlockFace facing = BlockFace.valueOf(section.getString(FACING_KEY, BlockFace.SOUTH.name()));
            return Optional.of(new RegistrationMachine(worldName, section.getInt(X_KEY),
                    section.getInt(Y_KEY), section.getInt(Z_KEY), facing));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public void save(List<RegistrationMachine> machines) throws IOException {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (RegistrationMachine machine : machines) {
            entries.add(Map.of(WORLD_KEY, machine.worldName(), X_KEY, machine.blockX(), Y_KEY, machine.blockY(),
                    Z_KEY, machine.blockZ(), FACING_KEY, machine.facing().name()));
        }
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(MACHINES_KEY, entries);
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, configuration.saveToString(), StandardCharsets.UTF_8);
    }
}

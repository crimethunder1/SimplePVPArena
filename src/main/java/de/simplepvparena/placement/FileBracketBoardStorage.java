package de.simplepvparena.placement;

import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class FileBracketBoardStorage {

    public static final String FILE_NAME = "bracket-boards.yml";

    private static final String BOARDS_KEY = "boards";
    private static final String WORLD_KEY = "world";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String FACING_KEY = "facing";

    private final Path file;

    public FileBracketBoardStorage(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public List<BracketBoard> load() throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException failure) {
            throw new IOException(failure.getMessage(), failure);
        }
        List<BracketBoard> boards = new ArrayList<>();
        for (Map<?, ?> entry : configuration.getMapList(BOARDS_KEY)) {
            YamlConfiguration section = new YamlConfiguration();
            entry.forEach((key, value) -> section.set(String.valueOf(key), value));
            readBoard(section).ifPresent(boards::add);
        }
        return boards;
    }

    private static Optional<BracketBoard> readBoard(ConfigurationSection section) {
        String worldName = section.getString(WORLD_KEY);
        if (worldName == null) {
            return Optional.empty();
        }
        try {
            BlockFace facing = BlockFace.valueOf(section.getString(FACING_KEY, BlockFace.SOUTH.name()));
            return Optional.of(new BracketBoard(worldName, section.getInt(X_KEY), section.getInt(Y_KEY),
                    section.getInt(Z_KEY), facing));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public void save(List<BracketBoard> boards) throws IOException {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (BracketBoard board : boards) {
            entries.add(Map.of(WORLD_KEY, board.worldName(), X_KEY, board.blockX(), Y_KEY, board.blockY(),
                    Z_KEY, board.blockZ(), FACING_KEY, board.facing().name()));
        }
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(BOARDS_KEY, entries);
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, configuration.saveToString(), StandardCharsets.UTF_8);
    }
}

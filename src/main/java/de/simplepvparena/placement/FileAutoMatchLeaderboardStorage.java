package de.simplepvparena.placement;

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
import java.util.Objects;
import java.util.UUID;

@NullMarked
public final class FileAutoMatchLeaderboardStorage {

    public static final String FILE_NAME = "auto-match-wins.yml";

    private static final String PLAYERS_KEY = "players";
    private static final String NAME_KEY = "name";
    private static final String WINS_KEY = "wins";

    private final Path file;

    public FileAutoMatchLeaderboardStorage(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public List<AutoMatchWins> load() throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException failure) {
            throw new IOException(failure.getMessage(), failure);
        }
        ConfigurationSection players = configuration.getConfigurationSection(PLAYERS_KEY);
        if (players == null) {
            return List.of();
        }
        List<AutoMatchWins> wins = new ArrayList<>();
        for (String key : players.getKeys(false)) {
            ConfigurationSection player = players.getConfigurationSection(key);
            if (player == null) {
                continue;
            }
            try {
                wins.add(new AutoMatchWins(UUID.fromString(key), player.getString(NAME_KEY, key),
                        player.getInt(WINS_KEY)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return wins;
    }

    public void save(List<AutoMatchWins> wins) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        for (AutoMatchWins entry : wins) {
            String path = PLAYERS_KEY + "." + entry.playerUniqueId();
            configuration.set(path + "." + NAME_KEY, entry.playerName());
            configuration.set(path + "." + WINS_KEY, entry.wins());
        }
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, configuration.saveToString(), StandardCharsets.UTF_8);
    }
}

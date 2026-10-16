package de.simplepvparena.kit;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class FilePlayerStateSnapshotStorage implements PlayerStateSnapshotStorage {

    public static final String DIRECTORY_NAME = "player-snapshots";

    private static final int FORMAT_VERSION = 1;
    private static final String FILE_EXTENSION = ".yml";
    private static final String BROKEN_FILE_EXTENSION = ".broken";
    private static final String TEMPORARY_FILE_EXTENSION = ".tmp";
    private static final String FORMAT_VERSION_KEY = "format-version";
    private static final String SAVED_AT_KEY = "saved-at";
    private static final String CONTENTS_KEY = "contents";
    private static final String LEVEL_KEY = "level";
    private static final String EXP_KEY = "exp";
    private static final String FOOD_LEVEL_KEY = "food-level";
    private static final String SATURATION_KEY = "saturation";
    private static final String HEALTH_KEY = "health";

    private final Path snapshotDirectory;
    private final ItemStackCodec itemStackCodec;

    public FilePlayerStateSnapshotStorage(Path snapshotDirectory, ItemStackCodec itemStackCodec) {
        this.snapshotDirectory = Objects.requireNonNull(snapshotDirectory, "snapshotDirectory");
        this.itemStackCodec = Objects.requireNonNull(itemStackCodec, "itemStackCodec");
    }

    @Override
    public void save(UUID playerUniqueId, PlayerStateSnapshot snapshot) throws IOException {
        String serializedSnapshot = serialize(snapshot);
        Files.createDirectories(snapshotDirectory);
        Path snapshotFile = resolveSnapshotFile(playerUniqueId);
        Path temporaryFile = Files.createTempFile(snapshotDirectory, snapshotFile.getFileName().toString(),
                TEMPORARY_FILE_EXTENSION);
        try {
            Files.writeString(temporaryFile, serializedSnapshot, StandardCharsets.UTF_8);
            moveIntoPlace(temporaryFile, snapshotFile);
        } catch (IOException | RuntimeException writeFailure) {
            removeTemporaryFile(temporaryFile, writeFailure);
            throw writeFailure;
        }
    }

    @Override
    public PlayerStateSnapshotLookup find(UUID playerUniqueId) {
        Path snapshotFile = resolveSnapshotFile(playerUniqueId);
        String fileName = snapshotFile.getFileName().toString();
        if (!Files.isRegularFile(snapshotFile)) {
            return PlayerStateSnapshotLookup.missing(fileName);
        }
        try {
            return PlayerStateSnapshotLookup.found(read(snapshotFile), fileName);
        } catch (IOException | InvalidConfigurationException | RuntimeException unreadableSnapshot) {
            return PlayerStateSnapshotLookup.broken(moveAside(snapshotFile));
        }
    }

    @Override
    public void delete(UUID playerUniqueId) throws IOException {
        Path snapshotFile = resolveSnapshotFile(playerUniqueId);
        if (Files.isRegularFile(snapshotFile)) {
            Files.delete(snapshotFile);
        }
    }

    private Path resolveSnapshotFile(UUID playerUniqueId) {
        return snapshotDirectory.resolve(playerUniqueId + FILE_EXTENSION);
    }

    private String serialize(PlayerStateSnapshot snapshot) throws IOException {
        byte[] encodedContents;
        try {
            encodedContents = itemStackCodec.encode(snapshot.getContents());
        } catch (RuntimeException encodingFailure) {
            throw new IOException("the inventory could not be encoded", encodingFailure);
        }
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(FORMAT_VERSION_KEY, FORMAT_VERSION);
        configuration.set(SAVED_AT_KEY, Instant.now().toString());
        configuration.set(CONTENTS_KEY, KitItemCodec.encode(encodedContents));
        configuration.set(LEVEL_KEY, snapshot.getLevel());
        configuration.set(EXP_KEY, (double) snapshot.getExp());
        configuration.set(FOOD_LEVEL_KEY, snapshot.getFoodLevel());
        configuration.set(SATURATION_KEY, (double) snapshot.getSaturation());
        configuration.set(HEALTH_KEY, snapshot.getHealth());
        return configuration.saveToString();
    }

    private PlayerStateSnapshot read(Path snapshotFile) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(Files.readString(snapshotFile, StandardCharsets.UTF_8));
        if (readNumber(configuration, FORMAT_VERSION_KEY).intValue() != FORMAT_VERSION) {
            throw new InvalidConfigurationException("unknown " + FORMAT_VERSION_KEY);
        }
        byte[] encodedContents = KitItemCodec.decode(configuration.getString(CONTENTS_KEY))
                .orElseThrow(() -> new InvalidConfigurationException("unreadable " + CONTENTS_KEY));
        ItemStack[] contents = itemStackCodec.decode(encodedContents);
        if (contents == null) {
            throw new InvalidConfigurationException("unreadable " + CONTENTS_KEY);
        }
        int level = readNumber(configuration, LEVEL_KEY).intValue();
        float exp = readNumber(configuration, EXP_KEY).floatValue();
        int foodLevel = readNumber(configuration, FOOD_LEVEL_KEY).intValue();
        float saturation = readNumber(configuration, SATURATION_KEY).floatValue();
        double health = readNumber(configuration, HEALTH_KEY).doubleValue();
        if (level < 0 || !(exp >= 0.0f && exp <= 1.0f) || foodLevel < 0 || !Float.isFinite(saturation)
                || !Double.isFinite(health)) {
            throw new InvalidConfigurationException("values out of range");
        }
        return new PlayerStateSnapshot(contents, level, exp, foodLevel, saturation, health);
    }

    private static Number readNumber(YamlConfiguration configuration, String key)
            throws InvalidConfigurationException {
        if (configuration.get(key) instanceof Number number) {
            return number;
        }
        throw new InvalidConfigurationException("missing or unreadable " + key);
    }

    private static void moveIntoPlace(Path temporaryFile, Path snapshotFile) throws IOException {
        try {
            Files.move(temporaryFile, snapshotFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailure) {
            Files.move(temporaryFile, snapshotFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void removeTemporaryFile(Path temporaryFile, Exception writeFailure) {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException deleteFailure) {
            writeFailure.addSuppressed(deleteFailure);
        }
    }

    private static String moveAside(Path snapshotFile) {
        Path brokenFile = snapshotFile.resolveSibling(snapshotFile.getFileName() + BROKEN_FILE_EXTENSION);
        try {
            Files.move(snapshotFile, brokenFile, StandardCopyOption.REPLACE_EXISTING);
            return brokenFile.getFileName().toString();
        } catch (IOException moveFailure) {
            return snapshotFile.getFileName().toString();
        }
    }
}

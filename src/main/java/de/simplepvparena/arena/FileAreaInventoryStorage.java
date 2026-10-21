package de.simplepvparena.arena;

import de.simplepvparena.kit.ItemStackCodec;
import de.simplepvparena.kit.KitItemCodec;
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

public final class FileAreaInventoryStorage implements AreaInventoryStorage {

    public static final String DIRECTORY_NAME = "area-inventories";

    private static final int FORMAT_VERSION = 1;
    private static final String FILE_EXTENSION = ".yml";
    private static final String BROKEN_FILE_EXTENSION = ".broken";
    private static final String TEMPORARY_FILE_EXTENSION = ".tmp";
    private static final String SEPARATOR = ".";
    private static final String FORMAT_VERSION_KEY = "format-version";
    private static final String SAVED_AT_KEY = "saved-at";
    private static final String CONTENTS_KEY = "contents";

    private final Path inventoryDirectory;
    private final ItemStackCodec itemStackCodec;

    public FileAreaInventoryStorage(Path inventoryDirectory, ItemStackCodec itemStackCodec) {
        this.inventoryDirectory = Objects.requireNonNull(inventoryDirectory, "inventoryDirectory");
        this.itemStackCodec = Objects.requireNonNull(itemStackCodec, "itemStackCodec");
    }

    @Override
    public void save(UUID playerUniqueId, ItemStack[] contents) throws IOException {
        String serializedInventory = serialize(contents);
        Files.createDirectories(inventoryDirectory);
        Path inventoryFile = resolveInventoryFile(playerUniqueId);
        Path temporaryFile = Files.createTempFile(inventoryDirectory, inventoryFile.getFileName().toString(),
                TEMPORARY_FILE_EXTENSION);
        try {
            Files.writeString(temporaryFile, serializedInventory, StandardCharsets.UTF_8);
            moveIntoPlace(temporaryFile, inventoryFile);
        } catch (IOException | RuntimeException writeFailure) {
            removeTemporaryFile(temporaryFile, writeFailure);
            throw writeFailure;
        }
    }

    @Override
    public AreaInventoryLookup find(UUID playerUniqueId) {
        Path inventoryFile = resolveInventoryFile(playerUniqueId);
        String fileName = inventoryFile.getFileName().toString();
        if (!Files.isRegularFile(inventoryFile)) {
            return AreaInventoryLookup.missing(fileName);
        }
        try {
            return AreaInventoryLookup.found(read(inventoryFile), fileName);
        } catch (IOException | InvalidConfigurationException | RuntimeException unreadableInventory) {
            return AreaInventoryLookup.broken(moveAside(inventoryFile));
        }
    }

    @Override
    public void delete(UUID playerUniqueId) throws IOException {
        Path inventoryFile = resolveInventoryFile(playerUniqueId);
        if (Files.isRegularFile(inventoryFile)) {
            Files.delete(inventoryFile);
        }
    }

    private Path resolveInventoryFile(UUID playerUniqueId) {
        return inventoryDirectory.resolve(playerUniqueId + FILE_EXTENSION);
    }

    private String serialize(ItemStack[] contents) throws IOException {
        byte[] encodedContents;
        try {
            encodedContents = itemStackCodec.encode(contents);
        } catch (RuntimeException encodingFailure) {
            throw new IOException("the inventory could not be encoded", encodingFailure);
        }
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(FORMAT_VERSION_KEY, FORMAT_VERSION);
        configuration.set(SAVED_AT_KEY, Instant.now().toString());
        configuration.set(CONTENTS_KEY, KitItemCodec.encode(encodedContents));
        return configuration.saveToString();
    }

    private ItemStack[] read(Path inventoryFile) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(Files.readString(inventoryFile, StandardCharsets.UTF_8));
        if (!(configuration.get(FORMAT_VERSION_KEY) instanceof Number formatVersion)
                || formatVersion.intValue() != FORMAT_VERSION) {
            throw new InvalidConfigurationException("missing or unknown " + FORMAT_VERSION_KEY);
        }
        byte[] encodedContents = KitItemCodec.decode(configuration.getString(CONTENTS_KEY))
                .orElseThrow(() -> new InvalidConfigurationException("unreadable " + CONTENTS_KEY));
        ItemStack[] contents = itemStackCodec.decode(encodedContents);
        if (contents == null) {
            throw new InvalidConfigurationException("unreadable " + CONTENTS_KEY);
        }
        return contents;
    }

    private static void moveIntoPlace(Path temporaryFile, Path inventoryFile) throws IOException {
        try {
            Files.move(temporaryFile, inventoryFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailure) {
            Files.move(temporaryFile, inventoryFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void removeTemporaryFile(Path temporaryFile, Exception writeFailure) {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException deleteFailure) {
            writeFailure.addSuppressed(deleteFailure);
        }
    }

    private static String moveAside(Path inventoryFile) {
        Path brokenFile = inventoryFile.resolveSibling(inventoryFile.getFileName() + BROKEN_FILE_EXTENSION);
        if (Files.exists(brokenFile)) {
            brokenFile = inventoryFile.resolveSibling(inventoryFile.getFileName() + SEPARATOR
                    + System.currentTimeMillis() + BROKEN_FILE_EXTENSION);
        }
        try {
            Files.move(inventoryFile, brokenFile);
            return brokenFile.getFileName().toString();
        } catch (IOException moveFailure) {
            return inventoryFile.getFileName().toString();
        }
    }
}

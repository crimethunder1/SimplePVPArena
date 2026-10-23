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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class FileAreaInventoryStorage implements AreaInventoryStorage {

    public static final String DIRECTORY_NAME = "area-inventories";

    private static final int FORMAT_VERSION = 2;
    private static final int FIRST_FORMAT_VERSION = 1;
    private static final String FILE_EXTENSION = ".yml";
    private static final String BROKEN_FILE_EXTENSION = ".broken";
    private static final String TEMPORARY_FILE_EXTENSION = ".tmp";
    private static final String SEPARATOR = ".";
    private static final String FORMAT_VERSION_KEY = "format-version";
    private static final String SAVED_AT_KEY = "saved-at";
    private static final String CONTENTS_KEY = "contents";
    private static final String LOOSE_ITEMS_KEY = "loose-items";

    private final Path inventoryDirectory;
    private final ItemStackCodec itemStackCodec;

    public FileAreaInventoryStorage(Path inventoryDirectory, ItemStackCodec itemStackCodec) {
        this.inventoryDirectory = Objects.requireNonNull(inventoryDirectory, "inventoryDirectory");
        this.itemStackCodec = Objects.requireNonNull(itemStackCodec, "itemStackCodec");
    }

    @Override
    public void save(UUID playerUniqueId, ItemStack[] contents, List<ItemStack> looseItems) throws IOException {
        String serializedInventory = serialize(contents, looseItems);
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
            return read(inventoryFile, fileName);
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

    private String serialize(ItemStack[] contents, List<ItemStack> looseItems) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(FORMAT_VERSION_KEY, FORMAT_VERSION);
        configuration.set(SAVED_AT_KEY, Instant.now().toString());
        configuration.set(CONTENTS_KEY, encode(contents));
        if (!looseItems.isEmpty()) {
            configuration.set(LOOSE_ITEMS_KEY, encode(looseItems.toArray(new ItemStack[0])));
        }
        return configuration.saveToString();
    }

    private String encode(ItemStack[] itemStacks) throws IOException {
        try {
            return KitItemCodec.encode(itemStackCodec.encode(itemStacks));
        } catch (RuntimeException encodingFailure) {
            throw new IOException("the inventory could not be encoded", encodingFailure);
        }
    }

    private AreaInventoryLookup read(Path inventoryFile, String fileName)
            throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(Files.readString(inventoryFile, StandardCharsets.UTF_8));
        if (!(configuration.get(FORMAT_VERSION_KEY) instanceof Number formatVersion)
                || formatVersion.intValue() < FIRST_FORMAT_VERSION || formatVersion.intValue() > FORMAT_VERSION) {
            throw new InvalidConfigurationException("missing or unknown " + FORMAT_VERSION_KEY);
        }
        ItemStack[] contents = decode(configuration, CONTENTS_KEY);
        List<ItemStack> looseItems = new ArrayList<>();
        if (configuration.contains(LOOSE_ITEMS_KEY)) {
            for (ItemStack looseItem : decode(configuration, LOOSE_ITEMS_KEY)) {
                if (looseItem != null) {
                    looseItems.add(looseItem);
                }
            }
        }
        return AreaInventoryLookup.found(contents, looseItems, fileName);
    }

    private ItemStack[] decode(YamlConfiguration configuration, String key) throws InvalidConfigurationException {
        byte[] encodedItemStacks = KitItemCodec.decode(configuration.getString(key))
                .orElseThrow(() -> new InvalidConfigurationException("unreadable " + key));
        ItemStack[] itemStacks = itemStackCodec.decode(encodedItemStacks);
        if (itemStacks == null) {
            throw new InvalidConfigurationException("unreadable " + key);
        }
        return itemStacks;
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

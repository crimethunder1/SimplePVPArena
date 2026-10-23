package de.simplepvparena.arena;

import de.simplepvparena.kit.ItemStackCodec;
import de.simplepvparena.kit.KitItemCodec;
import org.bukkit.configuration.ConfigurationSection;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class FileAreaInventoryStorage implements AreaInventoryStorage {

    public static final String DIRECTORY_NAME = "area-inventories";

    private static final int FORMAT_VERSION = 3;
    private static final int FIRST_FORMAT_VERSION = 1;
    private static final int FIRST_FORMAT_VERSION_WITH_CONDITION = 3;
    private static final String FILE_EXTENSION = ".yml";
    private static final String BROKEN_FILE_EXTENSION = ".broken";
    private static final String TEMPORARY_FILE_EXTENSION = ".tmp";
    private static final String SEPARATOR = ".";
    private static final String FORMAT_VERSION_KEY = "format-version";
    private static final String SAVED_AT_KEY = "saved-at";
    private static final String CONTENTS_KEY = "contents";
    private static final String LOOSE_ITEMS_KEY = "loose-items";
    private static final String POTION_EFFECTS_KEY = "potion-effects";
    private static final String EFFECT_TYPE_KEY = "type";
    private static final String AMPLIFIER_KEY = "amplifier";
    private static final String DURATION_TICKS_KEY = "duration-ticks";
    private static final String AMBIENT_KEY = "ambient";
    private static final String PARTICLES_KEY = "particles";
    private static final String ICON_KEY = "icon";
    private static final String HUNGER_KEY = "hunger";
    private static final String FOOD_LEVEL_KEY = "food-level";
    private static final String SATURATION_KEY = "saturation";
    private static final String EXHAUSTION_KEY = "exhaustion";

    private final Path inventoryDirectory;
    private final ItemStackCodec itemStackCodec;

    public FileAreaInventoryStorage(Path inventoryDirectory, ItemStackCodec itemStackCodec) {
        this.inventoryDirectory = Objects.requireNonNull(inventoryDirectory, "inventoryDirectory");
        this.itemStackCodec = Objects.requireNonNull(itemStackCodec, "itemStackCodec");
    }

    @Override
    public void save(UUID playerUniqueId, ItemStack[] contents, List<ItemStack> looseItems,
                     AreaPlayerCondition condition) throws IOException {
        String serializedInventory = serialize(contents, looseItems, condition);
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

    private String serialize(ItemStack[] contents, List<ItemStack> looseItems, AreaPlayerCondition condition)
            throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(FORMAT_VERSION_KEY, FORMAT_VERSION);
        configuration.set(SAVED_AT_KEY, Instant.now().toString());
        configuration.set(CONTENTS_KEY, encode(contents));
        if (!looseItems.isEmpty()) {
            configuration.set(LOOSE_ITEMS_KEY, encode(looseItems.toArray(new ItemStack[0])));
        }
        configuration.set(POTION_EFFECTS_KEY, condition.potionEffects().stream()
                .map(FileAreaInventoryStorage::describe)
                .toList());
        configuration.set(HUNGER_KEY + SEPARATOR + FOOD_LEVEL_KEY, condition.foodLevel());
        configuration.set(HUNGER_KEY + SEPARATOR + SATURATION_KEY, condition.saturation());
        configuration.set(HUNGER_KEY + SEPARATOR + EXHAUSTION_KEY, condition.exhaustion());
        return configuration.saveToString();
    }

    private static Map<String, Object> describe(SavedPotionEffect potionEffect) {
        Map<String, Object> description = new LinkedHashMap<>();
        description.put(EFFECT_TYPE_KEY, potionEffect.effectTypeKey());
        description.put(AMPLIFIER_KEY, potionEffect.amplifier());
        description.put(DURATION_TICKS_KEY, potionEffect.durationTicks());
        description.put(AMBIENT_KEY, potionEffect.ambient());
        description.put(PARTICLES_KEY, potionEffect.particles());
        description.put(ICON_KEY, potionEffect.icon());
        return description;
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
        Optional<AreaPlayerCondition> condition = formatVersion.intValue() < FIRST_FORMAT_VERSION_WITH_CONDITION
                ? Optional.empty()
                : Optional.of(readCondition(configuration));
        return AreaInventoryLookup.found(contents, looseItems, condition, fileName);
    }

    private static AreaPlayerCondition readCondition(YamlConfiguration configuration)
            throws InvalidConfigurationException {
        ConfigurationSection hunger = configuration.getConfigurationSection(HUNGER_KEY);
        if (hunger == null || !configuration.isList(POTION_EFFECTS_KEY)
                || !(hunger.get(FOOD_LEVEL_KEY) instanceof Number foodLevel)
                || !(hunger.get(SATURATION_KEY) instanceof Number saturation)
                || !(hunger.get(EXHAUSTION_KEY) instanceof Number exhaustion)) {
            throw new InvalidConfigurationException("missing or unreadable " + POTION_EFFECTS_KEY + " or "
                    + HUNGER_KEY);
        }
        List<SavedPotionEffect> potionEffects = new ArrayList<>();
        for (Map<?, ?> description : configuration.getMapList(POTION_EFFECTS_KEY)) {
            potionEffects.add(readPotionEffect(description));
        }
        return new AreaPlayerCondition(potionEffects, foodLevel.intValue(), saturation.floatValue(),
                exhaustion.floatValue());
    }

    private static SavedPotionEffect readPotionEffect(Map<?, ?> description) throws InvalidConfigurationException {
        if (!(description.get(EFFECT_TYPE_KEY) instanceof String effectTypeKey)
                || !(description.get(AMPLIFIER_KEY) instanceof Number amplifier)
                || !(description.get(DURATION_TICKS_KEY) instanceof Number durationTicks)
                || !(description.get(AMBIENT_KEY) instanceof Boolean ambient)
                || !(description.get(PARTICLES_KEY) instanceof Boolean particles)
                || !(description.get(ICON_KEY) instanceof Boolean icon)) {
            throw new InvalidConfigurationException("unreadable " + POTION_EFFECTS_KEY);
        }
        return new SavedPotionEffect(effectTypeKey, amplifier.intValue(), durationTicks.intValue(), ambient,
                particles, icon);
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

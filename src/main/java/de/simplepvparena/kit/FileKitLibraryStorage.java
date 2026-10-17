package de.simplepvparena.kit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class FileKitLibraryStorage implements KitLibraryStorage {

    public static final String FILE_NAME = "kits.yml";

    private static final int FORMAT_VERSION = 1;
    private static final String BROKEN_FILE_EXTENSION = ".broken";
    private static final String TEMPORARY_FILE_EXTENSION = ".tmp";
    private static final String FORMAT_VERSION_KEY = "format-version";
    private static final String DEFAULT_KIT_KEY = "default-kit";
    private static final String KITS_KEY = "kits";

    private final Path libraryFile;
    private final Consumer<String> problemReporter;

    public FileKitLibraryStorage(Path libraryFile, Consumer<String> problemReporter) {
        this.libraryFile = Objects.requireNonNull(libraryFile, "libraryFile");
        this.problemReporter = Objects.requireNonNull(problemReporter, "problemReporter");
    }

    @Override
    public void save(SavedKitLibrary savedKitLibrary) throws IOException {
        String serializedLibrary = serialize(savedKitLibrary);
        Path directory = libraryFile.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporaryFile = Files.createTempFile(directory, libraryFile.getFileName().toString(),
                TEMPORARY_FILE_EXTENSION);
        try {
            Files.writeString(temporaryFile, serializedLibrary, StandardCharsets.UTF_8);
            moveIntoPlace(temporaryFile, libraryFile);
        } catch (IOException | RuntimeException writeFailure) {
            removeTemporaryFile(temporaryFile, writeFailure);
            throw writeFailure;
        }
    }

    @Override
    public KitLibraryLookup find() {
        String fileName = libraryFile.getFileName().toString();
        if (!Files.isRegularFile(libraryFile)) {
            return KitLibraryLookup.missing(fileName);
        }
        try {
            return KitLibraryLookup.found(read(libraryFile), fileName);
        } catch (IOException | InvalidConfigurationException | RuntimeException unreadableLibrary) {
            return KitLibraryLookup.broken(moveAside(libraryFile), describe(unreadableLibrary));
        }
    }

    private static String serialize(SavedKitLibrary savedKitLibrary) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(FORMAT_VERSION_KEY, FORMAT_VERSION);
        savedKitLibrary.defaultKitName().ifPresent(defaultKitName ->
                configuration.set(DEFAULT_KIT_KEY, defaultKitName));
        ConfigurationSection kitsSection = configuration.createSection(KITS_KEY);
        for (SavedKit savedKit : savedKitLibrary.kits()) {
            savedKit.serializedKit().writeInto(kitsSection, savedKit.name());
        }
        return configuration.saveToString();
    }

    private SavedKitLibrary read(Path file) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        Object formatVersion = configuration.get(FORMAT_VERSION_KEY);
        if (!(formatVersion instanceof Integer version)) {
            throw new InvalidConfigurationException("missing or unreadable " + FORMAT_VERSION_KEY);
        }
        if (version != FORMAT_VERSION) {
            throw new InvalidConfigurationException("unknown " + FORMAT_VERSION_KEY + " " + version);
        }
        SavedKitLibrary savedKitLibrary = new SavedKitLibrary(readKits(configuration),
                readDefaultKitName(configuration));
        Optional<String> inconsistency = KitLibrary.findInconsistency(savedKitLibrary);
        if (inconsistency.isPresent()) {
            throw new InvalidConfigurationException(inconsistency.get());
        }
        return savedKitLibrary;
    }

    private List<SavedKit> readKits(YamlConfiguration configuration) throws InvalidConfigurationException {
        if (!configuration.contains(KITS_KEY)) {
            return List.of();
        }
        ConfigurationSection kitsSection = configuration.getConfigurationSection(KITS_KEY);
        if (kitsSection == null) {
            throw new InvalidConfigurationException("unreadable " + KITS_KEY);
        }
        List<SavedKit> kits = new ArrayList<>();
        for (String kitName : kitsSection.getKeys(false)) {
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitName);
            if (!SerializedKit.isWrittenIn(kitSection)) {
                throw new InvalidConfigurationException("the kit " + kitName + " holds neither "
                        + SerializedKit.EQUIPMENT_SECTION + " nor " + SerializedKit.SLOTS_SECTION);
            }
            kits.add(new SavedKit(kitName, SerializedKit.read(kitSection,
                    problem -> problemReporter.accept("Kit " + kitName + " in " + FILE_NAME + ": " + problem))));
        }
        return kits;
    }

    private static Optional<String> readDefaultKitName(YamlConfiguration configuration)
            throws InvalidConfigurationException {
        Object storedValue = configuration.get(DEFAULT_KIT_KEY);
        if (storedValue == null) {
            return Optional.empty();
        }
        if (storedValue instanceof String text && !text.isBlank()) {
            return Optional.of(text);
        }
        throw new InvalidConfigurationException("unreadable " + DEFAULT_KIT_KEY + " " + storedValue);
    }

    private static String describe(Exception failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    private static void moveIntoPlace(Path temporaryFile, Path targetFile) throws IOException {
        try {
            Files.move(temporaryFile, targetFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailure) {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void removeTemporaryFile(Path temporaryFile, Exception writeFailure) {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException deleteFailure) {
            writeFailure.addSuppressed(deleteFailure);
        }
    }

    private static String moveAside(Path file) {
        Path brokenFile = file.resolveSibling(file.getFileName() + BROKEN_FILE_EXTENSION);
        try {
            Files.move(file, brokenFile, StandardCopyOption.REPLACE_EXISTING);
            return brokenFile.getFileName().toString();
        } catch (IOException moveFailure) {
            return file.getFileName().toString();
        }
    }
}

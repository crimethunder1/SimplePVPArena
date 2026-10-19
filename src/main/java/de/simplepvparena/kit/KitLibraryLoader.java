package de.simplepvparena.kit;

import de.simplepvparena.message.KitMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;
import java.util.function.Consumer;

public final class KitLibraryLoader {

    public static final String MIGRATED_KIT_NAME = "default";

    private static final Placeholder LIBRARY_FILE = Placeholder.of("file", FileKitLibraryStorage.FILE_NAME);

    private final KitLibraryStorage storage;
    private final KitLibrary kitLibrary;
    private final KitConfigurationLoader kitConfigurationLoader;
    private final Consumer<String> adminNotifier;
    private final Consumer<String> problemReporter;

    public KitLibraryLoader(KitLibraryStorage storage, KitLibrary kitLibrary,
                            KitConfigurationLoader kitConfigurationLoader, Consumer<String> adminNotifier,
                            Consumer<String> problemReporter) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.kitLibrary = Objects.requireNonNull(kitLibrary, "kitLibrary");
        this.kitConfigurationLoader = Objects.requireNonNull(kitConfigurationLoader, "kitConfigurationLoader");
        this.adminNotifier = Objects.requireNonNull(adminNotifier, "adminNotifier");
        this.problemReporter = Objects.requireNonNull(problemReporter, "problemReporter");
    }

    public void load(ConfigurationSection legacyKitSection) {
        KitLibraryLookup lookup = storage.find();
        switch (lookup.outcome()) {
            case FOUND -> kitLibrary.restore(lookup.savedKitLibrary().orElseThrow());
            case MISSING -> migrate(legacyKitSection);
            case BROKEN -> {
                kitLibrary.restore(SavedKitLibrary.empty());
                adminNotifier.accept(Messages.text(KitMessage.LOADER_FILE_UNREADABLE, LIBRARY_FILE,
                        Placeholder.of("reason", lookup.failureReason()
                                .orElseGet(() -> Messages.text(KitMessage.LOADER_UNKNOWN_REASON))),
                        Placeholder.of("keptFile", lookup.fileName())));
            }
            case SET_ASIDE -> adminNotifier.accept(Messages.text(KitMessage.LOADER_FILE_SET_ASIDE, LIBRARY_FILE,
                    Placeholder.of("keptFile", lookup.fileName())));
        }
    }

    private void migrate(ConfigurationSection legacyKitSection) {
        if (SerializedKit.isWrittenIn(legacyKitSection)) {
            takeOver(SerializedKit.read(legacyKitSection,
                    problem -> problemReporter.accept("Kit section of config.yml: " + problem)));
            return;
        }
        if (legacyKitSection == null || legacyKitSection.getKeys(false).isEmpty()) {
            return;
        }
        SerializedKit typedKit = kitConfigurationLoader.loadSerializedKit(legacyKitSection);
        if (typedKit.isEmpty()) {
            adminNotifier.accept(Messages.text(KitMessage.LOADER_LEGACY_KIT_UNREADABLE, LIBRARY_FILE));
            return;
        }
        takeOver(typedKit);
    }

    private void takeOver(SerializedKit legacyKit) {
        if (legacyKit.isEmpty()) {
            return;
        }
        kitLibrary.createKit(MIGRATED_KIT_NAME, legacyKit);
        adminNotifier.accept(Messages.text(KitMessage.LOADER_LEGACY_KIT_TAKEN_OVER, LIBRARY_FILE,
                Placeholder.of("kit", MIGRATED_KIT_NAME)));
    }
}

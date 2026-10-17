package de.simplepvparena.kit;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;
import java.util.function.Consumer;

public final class KitLibraryLoader {

    public static final String MIGRATED_KIT_NAME = "default";

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
                adminNotifier.accept("The kits in " + FileKitLibraryStorage.FILE_NAME + " could not be read: "
                        + lookup.failureReason().orElse("unknown reason") + ". The file was kept as "
                        + lookup.fileName() + ", there is no kit and every fighter starts a round with an empty "
                        + "kit until a kit is created in the kit menu of /tournament");
            }
            case SET_ASIDE -> adminNotifier.accept("There is no " + FileKitLibraryStorage.FILE_NAME
                    + ", the kits lie in " + lookup.fileName() + " because it could not be read at an earlier "
                    + "start. Repair " + lookup.fileName() + " and rename it to " + FileKitLibraryStorage.FILE_NAME
                    + ", or delete it if its kits are to be discarded, then the next start takes the kit section of "
                    + "config.yml over");
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
            adminNotifier.accept("The kit section of config.yml holds no item that could be read, so no kit was "
                    + "taken over into " + FileKitLibraryStorage.FILE_NAME + ". There is no kit and every fighter "
                    + "starts a round with an empty kit until a kit is created in the kit menu of /tournament");
            return;
        }
        takeOver(typedKit);
    }

    private void takeOver(SerializedKit legacyKit) {
        if (legacyKit.isEmpty()) {
            return;
        }
        kitLibrary.createKit(MIGRATED_KIT_NAME, legacyKit);
        adminNotifier.accept("The kit of config.yml was taken over into " + FileKitLibraryStorage.FILE_NAME
                + " as the kit " + MIGRATED_KIT_NAME + " and is the default kit. The kit section of config.yml "
                + "is not read any more, the kits are managed in the kit menu of /tournament");
    }
}

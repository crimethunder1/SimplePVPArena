package de.simplepvparena.message;

import org.bukkit.configuration.InvalidConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class MessageCatalogLoader {

    public static final String DEFAULT_LANGUAGE = "en";
    public static final List<String> SHIPPED_LANGUAGES = List.of(DEFAULT_LANGUAGE, "de");
    public static final String FOLDER_NAME = "lang";

    private static final String FILE_EXTENSION = ".yml";
    private static final int LISTED_MISSING_KEY_COUNT = 10;

    private final Path languageFolder;
    private final Consumer<String> warningLogger;
    private final Consumer<String> adminNotifier;

    public MessageCatalogLoader(Path languageFolder, Consumer<String> warningLogger, Consumer<String> adminNotifier) {
        this.languageFolder = Objects.requireNonNull(languageFolder, "languageFolder");
        this.warningLogger = Objects.requireNonNull(warningLogger, "warningLogger");
        this.adminNotifier = Objects.requireNonNull(adminNotifier, "adminNotifier");
    }

    public static boolean isShipped(String language) {
        return SHIPPED_LANGUAGES.contains(language);
    }

    public static String fileNameOf(String language) {
        return language + FILE_EXTENSION;
    }

    public static String resourcePathOf(String language) {
        return FOLDER_NAME + "/" + fileNameOf(language);
    }

    public static YamlMessageCatalog readShipped(String language) {
        return YamlMessageCatalog.readResource(resourcePathOf(language));
    }

    public static MessageCatalog shippedCatalog(String language) {
        List<MessageCatalog> layers = new ArrayList<>();
        addShippedLayers(layers, readShipped(language), language);
        return new LayeredMessageCatalog(layers);
    }

    public void installShippedFiles() {
        for (String language : SHIPPED_LANGUAGES) {
            installShippedFile(language);
        }
    }

    public MessageCatalog load(String language) {
        return loadLanguage(language).catalog();
    }

    public void useLanguage(String language) {
        LoadedLanguage loadedLanguage = loadLanguage(language);
        Messages.use(loadedLanguage.catalog());
        loadedLanguage.unreadableFileReason().ifPresent(reason -> adminNotifier.accept(Messages.text(
                CommonMessage.LANGUAGE_FILE_UNREADABLE, Placeholder.of("file", loadedLanguage.fileName()),
                Placeholder.of("reason", reason))));
    }

    private LoadedLanguage loadLanguage(String language) {
        String shippedLanguage = language;
        if (!isShipped(language)) {
            warningLogger.accept("The language " + language + " has no shipped language file, the texts are shown in "
                    + DEFAULT_LANGUAGE);
            shippedLanguage = DEFAULT_LANGUAGE;
        }
        String fileName = resourcePathOf(shippedLanguage);
        Path adminFile = languageFolder.resolve(fileNameOf(shippedLanguage));
        YamlMessageCatalog shippedCatalog = readShipped(shippedLanguage);
        List<MessageCatalog> layers = new ArrayList<>();
        Optional<String> unreadableFileReason = Optional.empty();
        if (!Files.isRegularFile(adminFile)) {
            warningLogger.accept(fileName + " does not exist, the built-in texts of the language " + shippedLanguage
                    + " are used. The file is written again at the next start");
        } else {
            try {
                YamlMessageCatalog adminCatalog = YamlMessageCatalog.readFile(adminFile);
                layers.add(adminCatalog);
                reportMissingKeys(fileName, adminCatalog, shippedCatalog);
            } catch (IOException | InvalidConfigurationException failure) {
                unreadableFileReason = Optional.of(describe(failure));
                warningLogger.accept(fileName + " could not be read (" + unreadableFileReason.get()
                        + "), the built-in texts of the language " + shippedLanguage + " are used until it is fixed");
            }
        }
        addShippedLayers(layers, shippedCatalog, shippedLanguage);
        return new LoadedLanguage(new LayeredMessageCatalog(layers), fileName, unreadableFileReason);
    }

    private void reportMissingKeys(String fileName, MessageCatalog adminCatalog, MessageCatalog shippedCatalog) {
        List<String> missingPaths = shippedCatalog.paths().stream()
                .filter(path -> adminCatalog.find(path).isEmpty())
                .toList();
        if (missingPaths.isEmpty()) {
            return;
        }
        int listedCount = Math.min(LISTED_MISSING_KEY_COUNT, missingPaths.size());
        int unlistedCount = missingPaths.size() - listedCount;
        warningLogger.accept(fileName + " lacks " + missingPaths.size() + " keys of the shipped file, their built-in "
                + "texts are used: " + String.join(", ", missingPaths.subList(0, listedCount))
                + (unlistedCount > 0 ? " and " + unlistedCount + " more" : ""));
    }

    private void installShippedFile(String language) {
        Path target = languageFolder.resolve(fileNameOf(language));
        if (Files.exists(target)) {
            return;
        }
        try (InputStream shippedFile = MessageCatalogLoader.class.getClassLoader()
                .getResourceAsStream(resourcePathOf(language))) {
            if (shippedFile == null) {
                throw new IOException(resourcePathOf(language) + " is missing from the plugin");
            }
            Files.createDirectories(languageFolder);
            Files.copy(shippedFile, target);
        } catch (IOException failure) {
            warningLogger.accept("Could not write " + target + " (" + describe(failure)
                    + "), the built-in texts are used");
        }
    }

    private static void addShippedLayers(List<MessageCatalog> layers, YamlMessageCatalog shippedCatalog,
                                         String language) {
        layers.add(shippedCatalog);
        if (!DEFAULT_LANGUAGE.equals(language)) {
            layers.add(readShipped(DEFAULT_LANGUAGE));
        }
    }

    private static String describe(Exception failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message.strip().replaceAll("\\s+", " ");
    }

    private record LoadedLanguage(MessageCatalog catalog, String fileName, Optional<String> unreadableFileReason) {
    }
}

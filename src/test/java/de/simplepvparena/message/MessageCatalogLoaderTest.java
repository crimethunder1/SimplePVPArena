package de.simplepvparena.message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageCatalogLoaderTest {

    @TempDir
    Path languageFolder;

    private final List<String> warnings = new ArrayList<>();
    private final List<String> adminNotices = new ArrayList<>();

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    void theFirstStartCopiesEveryShippedLanguageFile() throws IOException {
        loader().installShippedFiles();

        for (String language : MessageCatalogLoader.SHIPPED_LANGUAGES) {
            Path copiedFile = languageFolder.resolve(MessageCatalogLoader.fileNameOf(language));
            assertEquals(shippedContent(language), Files.readString(copiedFile, StandardCharsets.UTF_8));
        }
        assertEquals(List.of(), warnings);
    }

    @Test
    void theCopyCreatesTheMissingLanguageFolder() throws IOException {
        Path missingFolder = languageFolder.resolve("plugin").resolve(MessageCatalogLoader.FOLDER_NAME);

        new MessageCatalogLoader(missingFolder, warnings::add, adminNotices::add).installShippedFiles();

        assertEquals(shippedContent("de"), Files.readString(missingFolder.resolve("de.yml"), StandardCharsets.UTF_8));
    }

    @Test
    void anExistingFileIsNeverOverwritten() throws IOException {
        String ownTexts = "common:\n  list:\n    and: '{items} sowie {last}'\n";
        Files.writeString(languageFolder.resolve("de.yml"), ownTexts, StandardCharsets.UTF_8);

        loader().installShippedFiles();

        assertEquals(ownTexts, Files.readString(languageFolder.resolve("de.yml"), StandardCharsets.UTF_8));
        assertEquals(shippedContent("en"), Files.readString(languageFolder.resolve("en.yml"), StandardCharsets.UTF_8));
    }

    @Test
    void aTextOfTheAdminFileWinsOverTheShippedText() throws IOException {
        writeAdminFile("de", "common:\n  list:\n    and: '{items} sowie {last}'\n");

        Messages.use(loader().load("de"));

        assertEquals("Alpha sowie Beta", Messages.join(List.of("Alpha", "Beta")));
    }

    @Test
    void aKeyMissingInTheAdminFileComesFromTheShippedFileOfTheSameLanguage() throws IOException {
        writeAdminFile("de", "common:\n  list:\n    and: '{items} sowie {last}'\n");

        Messages.use(loader().load("de"));

        assertEquals("A, B und 2 weitere", Messages.listLimited(List.of("A", "B", "C", "D"), 2));
    }

    @Test
    void theKeysMissingInTheAdminFileAreLoggedOnceWithTheirNumberAndTheFirstTen() throws IOException {
        writeAdminFile("de", "common:\n  list:\n    and: '{items} sowie {last}'\n");
        List<String> missingPaths = MessageCatalogLoader.readShipped("de").paths().stream()
                .filter(path -> !path.equals(CommonMessage.LIST_AND.path()))
                .toList();
        List<String> listedPaths = missingPaths.subList(0, Math.min(10, missingPaths.size()));
        int unlistedCount = missingPaths.size() - listedPaths.size();

        loader().load("de");

        assertEquals(List.of("lang/de.yml lacks " + missingPaths.size() + " keys of the shipped file, their built-in "
                + "texts are used: " + String.join(", ", listedPaths)
                + (unlistedCount > 0 ? " and " + unlistedCount + " more" : "")), warnings);
    }

    @Test
    void aCompleteAdminFileIsLoadedWithoutAWarning() throws IOException {
        loader().installShippedFiles();

        loader().load("de");
        loader().load("en");

        assertEquals(List.of(), warnings);
    }

    @Test
    void aBrokenAdminFileGivesTheWholeShippedLanguageAndIsReportedToTheLog() throws IOException {
        writeAdminFile("de", "common:\n  list:\n    and: '{items} sowie {last}\n    more: [\n");

        Messages.use(loader().load("de"));

        assertEquals("Alpha und Beta", Messages.join(List.of("Alpha", "Beta")));
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).startsWith("lang/de.yml could not be read ("), warnings.get(0));
        assertTrue(warnings.get(0).endsWith("), the built-in texts of the language de are used until it is fixed"),
                warnings.get(0));
    }

    @Test
    void aBrokenAdminFileIsReportedToTheAdminsInTheShippedLanguage() throws IOException {
        writeAdminFile("de", "common:\n  list:\n    and: '{items} sowie {last}\n    more: [\n");

        loader().useLanguage("de");

        assertEquals(1, adminNotices.size(), adminNotices.toString());
        assertTrue(adminNotices.get(0).startsWith("Die Sprachdatei lang/de.yml konnte nicht gelesen werden ("),
                adminNotices.get(0));
        assertTrue(adminNotices.get(0).endsWith("), bis zur Korrektur gelten die mitgelieferten Texte"),
                adminNotices.get(0));
    }

    @Test
    void aMissingAdminFileGivesTheShippedLanguageAndIsLogged() {
        Messages.use(loader().load("de"));

        assertEquals("Alpha und Beta", Messages.join(List.of("Alpha", "Beta")));
        assertEquals(List.of("lang/de.yml does not exist, the built-in texts of the language de are used. "
                + "The file is written again at the next start"), warnings);
        assertEquals(List.of(), adminNotices);
    }

    @Test
    void anUnknownLanguageIsShownInEnglishWithAWarning() throws IOException {
        loader().installShippedFiles();

        Messages.use(loader().load("fr"));

        assertEquals("Alpha and Beta", Messages.join(List.of("Alpha", "Beta")));
        assertEquals(List.of("The language fr has no shipped language file, the texts are shown in en"), warnings);
    }

    @Test
    void useLanguageSwitchesTheTextsOfTheWholePlugin() throws IOException {
        loader().installShippedFiles();

        loader().useLanguage("de");

        assertEquals("Alpha und Beta", Messages.join(List.of("Alpha", "Beta")));
        assertEquals(List.of(), adminNotices);
    }

    @Test
    void aReloadReadsTheChangedAdminFileAgain() throws IOException {
        loader().installShippedFiles();
        loader().useLanguage("de");
        writeAdminFile("de", "common:\n  list:\n    and: '{items} sowie {last}'\n");

        loader().useLanguage("de");

        assertEquals("Alpha sowie Beta", Messages.join(List.of("Alpha", "Beta")));
    }

    @Test
    void theLayersAreAskedInOrderAndTheShippedEnglishFileComesLast() {
        MessageCatalog adminFile = new MapMessageCatalog().with("menu.title", "Eigenes Menü");
        MessageCatalog shippedGerman = new MapMessageCatalog()
                .with("menu.title", "Menü")
                .with("menu.back", "Zurück");
        MessageCatalog shippedEnglish = new MapMessageCatalog()
                .with("menu.title", "Menu")
                .with("menu.back", "Back")
                .with("menu.close", "Close");

        LayeredMessageCatalog catalog = new LayeredMessageCatalog(List.of(adminFile, shippedGerman, shippedEnglish));

        assertEquals(Optional.of(List.of("Eigenes Menü")), catalog.find("menu.title"));
        assertEquals(Optional.of(List.of("Zurück")), catalog.find("menu.back"));
        assertEquals(Optional.of(List.of("Close")), catalog.find("menu.close"));
        assertEquals(Optional.empty(), catalog.find("menu.open"));
        assertEquals(List.of("menu.title", "menu.back", "menu.close"), List.copyOf(catalog.paths()));
    }

    @Test
    void theShippedGermanCatalogFallsBackToTheShippedEnglishFile() {
        MessageCatalog catalog = MessageCatalogLoader.shippedCatalog("de");

        assertEquals(MessageCatalogLoader.readShipped("de").paths(), catalog.paths());
        assertEquals(Optional.of(List.of("{items} und {last}")), catalog.find(CommonMessage.LIST_AND.path()));
    }

    private MessageCatalogLoader loader() {
        return new MessageCatalogLoader(languageFolder, warnings::add, adminNotices::add);
    }

    private void writeAdminFile(String language, String content) throws IOException {
        Files.writeString(languageFolder.resolve(MessageCatalogLoader.fileNameOf(language)), content,
                StandardCharsets.UTF_8);
    }

    private static String shippedContent(String language) throws IOException {
        try (InputStream shippedFile = MessageCatalogLoaderTest.class.getClassLoader()
                .getResourceAsStream(MessageCatalogLoader.resourcePathOf(language))) {
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

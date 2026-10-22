package de.simplepvparena.message;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShippedLanguageFilesTest {

    private static final List<Class<? extends MessageKey>> MESSAGE_KEY_TYPES = List.of(CommonMessage.class,
            MenuMessage.class, InputMessage.class, MainMenuMessage.class, MatchMenuMessage.class,
            WaitingMatchMessage.class, ConfirmationMessage.class, TeamMessage.class,
            TeamMenuMessage.class, TeamActionsMessage.class, KitMessage.class, KitChoiceMessage.class,
            KitMenuMessage.class, KitActionsMessage.class, ArenaSpawnMessage.class, ArenaSetupMessage.class,
            ArenaMenuMessage.class, ArenaCleaningMessage.class, ArenaHeightMessage.class,
            ArenaGapMessage.class, SpectatorMessage.class, BorderMessage.class, ListenerMessage.class,
            StartGateMessage.class, TournamentStatusMessage.class, FinalMessage.class, MatchMessage.class,
            TournamentMessage.class, CommandMessage.class, PlayerStateMessage.class, DryRunMessage.class,
            DryRunCheckMessage.class, ConfigMessage.class, PluginMessage.class, BracketViewMessage.class,
            AutoMatchMessage.class, AreaInventoryMessage.class, VisualMessage.class, RegistrationMessage.class,
            RegistrationMenuMessage.class, UnequalTeamVoteMessage.class, DecidedMatchMessage.class,
            TeamChoiceMessage.class);

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_-]+)}");
    private static final Pattern VALUE_LINE = Pattern.compile("^\\s*(- |[a-z0-9-]+: )(.*)$");

    @Test
    void bothFilesHoldTheSameKeys() {
        Set<String> englishPaths = MessageCatalogLoader.readShipped("en").paths();
        Set<String> germanPaths = MessageCatalogLoader.readShipped("de").paths();

        assertEquals(List.of(), difference(englishPaths, germanPaths), "keys of en.yml missing in de.yml");
        assertEquals(List.of(), difference(germanPaths, englishPaths), "keys of de.yml missing in en.yml");
    }

    @Test
    void everyKeyNamesTheSamePlaceholdersInBothFiles() {
        YamlMessageCatalog english = MessageCatalogLoader.readShipped("en");
        YamlMessageCatalog german = MessageCatalogLoader.readShipped("de");
        List<String> mismatches = new ArrayList<>();

        for (String path : english.paths()) {
            german.find(path).ifPresent(germanLines -> {
                Set<String> englishNames = placeholderNames(english.find(path).orElseThrow());
                Set<String> germanNames = placeholderNames(germanLines);
                if (!englishNames.equals(germanNames)) {
                    mismatches.add(path + " en " + new TreeSet<>(englishNames) + " de " + new TreeSet<>(germanNames));
                }
            });
        }

        assertEquals(List.of(), mismatches);
    }

    @Test
    void everyConstantHasATextInTheEnglishFile() {
        YamlMessageCatalog english = MessageCatalogLoader.readShipped("en");
        List<String> constantsWithoutText = new ArrayList<>();

        for (MessageKey key : everyMessageKey()) {
            if (textsOf(key, english).isEmpty()) {
                constantsWithoutText.add(key + " " + key.path());
            }
        }

        assertEquals(List.of(), constantsWithoutText);
    }

    @Test
    void everyTextOfTheEnglishFileBelongsToAConstant() {
        Set<String> constantPaths = new HashSet<>();
        for (MessageKey key : everyMessageKey()) {
            constantPaths.add(key.path());
            constantPaths.add(key.path() + Messages.SINGULAR_SUFFIX);
            constantPaths.add(key.path() + Messages.PLURAL_SUFFIX);
        }

        assertEquals(List.of(), difference(MessageCatalogLoader.readShipped("en").paths(), constantPaths),
                "keys of en.yml without a constant");
    }

    @Test
    void everyConstantNamesThePlaceholdersOfItsText() {
        YamlMessageCatalog english = MessageCatalogLoader.readShipped("en");
        List<String> mismatches = new ArrayList<>();

        for (MessageKey key : everyMessageKey()) {
            Set<String> textNames = new HashSet<>();
            textsOf(key, english).values().forEach(lines -> textNames.addAll(placeholderNames(lines)));
            if (!textNames.equals(key.placeholderNames())) {
                mismatches.add(key + " code " + new TreeSet<>(key.placeholderNames()) + " en.yml "
                        + new TreeSet<>(textNames));
            }
        }

        assertEquals(List.of(), mismatches);
    }

    @Test
    void noTextIsEmptyOrHoldsTheSectionSignOrALineBreak() {
        List<String> problems = new ArrayList<>();

        for (String language : MessageCatalogLoader.SHIPPED_LANGUAGES) {
            YamlMessageCatalog catalog = MessageCatalogLoader.readShipped(language);
            for (String path : catalog.paths()) {
                List<String> lines = catalog.find(path).orElseThrow();
                if (lines.isEmpty()) {
                    problems.add(language + " " + path + " is an empty list");
                }
                for (String line : lines) {
                    if (line.isBlank()) {
                        problems.add(language + " " + path + " is empty");
                    }
                    if (line.contains("§")) {
                        problems.add(language + " " + path + " holds the section sign");
                    }
                    if (line.contains("\n") || line.contains("\r") || line.contains("\\n")) {
                        problems.add(language + " " + path + " holds a line break, write a list of lines instead");
                    }
                }
            }
        }

        assertEquals(List.of(), problems);
    }

    @Test
    void everyValueIsWrittenInSingleQuotes() throws IOException {
        List<String> problems = new ArrayList<>();

        for (String language : MessageCatalogLoader.SHIPPED_LANGUAGES) {
            List<String> fileLines = shippedLines(language);
            for (int lineIndex = 0; lineIndex < fileLines.size(); lineIndex++) {
                String fileLine = fileLines.get(lineIndex);
                Matcher valueLine = VALUE_LINE.matcher(fileLine);
                if (fileLine.isBlank() || fileLine.stripLeading().startsWith("#") || !valueLine.matches()) {
                    continue;
                }
                String value = valueLine.group(2);
                if (!value.startsWith("'") || !value.endsWith("'")) {
                    problems.add(language + ".yml line " + (lineIndex + 1) + ": " + fileLine);
                }
            }
        }

        assertEquals(List.of(), problems);
    }

    static List<MessageKey> everyMessageKey() {
        List<MessageKey> keys = new ArrayList<>();
        for (Class<? extends MessageKey> type : MESSAGE_KEY_TYPES) {
            keys.addAll(Arrays.asList(type.getEnumConstants()));
        }
        return keys;
    }

    private static Map<String, List<String>> textsOf(MessageKey key, MessageCatalog catalog) {
        Map<String, List<String>> texts = new LinkedHashMap<>();
        catalog.find(key.path()).ifPresent(lines -> texts.put(key.path(), lines));
        String singularPath = key.path() + Messages.SINGULAR_SUFFIX;
        String pluralPath = key.path() + Messages.PLURAL_SUFFIX;
        if (texts.isEmpty() && catalog.find(singularPath).isPresent() && catalog.find(pluralPath).isPresent()) {
            texts.put(singularPath, catalog.find(singularPath).orElseThrow());
            texts.put(pluralPath, catalog.find(pluralPath).orElseThrow());
        }
        return texts;
    }

    private static Set<String> placeholderNames(List<String> lines) {
        Set<String> names = new HashSet<>();
        for (String line : lines) {
            Matcher matcher = PLACEHOLDER.matcher(line);
            while (matcher.find()) {
                names.add(matcher.group(1));
            }
        }
        return names;
    }

    private static List<String> difference(Set<String> paths, Set<String> otherPaths) {
        return paths.stream().filter(path -> !otherPaths.contains(path)).sorted().toList();
    }

    private static List<String> shippedLines(String language) throws IOException {
        try (InputStream shippedFile = ShippedLanguageFilesTest.class.getClassLoader()
                .getResourceAsStream(MessageCatalogLoader.resourcePathOf(language))) {
            assertNotNull(shippedFile, language + ".yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}

package de.simplepvparena.config;

import de.simplepvparena.support.UseLanguage;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceChatSettingsTest {

    private static final String FALLBACK_PROBLEM = "voice-chat.enabled must be true or false, falling back to true";

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsTheIsolationSwitchedOn() {
        PluginSettings defaults = PluginSettings.defaults();

        assertTrue(defaults.voiceChatIsolationEnabled());
        assertEquals("voice-chat.enabled", PluginSettings.VOICE_CHAT_ENABLED_PATH);
        assertTrue(PluginSettings.CONFIGURATION_PATHS.contains(PluginSettings.VOICE_CHAT_ENABLED_PATH));
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsTheEntryWithAnEnglishComment() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int sectionIndex = lines.indexOf("voice-chat:");
        assertTrue(sectionIndex > 0, "config.yml has to hold the section voice-chat: " + lines);
        assertEquals("# SimpleVoiceChat during a match, only used when SimpleVoiceChat runs on the server.",
                lines.get(sectionIndex - 1));
        assertTrue(lines.get(sectionIndex + 1).startsWith("  # When true, the fighters only hear the other fighters"));
        assertEquals("  enabled: true", lines.get(sectionIndex + 2));
    }

    @Test
    void readsBothTruthValues() {
        assertFalse(PluginSettings.read(configurationWith(false), problems::add).voiceChatIsolationEnabled());
        assertTrue(PluginSettings.read(configurationWith(true), problems::add).voiceChatIsolationEnabled());
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToSwitchedOnForAValueThatIsNoTruthValueAndReportsIt() {
        for (Object writtenValue : new Object[] {"off", 0, "false"}) {
            problems.clear();

            assertTrue(PluginSettings.read(configurationWith(writtenValue), problems::add)
                    .voiceChatIsolationEnabled(), "a value of " + writtenValue);
            assertEquals(List.of(FALLBACK_PROBLEM), problems, "a value of " + writtenValue);
        }
    }

    @Test
    @UseLanguage("de")
    void reportsTheBrokenEntryInGerman() {
        PluginSettings.read(configurationWith("aus"), problems::add);

        assertEquals(List.of("voice-chat.enabled muss true oder false sein, stattdessen gilt true"), problems);
    }

    @Test
    void theStartAndEveryReloadHandTheSwitchOn() {
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWith(false));
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                liveConfiguration, problems::add);

        reloadService.applyStoredConfiguration();
        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.VOICE_CHAT_ENABLED));

        fileOnDisk.set(configurationWith(true));
        reloadService.reloadFromFile();
        assertTrue(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.VOICE_CHAT_ENABLED));
    }

    @Test
    void theReloadAnswerNamesTheVoiceChat() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  whether the fighters only hear the fighters in SimpleVoiceChat, at once"),
                lines.toString());
    }

    private static MemoryConfiguration configurationWith(Object enabled) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.VOICE_CHAT_ENABLED_PATH, enabled);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = VoiceChatSettingsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml has to be shipped");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}

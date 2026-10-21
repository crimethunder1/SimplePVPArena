package de.simplepvparena.config;

import de.simplepvparena.arena.ActiveArenaState;
import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaBorderConfirmation;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaCleaningVisualEffect;
import de.simplepvparena.arena.ArenaGapGuide;
import de.simplepvparena.arena.ArenaHeightMarkers;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSafetyZone;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.ConfigMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.FakeSpectatorItems;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconnectSettingsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsSixtySecondsToComeBack() {
        PluginSettings defaults = PluginSettings.defaults();

        assertEquals(60, defaults.reconnectSeconds());
        assertEquals(MatchService.DEFAULT_RECONNECT_SECONDS, defaults.reconnectSeconds());
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsTheEntryWithItsDefaultAndACommentInTheMatchSection() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int entryIndex = lines.indexOf("  reconnect-seconds: 60");

        assertTrue(entryIndex > lines.indexOf("match:"), "config.yml has to hold match.reconnect-seconds: " + lines);
        assertTrue(lines.get(entryIndex - 1).startsWith("  # "), "a comment explains match.reconnect-seconds");
    }

    @Test
    void takesZeroAndSixHundredSecondsWithoutAProblem() {
        for (int reconnectSeconds : new int[] {0, 1, 600}) {
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.RECONNECT_SECONDS_PATH, reconnectSeconds);

            assertEquals(reconnectSeconds, PluginSettings.read(configuration, problems::add).reconnectSeconds());
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackForAValueOutsideOfZeroToSixHundredOrNoWholeNumberAndReportsIt() {
        for (Object reconnectSeconds : new Object[] {-1, 601, "long", 12.5D, true}) {
            problems.clear();
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.RECONNECT_SECONDS_PATH, reconnectSeconds);

            PluginSettings settings = PluginSettings.read(configuration, problems::add);

            assertEquals(MatchService.DEFAULT_RECONNECT_SECONDS, settings.reconnectSeconds(),
                    "a value of " + reconnectSeconds);
            assertEquals(List.of("match.reconnect-seconds must be a whole number between 0 and 600 seconds, "
                    + "falling back to 60 seconds"), problems, "a value of " + reconnectSeconds + " is reported");
        }
    }

    @Test
    void writesAFallbackOfOneSecondInTheSingular() {
        assertEquals("match.reconnect-seconds must be a whole number between 0 and 600 seconds, falling back to "
                + "1 second", Messages.plural(ConfigMessage.RECONNECT_SECONDS_OUT_OF_RANGE, 1,
                Placeholder.of("path", PluginSettings.RECONNECT_SECONDS_PATH), Placeholder.of("minimum", 0),
                Placeholder.of("maximum", 600)));
    }

    @Test
    @UseLanguage("de")
    void reportsTheBrokenEntryInGerman() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.RECONNECT_SECONDS_PATH, 9999);

        PluginSettings.read(configuration, problems::add);

        assertEquals(List.of("match.reconnect-seconds muss eine ganze Zahl zwischen 0 und 600 Sekunden sein, "
                + "stattdessen gelten 60 Sekunden"), problems);
        assertEquals("match.reconnect-seconds muss eine ganze Zahl zwischen 0 und 600 Sekunden sein, stattdessen "
                + "gilt 1 Sekunde", Messages.plural(ConfigMessage.RECONNECT_SECONDS_OUT_OF_RANGE, 1,
                Placeholder.of("path", PluginSettings.RECONNECT_SECONDS_PATH), Placeholder.of("minimum", 0),
                Placeholder.of("maximum", 600)));
    }

    @Test
    void aReloadChangesTheSecondsOfTheRunningMatchService() {
        MatchService matchService = createMatchService();
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWithSeconds(0));
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                createLiveConfiguration(matchService), problems::add);
        assertEquals(60, matchService.getReconnectSeconds(), "the match service starts with the default");

        assertTrue(reloadService.reloadFromFile().isClean());

        assertEquals(0, matchService.getReconnectSeconds());

        fileOnDisk.set(configurationWithSeconds(120));
        reloadService.reloadFromFile();

        assertEquals(120, matchService.getReconnectSeconds());

        fileOnDisk.set(configurationWithSeconds(-3));
        reloadService.reloadFromFile();

        assertEquals(60, matchService.getReconnectSeconds(), "a broken value falls back to the default");
        assertEquals(List.of("match.reconnect-seconds must be a whole number between 0 and 600 seconds, "
                + "falling back to 60 seconds"), problems);
    }

    @Test
    void theMatchServiceKeepsItsSecondsWithinZeroAndSixHundred() {
        MatchService matchService = createMatchService();

        matchService.setReconnectSeconds(-5);

        assertEquals(0, matchService.getReconnectSeconds());

        matchService.setReconnectSeconds(9999);

        assertEquals(600, matchService.getReconnectSeconds());
    }

    @Test
    void theReloadAnswerNamesTheNewValue() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  the seconds a fighter who lost the connection has to come back, for every "
                + "connection lost from now on"), lines.toString());
    }

    private static MemoryConfiguration configurationWithSeconds(Object reconnectSeconds) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.RECONNECT_SECONDS_PATH, reconnectSeconds);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = ReconnectSettingsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static Plugin createPlugin() {
        SharedBukkitServer.install();
        return SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> {
            throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    private static MatchService createMatchService() {
        return new MatchService(createPlugin(), new KitService(),
                new SpectatorService(new ArenaRegistry(), new FakeSpectatorItems()),
                SharedBukkitServer.proxy(FighterColours.class, (method, arguments) -> null), StartGates.NONE);
    }

    private PluginLiveConfiguration createLiveConfiguration(MatchService matchService) {
        Plugin plugin = createPlugin();
        ArenaRegistry arenaRegistry = new ArenaRegistry();
        BorderVisualEffect borderVisualEffect = SharedBukkitServer.proxy(BorderVisualEffect.class,
                (method, arguments) -> null);
        ArenaCleaningService cleaningService = new ArenaCleaningService(plugin, (cleanedWorld, shape) -> 0);
        ArenaBaselineService baselineService = new ArenaBaselineService(plugin, new ArenaBaselineRegistry());
        ArenaBorderConfirmation borderConfirmation = new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape);
        BorderWallPreview wallPreview = new BorderWallPreview(plugin, borderVisualEffect);
        ArenaSetupService setupService = new ArenaSetupService(
                new ActiveArenaState(arenaRegistry, new ArenaHeightMarkers()), matchService,
                new SpectatorBorderService(new BorderMarkIndex(), () -> { }), wallPreview, new ArenaGapGuide(plugin),
                cleaningService, baselineService, borderConfirmation, () -> { });
        return new PluginLiveConfiguration(matchService, new StartGateDuration(),
                SharedBukkitServer.proxy(FighterColours.class, (method, arguments) -> null), cleaningService,
                SharedBukkitServer.proxy(ArenaCleaningVisualEffect.class, (method, arguments) -> null),
                baselineService, setupService,
                new ArenaSafetyZone(arenaRegistry.getArena()::getShape, playerUniqueId -> false), arenaRegistry,
                new BorderParticleService(plugin, new BorderMarkIndex(), borderVisualEffect,
                        playerUniqueId -> false, borderConfirmation),
                wallPreview, borderVisualEffect, new TeamSizeSetting(), new TeamRegistry(), () -> false,
                notice -> { }, problems::add, language -> { }, autoMatchSettings -> { });
    }
}

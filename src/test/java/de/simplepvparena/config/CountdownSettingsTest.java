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

class CountdownSettingsTest {

    private static final String FALLBACK_PROBLEM = "match.countdown-seconds must be a whole number between 0 and 10 "
            + "seconds, falling back to 3 seconds";

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsThreeSecondsOfCountdown() {
        PluginSettings defaults = PluginSettings.defaults();

        assertEquals(3, defaults.countdownSeconds());
        assertEquals(MatchService.DEFAULT_COUNTDOWN_SECONDS, defaults.countdownSeconds());
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsTheEntryWithItsDefaultAndACommentRightAfterTheStartGate() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int entryIndex = lines.indexOf("  countdown-seconds: 3");

        assertTrue(entryIndex > lines.indexOf("match:"), "config.yml has to hold match.countdown-seconds: " + lines);
        assertTrue(lines.get(entryIndex - 1).startsWith("  # "), "a comment explains match.countdown-seconds");
        assertEquals("  start-gate-duration-seconds: 10", lines.get(entryIndex - 2));
    }

    @Test
    void takesZeroToTenSecondsWithoutAProblem() {
        for (int countdownSeconds : new int[] {0, 1, 3, 10}) {
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.COUNTDOWN_SECONDS_PATH, countdownSeconds);

            assertEquals(countdownSeconds, PluginSettings.read(configuration, problems::add).countdownSeconds());
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackForAValueOutsideOfZeroToTenOrNoWholeNumberAndReportsIt() {
        for (Object countdownSeconds : new Object[] {-1, 11, "short", 2.5D, true}) {
            problems.clear();
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.COUNTDOWN_SECONDS_PATH, countdownSeconds);

            PluginSettings settings = PluginSettings.read(configuration, problems::add);

            assertEquals(MatchService.DEFAULT_COUNTDOWN_SECONDS, settings.countdownSeconds(),
                    "a value of " + countdownSeconds);
            assertEquals(List.of(FALLBACK_PROBLEM), problems, "a value of " + countdownSeconds + " is reported");
        }
    }

    @Test
    void writesAFallbackOfOneSecondInTheSingular() {
        assertEquals("match.countdown-seconds must be a whole number between 0 and 10 seconds, falling back to "
                + "1 second", Messages.plural(ConfigMessage.COUNTDOWN_SECONDS_OUT_OF_RANGE, 1,
                Placeholder.of("path", PluginSettings.COUNTDOWN_SECONDS_PATH), Placeholder.of("minimum", 0),
                Placeholder.of("maximum", 10)));
    }

    @Test
    @UseLanguage("de")
    void reportsTheBrokenEntryInGerman() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.COUNTDOWN_SECONDS_PATH, 99);

        PluginSettings.read(configuration, problems::add);

        assertEquals(List.of("match.countdown-seconds muss eine ganze Zahl zwischen 0 und 10 Sekunden sein, "
                + "stattdessen gelten 3 Sekunden"), problems);
        assertEquals("match.countdown-seconds muss eine ganze Zahl zwischen 0 und 10 Sekunden sein, stattdessen "
                + "gilt 1 Sekunde", Messages.plural(ConfigMessage.COUNTDOWN_SECONDS_OUT_OF_RANGE, 1,
                Placeholder.of("path", PluginSettings.COUNTDOWN_SECONDS_PATH), Placeholder.of("minimum", 0),
                Placeholder.of("maximum", 10)));
    }

    @Test
    void aReloadChangesTheSecondsOfTheRunningMatchService() {
        MatchService matchService = createMatchService();
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWithSeconds(0));
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                createLiveConfiguration(matchService), problems::add);
        assertEquals(3, matchService.getCountdownSeconds(), "the match service starts with the default");

        assertTrue(reloadService.reloadFromFile().isClean());

        assertEquals(0, matchService.getCountdownSeconds());

        fileOnDisk.set(configurationWithSeconds(10));
        reloadService.reloadFromFile();

        assertEquals(10, matchService.getCountdownSeconds());

        fileOnDisk.set(configurationWithSeconds(-3));
        reloadService.reloadFromFile();

        assertEquals(3, matchService.getCountdownSeconds(), "a broken value falls back to the default");
        assertEquals(List.of(FALLBACK_PROBLEM), problems);
    }

    @Test
    void theMatchServiceKeepsItsSecondsWithinZeroAndTen() {
        MatchService matchService = createMatchService();

        matchService.setCountdownSeconds(-5);

        assertEquals(0, matchService.getCountdownSeconds());

        matchService.setCountdownSeconds(99);

        assertEquals(10, matchService.getCountdownSeconds());
    }

    @Test
    void theReloadAnswerNamesTheNewValueRightAfterTheStartGate() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        int countdownIndex = lines.indexOf("  the seconds of the countdown right before the fight, from the next "
                + "round on");

        assertTrue(countdownIndex > 0, lines.toString());
        assertEquals("  the seconds the start gate takes at the beginning of a round", lines.get(countdownIndex - 1));
    }

    @Test
    @UseLanguage("de")
    void theReloadAnswerNamesTheNewValueInGerman() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  die Sekunden des Countdowns direkt vor dem Kampf, ab der nächsten Runde"),
                lines.toString());
    }

    private static MemoryConfiguration configurationWithSeconds(Object countdownSeconds) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.COUNTDOWN_SECONDS_PATH, countdownSeconds);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = CountdownSettingsTest.class.getResourceAsStream("/config.yml")) {
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
                notice -> { }, problems::add, language -> { }, autoMatchSettings -> { },
                enabled -> { });
    }
}

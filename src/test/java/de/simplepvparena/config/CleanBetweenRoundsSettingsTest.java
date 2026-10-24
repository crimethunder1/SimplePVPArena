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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanBetweenRoundsSettingsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsTheCleaningBetweenTheRoundsSwitchedOn() {
        PluginSettings defaults = PluginSettings.defaults();

        assertTrue(defaults.cleanBetweenRounds());
        assertTrue(MatchService.DEFAULT_CLEAN_BETWEEN_ROUNDS);
        assertTrue(createMatchService().isCleanBetweenRounds(), "the match service starts with the default");
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsTheEntryWithItsDefaultAndACommentInTheMatchSection() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int entryIndex = lines.indexOf("  clean-between-rounds: true");

        assertTrue(entryIndex > lines.indexOf("match:"), "config.yml has to hold match.clean-between-rounds: "
                + lines);
        assertTrue(lines.get(entryIndex - 1).startsWith("  # "), "a comment explains match.clean-between-rounds");
        assertTrue(PluginSettings.CONFIGURATION_PATHS.contains("match.clean-between-rounds"));
    }

    @Test
    void readsTrueAndFalseWithoutAProblem() {
        for (boolean cleanBetweenRounds : new boolean[] {true, false}) {
            MemoryConfiguration configuration = configurationWith(cleanBetweenRounds);

            assertEquals(cleanBetweenRounds, PluginSettings.read(configuration, problems::add).cleanBetweenRounds());
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToTrueForAnythingButATruthValueAndReportsIt() {
        for (Object cleanBetweenRounds : new Object[] {"maybe", 1, 0.5D, "off"}) {
            problems.clear();

            PluginSettings settings = PluginSettings.read(configurationWith(cleanBetweenRounds), problems::add);

            assertTrue(settings.cleanBetweenRounds(), "a value of " + cleanBetweenRounds);
            assertEquals(List.of("match.clean-between-rounds must be true or false, falling back to true"), problems,
                    "a value of " + cleanBetweenRounds + " is reported");
        }
    }

    @Test
    @UseLanguage("de")
    void reportsTheBrokenEntryInGerman() {
        PluginSettings.read(configurationWith("vielleicht"), problems::add);

        assertEquals(List.of("match.clean-between-rounds muss true oder false sein, stattdessen gilt true"),
                problems);
        assertTrue(ConfigurationReloadReport.describeReloadedCategories().contains("ob die Arena zwischen den "
                + "Runden eines Matches gereinigt wird, ab dem nächsten Rundenende"));
    }

    @Test
    void aReloadSwitchesTheCleaningOfTheRunningMatchService() {
        MatchService matchService = createMatchService();
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWith(false));
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                createLiveConfiguration(matchService), problems::add);

        assertTrue(reloadService.reloadFromFile().isClean());

        assertFalse(matchService.isCleanBetweenRounds());

        fileOnDisk.set(configurationWith(true));
        reloadService.reloadFromFile();

        assertTrue(matchService.isCleanBetweenRounds());

        fileOnDisk.set(configurationWith(false));
        reloadService.reloadFromFile();
        fileOnDisk.set(configurationWith("broken"));
        reloadService.reloadFromFile();

        assertTrue(matchService.isCleanBetweenRounds(), "a broken value falls back to the default");
        assertEquals(List.of("match.clean-between-rounds must be true or false, falling back to true"), problems);
    }

    @Test
    void theReloadAnswerNamesTheNewValue() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  whether the arena is cleaned between the rounds of a match, from the next end "
                + "of a round on"), lines.toString());
    }

    private static MemoryConfiguration configurationWith(Object cleanBetweenRounds) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.CLEAN_BETWEEN_ROUNDS_PATH, cleanBetweenRounds);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = CleanBetweenRoundsSettingsTest.class.getResourceAsStream("/config.yml")) {
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
                notice -> { }, problems::add, language -> { }, autoMatchSettings -> { }, teamMode -> { },
                enabled -> { }, settings -> { }, enabled -> { }, enabled -> { });
    }
}

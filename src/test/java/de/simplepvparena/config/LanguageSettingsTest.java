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
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.FakeSpectatorItems;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageSettingsTest {

    @TempDir
    Path languageFolder;

    private final List<String> problems = new ArrayList<>();

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    void aShippedLanguageIsTakenInAnySpelling() {
        assertEquals("de", PluginSettings.readLanguage(configurationWithLanguage("de"), problems::add));
        assertEquals("de", PluginSettings.readLanguage(configurationWithLanguage(" DE "), problems::add));
        assertEquals("en", PluginSettings.readLanguage(configurationWithLanguage("En"), problems::add));
        PluginSettings.read(configurationWithLanguage(" DE "), problems::add);
        assertEquals(List.of(), problems);
    }

    @Test
    void aFileWithoutTheLanguageIsEnglishWithoutAProblem() {
        assertEquals(MessageCatalogLoader.DEFAULT_LANGUAGE,
                PluginSettings.readLanguage(new MemoryConfiguration(), problems::add));
        assertEquals(MessageCatalogLoader.DEFAULT_LANGUAGE, PluginSettings.readLanguage(null, problems::add));
        PluginSettings.read(new MemoryConfiguration(), problems::add);
        assertEquals(List.of(), problems);
    }

    @Test
    void anUnknownLanguageFallsBackToEnglishAndIsReported() {
        assertEquals("en", PluginSettings.readLanguage(configurationWithLanguage("fr"), problem -> { }));

        PluginSettings.read(configurationWithLanguage("fr"), problems::add);

        assertEquals(List.of("language fr is no shipped language, falling back to en. Shipped languages: en and de"),
                problems);
    }

    @Test
    void aLanguageThatIsNoTextFallsBackToEnglishAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.LANGUAGE_PATH, 7);

        assertEquals("en", PluginSettings.readLanguage(configuration, problems::add));
        assertEquals(List.of("language 7 is no shipped language, falling back to en. Shipped languages: en and de"),
                problems);
    }

    @Test
    void aReloadAppliesTheLanguageBeforeEveryOtherValue() {
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();

        reloadServiceFor(configurationWithLanguage("de"), liveConfiguration).reloadFromFile();

        assertEquals(RecordingLiveConfiguration.LANGUAGE, liveConfiguration.appliedValueNames().get(0));
        assertEquals("de", liveConfiguration.valueOf(RecordingLiveConfiguration.LANGUAGE));
    }

    @Test
    void theStartLeavesTheLanguageToThePluginThatLoadedItBeforeAnyText() {
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();

        reloadServiceFor(configurationWithLanguage("de"), liveConfiguration).applyStoredConfiguration();

        assertNull(liveConfiguration.valueOf(RecordingLiveConfiguration.LANGUAGE));
    }

    @Test
    void theReloadAnswerNamesTheLanguageAndAnUnknownOne() {
        List<String> lines = reloadServiceFor(configurationWithLanguage("fr"), new RecordingLiveConfiguration())
                .reloadFromFile().describe();

        assertTrue(lines.contains("  the language and the language files in the lang folder, for every text shown "
                + "from now on"), lines.toString());
        assertTrue(lines.contains("  language fr is no shipped language, falling back to en. Shipped languages: "
                + "en and de"), lines.toString());
    }

    @Test
    void aReloadSwitchesTheTextsOfTheRunningPlugin() {
        MessageCatalogLoader messageCatalogLoader = new MessageCatalogLoader(languageFolder, problems::add,
                notice -> { });
        messageCatalogLoader.installShippedFiles();
        PluginLiveConfiguration liveConfiguration = createLiveConfiguration(messageCatalogLoader::useLanguage);

        reloadServiceFor(configurationWithLanguage("de"), liveConfiguration).reloadFromFile();
        String germanText = Messages.join(List.of("Alpha", "Beta"));
        reloadServiceFor(configurationWithLanguage("en"), liveConfiguration).reloadFromFile();
        String englishText = Messages.join(List.of("Alpha", "Beta"));

        assertEquals("Alpha und Beta", germanText);
        assertEquals("Alpha and Beta", englishText);
        assertEquals(List.of(), problems);
    }

    @Test
    void aReloadThatSwitchesToGermanWritesTheWarningsInGerman() {
        PluginLiveConfiguration liveConfiguration = createLiveConfigurationWithShippedLanguageFiles();
        MemoryConfiguration configuration = configurationWithLanguage("de");
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -1);
        String germanWarning = "arena.safety-zone-radius darf nicht negativ sein, stattdessen gelten 64 Blöcke. Mit "
                + "null schaltest du die Schutzzone ab";

        List<String> lines = reloadServiceFor(configuration, liveConfiguration).reloadFromFile().describe();

        assertEquals(List.of(germanWarning), problems);
        assertEquals("config.yml wurde neu geladen und angewendet", lines.get(0));
        assertEquals("  " + germanWarning, lines.get(lines.size() - 1));
    }

    @Test
    void aReloadThatFallsBackFromGermanToEnglishWritesTheWarningsInEnglish() {
        PluginLiveConfiguration liveConfiguration = createLiveConfigurationWithShippedLanguageFiles();
        reloadServiceFor(configurationWithLanguage("de"), liveConfiguration).reloadFromFile();
        MemoryConfiguration configuration = configurationWithLanguage("fr");
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -1);

        List<String> lines = reloadServiceFor(configuration, liveConfiguration).reloadFromFile().describe();

        assertEquals(List.of("arena.safety-zone-radius cannot be negative, falling back to 64 blocks. Use zero to "
                        + "switch the safety zone off",
                "language fr is no shipped language, falling back to en. Shipped languages: en and de"), problems);
        assertEquals("config.yml was reloaded and applied", lines.get(0));
        assertEquals("  language fr is no shipped language, falling back to en. Shipped languages: en and de",
                lines.get(lines.size() - 1));
    }

    private PluginLiveConfiguration createLiveConfigurationWithShippedLanguageFiles() {
        MessageCatalogLoader messageCatalogLoader = new MessageCatalogLoader(languageFolder, problems::add,
                notice -> { });
        messageCatalogLoader.installShippedFiles();
        return createLiveConfiguration(messageCatalogLoader::useLanguage);
    }

    private ConfigurationReloadService reloadServiceFor(MemoryConfiguration configuration,
                                                        LiveConfiguration liveConfiguration) {
        return new ConfigurationReloadService(() -> configuration, () -> { }, liveConfiguration, problems::add);
    }

    private static MemoryConfiguration configurationWithLanguage(String language) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.LANGUAGE_PATH, language);
        return configuration;
    }

    private PluginLiveConfiguration createLiveConfiguration(Consumer<String> languageApplier) {
        SharedBukkitServer.install();
        ArenaRegistry arenaRegistry = new ArenaRegistry();
        Plugin plugin = SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> {
            throw new UnsupportedOperationException("Plugin." + method.getName());
        });
        BorderVisualEffect borderVisualEffect = SharedBukkitServer.proxy(BorderVisualEffect.class,
                (method, arguments) -> null);
        MatchService matchService = new MatchService(plugin, new KitService(),
                new SpectatorService(arenaRegistry, new FakeSpectatorItems()),
                SharedBukkitServer.proxy(FighterColours.class, (method, arguments) -> null), StartGates.NONE);
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
                notice -> { }, problems::add, languageApplier, autoMatchSettings -> { }, teamMode -> { },
                enabled -> { }, settings -> { });
    }
}

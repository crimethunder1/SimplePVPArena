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
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
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

class ConfirmedBorderParticleSwitchSettingsTest {

    private static final String FALLBACK_PROBLEM = "border.confirmed-particle.enabled must be true or false, "
            + "falling back to true";

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsTheParticlesShown() {
        PluginSettings defaults = PluginSettings.defaults();

        assertTrue(defaults.confirmedBorderParticleEnabled());
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsTheEntryWithACommentAtTheTopOfTheMarkerSection() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int sectionIndex = lines.indexOf("  confirmed-particle:");
        int entryIndex = sectionIndex + lines.subList(sectionIndex, lines.size()).indexOf("    enabled: true");

        assertTrue(entryIndex > sectionIndex, "config.yml has to hold border.confirmed-particle.enabled: " + lines);
        assertEquals("    # false hides the marker, the outline shown after a calculation stays. The arena menu "
                + "writes this value.", lines.get(entryIndex - 1));
        assertEquals("  confirmed-particle:", lines.get(entryIndex - 2));
        assertEquals("    type: END_ROD", lines.get(entryIndex + 2));
        assertTrue(PluginSettings.CONFIGURATION_PATHS.contains(
                ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH));
    }

    @Test
    void readsBothTruthValuesWithoutAProblem() {
        assertFalse(PluginSettings.read(configurationWithSwitch(false), problems::add)
                .confirmedBorderParticleEnabled());
        assertTrue(PluginSettings.read(configurationWithSwitch(true), problems::add)
                .confirmedBorderParticleEnabled());
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToShownForAValueThatIsNoTruthValueAndReportsIt() {
        for (Object writtenValue : new Object[] {"hidden", 0, 1.5D}) {
            problems.clear();

            PluginSettings settings = PluginSettings.read(configurationWithSwitch(writtenValue), problems::add);

            assertTrue(settings.confirmedBorderParticleEnabled(), "a value of " + writtenValue);
            assertEquals(List.of(FALLBACK_PROBLEM), problems, "a value of " + writtenValue + " is reported");
        }
    }

    @Test
    @UseLanguage("de")
    void reportsTheBrokenEntryInGerman() {
        PluginSettings.read(configurationWithSwitch("versteckt"), problems::add);

        assertEquals(List.of("border.confirmed-particle.enabled muss true oder false sein, stattdessen gilt true"),
                problems);
    }

    @Test
    void aReloadSwitchesTheParticlesOfTheRunningServiceOffAndOnAgain() {
        BorderParticleService borderParticleService = createBorderParticleService();
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWithSwitch(false));
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                createLiveConfiguration(borderParticleService), problems::add);
        assertTrue(borderParticleService.isConfirmedParticleEnabled(), "the service starts with the particles shown");

        assertTrue(reloadService.reloadFromFile().isClean());

        assertFalse(borderParticleService.isConfirmedParticleEnabled());

        fileOnDisk.set(configurationWithSwitch(true));
        reloadService.reloadFromFile();

        assertTrue(borderParticleService.isConfirmedParticleEnabled());

        fileOnDisk.set(configurationWithSwitch(false));
        reloadService.reloadFromFile();
        fileOnDisk.set(configurationWithSwitch("hidden"));
        reloadService.reloadFromFile();

        assertTrue(borderParticleService.isConfirmedParticleEnabled(), "a broken value falls back to shown");
        assertEquals(List.of(FALLBACK_PROBLEM), problems);
    }

    @Test
    void theReloadAnswerNamesTheMarkerInTheBorderCategory() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  the border particles, their limits and the confirmed border marker"),
                lines.toString());
    }

    private static MemoryConfiguration configurationWithSwitch(Object enabled) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH, enabled);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = ConfirmedBorderParticleSwitchSettingsTest.class
                .getResourceAsStream("/config.yml")) {
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

    private static BorderParticleService createBorderParticleService() {
        return new BorderParticleService(createPlugin(), new BorderMarkIndex(),
                SharedBukkitServer.proxy(BorderVisualEffect.class, (method, arguments) -> null),
                playerUniqueId -> false, new ArenaBorderConfirmation(() -> false));
    }

    private PluginLiveConfiguration createLiveConfiguration(BorderParticleService borderParticleService) {
        Plugin plugin = createPlugin();
        MatchService matchService = new MatchService(plugin, new KitService(),
                new SpectatorService(new ArenaRegistry(), new FakeSpectatorItems()),
                SharedBukkitServer.proxy(FighterColours.class, (method, arguments) -> null), StartGates.NONE);
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
                borderParticleService, wallPreview, borderVisualEffect, new TeamSizeSetting(), new TeamRegistry(),
                () -> false, notice -> { }, problems::add, language -> { }, autoMatchSettings -> { }, teamMode -> { },
                enabled -> { }, settings -> { });
    }
}

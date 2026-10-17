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
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.arena.HorizontalCellCodec;
import de.simplepvparena.arena.PositionSnapshot;
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
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectAreaSettingsTest {

    private static final String WORLD_NAME = "event";
    private static final double PRECISION = 0.000001D;

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsAMarginOfNinetySixBlocksAndTheTeleportOnJoin() {
        PluginSettings defaults = PluginSettings.defaults();

        assertEquals(96, defaults.arenaEffectMargin());
        assertTrue(defaults.teleportOnJoin());
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void takesAMarginOfZeroAndOfFiveHundredTwelveWithoutAProblem() {
        for (int margin : new int[] {0, 512}) {
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.ARENA_EFFECT_MARGIN_PATH, margin);

            assertEquals(margin, PluginSettings.read(configuration, problems::add).arenaEffectMargin());
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackForAMarginOutsideOfZeroToFiveHundredTwelveOrNoNumberAndReportsIt() {
        for (Object margin : new Object[] {-1, 513, "wide", 12.5D, true}) {
            problems.clear();
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.ARENA_EFFECT_MARGIN_PATH, margin);

            PluginSettings settings = PluginSettings.read(configuration, problems::add);

            assertEquals(EffectArea.DEFAULT_MARGIN, settings.arenaEffectMargin(), "a margin of " + margin);
            assertEquals(List.of("arena.effect-margin must be a whole number between 0 and 512 blocks, "
                    + "falling back to 96 blocks"), problems, "a margin of " + margin + " is reported");
        }
    }

    @Test
    void readsWhetherAJoiningPlayerIsSentToTheTribune() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_TELEPORT_ON_JOIN_PATH, false);

        assertFalse(PluginSettings.read(configuration, problems::add).teleportOnJoin());

        configuration.set(PluginSettings.ARENA_TELEPORT_ON_JOIN_PATH, true);

        assertTrue(PluginSettings.read(configuration, problems::add).teleportOnJoin());
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsBothEntriesWithTheirDefaultAndAComment() throws IOException {
        List<String> lines = shippedConfigurationLines();

        assertCommentedEntry(lines, "  effect-margin: 96");
        assertCommentedEntry(lines, "  teleport-on-join: true");
    }

    @Test
    void aReloadChangesTheAreaOfEffectOfTheRunningPlugin() {
        ArenaRegistry arenaRegistry = new ArenaRegistry();
        arenaRegistry.getArena().setShape(rectangle());
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWithMargin(10));
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                createLiveConfiguration(arenaRegistry), problems::add);

        ConfigurationReloadReport firstReport = reloadService.reloadFromFile();

        assertTrue(firstReport.isClean(), firstReport.problems().toString());
        assertEquals(15.0D, arenaRegistry.findEffectArea().orElseThrow().getRadius(), PRECISION);
        assertFalse(arenaRegistry.isTeleportOnJoin());

        fileOnDisk.set(configurationWithMargin(40));
        reloadService.reloadFromFile();

        assertEquals(45.0D, arenaRegistry.findEffectArea().orElseThrow().getRadius(), PRECISION);
        assertEquals(List.of(), problems);
    }

    @Test
    void namesATribuneThatAReloadLeavesOutsideTheAreaInTheLogAndKeepsIt() {
        ArenaRegistry arenaRegistry = new ArenaRegistry();
        arenaRegistry.getArena().setShape(rectangle());
        PositionSnapshot tribune = new PositionSnapshot(WORLD_NAME, 30.5D, 64.0D, 4.0D, 0F, 0F);
        arenaRegistry.getArena().setTribuneSpawn(tribune);
        List<String> logLines = new ArrayList<>();
        ConfigurationReloadService reloadService = new ConfigurationReloadService(() -> configurationWithMargin(10),
                () -> { }, createLiveConfiguration(arenaRegistry, logLines::add), problems::add);

        ConfigurationReloadReport report = reloadService.reloadFromFile();

        assertTrue(report.isClean(), "the tribune is no broken entry of config.yml: " + report.problems());
        assertEquals(List.of("The tribune spawn event 30.5/64.0/4.0 lies outside of the area of effect, "
                + "27.5 blocks from its centre 3/4, radius 15 blocks. Nothing was dropped, set the tribune spawn "
                + "again closer to the fight area or raise arena.effect-margin in config.yml"), logLines);
        assertEquals(tribune, arenaRegistry.getArena().getConfiguredTribuneSpawn().orElseThrow());
        assertTrue(arenaRegistry.isArenaFullyConfigured());
    }

    @Test
    void theReloadAnswerNamesBothNewValues() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  the margin of the area of effect around the fight area"), lines.toString());
        assertTrue(lines.contains("  whether joining players are sent to the tribune"), lines.toString());
    }

    private static void assertCommentedEntry(List<String> lines, String entry) {
        int entryIndex = lines.indexOf(entry);
        assertTrue(entryIndex > 0, "config.yml has to hold " + entry);
        assertTrue(lines.get(entryIndex - 1).startsWith("  # "), "a comment explains " + entry);
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = EffectAreaSettingsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static MemoryConfiguration configurationWithMargin(int margin) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_EFFECT_MARGIN_PATH, margin);
        configuration.set(PluginSettings.ARENA_TELEPORT_ON_JOIN_PATH, false);
        return configuration;
    }

    private static ArenaShape rectangle() {
        List<Long> cells = new ArrayList<>();
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 7; z++) {
                cells.add(HorizontalCellCodec.encode(x, z));
            }
        }
        return new ArenaShape(WORLD_NAME, 64, 67, cells.stream().mapToLong(Long::longValue).toArray());
    }

    private PluginLiveConfiguration createLiveConfiguration(ArenaRegistry arenaRegistry) {
        return createLiveConfiguration(arenaRegistry, problems::add);
    }

    private static PluginLiveConfiguration createLiveConfiguration(ArenaRegistry arenaRegistry,
                                                                   Consumer<String> logger) {
        SharedBukkitServer.install();
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
                wallPreview, borderVisualEffect, logger);
    }
}

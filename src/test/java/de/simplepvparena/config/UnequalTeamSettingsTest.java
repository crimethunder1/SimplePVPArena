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
import de.simplepvparena.match.UnequalTeamSettings;
import de.simplepvparena.match.UnequalTeamsMode;
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

class UnequalTeamSettingsTest {

    private static final String UNKNOWN_MODE_PROBLEM =
            "match.unequal-teams.mode must be one of vote and all, falling back to vote";
    private static final String VOTE_SECONDS_PROBLEM = "match.unequal-teams.vote-seconds must be a whole number "
            + "between 5 and 120 seconds, falling back to 30 seconds";

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsTheVoteWithThirtySeconds() {
        PluginSettings defaults = PluginSettings.defaults();

        assertEquals(new UnequalTeamSettings(UnequalTeamsMode.VOTE, 30), defaults.unequalTeamSettings());
        assertEquals(UnequalTeamSettings.defaults(), defaults.unequalTeamSettings());
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsBothEntriesWithTheirDefaultsAndACommentInTheMatchSection() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int sectionIndex = lines.indexOf("  unequal-teams:");
        int modeIndex = lines.indexOf("    mode: vote");
        int voteSecondsIndex = lines.indexOf("    vote-seconds: 30");

        assertTrue(sectionIndex > lines.indexOf("match:"), "config.yml has to hold match.unequal-teams: " + lines);
        assertTrue(sectionIndex < lines.indexOf("auto-match:"), "the section belongs into the match section");
        assertTrue(lines.get(sectionIndex - 1).startsWith("  # "), "a comment explains match.unequal-teams");
        assertEquals(sectionIndex + 2, modeIndex);
        assertTrue(lines.get(modeIndex - 1).startsWith("    # "), "a comment explains the mode");
        assertEquals(modeIndex + 2, voteSecondsIndex);
        assertTrue(lines.get(voteSecondsIndex - 1).startsWith("    # "), "a comment explains the vote seconds");
    }

    @Test
    void readsBothModesWhateverTheCaseAndTheSpacesAround() {
        assertEquals(UnequalTeamsMode.ALL, readMode("all"));
        assertEquals(UnequalTeamsMode.ALL, readMode(" ALL "));
        assertEquals(UnequalTeamsMode.VOTE, readMode("vote"));
        assertEquals(UnequalTeamsMode.VOTE, readMode("Vote"));
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToTheVoteForAnUnknownModeAndReportsIt() {
        for (Object mode : new Object[] {"everyone", "", 3, true}) {
            problems.clear();

            assertEquals(UnequalTeamsMode.VOTE, readMode(mode), "a mode of " + mode);
            assertEquals(List.of(UNKNOWN_MODE_PROBLEM), problems, "a mode of " + mode + " is reported");
        }
    }

    @Test
    void takesFiveAndOneHundredTwentySecondsWithoutAProblem() {
        for (int voteSeconds : new int[] {5, 30, 120}) {
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.UNEQUAL_TEAMS_VOTE_SECONDS_PATH, voteSeconds);

            assertEquals(voteSeconds, PluginSettings.read(configuration, problems::add).unequalTeamSettings()
                    .voteSeconds());
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackForSecondsOutsideOfFiveToOneHundredTwentyOrNoWholeNumberAndReportsIt() {
        for (Object voteSeconds : new Object[] {4, 121, -1, "long", 12.5D, true}) {
            problems.clear();
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.UNEQUAL_TEAMS_VOTE_SECONDS_PATH, voteSeconds);

            PluginSettings settings = PluginSettings.read(configuration, problems::add);

            assertEquals(UnequalTeamSettings.DEFAULT_VOTE_SECONDS, settings.unequalTeamSettings().voteSeconds(),
                    "a value of " + voteSeconds);
            assertEquals(List.of(VOTE_SECONDS_PROBLEM), problems, "a value of " + voteSeconds + " is reported");
        }
    }

    @Test
    void keepsTheValidEntryWhenTheOtherOneIsBroken() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.UNEQUAL_TEAMS_MODE_PATH, "all");
        configuration.set(PluginSettings.UNEQUAL_TEAMS_VOTE_SECONDS_PATH, 999);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(new UnequalTeamSettings(UnequalTeamsMode.ALL, 30), settings.unequalTeamSettings());
        assertEquals(List.of(VOTE_SECONDS_PROBLEM), problems);
    }

    @Test
    void writesAFallbackOfOneSecondInTheSingular() {
        assertEquals("match.unequal-teams.vote-seconds must be a whole number between 5 and 120 seconds, falling "
                + "back to 1 second", Messages.plural(ConfigMessage.UNEQUAL_TEAMS_VOTE_SECONDS_OUT_OF_RANGE, 1,
                Placeholder.of("path", PluginSettings.UNEQUAL_TEAMS_VOTE_SECONDS_PATH), Placeholder.of("minimum", 5),
                Placeholder.of("maximum", 120)));
    }

    @Test
    @UseLanguage("de")
    void reportsBothBrokenEntriesInGerman() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.UNEQUAL_TEAMS_MODE_PATH, "jeder");
        configuration.set(PluginSettings.UNEQUAL_TEAMS_VOTE_SECONDS_PATH, 2);

        PluginSettings.read(configuration, problems::add);

        assertEquals(List.of("match.unequal-teams.mode muss einer der Werte vote und all sein, stattdessen gilt vote",
                "match.unequal-teams.vote-seconds muss eine ganze Zahl zwischen 5 und 120 Sekunden sein, stattdessen "
                        + "gelten 30 Sekunden"), problems);
    }

    @Test
    void keepsTheSecondsOfTheSettingsWithinFiveAndOneHundredTwenty() {
        assertEquals(5, new UnequalTeamSettings(UnequalTeamsMode.VOTE, 1).voteSeconds());
        assertEquals(120, new UnequalTeamSettings(UnequalTeamsMode.VOTE, 9999).voteSeconds());
    }

    @Test
    void aReloadChangesTheSettingsOfTheRunningMatchService() {
        MatchService matchService = createMatchService();
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWith("all", 60));
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                createLiveConfiguration(matchService), problems::add);
        assertEquals(UnequalTeamSettings.defaults(), matchService.getUnequalTeamSettings(),
                "the match service starts with the default");

        assertTrue(reloadService.reloadFromFile().isClean());

        assertEquals(new UnequalTeamSettings(UnequalTeamsMode.ALL, 60), matchService.getUnequalTeamSettings());

        fileOnDisk.set(configurationWith("vote", 3));
        reloadService.reloadFromFile();

        assertEquals(new UnequalTeamSettings(UnequalTeamsMode.VOTE, 30), matchService.getUnequalTeamSettings(),
                "a broken value falls back to the default");
        assertEquals(List.of(VOTE_SECONDS_PROBLEM), problems);
    }

    @Test
    void theReloadHandsTheSettingsOnRightAfterTheReconnectSeconds() {
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();

        new ConfigurationReloadService(() -> configurationWith("all", 45), () -> { }, liveConfiguration,
                problems::add).reloadFromFile();

        assertEquals(new UnequalTeamSettings(UnequalTeamsMode.ALL, 45), liveConfiguration.unequalTeamSettings());
        List<String> appliedValueNames = liveConfiguration.appliedValueNames();
        assertEquals(appliedValueNames.indexOf(RecordingLiveConfiguration.RECONNECT_SECONDS) + 1,
                appliedValueNames.indexOf(RecordingLiveConfiguration.UNEQUAL_TEAMS));
    }

    @Test
    void theReloadAnswerNamesTheNewValuesRightAfterTheReconnectSeconds() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        int unequalTeamsIndex = lines.indexOf("  whether the larger team votes who fights when the teams have "
                + "different numbers of players online and the seconds of the vote, from the next match on");
        assertTrue(unequalTeamsIndex > 0, lines.toString());
        assertTrue(lines.get(unequalTeamsIndex - 1).startsWith("  the seconds a fighter who lost the connection"),
                lines.toString());
    }

    @Test
    @UseLanguage("de")
    void theReloadAnswerNamesTheNewValuesInGerman() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  ob das größere Team abstimmt, wer kämpft, wenn die Teams unterschiedlich viele "
                + "Spieler online haben, und die Sekunden der Abstimmung, ab dem nächsten Match"), lines.toString());
    }

    private UnequalTeamsMode readMode(Object mode) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.UNEQUAL_TEAMS_MODE_PATH, mode);
        return PluginSettings.read(configuration, problems::add).unequalTeamSettings().mode();
    }

    private static MemoryConfiguration configurationWith(Object mode, Object voteSeconds) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.UNEQUAL_TEAMS_MODE_PATH, mode);
        configuration.set(PluginSettings.UNEQUAL_TEAMS_VOTE_SECONDS_PATH, voteSeconds);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = UnequalTeamSettingsTest.class.getResourceAsStream("/config.yml")) {
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
                enabled -> { }, settings -> { });
    }
}

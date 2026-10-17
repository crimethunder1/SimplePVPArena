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
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import de.simplepvparena.team.TeamStorage;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamSizeSettingsTest {

    private final List<String> problems = new ArrayList<>();
    private final List<String> logLines = new ArrayList<>();
    private final List<String> adminNotices = new ArrayList<>();
    private final TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
    private final TeamRegistry teamRegistry = new TeamRegistry();

    private boolean tournamentInProgress;

    @Test
    void shipsTeamsOfTwoPlayers() {
        assertEquals(2, PluginSettings.defaults().teamSize());
        assertEquals(2, PluginSettings.read(new MemoryConfiguration(), problems::add).teamSize());
        assertEquals(List.of(), problems);
    }

    @Test
    void readsEveryTeamSizeFromOneToEightWithoutAProblem() {
        for (int teamSize = 1; teamSize <= 8; teamSize++) {
            assertEquals(teamSize, PluginSettings.read(configurationWithTeamSize(teamSize), problems::add).teamSize());
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToTwoForATeamSizeOutsideOfOneToEightOrNoWholeNumberAndReportsIt() {
        for (Object teamSize : new Object[] {0, -3, 9, 100, "three", 2.5D, 3.0D, true}) {
            problems.clear();

            PluginSettings settings = PluginSettings.read(configurationWithTeamSize(teamSize), problems::add);

            assertEquals(2, settings.teamSize(), "a team size of " + teamSize);
            assertEquals(List.of("match.team-size must be a whole number between 1 and 8 players, "
                    + "falling back to 2 players"), problems, "a team size of " + teamSize + " is reported");
        }
    }

    @Test
    void shipsTheTeamSizeInTheMatchSectionWithItsDefaultAndAComment() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int entryIndex = lines.indexOf("  team-size: 2");
        assertTrue(entryIndex > lines.indexOf("match:"), "config.yml holds match.team-size");
        assertTrue(lines.get(entryIndex - 1).startsWith("  # "), "a comment explains match.team-size");
        assertTrue(PluginSettings.CONFIGURATION_PATHS.contains(PluginSettings.TEAM_SIZE_PATH));
    }

    @Test
    void theReloadAnswerNamesTheTeamSize() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  the team size, unless a tournament is in progress or a team has more players "
                + "than the new size"), lines.toString());
    }

    @Test
    void aReloadChangesTheTeamSizeOfTheRunningPlugin() {
        ConfigurationReloadService reloadService = reloadServiceFor(configurationWithTeamSize(5));

        ConfigurationReloadReport report = reloadService.reloadFromFile();

        assertTrue(report.isClean(), report.problems().toString());
        assertEquals(5, teamSizeSetting.getTeamSize());
        assertNoWarning();
    }

    @Test
    void aReloadKeepsTheTeamSizeWhileATournamentIsInProgressAndWarnsTheLogAndTheAdmins() {
        teamSizeSetting.setTeamSize(3);
        tournamentInProgress = true;

        ConfigurationReloadReport report = reloadServiceFor(configurationWithTeamSize(5)).reloadFromFile();

        assertTrue(report.isClean(), "the value itself is no broken entry: " + report.problems());
        assertEquals(3, teamSizeSetting.getTeamSize());
        String warning = "The team size cannot be changed while a tournament is in progress. The team size 5 of "
                + "match.team-size in config.yml was not taken over, the team size stays 3 (3 versus 3)";
        assertEquals(List.of(warning), logLines);
        assertEquals(List.of(warning), adminNotices);
    }

    @Test
    void aReloadDuringATournamentWithTheSameTeamSizeWarnsNobody() {
        teamSizeSetting.setTeamSize(3);
        tournamentInProgress = true;

        reloadServiceFor(configurationWithTeamSize(3)).reloadFromFile();

        assertEquals(3, teamSizeSetting.getTeamSize());
        assertNoWarning();
    }

    @Test
    void aReloadKeepsTheTeamSizeWhenATeamHasMorePlayersAndNamesTheTeam() {
        teamSizeSetting.setTeamSize(4);
        registerTeam("Alpha", 2);
        registerTeam("Bravo", 4);

        reloadServiceFor(configurationWithTeamSize(3)).reloadFromFile();

        assertEquals(4, teamSizeSetting.getTeamSize());
        String warning = "1 team has more than 3 players: Bravo (Bravo1, Bravo2, Bravo3, Bravo4). Remove it first. "
                + "The team size 3 of match.team-size in config.yml was not taken over, the team size stays "
                + "4 (4 versus 4)";
        assertEquals(List.of(warning), logLines);
        assertEquals(List.of(warning), adminNotices);
        assertEquals(List.of("Alpha", "Bravo"), teamRegistry.getTeamNames());
    }

    @Test
    void aReloadTakesASmallerTeamSizeThatEveryTeamFitsAndLeavesTheSmallerTeamsAsTheyAre() {
        teamSizeSetting.setTeamSize(6);
        registerTeam("Solo", 1);
        registerTeam("Trio", 3);

        reloadServiceFor(configurationWithTeamSize(3)).reloadFromFile();

        assertEquals(3, teamSizeSetting.getTeamSize());
        assertNoWarning();
        assertEquals("Solo1", teamRegistry.findTeam("Solo").orElseThrow().getMemberNamesJoined());
        assertEquals("Trio1, Trio2, Trio3", teamRegistry.findTeam("Trio").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void aBrokenTeamSizeFallsBackToTwoUnlessATeamIsLarger() {
        teamSizeSetting.setTeamSize(5);
        registerTeam("Bravo", 3);

        ConfigurationReloadReport report = reloadServiceFor(configurationWithTeamSize(12)).reloadFromFile();

        assertEquals(List.of("match.team-size must be a whole number between 1 and 8 players, falling back to "
                + "2 players"), report.problems());
        assertEquals(5, teamSizeSetting.getTeamSize());
        assertEquals(List.of("1 team has more than 2 players: Bravo (Bravo1, Bravo2, Bravo3). Remove it first. "
                + "The team size 2 of match.team-size in config.yml was not taken over, the team size stays "
                + "5 (5 versus 5)"), adminNotices);
    }

    @Test
    void theStartKeepsTwoWhenTheTeamsOfTeamsYmlAreLargerThanTheConfiguredTeamSize(@TempDir Path dataFolder)
            throws IOException {
        Path teamsFile = dataFolder.resolve("teams.yml");
        Files.writeString(teamsFile, """
                teams:
                - name: Bravo
                  members:
                  - unique-id: 6f9d1c52-1d1e-4a8e-9d55-0d4f3b3e1a01
                    name: Steve
                  - unique-id: 6f9d1c52-1d1e-4a8e-9d55-0d4f3b3e1a02
                    name: Alex
                  - unique-id: 6f9d1c52-1d1e-4a8e-9d55-0d4f3b3e1a03
                    name: Notch
                """, StandardCharsets.UTF_8);
        new TeamStorage(new File(teamsFile.toString()), quietLogger()).load(teamRegistry);
        ConfigurationReloadService reloadService = new ConfigurationReloadService(() -> configurationWithTeamSize(1),
                () -> { }, createLiveConfiguration(), problems::add);

        ConfigurationReloadReport report = reloadService.applyStoredConfiguration();

        assertTrue(report.isClean(), report.problems().toString());
        assertEquals(List.of("Bravo"), teamRegistry.getTeamNames(), "the start never drops a team");
        assertEquals(2, teamSizeSetting.getTeamSize());
        assertEquals(List.of("1 team has more than 1 player: Bravo (Steve, Alex, Notch). Remove it first. "
                + "The team size 1 of match.team-size in config.yml was not taken over, the team size stays "
                + "2 (2 versus 2)"), logLines);
    }

    @Test
    void theStartTakesTheConfiguredTeamSizeWhenEveryTeamFits() {
        registerTeam("Bravo", 3);
        ConfigurationReloadService reloadService = new ConfigurationReloadService(() -> configurationWithTeamSize(3),
                () -> { }, createLiveConfiguration(), problems::add);

        reloadService.applyStoredConfiguration();

        assertEquals(3, teamSizeSetting.getTeamSize());
        assertNoWarning();
    }

    private void assertNoWarning() {
        assertEquals(List.of(), logLines);
        assertEquals(List.of(), adminNotices);
    }

    private ConfigurationReloadService reloadServiceFor(MemoryConfiguration configuration) {
        return new ConfigurationReloadService(() -> configuration, () -> { }, createLiveConfiguration(),
                problems::add);
    }

    private void registerTeam(String teamName, int playerCount) {
        List<TeamMember> members = new ArrayList<>();
        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            members.add(new TeamMember(UUID.randomUUID(), teamName + playerNumber));
        }
        teamRegistry.registerTeam(teamName, members);
    }

    private static MemoryConfiguration configurationWithTeamSize(Object teamSize) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.TEAM_SIZE_PATH, teamSize);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = TeamSizeSettingsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static Logger quietLogger() {
        Logger logger = Logger.getLogger("TeamSizeSettingsTest");
        logger.setUseParentHandlers(false);
        return logger;
    }

    private PluginLiveConfiguration createLiveConfiguration() {
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
                wallPreview, borderVisualEffect, teamSizeSetting, teamRegistry, () -> tournamentInProgress,
                adminNotices::add, logLines::add);
    }
}

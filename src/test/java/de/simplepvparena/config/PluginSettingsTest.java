package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaCleaningParticleSettings;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaSafetyZone;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.match.TeamColourSettings;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.tournament.AutoMatchSettings;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginSettingsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void readsEveryValueOfTheFile() {
        MemoryConfiguration configuration = configurationWithEveryValueChanged();

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(List.of(), problems, "a fully valid file must not report a single problem");
        assertEquals(60L, settings.roundResetDelayTicks());
        assertEquals(new RoundTimeoutSettings(120, 30, 3.5D, 0.5D), settings.roundTimeoutSettings());
        assertFalse(settings.suddenDeathDisabledInFinal());
        assertEquals(4, settings.startGateDurationSeconds());
        assertEquals(7, settings.countdownSeconds());
        assertEquals(25, settings.reconnectSeconds());
        assertEquals(new TeamColourSettings(NamedTextColor.GOLD, NamedTextColor.DARK_PURPLE),
                settings.teamColourSettings());
        assertEquals(4, settings.teamSize());
        assertEquals(new AutoMatchSettings(true, 45, 0), settings.autoMatchSettings());
        assertEquals(1024, settings.arenaCleaningBlocksPerTick());
        assertEquals(2048, settings.arenaBaselineBlocksPerTick());
        assertEquals(20, settings.arenaBaselineDepthBelowFloor());
        assertEquals(new ArenaCleaningParticleSettings(false, "COMPOSTER", 8.0D, 3),
                settings.arenaCleaningParticleSettings());
        assertEquals(12345, settings.maximumArenaCellCount());
        assertEquals(17, settings.safetyZoneRadius());
        assertEquals(40, settings.arenaEffectMargin());
        assertFalse(settings.teleportOnJoin());
        assertEquals(11L, settings.borderParticlePeriodTicks());
        assertEquals(7, settings.borderParticleViewRadius());
        assertEquals(9, settings.borderParticleMaximumBlocksPerPlayer());
        assertEquals(13, settings.borderParticleMaximumLinesPerPlayer());
        assertEquals(6L * PluginSettings.TICKS_PER_SECOND, settings.borderWallPreviewDurationTicks());
        assertEquals(new ConfirmedBorderParticleSettings("DUST", 1.5D, 10, 20, 30),
                settings.confirmedBorderParticleSettings());
        assertEquals("de", PluginSettings.readLanguage(configuration, problems::add));
    }

    @Test
    void everyValueOfTheFileDiffersFromTheShippedDefault() {
        PluginSettings shipped = PluginSettings.defaults();

        PluginSettings settings = PluginSettings.read(configurationWithEveryValueChanged(), problems::add);

        assertTrue(shipped.roundResetDelayTicks() != settings.roundResetDelayTicks());
        assertTrue(!shipped.roundTimeoutSettings().equals(settings.roundTimeoutSettings()));
        assertTrue(shipped.suddenDeathDisabledInFinal() != settings.suddenDeathDisabledInFinal());
        assertTrue(shipped.startGateDurationSeconds() != settings.startGateDurationSeconds());
        assertTrue(shipped.countdownSeconds() != settings.countdownSeconds());
        assertTrue(shipped.reconnectSeconds() != settings.reconnectSeconds());
        assertTrue(!shipped.teamColourSettings().equals(settings.teamColourSettings()));
        assertTrue(shipped.teamSize() != settings.teamSize());
        assertTrue(shipped.autoMatchSettings().enabled() != settings.autoMatchSettings().enabled());
        assertTrue(shipped.autoMatchSettings().startDelaySeconds()
                != settings.autoMatchSettings().startDelaySeconds());
        assertTrue(shipped.autoMatchSettings().pauseSeconds() != settings.autoMatchSettings().pauseSeconds());
        assertTrue(shipped.arenaCleaningBlocksPerTick() != settings.arenaCleaningBlocksPerTick());
        assertTrue(shipped.arenaBaselineBlocksPerTick() != settings.arenaBaselineBlocksPerTick());
        assertTrue(shipped.arenaBaselineDepthBelowFloor() != settings.arenaBaselineDepthBelowFloor());
        assertTrue(!shipped.arenaCleaningParticleSettings().equals(settings.arenaCleaningParticleSettings()));
        assertTrue(shipped.maximumArenaCellCount() != settings.maximumArenaCellCount());
        assertTrue(shipped.safetyZoneRadius() != settings.safetyZoneRadius());
        assertTrue(shipped.arenaEffectMargin() != settings.arenaEffectMargin());
        assertTrue(shipped.teleportOnJoin() != settings.teleportOnJoin());
        assertTrue(shipped.borderParticlePeriodTicks() != settings.borderParticlePeriodTicks());
        assertTrue(shipped.borderParticleViewRadius() != settings.borderParticleViewRadius());
        assertTrue(shipped.borderParticleMaximumBlocksPerPlayer() != settings.borderParticleMaximumBlocksPerPlayer());
        assertTrue(shipped.borderParticleMaximumLinesPerPlayer() != settings.borderParticleMaximumLinesPerPlayer());
        assertTrue(shipped.borderWallPreviewDurationTicks() != settings.borderWallPreviewDurationTicks());
        assertTrue(!shipped.confirmedBorderParticleSettings().equals(settings.confirmedBorderParticleSettings()));
        assertTrue(!MessageCatalogLoader.DEFAULT_LANGUAGE.equals(
                PluginSettings.readLanguage(configurationWithEveryValueChanged(), problems::add)));
    }

    @Test
    void anEmptyFileIsTheShippedDefault() {
        PluginSettings settings = PluginSettings.read(new MemoryConfiguration(), problems::add);

        assertEquals(List.of(), problems);
        assertEquals(PluginSettings.defaults(), settings);
        assertEquals(ArenaCleaningService.DEFAULT_BLOCKS_PER_TICK, settings.arenaCleaningBlocksPerTick());
        assertEquals(ArenaBaselineService.DEFAULT_BLOCKS_PER_TICK, settings.arenaBaselineBlocksPerTick());
        assertEquals(ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR, settings.arenaBaselineDepthBelowFloor());
        assertEquals(ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT, settings.maximumArenaCellCount());
        assertEquals(ArenaSafetyZone.DEFAULT_RADIUS, settings.safetyZoneRadius());
        assertEquals(BorderParticleService.DEFAULT_PERIOD_TICKS, settings.borderParticlePeriodTicks());
    }

    @Test
    void aFileThatIsNotThereAtAllIsTheShippedDefault() {
        assertEquals(PluginSettings.defaults(), PluginSettings.read(null, problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void aCellLimitBelowOneCellFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_MAXIMUM_CELLS_PATH, 0);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT, settings.maximumArenaCellCount());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(PluginSettings.ARENA_MAXIMUM_CELLS_PATH),
                "the problem has to name the entry an admin fixes: " + problems.get(0));
    }

    @Test
    void aBaselineDepthOutsideOfZeroToSixtyFourFallsBackAndIsReported() {
        for (int depthBelowFloor : new int[] {-1, ArenaBaselineService.MAXIMUM_DEPTH_BELOW_FLOOR + 1}) {
            problems.clear();
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH, depthBelowFloor);

            PluginSettings settings = PluginSettings.read(configuration, problems::add);

            assertEquals(ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR, settings.arenaBaselineDepthBelowFloor());
            assertEquals(1, problems.size(), "a depth of " + depthBelowFloor + " is reported");
            assertTrue(problems.get(0).startsWith(PluginSettings.ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH),
                    problems.get(0));
        }
    }

    @Test
    void aBaselineDepthOfZeroAndOfSixtyFourIsTakenWithoutAProblem() {
        for (int depthBelowFloor : new int[] {0, ArenaBaselineService.MAXIMUM_DEPTH_BELOW_FLOOR}) {
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH, depthBelowFloor);

            assertEquals(depthBelowFloor,
                    PluginSettings.read(configuration, problems::add).arenaBaselineDepthBelowFloor());
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void aNegativeSafetyZoneRadiusFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -1);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(ArenaSafetyZone.DEFAULT_RADIUS, settings.safetyZoneRadius());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH), problems.get(0));
    }

    @Test
    void aSafetyZoneRadiusOfZeroSwitchesTheZoneOffWithoutAProblem() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, 0);

        assertEquals(0, PluginSettings.read(configuration, problems::add).safetyZoneRadius());
        assertEquals(List.of(), problems);
    }

    @Test
    void aRoundThatIsOverBeforeItStartsFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH, 0);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(RoundTimeoutSettings.DEFAULT_ROUND_TIMEOUT_SECONDS,
                settings.roundTimeoutSettings().roundTimeoutSeconds());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH), problems.get(0));
    }

    @Test
    void readsTheStartGateDurationAndFallsBackForOneBelowASecond() {
        MemoryConfiguration shortCountdown = new MemoryConfiguration();
        shortCountdown.set(PluginSettings.START_GATE_DURATION_SECONDS_PATH, 3);

        assertEquals(3, PluginSettings.read(shortCountdown, problems::add).startGateDurationSeconds());
        assertEquals(List.of(), problems, "a countdown of three seconds is a setting, not a problem");

        MemoryConfiguration noCountdown = new MemoryConfiguration();
        noCountdown.set(PluginSettings.START_GATE_DURATION_SECONDS_PATH, 0);

        PluginSettings settings = PluginSettings.read(noCountdown, problems::add);

        assertEquals(StartGateDuration.DEFAULT_SECONDS, settings.startGateDurationSeconds());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(PluginSettings.START_GATE_DURATION_SECONDS_PATH),
                "the problem has to name the entry an admin fixes: " + problems.get(0));
    }

    @Test
    void aFileWithoutTheStartGateEntryKeepsTheShippedTenSeconds() {
        assertEquals(StartGateDuration.DEFAULT_SECONDS,
                PluginSettings.read(new MemoryConfiguration(), problems::add).startGateDurationSeconds());
        assertEquals(List.of(), problems);
    }

    @Test
    void aNegativeParticleHeightFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH, -0.5D);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(ConfirmedBorderParticleSettings.DEFAULT_HEIGHT,
                settings.confirmedBorderParticleSettings().height());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH),
                problems.get(0));
    }

    @Test
    void aParticleHeightOfZeroIsTheShortestMarkerAndNoProblem() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH, 0.0D);

        assertEquals(0.0D, PluginSettings.read(configuration, problems::add)
                .confirmedBorderParticleSettings().height());
        assertEquals(List.of(), problems);
    }

    @Test
    void aTeamColourThatIsNoColourFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.FIRST_TEAM_COLOUR_PATH, "hellrot");
        configuration.set(PluginSettings.SECOND_TEAM_COLOUR_PATH, "blue");

        TeamColourSettings settings = PluginSettings.read(configuration, problems::add).teamColourSettings();

        assertEquals(TeamColourSettings.defaults().firstTeamColour(), settings.firstTeamColour());
        assertEquals(NamedTextColor.BLUE, settings.secondTeamColour(), "the entry that is usable still arrives");
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(PluginSettings.FIRST_TEAM_COLOUR_PATH),
                "the problem has to name the entry an admin fixes: " + problems.get(0));
        assertTrue(problems.get(0).contains("light_purple"),
                "and the colours the admin can write instead: " + problems.get(0));
    }

    @Test
    void twoTeamsInTheSameColourFallBackToTheShippedPairAndAreReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.FIRST_TEAM_COLOUR_PATH, "gold");
        configuration.set(PluginSettings.SECOND_TEAM_COLOUR_PATH, "minecraft:GOLD");

        TeamColourSettings settings = PluginSettings.read(configuration, problems::add).teamColourSettings();

        assertEquals(TeamColourSettings.defaults(), settings);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(TeamColourSettings.SECTION_CONFIGURATION_PATH), problems.get(0));
    }

    @Test
    void aFileWithoutTeamColoursKeepsTheShippedPair() {
        PluginSettings settings = PluginSettings.read(new MemoryConfiguration(), problems::add);

        assertEquals(TeamColourSettings.defaults(), settings.teamColourSettings());
        assertEquals(NamedTextColor.RED, settings.teamColourSettings().firstTeamColour());
        assertEquals(NamedTextColor.AQUA, settings.teamColourSettings().secondTeamColour());
        assertEquals(List.of(), problems);
    }

    @Test
    void aCleaningEffectTurnedDownToNothingIsAcceptedAndANegativeOneIsReported() {
        MemoryConfiguration switchedOff = new MemoryConfiguration();
        switchedOff.set(ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH, 0);
        switchedOff.set(ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH, 0.0D);

        ArenaCleaningParticleSettings settings =
                PluginSettings.read(switchedOff, problems::add).arenaCleaningParticleSettings();

        assertEquals(List.of(), problems);
        assertEquals(0, settings.columnsPerTick());
        assertEquals(0.0D, settings.totalDurationSeconds());
        assertFalse(settings.isShown(), "no cell per tick is a cleaning without an effect");
        assertEquals(0L, settings.animationDurationTicks(), "and one that is held back by nothing");

        MemoryConfiguration negative = new MemoryConfiguration();
        negative.set(ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH, -3);
        negative.set(ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH, -1.0D);

        ArenaCleaningParticleSettings answered =
                PluginSettings.read(negative, problems::add).arenaCleaningParticleSettings();

        assertEquals(ArenaCleaningParticleSettings.defaults(), answered);
        assertEquals(2, problems.size(), "both broken entries are reported: " + problems);
        assertTrue(problems.stream().anyMatch(problem ->
                        problem.startsWith(ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH)),
                "the problem has to name the entry an admin fixes: " + problems);
        assertTrue(problems.stream().anyMatch(problem ->
                        problem.startsWith(ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH)),
                problems.toString());
    }

    @Test
    void everyBrokenEntryOfOneFileIsReportedAndAnswered() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_MAXIMUM_CELLS_PATH, -5);
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -5);
        configuration.set(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH, -5);
        configuration.set(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH, -5.0D);
        configuration.set(PluginSettings.BORDER_PARTICLE_VIEW_RADIUS_PATH, 21);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(4, problems.size(), "every broken entry is reported: " + problems);
        assertEquals(PluginSettings.defaults().maximumArenaCellCount(), settings.maximumArenaCellCount());
        assertEquals(PluginSettings.defaults().safetyZoneRadius(), settings.safetyZoneRadius());
        assertEquals(PluginSettings.defaults().roundTimeoutSettings(), settings.roundTimeoutSettings());
        assertEquals(PluginSettings.defaults().confirmedBorderParticleSettings(),
                settings.confirmedBorderParticleSettings());
        assertEquals(21, settings.borderParticleViewRadius(), "the usable entries of the same file still arrive");
    }

    @Test
    void anEntryThatIsMissingKeepsItsDefaultNextToTheOnesThatAreThere() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.BORDER_PARTICLE_MAXIMUM_LINES_PATH, 0);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(0, settings.borderParticleMaximumLinesPerPlayer());
        assertEquals(PluginSettings.defaults().borderParticleMaximumBlocksPerPlayer(),
                settings.borderParticleMaximumBlocksPerPlayer());
        assertEquals(List.of(), problems);
    }

    @Test
    void readsWhetherTheFinalIsFoughtWithoutATimeLimit() {
        assertTrue(PluginSettings.defaults().suddenDeathDisabledInFinal(),
                "the plugin ships with a final that is decided by the finalists and not by a clock");
        assertEquals(MatchService.DEFAULT_SUDDEN_DEATH_DISABLED_IN_FINAL,
                PluginSettings.defaults().suddenDeathDisabledInFinal(),
                "the shipped value and the default of the running service are one value");

        MemoryConfiguration suddenDeathEverywhere = new MemoryConfiguration();
        suddenDeathEverywhere.set(PluginSettings.DISABLE_SUDDEN_DEATH_IN_FINAL_PATH, false);

        assertFalse(PluginSettings.read(suddenDeathEverywhere, problems::add).suddenDeathDisabledInFinal());
        assertEquals(List.of(), problems, "both values of a boolean are settings, not problems");

        MemoryConfiguration switchedOn = new MemoryConfiguration();
        switchedOn.set(PluginSettings.DISABLE_SUDDEN_DEATH_IN_FINAL_PATH, true);

        assertTrue(PluginSettings.read(switchedOn, problems::add).suddenDeathDisabledInFinal());
        assertEquals(List.of(), problems);
    }

    @Test
    void aFileWithoutTheFinalEntryKeepsTheShippedSwitch() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH, 90);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertTrue(settings.suddenDeathDisabledInFinal());
        assertEquals(90, settings.roundTimeoutSettings().roundTimeoutSeconds());
        assertEquals(List.of(), problems);
    }

    static MemoryConfiguration configurationWithEveryValueChanged() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.LANGUAGE_PATH, "de");
        configuration.set(PluginSettings.ROUND_RESET_DELAY_PATH, 60);
        configuration.set(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH, 120);
        configuration.set(PluginSettings.SUDDEN_DEATH_WARNING_SECONDS_PATH, 30);
        configuration.set(PluginSettings.SUDDEN_DEATH_DAMAGE_PER_SECOND_PATH, 3.5D);
        configuration.set(PluginSettings.SUDDEN_DEATH_DAMAGE_INCREASE_PATH, 0.5D);
        configuration.set(PluginSettings.DISABLE_SUDDEN_DEATH_IN_FINAL_PATH, false);
        configuration.set(PluginSettings.START_GATE_DURATION_SECONDS_PATH, 4);
        configuration.set(PluginSettings.COUNTDOWN_SECONDS_PATH, 7);
        configuration.set(PluginSettings.RECONNECT_SECONDS_PATH, 25);
        configuration.set(PluginSettings.TEAM_SIZE_PATH, 4);
        configuration.set(PluginSettings.AUTO_MATCH_ENABLED_PATH, true);
        configuration.set(PluginSettings.AUTO_MATCH_START_DELAY_SECONDS_PATH, 45);
        configuration.set(PluginSettings.AUTO_MATCH_PAUSE_SECONDS_PATH, 0);
        configuration.set(PluginSettings.FIRST_TEAM_COLOUR_PATH, "GOLD");
        configuration.set(PluginSettings.SECOND_TEAM_COLOUR_PATH, "dark purple");
        configuration.set(PluginSettings.ARENA_CLEAN_BLOCKS_PER_TICK_PATH, 1024);
        configuration.set(PluginSettings.ARENA_BASELINE_BLOCKS_PER_TICK_PATH, 2048);
        configuration.set(PluginSettings.ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH, 20);
        configuration.set(ArenaCleaningParticleSettings.ENABLED_CONFIGURATION_PATH, false);
        configuration.set(ArenaCleaningParticleSettings.TYPE_CONFIGURATION_PATH, "minecraft:composter");
        configuration.set(ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH, 8.0D);
        configuration.set(ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH, 3);
        configuration.set(PluginSettings.ARENA_MAXIMUM_CELLS_PATH, 12345);
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, 17);
        configuration.set(PluginSettings.ARENA_EFFECT_MARGIN_PATH, 40);
        configuration.set(PluginSettings.ARENA_TELEPORT_ON_JOIN_PATH, false);
        configuration.set(PluginSettings.BORDER_PARTICLE_PERIOD_PATH, 11);
        configuration.set(PluginSettings.BORDER_PARTICLE_VIEW_RADIUS_PATH, 7);
        configuration.set(PluginSettings.BORDER_PARTICLE_MAXIMUM_BLOCKS_PATH, 9);
        configuration.set(PluginSettings.BORDER_PARTICLE_MAXIMUM_LINES_PATH, 13);
        configuration.set(PluginSettings.BORDER_WALL_PREVIEW_SECONDS_PATH, 6);
        configuration.set(ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH, "dust");
        configuration.set(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH, 1.5D);
        configuration.set(ConfirmedBorderParticleSettings.RED_CONFIGURATION_PATH, 10);
        configuration.set(ConfirmedBorderParticleSettings.GREEN_CONFIGURATION_PATH, 20);
        configuration.set(ConfirmedBorderParticleSettings.BLUE_CONFIGURATION_PATH, 30);
        configuration.set(PluginSettings.KIT_SECTION_PATH + ".items", List.of());
        return configuration;
    }
}

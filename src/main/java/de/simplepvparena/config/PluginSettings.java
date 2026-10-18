package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaCleaningParticleSettings;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSafetyZone;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.match.TeamColourSettings;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamSizeSetting;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public record PluginSettings(long roundResetDelayTicks,
                             RoundTimeoutSettings roundTimeoutSettings,
                             boolean suddenDeathDisabledInFinal,
                             int startGateDurationSeconds,
                             TeamColourSettings teamColourSettings,
                             int teamSize,
                             int arenaCleaningBlocksPerTick,
                             int arenaBaselineBlocksPerTick,
                             int arenaBaselineDepthBelowFloor,
                             ArenaCleaningParticleSettings arenaCleaningParticleSettings,
                             int maximumArenaCellCount,
                             int safetyZoneRadius,
                             int arenaEffectMargin,
                             boolean teleportOnJoin,
                             long borderParticlePeriodTicks,
                             int borderParticleViewRadius,
                             int borderParticleMaximumBlocksPerPlayer,
                             int borderParticleMaximumLinesPerPlayer,
                             long borderWallPreviewDurationTicks,
                             ConfirmedBorderParticleSettings confirmedBorderParticleSettings,
                             String language) {

    public static final String KIT_SECTION_PATH = "kit";

    public static final String LANGUAGE_PATH = "language";

    public static final String ROUND_RESET_DELAY_PATH = "match.round-reset-delay-ticks";
    public static final String ROUND_TIMEOUT_SECONDS_PATH = "match.round-timeout-seconds";
    public static final String SUDDEN_DEATH_WARNING_SECONDS_PATH = "match.sudden-death-warning-seconds";
    public static final String SUDDEN_DEATH_DAMAGE_PER_SECOND_PATH = "match.sudden-death-damage-per-second";
    public static final String SUDDEN_DEATH_DAMAGE_INCREASE_PATH = "match.sudden-death-damage-increase-per-second";
    public static final String DISABLE_SUDDEN_DEATH_IN_FINAL_PATH = "match.disable-sudden-death-in-final";
    public static final String START_GATE_DURATION_SECONDS_PATH = "match.start-gate-duration-seconds";
    public static final String TEAM_SIZE_PATH = "match.team-size";
    public static final String FIRST_TEAM_COLOUR_PATH = TeamColourSettings.FIRST_TEAM_CONFIGURATION_PATH;
    public static final String SECOND_TEAM_COLOUR_PATH = TeamColourSettings.SECOND_TEAM_CONFIGURATION_PATH;
    public static final String ARENA_CLEAN_BLOCKS_PER_TICK_PATH = "arena.clean-blocks-per-tick";
    public static final String ARENA_BASELINE_BLOCKS_PER_TICK_PATH = "arena.baseline-blocks-per-tick";
    public static final String ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH = "arena.baseline-depth-below-floor";
    public static final String ARENA_MAXIMUM_CELLS_PATH = ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH;
    public static final String ARENA_SAFETY_ZONE_RADIUS_PATH = ArenaSafetyZone.RADIUS_CONFIGURATION_PATH;
    public static final String ARENA_EFFECT_MARGIN_PATH = EffectArea.MARGIN_CONFIGURATION_PATH;
    public static final String ARENA_TELEPORT_ON_JOIN_PATH = "arena.teleport-on-join";
    public static final String BORDER_PARTICLE_PERIOD_PATH = "border.particle-period-ticks";
    public static final String BORDER_PARTICLE_VIEW_RADIUS_PATH = "border.particle-view-radius";
    public static final String BORDER_PARTICLE_MAXIMUM_BLOCKS_PATH = "border.particle-maximum-blocks-per-player";
    public static final String BORDER_PARTICLE_MAXIMUM_LINES_PATH = "border.particle-maximum-lines-per-player";
    public static final String BORDER_WALL_PREVIEW_SECONDS_PATH = "border.wall-preview-seconds";

    public static final long DEFAULT_ROUND_RESET_DELAY_TICKS = 100L;
    public static final int DEFAULT_WALL_PREVIEW_SECONDS = 4;

    public static final long TICKS_PER_SECOND = 20L;

    public static final List<String> CONFIGURATION_PATHS = List.of(
            LANGUAGE_PATH,
            ROUND_RESET_DELAY_PATH,
            ROUND_TIMEOUT_SECONDS_PATH,
            SUDDEN_DEATH_WARNING_SECONDS_PATH,
            SUDDEN_DEATH_DAMAGE_PER_SECOND_PATH,
            SUDDEN_DEATH_DAMAGE_INCREASE_PATH,
            DISABLE_SUDDEN_DEATH_IN_FINAL_PATH,
            START_GATE_DURATION_SECONDS_PATH,
            TEAM_SIZE_PATH,
            FIRST_TEAM_COLOUR_PATH,
            SECOND_TEAM_COLOUR_PATH,
            ARENA_CLEAN_BLOCKS_PER_TICK_PATH,
            ARENA_BASELINE_BLOCKS_PER_TICK_PATH,
            ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH,
            ArenaCleaningParticleSettings.ENABLED_CONFIGURATION_PATH,
            ArenaCleaningParticleSettings.TYPE_CONFIGURATION_PATH,
            ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH,
            ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH,
            ARENA_MAXIMUM_CELLS_PATH,
            ARENA_SAFETY_ZONE_RADIUS_PATH,
            ARENA_EFFECT_MARGIN_PATH,
            ARENA_TELEPORT_ON_JOIN_PATH,
            BORDER_PARTICLE_PERIOD_PATH,
            BORDER_PARTICLE_VIEW_RADIUS_PATH,
            BORDER_PARTICLE_MAXIMUM_BLOCKS_PATH,
            BORDER_PARTICLE_MAXIMUM_LINES_PATH,
            BORDER_WALL_PREVIEW_SECONDS_PATH,
            ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH,
            ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH,
            ConfirmedBorderParticleSettings.RED_CONFIGURATION_PATH,
            ConfirmedBorderParticleSettings.GREEN_CONFIGURATION_PATH,
            ConfirmedBorderParticleSettings.BLUE_CONFIGURATION_PATH);

    public static PluginSettings defaults() {
        return new PluginSettings(DEFAULT_ROUND_RESET_DELAY_TICKS,
                RoundTimeoutSettings.defaults(),
                MatchService.DEFAULT_SUDDEN_DEATH_DISABLED_IN_FINAL,
                StartGateDuration.DEFAULT_SECONDS,
                TeamColourSettings.defaults(),
                TeamSizeSetting.DEFAULT_TEAM_SIZE,
                ArenaCleaningService.DEFAULT_BLOCKS_PER_TICK,
                ArenaBaselineService.DEFAULT_BLOCKS_PER_TICK,
                ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR,
                ArenaCleaningParticleSettings.defaults(),
                ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT,
                ArenaSafetyZone.DEFAULT_RADIUS,
                EffectArea.DEFAULT_MARGIN,
                ArenaRegistry.DEFAULT_TELEPORT_ON_JOIN,
                BorderParticleService.DEFAULT_PERIOD_TICKS,
                BorderParticleService.DEFAULT_VIEW_RADIUS,
                BorderParticleService.DEFAULT_MAXIMUM_BLOCKS_PER_PLAYER,
                BorderParticleService.DEFAULT_MAXIMUM_LINES_PER_PLAYER,
                BorderWallPreview.DEFAULT_DURATION_TICKS,
                ConfirmedBorderParticleSettings.defaults(),
                MessageCatalogLoader.DEFAULT_LANGUAGE);
    }

    public static PluginSettings read(ConfigurationSection configuration, Consumer<String> problemReporter) {
        if (configuration == null) {
            return defaults();
        }
        PluginSettings shipped = defaults();
        return new PluginSettings(
                configuration.getLong(ROUND_RESET_DELAY_PATH, shipped.roundResetDelayTicks()),
                readRoundTimeoutSettings(configuration, problemReporter),
                configuration.getBoolean(DISABLE_SUDDEN_DEATH_IN_FINAL_PATH,
                        shipped.suddenDeathDisabledInFinal()),
                readStartGateDurationSeconds(configuration, problemReporter),
                readTeamColourSettings(configuration, problemReporter),
                readTeamSize(configuration, problemReporter),
                configuration.getInt(ARENA_CLEAN_BLOCKS_PER_TICK_PATH, shipped.arenaCleaningBlocksPerTick()),
                configuration.getInt(ARENA_BASELINE_BLOCKS_PER_TICK_PATH, shipped.arenaBaselineBlocksPerTick()),
                readBaselineDepthBelowFloor(configuration, problemReporter),
                readArenaCleaningParticleSettings(configuration, problemReporter),
                readMaximumArenaCellCount(configuration, problemReporter),
                readSafetyZoneRadius(configuration, problemReporter),
                readEffectMargin(configuration, problemReporter),
                configuration.getBoolean(ARENA_TELEPORT_ON_JOIN_PATH, shipped.teleportOnJoin()),
                configuration.getLong(BORDER_PARTICLE_PERIOD_PATH, shipped.borderParticlePeriodTicks()),
                configuration.getInt(BORDER_PARTICLE_VIEW_RADIUS_PATH, shipped.borderParticleViewRadius()),
                configuration.getInt(BORDER_PARTICLE_MAXIMUM_BLOCKS_PATH,
                        shipped.borderParticleMaximumBlocksPerPlayer()),
                configuration.getInt(BORDER_PARTICLE_MAXIMUM_LINES_PATH,
                        shipped.borderParticleMaximumLinesPerPlayer()),
                TICKS_PER_SECOND * configuration.getInt(BORDER_WALL_PREVIEW_SECONDS_PATH,
                        DEFAULT_WALL_PREVIEW_SECONDS),
                readConfirmedBorderParticleSettings(configuration, problemReporter),
                readLanguage(configuration, problemReporter));
    }

    public static String readLanguage(ConfigurationSection configuration, Consumer<String> problemReporter) {
        if (configuration == null || !configuration.isSet(LANGUAGE_PATH)) {
            return MessageCatalogLoader.DEFAULT_LANGUAGE;
        }
        String writtenLanguage = String.valueOf(configuration.get(LANGUAGE_PATH));
        String language = writtenLanguage.strip().toLowerCase(Locale.ROOT);
        if (MessageCatalogLoader.isShipped(language)) {
            return language;
        }
        problemReporter.accept(LANGUAGE_PATH + " " + writtenLanguage + " is no shipped language, falling back to "
                + MessageCatalogLoader.DEFAULT_LANGUAGE + ". Shipped languages: "
                + String.join(", ", MessageCatalogLoader.SHIPPED_LANGUAGES));
        return MessageCatalogLoader.DEFAULT_LANGUAGE;
    }

    public void applyTo(LiveConfiguration liveConfiguration) {
        liveConfiguration.setLanguage(language);
        applyTo(liveConfiguration, liveConfiguration::setTeamSize);
    }

    public void applyAtStartTo(LiveConfiguration liveConfiguration) {
        applyTo(liveConfiguration, liveConfiguration::setTeamSizeAtStart);
    }

    private void applyTo(LiveConfiguration liveConfiguration, IntConsumer teamSizeTarget) {
        liveConfiguration.setRoundResetDelayTicks(roundResetDelayTicks);
        liveConfiguration.setRoundTimeoutSettings(roundTimeoutSettings);
        liveConfiguration.setSuddenDeathDisabledInFinal(suddenDeathDisabledInFinal);
        liveConfiguration.setStartGateDurationSeconds(startGateDurationSeconds);
        liveConfiguration.setTeamColourSettings(teamColourSettings);
        teamSizeTarget.accept(teamSize);
        liveConfiguration.setArenaCleaningBlocksPerTick(arenaCleaningBlocksPerTick);
        liveConfiguration.setArenaBaselineBlocksPerTick(arenaBaselineBlocksPerTick);
        liveConfiguration.setArenaBaselineDepthBelowFloor(arenaBaselineDepthBelowFloor);
        liveConfiguration.setArenaCleaningParticleSettings(arenaCleaningParticleSettings);
        liveConfiguration.setMaximumArenaCellCount(maximumArenaCellCount);
        liveConfiguration.setSafetyZoneRadius(safetyZoneRadius);
        liveConfiguration.setArenaEffectMargin(arenaEffectMargin);
        liveConfiguration.setTeleportOnJoin(teleportOnJoin);
        liveConfiguration.setBorderParticlePeriodTicks(borderParticlePeriodTicks);
        liveConfiguration.setBorderParticleViewRadius(borderParticleViewRadius);
        liveConfiguration.setBorderParticleMaximumBlocksPerPlayer(borderParticleMaximumBlocksPerPlayer);
        liveConfiguration.setBorderParticleMaximumLinesPerPlayer(borderParticleMaximumLinesPerPlayer);
        liveConfiguration.setBorderWallPreviewDurationTicks(borderWallPreviewDurationTicks);
        liveConfiguration.setConfirmedBorderParticleSettings(confirmedBorderParticleSettings);
    }

    private static int readMaximumArenaCellCount(ConfigurationSection configuration,
                                                 Consumer<String> problemReporter) {
        int maximumCellCount = configuration.getInt(ARENA_MAXIMUM_CELLS_PATH,
                ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT);
        if (maximumCellCount >= 1) {
            return maximumCellCount;
        }
        problemReporter.accept(ARENA_MAXIMUM_CELLS_PATH + " must be at least one cell, falling back to "
                + ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT);
        return ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT;
    }

    private static int readBaselineDepthBelowFloor(ConfigurationSection configuration,
                                                   Consumer<String> problemReporter) {
        int depthBelowFloor = configuration.getInt(ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH,
                ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR);
        if (depthBelowFloor >= 0 && depthBelowFloor <= ArenaBaselineService.MAXIMUM_DEPTH_BELOW_FLOOR) {
            return depthBelowFloor;
        }
        problemReporter.accept(ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH + " must lie between 0 and "
                + ArenaBaselineService.MAXIMUM_DEPTH_BELOW_FLOOR + " blocks, falling back to "
                + ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR + " blocks");
        return ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR;
    }

    private static int readSafetyZoneRadius(ConfigurationSection configuration, Consumer<String> problemReporter) {
        int radius = configuration.getInt(ARENA_SAFETY_ZONE_RADIUS_PATH, ArenaSafetyZone.DEFAULT_RADIUS);
        if (radius >= 0) {
            return radius;
        }
        problemReporter.accept(ARENA_SAFETY_ZONE_RADIUS_PATH + " cannot be negative, falling back to "
                + ArenaSafetyZone.DEFAULT_RADIUS + " blocks. Use zero to switch the safety zone off");
        return ArenaSafetyZone.DEFAULT_RADIUS;
    }

    private static int readEffectMargin(ConfigurationSection configuration, Consumer<String> problemReporter) {
        if (!configuration.isSet(ARENA_EFFECT_MARGIN_PATH)) {
            return EffectArea.DEFAULT_MARGIN;
        }
        if (configuration.isInt(ARENA_EFFECT_MARGIN_PATH)) {
            int margin = configuration.getInt(ARENA_EFFECT_MARGIN_PATH);
            if (margin >= EffectArea.MINIMUM_MARGIN && margin <= EffectArea.MAXIMUM_MARGIN) {
                return margin;
            }
        }
        problemReporter.accept(ARENA_EFFECT_MARGIN_PATH + " must be a whole number between "
                + EffectArea.MINIMUM_MARGIN + " and " + EffectArea.MAXIMUM_MARGIN + " blocks, falling back to "
                + EffectArea.DEFAULT_MARGIN + " blocks");
        return EffectArea.DEFAULT_MARGIN;
    }

    private static int readTeamSize(ConfigurationSection configuration, Consumer<String> problemReporter) {
        if (!configuration.isSet(TEAM_SIZE_PATH)) {
            return TeamSizeSetting.DEFAULT_TEAM_SIZE;
        }
        if (configuration.isInt(TEAM_SIZE_PATH)) {
            int teamSize = configuration.getInt(TEAM_SIZE_PATH);
            if (TeamSizeSetting.isAllowed(teamSize)) {
                return teamSize;
            }
        }
        problemReporter.accept(TEAM_SIZE_PATH + " must be a whole number between " + Team.MINIMUM_MEMBER_COUNT
                + " and " + Team.MAXIMUM_MEMBER_COUNT + " players, falling back to "
                + TeamSizeSetting.DEFAULT_TEAM_SIZE + " players");
        return TeamSizeSetting.DEFAULT_TEAM_SIZE;
    }

    private static ArenaCleaningParticleSettings readArenaCleaningParticleSettings(
            ConfigurationSection configuration, Consumer<String> problemReporter) {
        ArenaCleaningParticleSettings defaults = ArenaCleaningParticleSettings.defaults();
        double totalDurationSeconds = configuration.getDouble(
                ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH,
                defaults.totalDurationSeconds());
        if (totalDurationSeconds < 0.0D) {
            problemReporter.accept(ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH
                    + " cannot be negative, falling back to " + defaults.totalDurationSeconds()
                    + " seconds. Use zero to clean without the two coloured phases");
            totalDurationSeconds = defaults.totalDurationSeconds();
        }
        int columnsPerTick = configuration.getInt(
                ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH, defaults.columnsPerTick());
        if (columnsPerTick < 0) {
            problemReporter.accept(ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH
                    + " cannot be negative, falling back to " + defaults.columnsPerTick()
                    + " columns per tick. Use zero to draw none");
            columnsPerTick = defaults.columnsPerTick();
        }
        return new ArenaCleaningParticleSettings(
                configuration.getBoolean(ArenaCleaningParticleSettings.ENABLED_CONFIGURATION_PATH,
                        defaults.enabled()),
                configuration.getString(ArenaCleaningParticleSettings.TYPE_CONFIGURATION_PATH,
                        defaults.particleName()),
                totalDurationSeconds, columnsPerTick);
    }

    private static int readStartGateDurationSeconds(ConfigurationSection configuration,
                                                    Consumer<String> problemReporter) {
        int durationSeconds = configuration.getInt(START_GATE_DURATION_SECONDS_PATH,
                StartGateDuration.DEFAULT_SECONDS);
        if (durationSeconds >= StartGateDuration.MINIMUM_SECONDS) {
            return durationSeconds;
        }
        problemReporter.accept(START_GATE_DURATION_SECONDS_PATH + " must be at least "
                + StartGateDuration.MINIMUM_SECONDS + " second, falling back to "
                + StartGateDuration.DEFAULT_SECONDS + " seconds");
        return StartGateDuration.DEFAULT_SECONDS;
    }

    private static RoundTimeoutSettings readRoundTimeoutSettings(ConfigurationSection configuration,
                                                                 Consumer<String> problemReporter) {
        RoundTimeoutSettings defaultSettings = RoundTimeoutSettings.defaults();
        int roundTimeoutSeconds = configuration.getInt(ROUND_TIMEOUT_SECONDS_PATH,
                defaultSettings.roundTimeoutSeconds());
        if (roundTimeoutSeconds < 1) {
            problemReporter.accept(ROUND_TIMEOUT_SECONDS_PATH + " must be at least one second, falling back to "
                    + defaultSettings.roundTimeoutSeconds() + " seconds");
            roundTimeoutSeconds = defaultSettings.roundTimeoutSeconds();
        }
        return new RoundTimeoutSettings(roundTimeoutSeconds,
                configuration.getInt(SUDDEN_DEATH_WARNING_SECONDS_PATH, defaultSettings.suddenDeathWarningSeconds()),
                configuration.getDouble(SUDDEN_DEATH_DAMAGE_PER_SECOND_PATH, defaultSettings.suddenDeathBaseDamage()),
                configuration.getDouble(SUDDEN_DEATH_DAMAGE_INCREASE_PATH,
                        defaultSettings.suddenDeathDamageIncreasePerSecond()));
    }

    private static TeamColourSettings readTeamColourSettings(ConfigurationSection configuration,
                                                             Consumer<String> problemReporter) {
        TeamColourSettings defaults = TeamColourSettings.defaults();
        NamedTextColor firstTeamColour = readTeamColour(configuration, FIRST_TEAM_COLOUR_PATH,
                defaults.firstTeamColour(), problemReporter);
        NamedTextColor secondTeamColour = readTeamColour(configuration, SECOND_TEAM_COLOUR_PATH,
                defaults.secondTeamColour(), problemReporter);
        if (firstTeamColour.equals(secondTeamColour)) {
            problemReporter.accept(TeamColourSettings.SECTION_CONFIGURATION_PATH + " gives both teams the colour "
                    + TeamColourSettings.nameOf(firstTeamColour) + ", which would be no colouring at all, falling "
                    + "back to " + TeamColourSettings.nameOf(defaults.firstTeamColour()) + " and "
                    + TeamColourSettings.nameOf(defaults.secondTeamColour()));
            return defaults;
        }
        return new TeamColourSettings(firstTeamColour, secondTeamColour);
    }

    private static NamedTextColor readTeamColour(ConfigurationSection configuration, String path,
                                                 NamedTextColor fallbackColour, Consumer<String> problemReporter) {
        String writtenName = configuration.getString(path);
        if (writtenName == null || writtenName.isBlank()) {
            return fallbackColour;
        }
        Optional<NamedTextColor> colour = TeamColourSettings.colourOfName(writtenName);
        if (colour.isPresent()) {
            return colour.get();
        }
        problemReporter.accept(path + " is no colour a name can be shown in, falling back to "
                + TeamColourSettings.nameOf(fallbackColour) + ". Usable colours: "
                + String.join(", ", TeamColourSettings.knownColourNames()));
        return fallbackColour;
    }

    private static ConfirmedBorderParticleSettings readConfirmedBorderParticleSettings(
            ConfigurationSection configuration, Consumer<String> problemReporter) {
        ConfirmedBorderParticleSettings defaults = ConfirmedBorderParticleSettings.defaults();
        double height = configuration.getDouble(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH,
                defaults.height());
        if (height < 0.0D) {
            problemReporter.accept(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH
                    + " cannot be negative, falling back to " + defaults.height()
                    + " blocks. Use zero for the shortest possible marker");
            height = defaults.height();
        }
        return new ConfirmedBorderParticleSettings(
                configuration.getString(ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH,
                        defaults.particleName()),
                height,
                configuration.getInt(ConfirmedBorderParticleSettings.RED_CONFIGURATION_PATH, defaults.red()),
                configuration.getInt(ConfirmedBorderParticleSettings.GREEN_CONFIGURATION_PATH, defaults.green()),
                configuration.getInt(ConfirmedBorderParticleSettings.BLUE_CONFIGURATION_PATH, defaults.blue()));
    }
}

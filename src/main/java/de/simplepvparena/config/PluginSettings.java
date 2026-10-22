package de.simplepvparena.config;

import de.simplepvparena.arena.AreaInventoryService;
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
import de.simplepvparena.message.ConfigMessage;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamSizeSetting;
import de.simplepvparena.tournament.AutoMatchSettings;
import de.simplepvparena.visual.CustomVisualSettings;
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
                             int countdownSeconds,
                             int reconnectSeconds,
                             TeamColourSettings teamColourSettings,
                             int teamSize,
                             AutoMatchSettings autoMatchSettings,
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
                             boolean confirmedBorderParticleEnabled,
                             boolean areaInventoryEnabled,
                             CustomVisualSettings customVisualSettings) {

    public static final String KIT_SECTION_PATH = "kit";

    public static final String LANGUAGE_PATH = "language";

    public static final String ROUND_RESET_DELAY_PATH = "match.round-reset-delay-ticks";
    public static final String ROUND_TIMEOUT_SECONDS_PATH = "match.round-timeout-seconds";
    public static final String SUDDEN_DEATH_WARNING_SECONDS_PATH = "match.sudden-death-warning-seconds";
    public static final String SUDDEN_DEATH_DAMAGE_PER_SECOND_PATH = "match.sudden-death-damage-per-second";
    public static final String SUDDEN_DEATH_DAMAGE_INCREASE_PATH = "match.sudden-death-damage-increase-per-second";
    public static final String DISABLE_SUDDEN_DEATH_IN_FINAL_PATH = "match.disable-sudden-death-in-final";
    public static final String START_GATE_DURATION_SECONDS_PATH = "match.start-gate-duration-seconds";
    public static final String COUNTDOWN_SECONDS_PATH = "match.countdown-seconds";
    public static final String RECONNECT_SECONDS_PATH = "match.reconnect-seconds";
    public static final String TEAM_SIZE_PATH = "match.team-size";
    public static final String AUTO_MATCH_ENABLED_PATH = "auto-match.enabled";
    public static final String AUTO_MATCH_START_DELAY_SECONDS_PATH = "auto-match.start-delay-seconds";
    public static final String AUTO_MATCH_PAUSE_SECONDS_PATH = "auto-match.pause-seconds";
    public static final String FIRST_TEAM_COLOUR_PATH = TeamColourSettings.FIRST_TEAM_CONFIGURATION_PATH;
    public static final String SECOND_TEAM_COLOUR_PATH = TeamColourSettings.SECOND_TEAM_CONFIGURATION_PATH;
    public static final String ARENA_CLEAN_BLOCKS_PER_TICK_PATH = "arena.clean-blocks-per-tick";
    public static final String ARENA_BASELINE_BLOCKS_PER_TICK_PATH = "arena.baseline-blocks-per-tick";
    public static final String ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH = "arena.baseline-depth-below-floor";
    public static final String ARENA_MAXIMUM_CELLS_PATH = ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH;
    public static final String ARENA_SAFETY_ZONE_RADIUS_PATH = ArenaSafetyZone.RADIUS_CONFIGURATION_PATH;
    public static final String ARENA_EFFECT_MARGIN_PATH = EffectArea.MARGIN_CONFIGURATION_PATH;
    public static final String ARENA_TELEPORT_ON_JOIN_PATH = "arena.teleport-on-join";
    public static final String ARENA_AREA_INVENTORY_ENABLED_PATH = AreaInventoryService.ENABLED_CONFIGURATION_PATH;
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
            COUNTDOWN_SECONDS_PATH,
            RECONNECT_SECONDS_PATH,
            TEAM_SIZE_PATH,
            FIRST_TEAM_COLOUR_PATH,
            SECOND_TEAM_COLOUR_PATH,
            AUTO_MATCH_ENABLED_PATH,
            AUTO_MATCH_START_DELAY_SECONDS_PATH,
            AUTO_MATCH_PAUSE_SECONDS_PATH,
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
            ARENA_AREA_INVENTORY_ENABLED_PATH,
            BORDER_PARTICLE_PERIOD_PATH,
            BORDER_PARTICLE_VIEW_RADIUS_PATH,
            BORDER_PARTICLE_MAXIMUM_BLOCKS_PATH,
            BORDER_PARTICLE_MAXIMUM_LINES_PATH,
            BORDER_WALL_PREVIEW_SECONDS_PATH,
            ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH,
            ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH,
            ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH,
            ConfirmedBorderParticleSettings.RED_CONFIGURATION_PATH,
            ConfirmedBorderParticleSettings.GREEN_CONFIGURATION_PATH,
            ConfirmedBorderParticleSettings.BLUE_CONFIGURATION_PATH,
            CustomVisualSettings.ENABLED_CONFIGURATION_PATH,
            CustomVisualSettings.THREE_IMAGE_CONFIGURATION_PATH,
            CustomVisualSettings.TWO_IMAGE_CONFIGURATION_PATH,
            CustomVisualSettings.ONE_IMAGE_CONFIGURATION_PATH,
            CustomVisualSettings.FIGHT_IMAGE_CONFIGURATION_PATH,
            CustomVisualSettings.TOTEM_ITEM_CONFIGURATION_PATH,
            CustomVisualSettings.STATION_ITEM_CONFIGURATION_PATH);

    public static PluginSettings defaults() {
        return new PluginSettings(DEFAULT_ROUND_RESET_DELAY_TICKS,
                RoundTimeoutSettings.defaults(),
                MatchService.DEFAULT_SUDDEN_DEATH_DISABLED_IN_FINAL,
                StartGateDuration.DEFAULT_SECONDS,
                MatchService.DEFAULT_COUNTDOWN_SECONDS,
                MatchService.DEFAULT_RECONNECT_SECONDS,
                TeamColourSettings.defaults(),
                TeamSizeSetting.DEFAULT_TEAM_SIZE,
                AutoMatchSettings.defaults(),
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
                BorderParticleService.DEFAULT_CONFIRMED_PARTICLE_ENABLED,
                AreaInventoryService.DEFAULT_ENABLED,
                CustomVisualSettings.defaults());
    }

    public static PluginSettings read(ConfigurationSection configuration, Consumer<String> problemReporter) {
        if (configuration == null) {
            return defaults();
        }
        PluginSettings shipped = defaults();
        PluginSettings settings = new PluginSettings(
                configuration.getLong(ROUND_RESET_DELAY_PATH, shipped.roundResetDelayTicks()),
                readRoundTimeoutSettings(configuration, problemReporter),
                configuration.getBoolean(DISABLE_SUDDEN_DEATH_IN_FINAL_PATH,
                        shipped.suddenDeathDisabledInFinal()),
                readStartGateDurationSeconds(configuration, problemReporter),
                readCountdownSeconds(configuration, problemReporter),
                readReconnectSeconds(configuration, problemReporter),
                readTeamColourSettings(configuration, problemReporter),
                readTeamSize(configuration, problemReporter),
                readAutoMatchSettings(configuration, problemReporter),
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
                readConfirmedBorderParticleEnabled(configuration, problemReporter),
                readAreaInventoryEnabled(configuration, problemReporter),
                readCustomVisualSettings(configuration, problemReporter));
        readLanguage(configuration, problemReporter);
        return settings;
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
        problemReporter.accept(Messages.text(ConfigMessage.LANGUAGE_NOT_SHIPPED,
                Placeholder.of("path", LANGUAGE_PATH), Placeholder.of("language", writtenLanguage),
                Placeholder.of("fallback", MessageCatalogLoader.DEFAULT_LANGUAGE),
                Placeholder.of("languages", Messages.join(MessageCatalogLoader.SHIPPED_LANGUAGES))));
        return MessageCatalogLoader.DEFAULT_LANGUAGE;
    }

    public void applyTo(LiveConfiguration liveConfiguration) {
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
        liveConfiguration.setCountdownSeconds(countdownSeconds);
        liveConfiguration.setReconnectSeconds(reconnectSeconds);
        liveConfiguration.setTeamColourSettings(teamColourSettings);
        teamSizeTarget.accept(teamSize);
        liveConfiguration.setAutoMatchSettings(autoMatchSettings);
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
        liveConfiguration.setConfirmedBorderParticleEnabled(confirmedBorderParticleEnabled);
        liveConfiguration.setAreaInventoryEnabled(areaInventoryEnabled);
        liveConfiguration.setCustomVisualSettings(customVisualSettings);
    }

    public static boolean readItemsAdderEnabled(ConfigurationSection configuration,
                                                Consumer<String> problemReporter) {
        if (configuration == null || !configuration.isSet(CustomVisualSettings.ENABLED_CONFIGURATION_PATH)) {
            return CustomVisualSettings.DEFAULT_ENABLED;
        }
        if (configuration.isBoolean(CustomVisualSettings.ENABLED_CONFIGURATION_PATH)) {
            return configuration.getBoolean(CustomVisualSettings.ENABLED_CONFIGURATION_PATH);
        }
        problemReporter.accept(Messages.text(ConfigMessage.ITEMS_ADDER_ENABLED_NOT_A_TRUTH_VALUE,
                Placeholder.of("path", CustomVisualSettings.ENABLED_CONFIGURATION_PATH),
                Placeholder.of("fallback", CustomVisualSettings.DEFAULT_ENABLED)));
        return CustomVisualSettings.DEFAULT_ENABLED;
    }

    private static CustomVisualSettings readCustomVisualSettings(ConfigurationSection configuration,
                                                                 Consumer<String> problemReporter) {
        return new CustomVisualSettings(readItemsAdderEnabled(configuration, problemReporter),
                readItemsAdderIdentifier(configuration, CustomVisualSettings.THREE_IMAGE_CONFIGURATION_PATH,
                        CustomVisualSettings.SUGGESTED_THREE_IMAGE_IDENTIFIER, problemReporter),
                readItemsAdderIdentifier(configuration, CustomVisualSettings.TWO_IMAGE_CONFIGURATION_PATH,
                        CustomVisualSettings.SUGGESTED_TWO_IMAGE_IDENTIFIER, problemReporter),
                readItemsAdderIdentifier(configuration, CustomVisualSettings.ONE_IMAGE_CONFIGURATION_PATH,
                        CustomVisualSettings.SUGGESTED_ONE_IMAGE_IDENTIFIER, problemReporter),
                readItemsAdderIdentifier(configuration, CustomVisualSettings.FIGHT_IMAGE_CONFIGURATION_PATH,
                        CustomVisualSettings.SUGGESTED_FIGHT_IMAGE_IDENTIFIER, problemReporter),
                readItemsAdderIdentifier(configuration, CustomVisualSettings.TOTEM_ITEM_CONFIGURATION_PATH,
                        CustomVisualSettings.SUGGESTED_TOTEM_ITEM_IDENTIFIER, problemReporter),
                readItemsAdderIdentifier(configuration, CustomVisualSettings.STATION_ITEM_CONFIGURATION_PATH,
                        CustomVisualSettings.SUGGESTED_STATION_ITEM_IDENTIFIER, problemReporter));
    }

    private static String readItemsAdderIdentifier(ConfigurationSection configuration, String path,
                                                   String suggestedIdentifier, Consumer<String> problemReporter) {
        if (!configuration.isSet(path)) {
            return CustomVisualSettings.NO_IDENTIFIER;
        }
        if (configuration.isString(path)) {
            return configuration.getString(path, CustomVisualSettings.NO_IDENTIFIER);
        }
        problemReporter.accept(Messages.text(ConfigMessage.ITEMS_ADDER_IDENTIFIER_NOT_TEXT,
                Placeholder.of("path", path), Placeholder.of("example", suggestedIdentifier)));
        return CustomVisualSettings.NO_IDENTIFIER;
    }

    private static int readMaximumArenaCellCount(ConfigurationSection configuration,
                                                 Consumer<String> problemReporter) {
        int maximumCellCount = configuration.getInt(ARENA_MAXIMUM_CELLS_PATH,
                ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT);
        if (maximumCellCount >= 1) {
            return maximumCellCount;
        }
        problemReporter.accept(Messages.text(ConfigMessage.CELL_LIMIT_TOO_LOW,
                Placeholder.of("path", ARENA_MAXIMUM_CELLS_PATH),
                Placeholder.of("fallback", ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT)));
        return ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT;
    }

    private static int readBaselineDepthBelowFloor(ConfigurationSection configuration,
                                                   Consumer<String> problemReporter) {
        int depthBelowFloor = configuration.getInt(ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH,
                ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR);
        if (depthBelowFloor >= 0 && depthBelowFloor <= ArenaBaselineService.MAXIMUM_DEPTH_BELOW_FLOOR) {
            return depthBelowFloor;
        }
        problemReporter.accept(Messages.plural(ConfigMessage.BASELINE_DEPTH_OUT_OF_RANGE,
                ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR,
                Placeholder.of("path", ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH),
                Placeholder.of("maximum", ArenaBaselineService.MAXIMUM_DEPTH_BELOW_FLOOR)));
        return ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR;
    }

    private static int readSafetyZoneRadius(ConfigurationSection configuration, Consumer<String> problemReporter) {
        int radius = configuration.getInt(ARENA_SAFETY_ZONE_RADIUS_PATH, ArenaSafetyZone.DEFAULT_RADIUS);
        if (radius >= 0) {
            return radius;
        }
        problemReporter.accept(Messages.plural(ConfigMessage.SAFETY_ZONE_RADIUS_NEGATIVE,
                ArenaSafetyZone.DEFAULT_RADIUS, Placeholder.of("path", ARENA_SAFETY_ZONE_RADIUS_PATH)));
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
        problemReporter.accept(Messages.plural(ConfigMessage.EFFECT_MARGIN_OUT_OF_RANGE, EffectArea.DEFAULT_MARGIN,
                Placeholder.of("path", ARENA_EFFECT_MARGIN_PATH), Placeholder.of("minimum", EffectArea.MINIMUM_MARGIN),
                Placeholder.of("maximum", EffectArea.MAXIMUM_MARGIN)));
        return EffectArea.DEFAULT_MARGIN;
    }

    private static int readCountdownSeconds(ConfigurationSection configuration, Consumer<String> problemReporter) {
        if (!configuration.isSet(COUNTDOWN_SECONDS_PATH)) {
            return MatchService.DEFAULT_COUNTDOWN_SECONDS;
        }
        if (configuration.isInt(COUNTDOWN_SECONDS_PATH)) {
            int countdownSeconds = configuration.getInt(COUNTDOWN_SECONDS_PATH);
            if (countdownSeconds >= MatchService.MINIMUM_COUNTDOWN_SECONDS
                    && countdownSeconds <= MatchService.MAXIMUM_COUNTDOWN_SECONDS) {
                return countdownSeconds;
            }
        }
        problemReporter.accept(Messages.plural(ConfigMessage.COUNTDOWN_SECONDS_OUT_OF_RANGE,
                MatchService.DEFAULT_COUNTDOWN_SECONDS, Placeholder.of("path", COUNTDOWN_SECONDS_PATH),
                Placeholder.of("minimum", MatchService.MINIMUM_COUNTDOWN_SECONDS),
                Placeholder.of("maximum", MatchService.MAXIMUM_COUNTDOWN_SECONDS)));
        return MatchService.DEFAULT_COUNTDOWN_SECONDS;
    }

    private static int readReconnectSeconds(ConfigurationSection configuration, Consumer<String> problemReporter) {
        if (!configuration.isSet(RECONNECT_SECONDS_PATH)) {
            return MatchService.DEFAULT_RECONNECT_SECONDS;
        }
        if (configuration.isInt(RECONNECT_SECONDS_PATH)) {
            int reconnectSeconds = configuration.getInt(RECONNECT_SECONDS_PATH);
            if (reconnectSeconds >= MatchService.MINIMUM_RECONNECT_SECONDS
                    && reconnectSeconds <= MatchService.MAXIMUM_RECONNECT_SECONDS) {
                return reconnectSeconds;
            }
        }
        problemReporter.accept(Messages.plural(ConfigMessage.RECONNECT_SECONDS_OUT_OF_RANGE,
                MatchService.DEFAULT_RECONNECT_SECONDS, Placeholder.of("path", RECONNECT_SECONDS_PATH),
                Placeholder.of("minimum", MatchService.MINIMUM_RECONNECT_SECONDS),
                Placeholder.of("maximum", MatchService.MAXIMUM_RECONNECT_SECONDS)));
        return MatchService.DEFAULT_RECONNECT_SECONDS;
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
        problemReporter.accept(Messages.plural(ConfigMessage.TEAM_SIZE_OUT_OF_RANGE, TeamSizeSetting.DEFAULT_TEAM_SIZE,
                Placeholder.of("path", TEAM_SIZE_PATH), Placeholder.of("minimum", Team.MINIMUM_MEMBER_COUNT),
                Placeholder.of("maximum", Team.MAXIMUM_MEMBER_COUNT)));
        return TeamSizeSetting.DEFAULT_TEAM_SIZE;
    }

    private static AutoMatchSettings readAutoMatchSettings(ConfigurationSection configuration,
                                                           Consumer<String> problemReporter) {
        return new AutoMatchSettings(readAutoMatchEnabled(configuration, problemReporter),
                readAutoMatchStartDelaySeconds(configuration, problemReporter),
                readAutoMatchPauseSeconds(configuration, problemReporter));
    }

    private static boolean readAutoMatchEnabled(ConfigurationSection configuration,
                                                Consumer<String> problemReporter) {
        if (!configuration.isSet(AUTO_MATCH_ENABLED_PATH)) {
            return AutoMatchSettings.DEFAULT_ENABLED;
        }
        if (configuration.isBoolean(AUTO_MATCH_ENABLED_PATH)) {
            return configuration.getBoolean(AUTO_MATCH_ENABLED_PATH);
        }
        problemReporter.accept(Messages.text(ConfigMessage.AUTO_MATCH_ENABLED_NOT_A_TRUTH_VALUE,
                Placeholder.of("path", AUTO_MATCH_ENABLED_PATH),
                Placeholder.of("fallback", AutoMatchSettings.DEFAULT_ENABLED)));
        return AutoMatchSettings.DEFAULT_ENABLED;
    }

    private static int readAutoMatchStartDelaySeconds(ConfigurationSection configuration,
                                                      Consumer<String> problemReporter) {
        if (!configuration.isSet(AUTO_MATCH_START_DELAY_SECONDS_PATH)) {
            return AutoMatchSettings.DEFAULT_START_DELAY_SECONDS;
        }
        if (configuration.isInt(AUTO_MATCH_START_DELAY_SECONDS_PATH)) {
            int startDelaySeconds = configuration.getInt(AUTO_MATCH_START_DELAY_SECONDS_PATH);
            if (startDelaySeconds >= AutoMatchSettings.MINIMUM_START_DELAY_SECONDS
                    && startDelaySeconds <= AutoMatchSettings.MAXIMUM_START_DELAY_SECONDS) {
                return startDelaySeconds;
            }
        }
        problemReporter.accept(Messages.plural(ConfigMessage.AUTO_MATCH_START_DELAY_OUT_OF_RANGE,
                AutoMatchSettings.DEFAULT_START_DELAY_SECONDS,
                Placeholder.of("path", AUTO_MATCH_START_DELAY_SECONDS_PATH),
                Placeholder.of("minimum", AutoMatchSettings.MINIMUM_START_DELAY_SECONDS),
                Placeholder.of("maximum", AutoMatchSettings.MAXIMUM_START_DELAY_SECONDS)));
        return AutoMatchSettings.DEFAULT_START_DELAY_SECONDS;
    }

    private static int readAutoMatchPauseSeconds(ConfigurationSection configuration,
                                                 Consumer<String> problemReporter) {
        if (!configuration.isSet(AUTO_MATCH_PAUSE_SECONDS_PATH)) {
            return AutoMatchSettings.DEFAULT_PAUSE_SECONDS;
        }
        if (configuration.isInt(AUTO_MATCH_PAUSE_SECONDS_PATH)) {
            int pauseSeconds = configuration.getInt(AUTO_MATCH_PAUSE_SECONDS_PATH);
            if (pauseSeconds >= AutoMatchSettings.MINIMUM_PAUSE_SECONDS
                    && pauseSeconds <= AutoMatchSettings.MAXIMUM_PAUSE_SECONDS) {
                return pauseSeconds;
            }
        }
        problemReporter.accept(Messages.plural(ConfigMessage.AUTO_MATCH_PAUSE_OUT_OF_RANGE,
                AutoMatchSettings.DEFAULT_PAUSE_SECONDS, Placeholder.of("path", AUTO_MATCH_PAUSE_SECONDS_PATH),
                Placeholder.of("minimum", AutoMatchSettings.MINIMUM_PAUSE_SECONDS),
                Placeholder.of("maximum", AutoMatchSettings.MAXIMUM_PAUSE_SECONDS)));
        return AutoMatchSettings.DEFAULT_PAUSE_SECONDS;
    }

    private static ArenaCleaningParticleSettings readArenaCleaningParticleSettings(
            ConfigurationSection configuration, Consumer<String> problemReporter) {
        ArenaCleaningParticleSettings defaults = ArenaCleaningParticleSettings.defaults();
        double totalDurationSeconds = configuration.getDouble(
                ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH,
                defaults.totalDurationSeconds());
        if (totalDurationSeconds < 0.0D) {
            problemReporter.accept(Messages.text(ConfigMessage.CLEANING_DURATION_NEGATIVE,
                    Placeholder.of("path", ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH),
                    Placeholder.of("seconds", defaults.totalDurationSeconds())));
            totalDurationSeconds = defaults.totalDurationSeconds();
        }
        int columnsPerTick = configuration.getInt(
                ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH, defaults.columnsPerTick());
        if (columnsPerTick < 0) {
            problemReporter.accept(Messages.plural(ConfigMessage.CLEANING_COLUMNS_NEGATIVE, defaults.columnsPerTick(),
                    Placeholder.of("path", ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH)));
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
        problemReporter.accept(Messages.plural(ConfigMessage.START_GATE_DURATION_TOO_SHORT,
                StartGateDuration.DEFAULT_SECONDS, Placeholder.of("path", START_GATE_DURATION_SECONDS_PATH),
                Placeholder.of("minimum", StartGateDuration.MINIMUM_SECONDS)));
        return StartGateDuration.DEFAULT_SECONDS;
    }

    private static RoundTimeoutSettings readRoundTimeoutSettings(ConfigurationSection configuration,
                                                                 Consumer<String> problemReporter) {
        RoundTimeoutSettings defaultSettings = RoundTimeoutSettings.defaults();
        int roundTimeoutSeconds = configuration.getInt(ROUND_TIMEOUT_SECONDS_PATH,
                defaultSettings.roundTimeoutSeconds());
        if (roundTimeoutSeconds < 1) {
            problemReporter.accept(Messages.plural(ConfigMessage.ROUND_TIMEOUT_TOO_SHORT,
                    defaultSettings.roundTimeoutSeconds(), Placeholder.of("path", ROUND_TIMEOUT_SECONDS_PATH)));
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
            problemReporter.accept(Messages.text(ConfigMessage.SAME_TEAM_COLOURS,
                    Placeholder.of("path", TeamColourSettings.SECTION_CONFIGURATION_PATH),
                    Placeholder.of("colour", TeamColourSettings.nameOf(firstTeamColour)),
                    Placeholder.of("firstColour", TeamColourSettings.nameOf(defaults.firstTeamColour())),
                    Placeholder.of("secondColour", TeamColourSettings.nameOf(defaults.secondTeamColour()))));
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
        problemReporter.accept(Messages.text(ConfigMessage.UNKNOWN_TEAM_COLOUR, Placeholder.of("path", path),
                Placeholder.of("fallback", TeamColourSettings.nameOf(fallbackColour)),
                Placeholder.of("colours", Messages.join(TeamColourSettings.knownColourNames()))));
        return fallbackColour;
    }

    private static ConfirmedBorderParticleSettings readConfirmedBorderParticleSettings(
            ConfigurationSection configuration, Consumer<String> problemReporter) {
        ConfirmedBorderParticleSettings defaults = ConfirmedBorderParticleSettings.defaults();
        double height = configuration.getDouble(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH,
                defaults.height());
        if (height < 0.0D) {
            problemReporter.accept(Messages.text(ConfigMessage.MARKER_HEIGHT_NEGATIVE,
                    Placeholder.of("path", ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH),
                    Placeholder.of("height", defaults.height())));
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

    private static boolean readConfirmedBorderParticleEnabled(ConfigurationSection configuration,
                                                              Consumer<String> problemReporter) {
        if (!configuration.isSet(ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH)) {
            return BorderParticleService.DEFAULT_CONFIRMED_PARTICLE_ENABLED;
        }
        if (configuration.isBoolean(ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH)) {
            return configuration.getBoolean(ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH);
        }
        problemReporter.accept(Messages.text(ConfigMessage.CONFIRMED_PARTICLE_ENABLED_NOT_A_TRUTH_VALUE,
                Placeholder.of("path", ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH),
                Placeholder.of("fallback", BorderParticleService.DEFAULT_CONFIRMED_PARTICLE_ENABLED)));
        return BorderParticleService.DEFAULT_CONFIRMED_PARTICLE_ENABLED;
    }

    private static boolean readAreaInventoryEnabled(ConfigurationSection configuration,
                                                    Consumer<String> problemReporter) {
        if (!configuration.isSet(ARENA_AREA_INVENTORY_ENABLED_PATH)) {
            return AreaInventoryService.DEFAULT_ENABLED;
        }
        if (configuration.isBoolean(ARENA_AREA_INVENTORY_ENABLED_PATH)) {
            return configuration.getBoolean(ARENA_AREA_INVENTORY_ENABLED_PATH);
        }
        problemReporter.accept(Messages.text(ConfigMessage.AREA_INVENTORY_ENABLED_NOT_A_TRUTH_VALUE,
                Placeholder.of("path", ARENA_AREA_INVENTORY_ENABLED_PATH),
                Placeholder.of("fallback", AreaInventoryService.DEFAULT_ENABLED)));
        return AreaInventoryService.DEFAULT_ENABLED;
    }
}

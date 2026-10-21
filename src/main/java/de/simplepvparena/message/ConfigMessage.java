package de.simplepvparena.message;

import java.util.Set;

public enum ConfigMessage implements MessageKey {

    LANGUAGE_NOT_SHIPPED("config.problem.language-not-shipped", "path", "language", "fallback", "languages"),
    CELL_LIMIT_TOO_LOW("config.problem.cell-limit-too-low", "path", "fallback"),
    BASELINE_DEPTH_OUT_OF_RANGE("config.problem.baseline-depth-out-of-range", "path", "maximum", "count"),
    SAFETY_ZONE_RADIUS_NEGATIVE("config.problem.safety-zone-radius-negative", "path", "count"),
    EFFECT_MARGIN_OUT_OF_RANGE("config.problem.effect-margin-out-of-range", "path", "minimum", "maximum", "count"),
    COUNTDOWN_SECONDS_OUT_OF_RANGE("config.problem.countdown-seconds-out-of-range", "path", "minimum", "maximum",
            "count"),
    RECONNECT_SECONDS_OUT_OF_RANGE("config.problem.reconnect-seconds-out-of-range", "path", "minimum", "maximum",
            "count"),
    TEAM_SIZE_OUT_OF_RANGE("config.problem.team-size-out-of-range", "path", "minimum", "maximum", "count"),
    AUTO_MATCH_ENABLED_NOT_A_TRUTH_VALUE("config.problem.auto-match-enabled-not-a-truth-value", "path",
            "fallback"),
    AUTO_MATCH_START_DELAY_OUT_OF_RANGE("config.problem.auto-match-start-delay-out-of-range", "path", "minimum",
            "maximum", "count"),
    AUTO_MATCH_PAUSE_OUT_OF_RANGE("config.problem.auto-match-pause-out-of-range", "path", "minimum", "maximum",
            "count"),
    CLEANING_DURATION_NEGATIVE("config.problem.cleaning-duration-negative", "path", "seconds"),
    CLEANING_COLUMNS_NEGATIVE("config.problem.cleaning-columns-negative", "path", "count"),
    START_GATE_DURATION_TOO_SHORT("config.problem.start-gate-duration-too-short", "path", "minimum", "count"),
    ROUND_TIMEOUT_TOO_SHORT("config.problem.round-timeout-too-short", "path", "count"),
    SAME_TEAM_COLOURS("config.problem.same-team-colours", "path", "colour", "firstColour", "secondColour"),
    UNKNOWN_TEAM_COLOUR("config.problem.unknown-team-colour", "path", "fallback", "colours"),
    MARKER_HEIGHT_NEGATIVE("config.problem.marker-height-negative", "path", "height"),
    CONFIRMED_PARTICLE_ENABLED_NOT_A_TRUTH_VALUE("config.problem.confirmed-particle-enabled-not-a-truth-value", "path",
            "fallback"),
    RELOADED("config.reload.reloaded"),
    LANGUAGE_CATEGORY("config.reload.category.language"),
    ROUND_CATEGORY("config.reload.category.round"),
    START_GATE_CATEGORY("config.reload.category.start-gate"),
    COUNTDOWN_CATEGORY("config.reload.category.countdown"),
    RECONNECT_CATEGORY("config.reload.category.reconnect"),
    TEAM_SIZE_CATEGORY("config.reload.category.team-size"),
    AUTO_MATCH_CATEGORY("config.reload.category.auto-match"),
    CLEANING_CATEGORY("config.reload.category.cleaning"),
    BASELINE_DEPTH_CATEGORY("config.reload.category.baseline-depth"),
    CELL_LIMIT_CATEGORY("config.reload.category.cell-limit"),
    SAFETY_ZONE_CATEGORY("config.reload.category.safety-zone"),
    EFFECT_MARGIN_CATEGORY("config.reload.category.effect-margin"),
    TELEPORT_ON_JOIN_CATEGORY("config.reload.category.teleport-on-join"),
    BORDER_CATEGORY("config.reload.category.border"),
    UNTOUCHED("config.reload.untouched"),
    UNUSABLE_ENTRIES("config.reload.unusable-entries"),
    TEAM_SIZE_NOT_TAKEN_OVER("config.notice.team-size-not-taken-over", "rejection", "teamSize", "path", "matchUp"),
    LARGER_TEAMS_AT_START("config.notice.larger-teams-at-start", "teams", "teamSize", "path"),
    TRIBUNE_OUTSIDE_OF_THE_EFFECT_AREA("config.notice.tribune-outside-of-the-effect-area", "warning", "path");

    private final String path;
    private final Set<String> placeholderNames;

    ConfigMessage(String path, String... placeholderNames) {
        this.path = path;
        this.placeholderNames = Set.of(placeholderNames);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Set<String> placeholderNames() {
        return placeholderNames;
    }
}

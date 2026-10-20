package de.simplepvparena.message;

import java.util.Set;

public enum DryRunCheckMessage implements MessageKey {

    ARENA_NOT_READY("dry-run.arena.not-ready"),
    ARENA_NEVER_CALCULATED("dry-run.arena.never-calculated"),
    FIRST_TEAM_SPAWN_MISSING("dry-run.arena.first-team-spawn-missing"),
    SECOND_TEAM_SPAWN_MISSING("dry-run.arena.second-team-spawn-missing"),
    BOTH_TEAM_SPAWNS_MISSING("dry-run.arena.both-team-spawns-missing"),
    FIGHT_AREA("dry-run.arena.fight-area", "area"),
    CLEANING_WRITES_BLOCKS("dry-run.arena.cleaning-writes-blocks", "count", "floorY", "topY"),
    SPAWNS_SET("dry-run.arena.spawns-set"),
    KIT_EMPTY("dry-run.kit.empty", "tile"),
    KIT_ITEMS("dry-run.kit.items", "count"),
    NO_GATE_BLOCK("dry-run.gate.no-block", "action"),
    COUNTDOWN("dry-run.gate.countdown", "seconds", "release"),
    LAYER_TOO_EARLY("dry-run.gate.layer-too-early", "layer", "world", "tick"),
    GATE_GONE_TOO_EARLY("dry-run.gate.gone-too-early", "world", "tick", "countdown"),
    GATE_OF_WORLD("dry-run.gate.world", "world", "blocks", "layers", "tick", "countdown"),
    GATE_BLOCK_COUNT("dry-run.gate.block-count", "count"),
    GATE_LAYER_COUNT("dry-run.gate.layer-count", "count"),
    GATE_TALLER_THAN_COUNTDOWN("dry-run.gate.taller-than-countdown", "count", "release"),
    SECOND_READS("dry-run.gate.second-reads", "second", "title"),
    END_READS("dry-run.gate.end-reads", "title", "release"),
    TEST_TEAMS_NOT_REGISTERED("dry-run.match.test-teams-not-registered"),
    BRACKET_BUILT("dry-run.match.bracket-built", "teams", "rounds", "matches"),
    BRACKET_TEAM_COUNT("dry-run.match.team-count", "count"),
    BRACKET_ROUND_COUNT("dry-run.match.round-count", "count"),
    BRACKET_MATCH_COUNT("dry-run.match.match-count", "count"),
    FINAL_WITHOUT_BOTH_TEST_TEAMS("dry-run.match.final-without-both-test-teams"),
    TEAM_ONLY_IN_BRACKET("dry-run.match.team-only-in-bracket", "team"),
    MATCH_RESOLVED("dry-run.match.resolved", "match", "firstTeam", "secondTeam"),
    SEQUENCE_STARTS_FINAL("dry-run.match.sequence-starts-final"),
    SEQUENCE_LEAVES_FINAL("dry-run.match.sequence-leaves-final"),
    NOTHING_REACHED_THE_ARENA("dry-run.match.nothing-reached-the-arena"),
    FINAL_STAGE_NOT_READY("dry-run.final.stage-not-ready", "stage", "expected"),
    FINAL_REFUSED("dry-run.final.refused", "reason"),
    FINAL_ANNOUNCED_TIMES("dry-run.final.announced-times", "count"),
    FINAL_TURNS_READY("dry-run.final.turns-ready", "stage", "announcement"),
    BRACKET_INCOMPLETE("dry-run.final.bracket-incomplete"),
    WRONG_CHAMPION("dry-run.final.wrong-champion", "champion", "winner"),
    NO_CHAMPION("dry-run.final.no-champion", "winner"),
    FINAL_STAGE_NOT_FINISHED("dry-run.final.stage-not-finished", "stage", "expected"),
    SIMULATED_RESULT("dry-run.final.simulated-result", "winner", "stage"),
    CLEANING_REFUSED_ARENA_LEFT_BEHIND("dry-run.cleaning.arena-left-behind"),
    NO_BASELINE("dry-run.cleaning.no-baseline", "action"),
    CLEANING_WOULD_START("dry-run.cleaning.would-start"),
    CLEANED_BLOCKS("dry-run.cleaning.blocks", "count", "floorY", "topY"),
    NOTHING_CLEANED("dry-run.cleaning.nothing-cleaned");

    private final String path;
    private final Set<String> placeholderNames;

    DryRunCheckMessage(String path, String... placeholderNames) {
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

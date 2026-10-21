package de.simplepvparena.message;

import java.util.Set;

public enum TournamentMessage implements MessageKey {

    STARTED_REPLY("tournament.reply.started"),
    PAUSED_REPLY("tournament.reply.paused"),
    RESUMED_REPLY("tournament.reply.resumed"),
    CANCELLED_REPLY("tournament.reply.cancelled"),
    AT_LEAST_TEAMS_REQUIRED("tournament.start.at-least-teams-required", "count"),
    UNEVEN_TEAM_COUNT("tournament.start.uneven-team-count", "action"),
    AUTO_MATCH_RUNNING("tournament.start.auto-match-running"),
    ARENA_NOT_CONFIGURED("tournament.arena.not-configured", "calculateAction", "tribuneSpawnAction"),
    ARENA_BEING_CLEANED("tournament.arena.being-cleaned"),
    ARENA_OCCUPIED("tournament.arena.occupied", "match"),
    TEAM_NOT_IN_MATCH("tournament.result.team-not-in-match", "team", "match"),
    MATCH_DECIDED("tournament.result.decided", "match", "team"),
    UNKNOWN_MATCH("tournament.result.unknown-match", "match"),
    MATCH_ALREADY_DECIDED("tournament.result.already-decided", "match"),
    KIT_CHOICE_RUNNING("tournament.kit-choice-running", "match"),
    FINAL_WHILE_PAUSED("tournament.final.paused", "action"),
    FINAL_ARENA_BEING_CLEANED("tournament.final.arena-being-cleaned"),
    FINAL_WALKOVER("tournament.final.walkover", "team"),
    FINAL_FINALIST_MISSING("tournament.final.finalist-missing"),
    FINAL_KIT_BEING_CHOSEN("tournament.final.kit-being-chosen"),
    FINAL_STARTED("tournament.final.started"),
    RENAME_IN_TOURNAMENT("tournament.team.rename-refusal"),
    TEAM_IN_MATCH("tournament.team.in-match", "team", "match"),
    TEAM_WAITING_FOR_KIT_CHOICE("tournament.team.waiting-for-kit-choice", "team", "match"),
    TEAM_ABOUT_TO_PLAY_AUTO_MATCH("tournament.team.about-to-play-auto-match", "team"),
    STARTED("tournament.announcement.started", "teams", "rounds"),
    TEAM_COUNT("tournament.announcement.team-count", "count"),
    ROUND_COUNT("tournament.announcement.round-count", "count"),
    BYES("tournament.announcement.byes", "teams"),
    PAUSED("tournament.announcement.paused"),
    CONTINUES("tournament.announcement.continues"),
    CANCELLED("tournament.announcement.cancelled"),
    MATCH_ANNOUNCEMENT("tournament.announcement.match", "match", "firstTeam", "secondTeam"),
    NEXT_MATCH("tournament.announcement.next-match", "firstTeam", "secondTeam", "match"),
    FINAL_PLAYERS("tournament.announcement.final-players", "firstTeam", "secondTeam"),
    WALKOVER("tournament.announcement.walkover", "team", "match"),
    WINNER("tournament.announcement.winner", "team"),
    NO_BASELINE("tournament.notice.no-baseline", "action"),
    SMALLER_TEAMS("tournament.notice.smaller-teams", "teams"),
    LARGER_TEAMS("tournament.notice.larger-teams", "teams"),
    CLEANING_SKIPPED_AFTER_FINAL("tournament.notice.cleaning-skipped-after-final", "match", "reason", "action"),
    CLEANING_SKIPPED("tournament.notice.cleaning-skipped", "match", "reason"),
    BOTH_TEAMS_MISSING("tournament.notice.both-teams-missing", "match", "tile"),
    RESTORE_UNREADABLE("tournament.restore.unreadable", "file", "reason"),
    RESTORE_UNREADABLE_WITHOUT_REASON("tournament.restore.unreadable-without-reason", "file"),
    RESTORE_INCONSISTENT("tournament.restore.inconsistent", "file", "reason"),
    RESTORED("tournament.restore.restored", "file", "count", "rounds", "action"),
    RESTORED_WITHOUT_BASELINE("tournament.restore.without-baseline", "action"),
    UNREGISTERED_TEAMS("tournament.restore.unregistered-teams", "teams"),
    SAVE_FAILURE("tournament.storage.save-failure", "reason"),
    DELETE_FAILURE("tournament.storage.delete-failure", "reason");

    private final String path;
    private final Set<String> placeholderNames;

    TournamentMessage(String path, String... placeholderNames) {
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

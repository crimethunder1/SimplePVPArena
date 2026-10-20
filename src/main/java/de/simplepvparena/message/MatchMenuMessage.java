package de.simplepvparena.message;

import java.util.Set;

public enum MatchMenuMessage implements MessageKey {

    TITLE("match-menu.title"),
    RUNNING_MATCH("match-menu.running-match", "match"),
    NO_RUNNING_MATCH("match-menu.no-running-match"),
    NO_MATCH_RUNNING("match-menu.no-match-running"),
    INFO_TITLE("match-menu.tile.info.title"),
    ROUND_TIMER_PAUSED("match-menu.tile.info.round-timer-paused"),
    ROUND_TIMER_RUNNING("match-menu.tile.info.round-timer-running"),
    START_TITLE("match-menu.tile.start.title"),
    START_LORE("match-menu.tile.start.lore"),
    FORCE_START_TITLE("match-menu.tile.force-start.title"),
    FORCE_START_LORE("match-menu.tile.force-start.lore"),
    PAUSE_TITLE("match-menu.tile.pause.title"),
    PAUSE_LORE("match-menu.tile.pause.lore"),
    CONTINUE_TITLE("match-menu.tile.continue.title"),
    CONTINUE_LORE("match-menu.tile.continue.lore"),
    CANCEL_TITLE("match-menu.tile.cancel.title"),
    CANCEL_LORE("match-menu.tile.cancel.lore"),
    DECIDE_WAITING_MATCH_TITLE("match-menu.tile.decide-waiting-match.title"),
    DECIDE_WAITING_MATCH_LORE("match-menu.tile.decide-waiting-match.lore"),
    FORCE_RESULT_TITLE("match-menu.tile.force-result.title"),
    FORCE_RESULT_LORE("match-menu.tile.force-result.lore"),
    FINAL_TITLE("match-menu.tile.final.title"),
    FINAL_LORE("match-menu.tile.final.lore"),
    FINAL_FINALISTS_NOT_DECIDED("match-menu.tile.final.finalists-not-decided"),
    FINAL_READY_WHILE_PAUSED("match-menu.tile.final.ready-while-paused"),
    FINAL_RUNNING("match-menu.tile.final.running"),
    FINAL_OVER("match-menu.tile.final.over"),
    CANCEL_CONFIRMATION("match-menu.cancel-confirmation"),
    FORCE_RESULT_MENU_TITLE("force-result-menu.title", "match"),
    FORCE_RESULT_MATCH_NOT_RUNNING("force-result-menu.match-not-running", "match"),
    FORCE_RESULT_INFO_TITLE("force-result-menu.tile.info.title"),
    FORCE_RESULT_INFO_MATCH("force-result-menu.tile.info.match", "match"),
    FORCE_RESULT_INFO_ROUND("force-result-menu.tile.info.round", "round"),
    FORCE_RESULT_INFO_HINT("force-result-menu.tile.info.hint"),
    FORCE_RESULT_TEAM_LORE("force-result-menu.tile.team.lore");

    private final String path;
    private final Set<String> placeholderNames;

    MatchMenuMessage(String path, String... placeholderNames) {
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

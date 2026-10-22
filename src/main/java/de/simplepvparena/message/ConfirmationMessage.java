package de.simplepvparena.message;

import java.util.Set;

public enum ConfirmationMessage implements MessageKey {

    CONFIRM_TITLE("confirmation.tile.confirm.title"),
    CONFIRM_LORE("confirmation.tile.confirm.lore"),
    REFUSE_TITLE("confirmation.tile.refuse.title"),
    REFUSE_LORE("confirmation.tile.refuse.lore"),
    CLEAN_ARENA_QUESTION("confirmation.question.clean-arena"),
    RESET_ARENA_QUESTION("confirmation.question.reset-arena"),
    REPLACE_KIT_QUESTION("confirmation.question.replace-kit"),
    DELETE_KIT_QUESTION("confirmation.question.delete-kit"),
    REMOVE_TEAM_QUESTION("confirmation.question.remove-team"),
    CANCEL_TOURNAMENT_QUESTION("confirmation.question.cancel-tournament"),
    RESTART_MATCH_QUESTION("confirmation.question.restart-match");

    private final String path;
    private final Set<String> placeholderNames;

    ConfirmationMessage(String path, String... placeholderNames) {
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

package de.simplepvparena.gui;

import de.simplepvparena.message.ConfirmationMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

import java.util.Optional;

public enum ConfirmationRequest {

    CLEAN_ARENA(ConfirmationMessage.CLEAN_ARENA_QUESTION),
    RESET_ARENA(ConfirmationMessage.RESET_ARENA_QUESTION),
    REPLACE_KIT(ConfirmationMessage.REPLACE_KIT_QUESTION),
    DELETE_KIT(ConfirmationMessage.DELETE_KIT_QUESTION),
    REMOVE_TEAM(ConfirmationMessage.REMOVE_TEAM_QUESTION),
    CANCEL_TOURNAMENT(ConfirmationMessage.CANCEL_TOURNAMENT_QUESTION),
    RESTART_MATCH(ConfirmationMessage.RESTART_MATCH_QUESTION),
    FORM_RANDOM_TEAMS(ConfirmationMessage.FORM_RANDOM_TEAMS_QUESTION),
    CLEAR_REGISTRATIONS(ConfirmationMessage.CLEAR_REGISTRATIONS_QUESTION),
    REVOKE_RESULT(ConfirmationMessage.REVOKE_RESULT_QUESTION, "match");

    private final MessageKey question;
    private final String subjectPlaceholderName;

    ConfirmationRequest(MessageKey question) {
        this(question, null);
    }

    ConfirmationRequest(MessageKey question, String subjectPlaceholderName) {
        this.question = question;
        this.subjectPlaceholderName = subjectPlaceholderName;
    }

    public String getTitle() {
        return Messages.text(question);
    }

    public String getTitle(Optional<String> subject) {
        if (subjectPlaceholderName == null || subject.isEmpty()) {
            return getTitle();
        }
        return Messages.text(question, Placeholder.of(subjectPlaceholderName, subject.get()));
    }
}

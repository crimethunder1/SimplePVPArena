package de.simplepvparena.gui;

import de.simplepvparena.message.ConfirmationMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;

public enum ConfirmationRequest {

    CLEAN_ARENA(ConfirmationMessage.CLEAN_ARENA_QUESTION),
    RESET_ARENA(ConfirmationMessage.RESET_ARENA_QUESTION),
    REPLACE_KIT(ConfirmationMessage.REPLACE_KIT_QUESTION),
    DELETE_KIT(ConfirmationMessage.DELETE_KIT_QUESTION),
    REMOVE_TEAM(ConfirmationMessage.REMOVE_TEAM_QUESTION),
    CANCEL_TOURNAMENT(ConfirmationMessage.CANCEL_TOURNAMENT_QUESTION);

    private final MessageKey question;

    ConfirmationRequest(MessageKey question) {
        this.question = question;
    }

    public String getTitle() {
        return Messages.text(question);
    }
}

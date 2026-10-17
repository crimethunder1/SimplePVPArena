package de.simplepvparena.tournament;

import java.util.ArrayList;
import java.util.List;

final class RecordingKitChoicePresenter implements KitChoicePresenter {

    private final List<PendingKitChoice> shownChoices = new ArrayList<>();
    private final List<String> closeMessages = new ArrayList<>();

    private boolean audience = true;

    @Override
    public void show(PendingKitChoice pendingKitChoice) {
        shownChoices.add(pendingKitChoice);
    }

    @Override
    public void close(String message) {
        closeMessages.add(message);
    }

    @Override
    public boolean hasAudience() {
        return audience;
    }

    void letEveryAdminLeave() {
        audience = false;
    }

    List<PendingKitChoice> shownChoices() {
        return shownChoices;
    }

    List<String> closeMessages() {
        return closeMessages;
    }
}

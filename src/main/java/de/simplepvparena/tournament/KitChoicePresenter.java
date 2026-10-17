package de.simplepvparena.tournament;

public interface KitChoicePresenter {

    KitChoicePresenter NONE = new KitChoicePresenter() {

        @Override
        public void show(PendingKitChoice pendingKitChoice) {
        }

        @Override
        public void close(String message) {
        }

        @Override
        public boolean hasAudience() {
            return false;
        }
    };

    void show(PendingKitChoice pendingKitChoice);

    void close(String message);

    boolean hasAudience();
}

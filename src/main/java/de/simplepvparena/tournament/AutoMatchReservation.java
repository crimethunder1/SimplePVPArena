package de.simplepvparena.tournament;

import java.util.Optional;

public interface AutoMatchReservation {

    AutoMatchReservation NONE = new AutoMatchReservation() {

        @Override
        public boolean occupiesTheArena() {
            return false;
        }

        @Override
        public Optional<String> findAnnouncedTeamName(String teamName) {
            return Optional.empty();
        }

        @Override
        public void cancelAnnouncementForATournament() {
        }
    };

    boolean occupiesTheArena();

    Optional<String> findAnnouncedTeamName(String teamName);

    void cancelAnnouncementForATournament();
}

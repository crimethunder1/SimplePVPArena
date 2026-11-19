package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
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

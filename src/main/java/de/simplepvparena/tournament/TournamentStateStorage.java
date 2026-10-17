package de.simplepvparena.tournament;

import java.io.IOException;

public interface TournamentStateStorage {

    TournamentStateStorage NONE = new TournamentStateStorage() {

        @Override
        public void save(SavedTournament savedTournament) {
        }

        @Override
        public TournamentStateLookup find() {
            return TournamentStateLookup.missing("");
        }

        @Override
        public void delete() {
        }
    };

    void save(SavedTournament savedTournament) throws IOException;

    TournamentStateLookup find();

    void delete() throws IOException;
}

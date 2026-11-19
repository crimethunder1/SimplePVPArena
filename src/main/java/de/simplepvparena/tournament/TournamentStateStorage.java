package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
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

package de.simplepvparena.tournament;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class InMemoryTournamentStateStorage implements TournamentStateStorage {

    static final String FILE_NAME = "memory.yml";
    static final String SAVE_FAILURE = "the disk is full";

    private final List<SavedTournament> savedTournaments = new ArrayList<>();
    private Optional<SavedTournament> storedTournament = Optional.empty();
    private int deleteCount;
    private int failedSaveCount;
    private boolean failingSaves;

    @Override
    public void save(SavedTournament savedTournament) throws IOException {
        if (failingSaves) {
            failedSaveCount++;
            throw new IOException(SAVE_FAILURE);
        }
        savedTournaments.add(savedTournament);
        storedTournament = Optional.of(savedTournament);
    }

    @Override
    public TournamentStateLookup find() {
        return storedTournament
                .map(savedTournament -> TournamentStateLookup.found(savedTournament, FILE_NAME))
                .orElseGet(() -> TournamentStateLookup.missing(FILE_NAME));
    }

    @Override
    public void delete() {
        deleteCount++;
        storedTournament = Optional.empty();
    }

    void store(SavedTournament savedTournament) {
        storedTournament = Optional.of(savedTournament);
    }

    void failSaves(boolean failing) {
        failingSaves = failing;
    }

    List<SavedTournament> savedTournaments() {
        return List.copyOf(savedTournaments);
    }

    SavedTournament lastSavedTournament() {
        return savedTournaments.get(savedTournaments.size() - 1);
    }

    Optional<SavedTournament> storedTournament() {
        return storedTournament;
    }

    int deleteCount() {
        return deleteCount;
    }

    int failedSaveCount() {
        return failedSaveCount;
    }
}

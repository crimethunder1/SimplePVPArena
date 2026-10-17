package de.simplepvparena.support;

import de.simplepvparena.kit.KitLibraryLookup;
import de.simplepvparena.kit.KitLibraryStorage;
import de.simplepvparena.kit.SavedKitLibrary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InMemoryKitLibraryStorage implements KitLibraryStorage {

    public static final String FILE_NAME = "memory-kits.yml";
    public static final String SAVE_FAILURE = "the disk is full";

    private final List<SavedKitLibrary> savedLibraries = new ArrayList<>();
    private Optional<SavedKitLibrary> storedLibrary = Optional.empty();
    private boolean failingSaves;
    private int failedSaveCount;

    @Override
    public void save(SavedKitLibrary savedKitLibrary) throws IOException {
        if (failingSaves) {
            failedSaveCount++;
            throw new IOException(SAVE_FAILURE);
        }
        savedLibraries.add(savedKitLibrary);
        storedLibrary = Optional.of(savedKitLibrary);
    }

    @Override
    public KitLibraryLookup find() {
        return storedLibrary
                .map(savedKitLibrary -> KitLibraryLookup.found(savedKitLibrary, FILE_NAME))
                .orElseGet(() -> KitLibraryLookup.missing(FILE_NAME));
    }

    public void store(SavedKitLibrary savedKitLibrary) {
        storedLibrary = Optional.of(savedKitLibrary);
    }

    public void failSaves(boolean failing) {
        failingSaves = failing;
    }

    public List<SavedKitLibrary> savedLibraries() {
        return List.copyOf(savedLibraries);
    }

    public SavedKitLibrary lastSavedLibrary() {
        return savedLibraries.get(savedLibraries.size() - 1);
    }

    public int failedSaveCount() {
        return failedSaveCount;
    }
}

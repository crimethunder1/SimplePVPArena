package de.simplepvparena.kit;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
public interface KitLibraryStorage {

    KitLibraryStorage NONE = new KitLibraryStorage() {

        @Override
        public void save(SavedKitLibrary savedKitLibrary) {
        }

        @Override
        public KitLibraryLookup find() {
            return KitLibraryLookup.missing("");
        }
    };

    void save(SavedKitLibrary savedKitLibrary) throws IOException;

    KitLibraryLookup find();
}

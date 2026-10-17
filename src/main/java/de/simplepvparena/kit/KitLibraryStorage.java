package de.simplepvparena.kit;

import java.io.IOException;

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

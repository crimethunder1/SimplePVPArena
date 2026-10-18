package de.simplepvparena.message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class MapMessageCatalog implements MessageCatalog {

    private final Map<String, List<String>> linesByPath = new LinkedHashMap<>();

    MapMessageCatalog with(String path, String... lines) {
        linesByPath.put(path, List.of(lines));
        return this;
    }

    @Override
    public Optional<List<String>> find(String path) {
        return Optional.ofNullable(linesByPath.get(path));
    }

    @Override
    public Set<String> paths() {
        return Collections.unmodifiableSet(linesByPath.keySet());
    }
}

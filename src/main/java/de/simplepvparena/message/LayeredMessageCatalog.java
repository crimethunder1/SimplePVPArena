package de.simplepvparena.message;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class LayeredMessageCatalog implements MessageCatalog {

    private final List<MessageCatalog> layers;

    LayeredMessageCatalog(List<MessageCatalog> layers) {
        this.layers = List.copyOf(layers);
    }

    @Override
    public Optional<List<String>> find(String path) {
        for (MessageCatalog layer : layers) {
            Optional<List<String>> lines = layer.find(path);
            if (lines.isPresent()) {
                return lines;
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<String> paths() {
        Set<String> paths = new LinkedHashSet<>();
        for (MessageCatalog layer : layers) {
            paths.addAll(layer.paths());
        }
        return Collections.unmodifiableSet(paths);
    }
}

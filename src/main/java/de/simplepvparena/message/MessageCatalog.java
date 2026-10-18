package de.simplepvparena.message;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MessageCatalog {

    Optional<List<String>> find(String path);

    Set<String> paths();
}

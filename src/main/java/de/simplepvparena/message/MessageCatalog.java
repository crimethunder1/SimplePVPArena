package de.simplepvparena.message;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@NullMarked
public interface MessageCatalog {

    Optional<List<String>> find(String path);

    Set<String> paths();
}

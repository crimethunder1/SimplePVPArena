package de.simplepvparena.message;

import java.util.Set;

public enum ArenaSpawnMessage implements MessageKey {

    FIRST_TEAM_NAME("arena.spawn.first-team"),
    SECOND_TEAM_NAME("arena.spawn.second-team"),
    TRIBUNE_NAME("arena.spawn.tribune"),
    FIRST_TEAM_PLACEMENT_HINT("arena.spawn-hint.first-team"),
    SECOND_TEAM_PLACEMENT_HINT("arena.spawn-hint.second-team"),
    TRIBUNE_PLACEMENT_HINT("arena.spawn-hint.tribune");

    private final String path;
    private final Set<String> placeholderNames;

    ArenaSpawnMessage(String path, String... placeholderNames) {
        this.path = path;
        this.placeholderNames = Set.of(placeholderNames);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Set<String> placeholderNames() {
        return placeholderNames;
    }
}

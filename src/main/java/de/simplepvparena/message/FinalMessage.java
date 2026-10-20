package de.simplepvparena.message;

import java.util.Set;

public enum FinalMessage implements MessageKey {

    START_REFERENCE("final.start-reference", "tile"),
    NOT_READY("final.refusal.not-ready"),
    ALREADY_RUNNING("final.refusal.already-running"),
    ALREADY_DECIDED("final.refusal.already-decided"),
    READY("final.ready", "firstTeam", "secondTeam", "action");

    private final String path;
    private final Set<String> placeholderNames;

    FinalMessage(String path, String... placeholderNames) {
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

package de.simplepvparena.border;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public final class OperatorBorderPassLog {

    private final Map<String, OperatorBorderPass> passesByPlayerName = new LinkedHashMap<>();

    public void recordPass(String playerName, long timestampMillis) {
        Objects.requireNonNull(playerName, "playerName");
        OperatorBorderPass previousPass = passesByPlayerName.get(playerName);
        if (previousPass == null) {
            passesByPlayerName.put(playerName,
                    new OperatorBorderPass(playerName, 1, timestampMillis, timestampMillis));
            return;
        }
        passesByPlayerName.put(playerName, new OperatorBorderPass(playerName, previousPass.passCount() + 1,
                previousPass.firstPassMillis(), timestampMillis));
    }

    public boolean isEmpty() {
        return passesByPlayerName.isEmpty();
    }

    public List<OperatorBorderPass> drainPasses() {
        List<OperatorBorderPass> passes = new ArrayList<>(passesByPlayerName.values());
        passesByPlayerName.clear();
        return passes;
    }

    public Optional<String> drainReport() {
        List<OperatorBorderPass> passes = drainPasses();
        if (passes.isEmpty()) {
            return Optional.empty();
        }
        StringJoiner passedPlayers = new StringJoiner(", ");
        for (OperatorBorderPass pass : passes) {
            passedPlayers.add(pass.describe());
        }
        return Optional.of("Operators walked through the spectator border: " + passedPlayers);
    }
}

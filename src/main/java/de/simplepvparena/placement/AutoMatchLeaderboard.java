package de.simplepvparena.placement;

import de.simplepvparena.team.TeamMember;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AutoMatchLeaderboard {

    private final Map<UUID, AutoMatchWins> winsByPlayer = new LinkedHashMap<>();
    private final Runnable changeListener;

    public AutoMatchLeaderboard(Runnable changeListener) {
        this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
    }

    public void load(List<AutoMatchWins> savedWins) {
        winsByPlayer.clear();
        for (AutoMatchWins wins : savedWins) {
            winsByPlayer.put(wins.playerUniqueId(), wins);
        }
    }

    public void recordWin(List<TeamMember> winners) {
        if (winners.isEmpty()) {
            return;
        }
        for (TeamMember winner : winners) {
            AutoMatchWins previous = winsByPlayer.get(winner.uniqueId());
            int wins = previous == null ? 0 : previous.wins();
            winsByPlayer.put(winner.uniqueId(), new AutoMatchWins(winner.uniqueId(), winner.playerName(), wins + 1));
        }
        changeListener.run();
    }

    public void clear() {
        if (winsByPlayer.isEmpty()) {
            return;
        }
        winsByPlayer.clear();
        changeListener.run();
    }

    public List<AutoMatchWins> all() {
        return List.copyOf(winsByPlayer.values());
    }

    public List<AutoMatchWins> top(int limit) {
        List<AutoMatchWins> ranked = new ArrayList<>(winsByPlayer.values());
        ranked.sort(Comparator.comparingInt(AutoMatchWins::wins).reversed()
                .thenComparing(AutoMatchWins::playerName, String.CASE_INSENSITIVE_ORDER));
        return ranked.size() <= limit ? List.copyOf(ranked) : List.copyOf(ranked.subList(0, limit));
    }

    public boolean isEmpty() {
        return winsByPlayer.isEmpty();
    }
}

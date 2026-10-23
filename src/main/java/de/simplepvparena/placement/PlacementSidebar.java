package de.simplepvparena.placement;

import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.arena.EffectAreaPresence;
import de.simplepvparena.config.PluginSettings;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class PlacementSidebar implements EffectAreaPresence {

    public static final String OBJECTIVE_NAME = "placement";
    public static final String LINE_ENTRY_PREFIX = "#";
    public static final long REFRESH_PERIOD_TICKS = 20L;

    private final Plugin plugin;
    private final Supplier<Optional<EffectArea>> effectAreaSource;
    private final Supplier<PlacementStandings> standingsSource;
    private final Map<UUID, ShownSidebar> shownSidebars = new LinkedHashMap<>();

    private boolean enabled = PluginSettings.DEFAULT_PLACEMENT_SIDEBAR_ENABLED;
    private PlacementStandings standings = PlacementStandings.none();
    private BukkitTask refreshTask;

    public PlacementSidebar(Plugin plugin, Supplier<Optional<EffectArea>> effectAreaSource,
                            Supplier<PlacementStandings> standingsSource) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.effectAreaSource = Objects.requireNonNull(effectAreaSource, "effectAreaSource");
        this.standingsSource = Objects.requireNonNull(standingsSource, "standingsSource");
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        refresh();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void start() {
        if (refreshTask != null) {
            return;
        }
        refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refresh, REFRESH_PERIOD_TICKS,
                REFRESH_PERIOD_TICKS);
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public boolean isRunning() {
        return refreshTask != null;
    }

    public void refresh() {
        standings = standingsSource.get();
        Set<UUID> onlinePlayers = new HashSet<>();
        for (Player player : List.copyOf(plugin.getServer().getOnlinePlayers())) {
            onlinePlayers.add(player.getUniqueId());
            reconcile(player, player.getLocation());
        }
        shownSidebars.keySet().retainAll(onlinePlayers);
    }

    @Override
    public void reconcile(Player player, Location location) {
        if (enabled && standings.hasContent() && isInside(location)) {
            show(player);
            return;
        }
        giveBack(player);
    }

    public void giveEveryScoreboardBack() {
        for (Player player : List.copyOf(plugin.getServer().getOnlinePlayers())) {
            giveBack(player);
        }
        shownSidebars.clear();
    }

    public boolean isShownTo(Player player) {
        ShownSidebar shown = shownSidebars.get(player.getUniqueId());
        return shown != null && player.getScoreboard() == shown.scoreboard;
    }

    private boolean isInside(Location location) {
        return location != null && effectAreaSource.get().map(effectArea -> effectArea.contains(location))
                .orElse(false);
    }

    private void show(Player player) {
        ScoreboardManager scoreboardManager = plugin.getServer().getScoreboardManager();
        if (scoreboardManager == null) {
            return;
        }
        ShownSidebar shown = shownSidebars.get(player.getUniqueId());
        if (shown == null || player.getScoreboard() != shown.scoreboard) {
            shown = new ShownSidebar(scoreboardManager.getNewScoreboard(), player.getScoreboard(),
                    PlacementLines.title(standings));
            shownSidebars.put(player.getUniqueId(), shown);
            player.setScoreboard(shown.scoreboard);
        }
        ScoreboardTeamMirror.mirrorTeams(scoreboardManager.getMainScoreboard(), shown.scoreboard);
        shown.update(PlacementLines.title(standings), PlacementLines.sidebarLines(standings));
    }

    private void giveBack(Player player) {
        ShownSidebar shown = shownSidebars.remove(player.getUniqueId());
        if (shown != null && player.isOnline() && player.getScoreboard() == shown.scoreboard) {
            player.setScoreboard(shown.previousScoreboard);
        }
    }

    private static String lineEntry(int lineIndex) {
        return LINE_ENTRY_PREFIX + lineIndex;
    }

    private static final class ShownSidebar {

        private final Scoreboard scoreboard;
        private final Scoreboard previousScoreboard;
        private final Objective objective;

        private Component shownTitle;
        private List<Component> shownLines = List.of();

        private ShownSidebar(Scoreboard scoreboard, Scoreboard previousScoreboard, Component title) {
            this.scoreboard = scoreboard;
            this.previousScoreboard = previousScoreboard;
            this.objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, title);
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            this.objective.numberFormat(NumberFormat.blank());
            this.shownTitle = title;
        }

        private void update(Component title, List<Component> lines) {
            if (!title.equals(shownTitle)) {
                objective.displayName(title);
                shownTitle = title;
            }
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                if (lineIndex < shownLines.size() && lines.get(lineIndex).equals(shownLines.get(lineIndex))) {
                    continue;
                }
                Score score = objective.getScore(lineEntry(lineIndex));
                score.setScore(PlacementLines.SIDEBAR_LINE_LIMIT - lineIndex);
                score.customName(lines.get(lineIndex));
            }
            for (int lineIndex = lines.size(); lineIndex < shownLines.size(); lineIndex++) {
                scoreboard.resetScores(lineEntry(lineIndex));
            }
            shownLines = List.copyOf(lines);
        }
    }
}

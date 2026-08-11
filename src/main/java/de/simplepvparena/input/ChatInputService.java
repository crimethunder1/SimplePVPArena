package de.simplepvparena.input;

import de.simplepvparena.message.TournamentMessages;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class ChatInputService {

    public static final String CANCEL_KEYWORD = "cancel";

    private static final long SWEEP_PERIOD_TICKS = 20L;
    private static final long MILLIS_PER_SECOND = 1000L;

    private final Plugin plugin;
    private final ChatInputRegistry registry;

    private BukkitTask sweepTask;

    public ChatInputService(Plugin plugin) {
        this(plugin, new ChatInputRegistry());
    }

    public ChatInputService(Plugin plugin, ChatInputRegistry registry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void start() {
        if (sweepTask != null) {
            return;
        }
        sweepTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::reportExpiredRequests,
                SWEEP_PERIOD_TICKS, SWEEP_PERIOD_TICKS);
    }

    public void stop() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
        registry.clear();
    }

    public void await(Player player, String description, List<String> promptLines, Consumer<String> lineHandler) {
        registry.await(player.getUniqueId(), description, lineHandler);
        TournamentMessages.sendInfoReport(player, promptLines);
        TournamentMessages.sendDetail(player, "Type " + CANCEL_KEYWORD + " to abort, the request expires in "
                + registry.getTimeoutMillis() / MILLIS_PER_SECOND + " seconds");
    }

    public boolean handleLine(Player player, String line) {
        Optional<PendingChatInput> request = registry.take(player.getUniqueId());
        if (request.isEmpty()) {
            return false;
        }
        String description = request.get().description();
        if (line.trim().equalsIgnoreCase(CANCEL_KEYWORD)) {
            runOnMainThread(() -> TournamentMessages.sendWarning(player, "The input for " + description
                    + " was cancelled, nothing was changed"));
            return true;
        }
        Consumer<String> lineHandler = request.get().lineHandler();
        runOnMainThread(() -> lineHandler.accept(line));
        return true;
    }

    public void forget(Player player) {
        registry.forget(player.getUniqueId());
    }

    public boolean isWaiting(Player player) {
        return registry.isWaiting(player.getUniqueId());
    }

    private void reportExpiredRequests() {
        for (ExpiredChatInput expiredRequest : registry.collectExpired()) {
            Player player = plugin.getServer().getPlayer(expiredRequest.playerUniqueId());
            if (player != null) {
                TournamentMessages.sendWarning(player, "The input for " + expiredRequest.description()
                        + " expired, nothing was changed"
                        + TournamentMessages.LINE_BREAK + "Open /tournament again to start over");
            }
        }
    }

    private void runOnMainThread(Runnable action) {
        plugin.getServer().getScheduler().runTask(plugin, action);
    }
}

package de.simplepvparena.input;

import de.simplepvparena.message.InputMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
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
        TournamentMessages.sendDetail(player, Messages.text(InputMessage.PROMPT_FOOTER,
                Placeholder.of("word", Messages.text(InputMessage.CANCEL_WORD)),
                Placeholder.of("seconds", registry.getTimeoutMillis() / MILLIS_PER_SECOND)));
    }

    public boolean handleLine(Player player, String line) {
        Optional<PendingChatInput> request = registry.take(player.getUniqueId());
        if (request.isEmpty()) {
            return false;
        }
        String description = request.get().description();
        if (isCancelWord(line)) {
            runOnMainThread(() -> TournamentMessages.sendWarning(player, Messages.text(InputMessage.CANCELLED,
                    Placeholder.of("description", description))));
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
                TournamentMessages.sendWarning(player, Messages.text(InputMessage.EXPIRED,
                        Placeholder.of("description", expiredRequest.description())));
            }
        }
    }

    private static boolean isCancelWord(String line) {
        String trimmedLine = line.trim();
        return trimmedLine.equalsIgnoreCase(CANCEL_KEYWORD)
                || trimmedLine.equalsIgnoreCase(Messages.text(InputMessage.CANCEL_WORD));
    }

    private void runOnMainThread(Runnable action) {
        plugin.getServer().getScheduler().runTask(plugin, action);
    }
}

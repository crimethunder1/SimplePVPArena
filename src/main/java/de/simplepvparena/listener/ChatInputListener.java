package de.simplepvparena.listener;

import de.simplepvparena.input.ChatInputService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

public final class ChatInputListener implements Listener {

    private final ChatInputService chatInputService;

    public ChatInputListener(ChatInputService chatInputService) {
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        String line = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (!chatInputService.handleLine(event.getPlayer(), line)) {
            return;
        }
        event.setCancelled(true);
        event.viewers().clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        chatInputService.forget(event.getPlayer());
    }
}

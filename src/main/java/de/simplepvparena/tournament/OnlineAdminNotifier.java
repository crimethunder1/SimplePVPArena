package de.simplepvparena.tournament;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class OnlineAdminNotifier implements AdminNotifier {

    private final Server server;

    public OnlineAdminNotifier(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public void notifyAdmins(String message) {
        Component notification = TournamentMessages.warning(message);
        for (Player player : server.getOnlinePlayers()) {
            if (AdminPermission.isAdmin(player)) {
                player.sendMessage(notification);
            }
        }
        server.getConsoleSender().sendMessage(notification);
    }
}

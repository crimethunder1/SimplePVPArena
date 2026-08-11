package de.simplepvparena.tournament;

import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class OnlineOperatorNotifier implements OperatorNotifier {

    private final Server server;

    public OnlineOperatorNotifier(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public void notifyOperators(String message) {
        Component notification = TournamentMessages.warning(message);
        for (Player player : server.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(notification);
            }
        }
        server.getConsoleSender().sendMessage(notification);
    }
}

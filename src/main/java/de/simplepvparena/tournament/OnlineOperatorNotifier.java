package de.simplepvparena.tournament;

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
        for (Player player : server.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(message);
            }
        }
        server.getConsoleSender().sendMessage(message);
    }
}

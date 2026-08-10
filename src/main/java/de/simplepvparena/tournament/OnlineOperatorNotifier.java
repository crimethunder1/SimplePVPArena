package de.simplepvparena.tournament;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Predicate;

public final class OnlineOperatorNotifier implements OperatorNotifier {

    private final Server server;
    private final Predicate<Player> receiverFilter;

    public OnlineOperatorNotifier(Server server) {
        this(server, player -> player.isOp());
    }

    private OnlineOperatorNotifier(Server server, Predicate<Player> receiverFilter) {
        this.server = Objects.requireNonNull(server, "server");
        this.receiverFilter = Objects.requireNonNull(receiverFilter, "receiverFilter");
    }

    public static OnlineOperatorNotifier administrators(Server server, String adminPermission) {
        Objects.requireNonNull(adminPermission, "adminPermission");
        return new OnlineOperatorNotifier(server,
                player -> player.isOp() || player.hasPermission(adminPermission));
    }

    @Override
    public void notifyOperators(String message) {
        for (Player player : server.getOnlinePlayers()) {
            if (receiverFilter.test(player)) {
                player.sendMessage(message);
            }
        }
        server.getConsoleSender().sendMessage(message);
    }
}

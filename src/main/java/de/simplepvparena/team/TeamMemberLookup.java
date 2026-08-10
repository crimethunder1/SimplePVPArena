package de.simplepvparena.team;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

public final class TeamMemberLookup {

    private final Server server;

    public TeamMemberLookup(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public Optional<TeamMember> find(String playerName) {
        Player onlinePlayer = server.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return Optional.of(new TeamMember(onlinePlayer.getUniqueId(), onlinePlayer.getName()));
        }
        OfflinePlayer cachedPlayer = server.getOfflinePlayerIfCached(playerName);
        if (cachedPlayer == null || cachedPlayer.getName() == null) {
            return Optional.empty();
        }
        return Optional.of(new TeamMember(cachedPlayer.getUniqueId(), cachedPlayer.getName()));
    }
}

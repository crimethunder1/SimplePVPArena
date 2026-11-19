package de.simplepvparena.team;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;

@NullMarked
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
        String cachedName = cachedPlayer == null ? null : cachedPlayer.getName();
        if (cachedPlayer == null || cachedName == null) {
            return Optional.empty();
        }
        return Optional.of(new TeamMember(cachedPlayer.getUniqueId(), cachedName));
    }
}

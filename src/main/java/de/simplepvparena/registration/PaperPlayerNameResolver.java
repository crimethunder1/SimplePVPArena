package de.simplepvparena.registration;

import de.simplepvparena.team.TeamMember;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@NullMarked
public final class PaperPlayerNameResolver implements PlayerNameResolver {

    private final Server server;

    public PaperPlayerNameResolver(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public CompletableFuture<Optional<TeamMember>> resolve(String playerName) {
        Player onlinePlayer = server.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(
                    Optional.of(new TeamMember(onlinePlayer.getUniqueId(), onlinePlayer.getName())));
        }
        OfflinePlayer knownPlayer = server.getOfflinePlayerIfCached(playerName);
        if (knownPlayer != null) {
            String knownName = knownPlayer.getName();
            return CompletableFuture.completedFuture(Optional.of(new TeamMember(knownPlayer.getUniqueId(),
                    knownName == null ? playerName : knownName)));
        }
        return server.createProfile(playerName).update().thenApply(profile -> {
            if (profile.getUniqueId() == null) {
                return Optional.empty();
            }
            String profileName = profile.getName();
            return Optional.of(new TeamMember(profile.getUniqueId(), profileName == null ? playerName : profileName));
        });
    }
}

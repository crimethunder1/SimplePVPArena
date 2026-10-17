package de.simplepvparena.arena;

import de.simplepvparena.team.Team;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class ArenaAudience {

    private final Supplier<Collection<? extends Player>> onlinePlayers;
    private final ArenaRegistry arenaRegistry;

    public ArenaAudience(Server server, ArenaRegistry arenaRegistry) {
        this(Objects.requireNonNull(server, "server")::getOnlinePlayers, arenaRegistry);
    }

    public ArenaAudience(Supplier<Collection<? extends Player>> onlinePlayers, ArenaRegistry arenaRegistry) {
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
    }

    public List<Player> findPlayersInTheArea() {
        Optional<EffectArea> effectArea = arenaRegistry.findEffectArea();
        if (effectArea.isEmpty()) {
            return List.of();
        }
        return onlinePlayers.get().stream()
                .filter(player -> effectArea.get().contains(player.getLocation()))
                .map(Player.class::cast)
                .toList();
    }

    public List<Player> findMatchAudience(Collection<Team> teams) {
        Set<UUID> memberUniqueIds = new HashSet<>();
        teams.forEach(team -> memberUniqueIds.addAll(team.getMemberUniqueIds()));
        Map<UUID, Player> receiversByUniqueId = new LinkedHashMap<>();
        for (Player player : onlinePlayers.get()) {
            if (memberUniqueIds.contains(player.getUniqueId())) {
                receiversByUniqueId.put(player.getUniqueId(), player);
            }
        }
        for (Player player : findPlayersInTheArea()) {
            receiversByUniqueId.putIfAbsent(player.getUniqueId(), player);
        }
        return List.copyOf(receiversByUniqueId.values());
    }
}

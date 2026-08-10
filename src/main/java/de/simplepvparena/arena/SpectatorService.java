package de.simplepvparena.arena;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SpectatorService {

    private static final int SPECTATOR_TOGGLE_ITEM_SLOT = 8;

    private final ArenaRegistry arenaRegistry;
    private final SpectatorItemFactory spectatorItemFactory;
    private final Map<UUID, SpectatorAssignment> assignmentsByPlayer = new LinkedHashMap<>();

    public SpectatorService(ArenaRegistry arenaRegistry, SpectatorItemFactory spectatorItemFactory) {
        this.arenaRegistry = arenaRegistry;
        this.spectatorItemFactory = spectatorItemFactory;
    }

    public boolean sendToTribune(Player player, Arena arena) {
        Optional<Location> tribuneLocation = arena.getTribuneSpawn().flatMap(PositionSnapshot::toLocation);
        if (tribuneLocation.isEmpty()) {
            return false;
        }
        player.teleport(tribuneLocation.get());
        player.setGameMode(GameMode.ADVENTURE);
        assignmentsByPlayer.put(player.getUniqueId(), new SpectatorAssignment(arena.getArenaName(), SpectatorMode.TRIBUNE));
        giveSpectatorToggleItem(player);
        return true;
    }

    public boolean enterSpectatorBox(Player player) {
        Optional<Arena> arena = findAssignedArena(player.getUniqueId());
        if (arena.isEmpty()) {
            return false;
        }
        Optional<Location> spectatorBoxLocation = arena.get().getSpectatorBoxSpawn().flatMap(PositionSnapshot::toLocation);
        if (spectatorBoxLocation.isEmpty()) {
            return false;
        }
        player.teleport(spectatorBoxLocation.get());
        player.setGameMode(GameMode.SPECTATOR);
        assignmentsByPlayer.put(player.getUniqueId(),
                new SpectatorAssignment(arena.get().getArenaName(), SpectatorMode.SPECTATOR_BOX));
        return true;
    }

    public boolean returnToTribune(Player player) {
        return findAssignedArena(player.getUniqueId())
                .map(arena -> sendToTribune(player, arena))
                .orElse(false);
    }

    public boolean toggleSpectatorMode(Player player) {
        Optional<SpectatorAssignment> assignment = findAssignment(player.getUniqueId());
        if (assignment.isEmpty()) {
            return false;
        }
        if (assignment.get().spectatorMode() == SpectatorMode.TRIBUNE) {
            return enterSpectatorBox(player);
        }
        return returnToTribune(player);
    }

    public void giveSpectatorToggleItem(Player player) {
        ItemStack toggleItem = spectatorItemFactory.createSpectatorToggleItem();
        player.getInventory().setItem(SPECTATOR_TOGGLE_ITEM_SLOT, toggleItem);
    }

    public boolean isSpectatorToggleItem(ItemStack itemStack) {
        return spectatorItemFactory.isSpectatorToggleItem(itemStack);
    }

    public Optional<SpectatorAssignment> findAssignment(UUID playerUniqueId) {
        return Optional.ofNullable(assignmentsByPlayer.get(playerUniqueId));
    }

    public Optional<Arena> findAssignedArena(UUID playerUniqueId) {
        return findAssignment(playerUniqueId).flatMap(assignment -> arenaRegistry.findArena(assignment.arenaName()));
    }

    public Set<UUID> getSpectatorsOfArena(String arenaName) {
        Set<UUID> spectators = new LinkedHashSet<>();
        assignmentsByPlayer.forEach((playerUniqueId, assignment) -> {
            if (assignment.arenaName().equalsIgnoreCase(arenaName)) {
                spectators.add(playerUniqueId);
            }
        });
        return spectators;
    }

    public void clearAssignment(UUID playerUniqueId) {
        assignmentsByPlayer.remove(playerUniqueId);
    }

    public void clearAllAssignments() {
        assignmentsByPlayer.clear();
    }
}

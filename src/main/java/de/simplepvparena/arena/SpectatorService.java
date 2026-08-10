package de.simplepvparena.arena;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SpectatorService {

    private static final int SPECTATOR_TOGGLE_ITEM_SLOT = 8;

    private final ArenaRegistry arenaRegistry;
    private final SpectatorItemFactory spectatorItemFactory;
    private final Map<UUID, SpectatorMode> spectatorModesByPlayer = new LinkedHashMap<>();

    public SpectatorService(ArenaRegistry arenaRegistry, SpectatorItemFactory spectatorItemFactory) {
        this.arenaRegistry = arenaRegistry;
        this.spectatorItemFactory = spectatorItemFactory;
    }

    public boolean sendToTribune(Player player) {
        Optional<Location> tribuneLocation = arenaRegistry.getArena().getTribuneSpawn()
                .flatMap(PositionSnapshot::toLocation);
        if (tribuneLocation.isEmpty()) {
            return false;
        }
        player.teleport(tribuneLocation.get());
        player.setGameMode(GameMode.ADVENTURE);
        spectatorModesByPlayer.put(player.getUniqueId(), SpectatorMode.TRIBUNE);
        giveSpectatorToggleItem(player);
        return true;
    }

    public boolean enterSpectatorBox(Player player) {
        Optional<Location> spectatorBoxLocation = arenaRegistry.getArena().getSpectatorBoxSpawn()
                .flatMap(PositionSnapshot::toLocation);
        if (spectatorBoxLocation.isEmpty()) {
            return false;
        }
        player.teleport(spectatorBoxLocation.get());
        player.setGameMode(GameMode.SPECTATOR);
        spectatorModesByPlayer.put(player.getUniqueId(), SpectatorMode.SPECTATOR_BOX);
        return true;
    }

    public boolean toggleSpectatorMode(Player player) {
        Optional<SpectatorMode> spectatorMode = findSpectatorMode(player.getUniqueId());
        if (spectatorMode.isEmpty()) {
            return false;
        }
        if (spectatorMode.get() == SpectatorMode.TRIBUNE) {
            return enterSpectatorBox(player);
        }
        return sendToTribune(player);
    }

    public void giveSpectatorToggleItem(Player player) {
        ItemStack toggleItem = spectatorItemFactory.createSpectatorToggleItem();
        player.getInventory().setItem(SPECTATOR_TOGGLE_ITEM_SLOT, toggleItem);
    }

    public boolean isSpectatorToggleItem(ItemStack itemStack) {
        return spectatorItemFactory.isSpectatorToggleItem(itemStack);
    }

    public Optional<SpectatorMode> findSpectatorMode(UUID playerUniqueId) {
        return Optional.ofNullable(spectatorModesByPlayer.get(playerUniqueId));
    }

    public Set<UUID> getSpectators() {
        return Set.copyOf(spectatorModesByPlayer.keySet());
    }

    public void clearAssignment(UUID playerUniqueId) {
        spectatorModesByPlayer.remove(playerUniqueId);
    }

    public void clearAllAssignments() {
        spectatorModesByPlayer.clear();
    }
}

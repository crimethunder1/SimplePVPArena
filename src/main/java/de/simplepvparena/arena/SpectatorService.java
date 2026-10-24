package de.simplepvparena.arena;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SpectatorService implements SpectatorToggleItemGiver {

    private static final int SPECTATOR_TOGGLE_ITEM_SLOT = 8;

    private final ArenaRegistry arenaRegistry;
    private final SpectatorItems spectatorItems;
    private final Map<UUID, SpectatorMode> spectatorModesByPlayer = new LinkedHashMap<>();

    public SpectatorService(ArenaRegistry arenaRegistry, SpectatorItems spectatorItems) {
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.spectatorItems = Objects.requireNonNull(spectatorItems, "spectatorItems");
    }

    public boolean sendToTribune(Player player) {
        Optional<Location> tribuneLocation = arenaRegistry.getArena().getTribuneSpawn()
                .flatMap(PositionSnapshot::toLocation);
        if (tribuneLocation.isEmpty()) {
            return false;
        }
        player.teleport(tribuneLocation.get());
        player.setGameMode(GameMode.ADVENTURE);
        giveSpectatorToggleItemIfMissing(player);
        spectatorModesByPlayer.put(player.getUniqueId(), SpectatorMode.TRIBUNE);
        return true;
    }

    public boolean enterArenaFlight(Player player) {
        Optional<Location> arenaLocation = arenaRegistry.getArena().getSpectatorFlightSpawn()
                .flatMap(PositionSnapshot::toLocation);
        if (arenaLocation.isEmpty()) {
            return false;
        }
        player.teleport(arenaLocation.get());
        player.setGameMode(GameMode.SPECTATOR);
        spectatorModesByPlayer.put(player.getUniqueId(), SpectatorMode.ARENA_FLIGHT);
        return true;
    }

    public boolean toggleSpectatorMode(Player player) {
        Optional<SpectatorMode> spectatorMode = findSpectatorMode(player.getUniqueId());
        if (spectatorMode.isEmpty()) {
            return false;
        }
        if (spectatorMode.get() == SpectatorMode.TRIBUNE) {
            return enterArenaFlight(player);
        }
        return sendToTribune(player);
    }

    public boolean isInsideEffectArea(Player player) {
        Location location = player.getLocation();
        return arenaRegistry.findEffectArea().map(effectArea -> effectArea.contains(location)).orElse(false);
    }

    public boolean hasTribuneSpawn() {
        return arenaRegistry.getArena().getTribuneSpawn().flatMap(PositionSnapshot::toLocation).isPresent();
    }

    @Override
    public void giveSpectatorToggleItemIfMissing(Player player) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (spectatorItems.isSpectatorToggleItem(itemStack)) {
                return;
            }
        }
        giveSpectatorToggleItem(player);
    }

    private void giveSpectatorToggleItem(Player player) {
        ItemStack toggleItem = spectatorItems.createSpectatorToggleItem();
        player.getInventory().setItem(SPECTATOR_TOGGLE_ITEM_SLOT, toggleItem);
    }

    public boolean isSpectatorToggleItem(ItemStack itemStack) {
        return spectatorItems.isSpectatorToggleItem(itemStack);
    }

    public Optional<SpectatorMode> findSpectatorMode(UUID playerUniqueId) {
        return Optional.ofNullable(spectatorModesByPlayer.get(playerUniqueId));
    }

    public boolean isFlyingSpectator(UUID playerUniqueId) {
        return findSpectatorMode(playerUniqueId)
                .filter(spectatorMode -> spectatorMode == SpectatorMode.ARENA_FLIGHT)
                .isPresent();
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

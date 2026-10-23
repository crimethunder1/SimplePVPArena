package de.simplepvparena.support;

import de.simplepvparena.arena.PlayerPotionEffects;
import de.simplepvparena.arena.SavedPotionEffect;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FakePlayerPotionEffects implements PlayerPotionEffects {

    private final Map<UUID, List<SavedPotionEffect>> potionEffectsByPlayer = new HashMap<>();

    @Override
    public List<SavedPotionEffect> read(Player player) {
        return effectsOf(player.getUniqueId());
    }

    @Override
    public void removeAll(Player player) {
        potionEffectsByPlayer.remove(player.getUniqueId());
    }

    @Override
    public void apply(Player player, List<SavedPotionEffect> potionEffects) {
        potionEffectsByPlayer.computeIfAbsent(player.getUniqueId(), playerUniqueId -> new ArrayList<>())
                .addAll(potionEffects);
    }

    public void give(UUID playerUniqueId, SavedPotionEffect... potionEffects) {
        potionEffectsByPlayer.computeIfAbsent(playerUniqueId, uniqueId -> new ArrayList<>())
                .addAll(List.of(potionEffects));
    }

    public List<SavedPotionEffect> effectsOf(UUID playerUniqueId) {
        return List.copyOf(potionEffectsByPlayer.getOrDefault(playerUniqueId, List.of()));
    }
}

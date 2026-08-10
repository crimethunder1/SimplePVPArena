package de.simplepvparena.kit;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KitService {

    private static final double FALLBACK_MAXIMUM_HEALTH = 20.0;
    private static final int FULL_FOOD_LEVEL = 20;
    private static final float FULL_SATURATION = 20.0f;

    private final ConcurrentHashMap.KeySetView<UUID, Boolean> playersWithAppliedKit = ConcurrentHashMap.newKeySet();

    private KitDefinition kitDefinition = KitDefinition.empty();

    public void setKitDefinition(KitDefinition kitDefinition) {
        this.kitDefinition = kitDefinition;
    }

    public KitDefinition getKitDefinition() {
        return kitDefinition;
    }

    public void applyKit(Player player) {
        resetPlayerState(player);
        PlayerInventory inventory = player.getInventory();
        kitDefinition.armorItems().forEach((equipmentSlot, kitItem) -> inventory.setItem(equipmentSlot, kitItem.toItemStack()));
        List<KitItem> inventoryItems = kitDefinition.inventoryItems();
        for (int slotIndex = 0; slotIndex < inventoryItems.size(); slotIndex++) {
            inventory.setItem(slotIndex, inventoryItems.get(slotIndex).toItemStack());
        }
        playersWithAppliedKit.add(player.getUniqueId());
    }

    public void clearKit(Player player) {
        player.getInventory().clear();
        playersWithAppliedKit.remove(player.getUniqueId());
    }

    public boolean hasAppliedKit(Player player) {
        return playersWithAppliedKit.contains(player.getUniqueId());
    }

    private void resetPlayerState(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setFireTicks(0);
        player.setFoodLevel(FULL_FOOD_LEVEL);
        player.setSaturation(FULL_SATURATION);
        player.setExp(0.0f);
        player.setLevel(0);
        player.setHealth(resolveMaximumHealth(player));
        for (PotionEffect activeEffect : List.copyOf(player.getActivePotionEffects())) {
            PotionEffectType effectType = activeEffect.getType();
            player.removePotionEffect(effectType);
        }
    }

    private double resolveMaximumHealth(Player player) {
        AttributeInstance maximumHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        return maximumHealthAttribute == null ? FALLBACK_MAXIMUM_HEALTH : maximumHealthAttribute.getValue();
    }
}

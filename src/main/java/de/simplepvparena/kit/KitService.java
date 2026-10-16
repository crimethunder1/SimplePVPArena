package de.simplepvparena.kit;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;

public final class KitService {

    public static final String KIT_APPLIED_KEY_NAME = "kit_applied";

    private static final byte KIT_APPLIED_VALUE = 1;
    private static final double FALLBACK_MAXIMUM_HEALTH = 20.0;
    private static final int FULL_FOOD_LEVEL = 20;
    private static final float FULL_SATURATION = 20.0f;

    private final NamespacedKey kitAppliedKey;

    private KitDefinition kitDefinition = KitDefinition.empty();

    public KitService() {
        this.kitAppliedKey = null;
    }

    public KitService(Plugin plugin) {
        this.kitAppliedKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), KIT_APPLIED_KEY_NAME);
    }

    public void setKitDefinition(KitDefinition kitDefinition) {
        this.kitDefinition = kitDefinition;
    }

    public KitDefinition getKitDefinition() {
        return kitDefinition;
    }

    public void applyKit(Player player) {
        resetPlayerState(player);
        PlayerInventory inventory = player.getInventory();
        kitDefinition.equipmentItems().forEach((equipmentSlot, itemStack) ->
                inventory.setItem(equipmentSlot.getEquipmentSlot(), itemStack.clone()));
        kitDefinition.inventoryItems().forEach((slotIndex, itemStack) ->
                inventory.setItem(slotIndex, itemStack.clone()));
        if (kitAppliedKey != null) {
            player.getPersistentDataContainer().set(kitAppliedKey, PersistentDataType.BYTE, KIT_APPLIED_VALUE);
        }
    }

    public void clearKit(Player player) {
        resetPlayerState(player);
        if (kitAppliedKey != null) {
            player.getPersistentDataContainer().remove(kitAppliedKey);
        }
    }

    public boolean hasAppliedKit(Player player) {
        return kitAppliedKey != null
                && player.getPersistentDataContainer().has(kitAppliedKey, PersistentDataType.BYTE);
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
        player.setAbsorptionAmount(0.0);
        player.setArrowsInBody(0, false);
        player.setFreezeTicks(0);
        player.setRemainingAir(player.getMaximumAir());
        player.setFallDistance(0.0f);
        player.setVelocity(new Vector());
        resetItemCooldowns(player);
    }

    private static void resetItemCooldowns(Player player) {
        for (Material material : Material.values()) {
            if (!material.isLegacy() && material.isItem() && player.getCooldown(material) > 0) {
                player.setCooldown(material, 0);
            }
        }
    }

    private double resolveMaximumHealth(Player player) {
        AttributeInstance maximumHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        return maximumHealthAttribute == null ? FALLBACK_MAXIMUM_HEALTH : maximumHealthAttribute.getValue();
    }
}

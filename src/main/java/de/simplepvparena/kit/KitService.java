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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class KitService {

    public static final String KIT_APPLIED_KEY_NAME = "kit_applied";

    private static final byte KIT_APPLIED_VALUE = 1;
    private static final double FALLBACK_MAXIMUM_HEALTH = 20.0;
    private static final double MINIMUM_RESTORED_HEALTH = 1.0;
    private static final int FULL_FOOD_LEVEL = 20;
    private static final float FULL_SATURATION = 20.0f;

    private final NamespacedKey kitAppliedKey;
    private final PlayerStateSnapshotStorage snapshotStorage;
    private final Consumer<String> warningReporter;
    private final Map<UUID, PlayerStateSnapshot> snapshotsInMemory = new HashMap<>();

    private KitDefinition kitDefinition = KitDefinition.empty();

    public KitService() {
        this.kitAppliedKey = null;
        this.snapshotStorage = null;
        this.warningReporter = warningMessage -> { };
    }

    public KitService(Plugin plugin) {
        this.kitAppliedKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), KIT_APPLIED_KEY_NAME);
        this.snapshotStorage = null;
        this.warningReporter = warningMessage -> { };
    }

    public KitService(Plugin plugin, PlayerStateSnapshotStorage snapshotStorage, Consumer<String> warningReporter) {
        this.kitAppliedKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), KIT_APPLIED_KEY_NAME);
        this.snapshotStorage = Objects.requireNonNull(snapshotStorage, "snapshotStorage");
        this.warningReporter = Objects.requireNonNull(warningReporter, "warningReporter");
    }

    public void setKitDefinition(KitDefinition kitDefinition) {
        this.kitDefinition = kitDefinition;
    }

    public KitDefinition getKitDefinition() {
        return kitDefinition;
    }

    public void applyKit(Player player) {
        if (snapshotStorage != null && !hasAppliedKit(player)) {
            saveSnapshot(player);
        }
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
        PlayerStateSnapshot snapshot = snapshotStorage == null ? null : findSnapshot(player);
        resetPlayerState(player);
        if (snapshot != null) {
            restoreSnapshot(player, snapshot);
        }
        removeMarker(player);
        if (snapshot != null) {
            forgetSnapshot(player);
        }
    }

    public boolean hasAppliedKit(Player player) {
        return kitAppliedKey != null
                && player.getPersistentDataContainer().has(kitAppliedKey, PersistentDataType.BYTE);
    }

    private void saveSnapshot(Player player) {
        PlayerStateSnapshot snapshot = new PlayerStateSnapshot(player.getInventory().getContents(),
                player.getLevel(), player.getExp(), player.getFoodLevel(), player.getSaturation(),
                player.getHealth());
        snapshotsInMemory.put(player.getUniqueId(), snapshot);
        try {
            snapshotStorage.save(player.getUniqueId(), snapshot);
        } catch (IOException failure) {
            warningReporter.accept("The inventory and state of " + player.getName()
                    + " could not be written to disk (" + failure.getMessage()
                    + "), they are kept in memory only and are lost if the server stops unexpectedly");
        }
    }

    private PlayerStateSnapshot findSnapshot(Player player) {
        PlayerStateSnapshot snapshotInMemory = snapshotsInMemory.get(player.getUniqueId());
        return snapshotInMemory != null ? snapshotInMemory : findStoredSnapshot(player);
    }

    private PlayerStateSnapshot findStoredSnapshot(Player player) {
        PlayerStateSnapshotLookup lookup = snapshotStorage.find(player.getUniqueId());
        switch (lookup.outcome()) {
            case FOUND -> {
                return lookup.snapshot().orElseThrow();
            }
            case MISSING -> warningReporter.accept("No saved inventory and state of " + player.getName()
                    + " was found, the kit was cleared without restoring anything");
            case BROKEN -> warningReporter.accept("The saved inventory and state of " + player.getName()
                    + " could not be read, the kit was cleared without restoring anything, the file is kept as "
                    + lookup.fileName());
        }
        return null;
    }

    private void restoreSnapshot(Player player, PlayerStateSnapshot snapshot) {
        player.getInventory().setContents(snapshot.getContents());
        player.setLevel(snapshot.getLevel());
        player.setExp(snapshot.getExp());
        player.setFoodLevel(snapshot.getFoodLevel());
        player.setSaturation(snapshot.getSaturation());
        player.setHealth(Math.max(MINIMUM_RESTORED_HEALTH,
                Math.min(snapshot.getHealth(), resolveMaximumHealth(player))));
    }

    private void forgetSnapshot(Player player) {
        try {
            snapshotStorage.delete(player.getUniqueId());
        } catch (IOException failure) {
            warningReporter.accept("The saved inventory and state of " + player.getName()
                    + " were restored, but the file could not be deleted (" + failure.getMessage() + ")");
        }
        snapshotsInMemory.remove(player.getUniqueId());
    }

    private void removeMarker(Player player) {
        if (kitAppliedKey != null) {
            player.getPersistentDataContainer().remove(kitAppliedKey);
        }
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

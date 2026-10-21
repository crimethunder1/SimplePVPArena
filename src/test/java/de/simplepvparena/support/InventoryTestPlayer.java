package de.simplepvparena.support;

import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class InventoryTestPlayer {

    public static final int SLOT_COUNT = 41;
    public static final int STORAGE_SLOT_COUNT = 36;
    public static final int OFF_HAND_SLOT = 40;
    public static final int MAIN_HAND_SLOT = 0;

    private final UUID uniqueId = UUID.randomUUID();
    private final String name;
    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];
    private final List<String> playedEffects = new ArrayList<>();
    private final List<ItemStack> droppedItems = new ArrayList<>();
    private final Location location;
    private final PlayerInventory inventory;
    private final Player player;

    private boolean online = true;
    private RuntimeException offHandFailure;

    public InventoryTestPlayer(String name) {
        this.name = name;
        World world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "dropItemNaturally" -> {
                droppedItems.add((ItemStack) arguments[1]);
                yield null;
            }
            case "getName" -> "event";
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
        this.location = new Location(world, 0.5D, 64.0D, 0.5D);
        this.inventory = SharedBukkitServer.proxy(PlayerInventory.class, (method, arguments) ->
                switch (method.getName()) {
                    case "getItemInOffHand" -> slots[OFF_HAND_SLOT];
                    case "setItemInOffHand" -> {
                        if (offHandFailure != null) {
                            throw offHandFailure;
                        }
                        slots[OFF_HAND_SLOT] = (ItemStack) arguments[0];
                        yield null;
                    }
                    case "getContents" -> slots.clone();
                    case "setItem" -> {
                        if (arguments[0] instanceof Integer slot) {
                            slots[slot] = (ItemStack) arguments[1];
                            yield null;
                        }
                        throw new UnsupportedOperationException("PlayerInventory.setItem(EquipmentSlot)");
                    }
                    case "getItem" -> arguments[0] instanceof EquipmentSlot equipmentSlot
                            ? slots[equipmentSlot == EquipmentSlot.OFF_HAND ? OFF_HAND_SLOT : MAIN_HAND_SLOT]
                            : slots[(Integer) arguments[0]];
                    case "addItem" -> addItems((ItemStack[]) arguments[0]);
                    default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
                });
        this.player = SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> uniqueId;
            case "getName" -> name;
            case "isOnline" -> online;
            case "getInventory" -> inventory;
            case "getWorld" -> world;
            case "getLocation" -> location.clone();
            case "playEffect" -> {
                playedEffects.add(arguments[0] + " with " + slots[OFF_HAND_SLOT] + " in the off hand");
                yield null;
            }
            case "addPotionEffect" -> true;
            case "removePotionEffect" -> null;
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }

    public Player player() {
        return player;
    }

    public String name() {
        return name;
    }

    public ItemStack item(int slot) {
        return slots[slot];
    }

    public ItemStack offHandItem() {
        return slots[OFF_HAND_SLOT];
    }

    public void putItem(int slot, ItemStack itemStack) {
        slots[slot] = itemStack;
    }

    public List<ItemStack> nonEmptyItems() {
        return Arrays.stream(slots).filter(itemStack -> itemStack != null).toList();
    }

    public List<String> playedEffects() {
        return playedEffects;
    }

    public List<ItemStack> droppedItems() {
        return droppedItems;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void failOffHandChangeWith(RuntimeException failure) {
        this.offHandFailure = failure;
    }

    public Item droppedEntityOf(ItemStack itemStack) {
        return SharedBukkitServer.proxy(Item.class, (method, arguments) -> switch (method.getName()) {
            case "getItemStack" -> itemStack;
            default -> throw new UnsupportedOperationException("Item." + method.getName());
        });
    }

    public static String totemEffect(String offHandLabel) {
        return EntityEffect.PROTECTED_FROM_DEATH + " with " + offHandLabel + " in the off hand";
    }

    private HashMap<Integer, ItemStack> addItems(ItemStack[] itemStacks) {
        HashMap<Integer, ItemStack> itemsThatDidNotFit = new HashMap<>();
        for (int index = 0; index < itemStacks.length; index++) {
            int freeSlot = findFreeStorageSlot();
            if (freeSlot < 0) {
                itemsThatDidNotFit.put(index, itemStacks[index]);
            } else {
                slots[freeSlot] = itemStacks[index];
            }
        }
        return itemsThatDidNotFit;
    }

    private int findFreeStorageSlot() {
        for (int slot = 0; slot < STORAGE_SLOT_COUNT; slot++) {
            if (slots[slot] == null) {
                return slot;
            }
        }
        return -1;
    }
}

package de.simplepvparena.support;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RecordingItemStack extends ItemStack {

    private final Material type;
    private final int amount;
    private ItemMeta itemMeta;

    public RecordingItemStack(Material type, int amount) {
        this(type, amount, recordingMeta(null, List.of(), new LinkedHashMap<>()));
    }

    private RecordingItemStack(Material type, int amount, ItemMeta itemMeta) {
        this.type = Objects.requireNonNull(type, "type");
        this.amount = amount;
        this.itemMeta = itemMeta;
    }

    public static RecordingItemStack named(Material type, String name) {
        RecordingItemStack itemStack = new RecordingItemStack(type, 1);
        itemStack.getItemMeta().displayName(Component.text(name));
        return itemStack;
    }

    @Override
    public Material getType() {
        return type;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public ItemMeta getItemMeta() {
        return itemMeta;
    }

    @Override
    public boolean hasItemMeta() {
        return itemMeta != null;
    }

    @Override
    public boolean setItemMeta(ItemMeta itemMeta) {
        this.itemMeta = itemMeta;
        return true;
    }

    @Override
    public ItemStack clone() {
        return new RecordingItemStack(type, amount, (ItemMeta) itemMeta.clone());
    }

    public String getName() {
        Component name = itemMeta.displayName();
        return name == null ? null : PlainTextComponentSerializer.plainText().serialize(name);
    }

    public List<String> getLore() {
        List<Component> lore = itemMeta.lore();
        return lore == null ? List.of() : lore.stream()
                .map(line -> PlainTextComponentSerializer.plainText().serialize(line))
                .toList();
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "RecordingItemStack{" + type + ", " + getName() + "}";
    }

    @SuppressWarnings("unchecked")
    private static ItemMeta recordingMeta(Component initialName, List<Component> initialLore,
                                          Map<Object, Object> persistentData) {
        Object[] state = {initialName, initialLore};
        return SharedBukkitServer.proxy(CompassMeta.class, (method, arguments) -> switch (method.getName()) {
            case "displayName" -> {
                if (arguments == null || arguments.length == 0) {
                    yield state[0];
                }
                state[0] = arguments[0];
                yield null;
            }
            case "hasDisplayName" -> state[0] != null;
            case "lore" -> {
                if (arguments == null || arguments.length == 0) {
                    yield state[1];
                }
                state[1] = arguments[0] == null ? List.of() : List.copyOf((List<Component>) arguments[0]);
                yield null;
            }
            case "getPersistentDataContainer" -> SharedBukkitServer.createPersistentDataContainer(persistentData);
            case "setLodestone", "setLodestoneTracked" -> null;
            case "clone" -> recordingMeta((Component) state[0], (List<Component>) state[1],
                    new LinkedHashMap<>(persistentData));
            default -> throw new UnsupportedOperationException("ItemMeta." + method.getName());
        });
    }
}

package de.simplepvparena.gate;

import de.simplepvparena.input.BlockItemRequest;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.StartGateMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;

public final class StartGateItemService {

    public static final String GATE_KEY_NAME = "start_gate";

    private static final String ITEM_MARKER_VALUE = "start-gate";

    private final NamespacedKey gateKey;
    private final StartGateMarks gateMarks;
    private final StartGateDuration duration;
    private final Runnable gateMarkChangeListener;

    public StartGateItemService(Plugin plugin, StartGateMarks gateMarks, StartGateDuration duration,
                                Runnable gateMarkChangeListener) {
        this.gateKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), GATE_KEY_NAME);
        this.gateMarks = Objects.requireNonNull(gateMarks, "gateMarks");
        this.duration = Objects.requireNonNull(duration, "duration");
        this.gateMarkChangeListener = Objects.requireNonNull(gateMarkChangeListener, "gateMarkChangeListener");
    }

    public NamespacedKey getGateKey() {
        return gateKey;
    }

    public ItemStack createGateItem(BlockItemRequest request) {
        return labelled(new ItemStack(request.material(), request.amount()));
    }

    ItemStack labelled(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(Messages.text(StartGateMessage.ITEM_NAME))
                .decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(Messages.lines(StartGateMessage.ITEM_LORE, Placeholder.of("duration", duration.describe()))
                .stream()
                .map(StartGateItemService::lore)
                .toList());
        itemMeta.getPersistentDataContainer().set(gateKey, PersistentDataType.STRING, ITEM_MARKER_VALUE);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isGateItem(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        return ITEM_MARKER_VALUE.equals(itemStack.getItemMeta().getPersistentDataContainer()
                .get(gateKey, PersistentDataType.STRING));
    }

    public boolean markPlacedBlock(Block block) {
        Material placedMaterial = block.getType();
        if (isAir(placedMaterial)) {
            return false;
        }
        if (!gateMarks.mark(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                placedMaterial)) {
            return false;
        }
        gateMarkChangeListener.run();
        return true;
    }

    public boolean clearBlockMark(Block block) {
        if (!gateMarks.clearMark(block.getWorld().getName(), block.getX(), block.getY(), block.getZ())) {
            return false;
        }
        gateMarkChangeListener.run();
        return true;
    }

    public boolean isGateBlock(Block block) {
        return gateMarks.isMarked(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public int getMarkedBlockCount() {
        return gateMarks.getMarkedBlockCount();
    }

    public List<String> describeGate() {
        return List.of(describeBlockCount(),
                Messages.text(StartGateMessage.TILE_COMES_DOWN_OVER, Placeholder.of("duration", duration.describe())));
    }

    private String describeBlockCount() {
        int markedBlockCount = getMarkedBlockCount();
        if (markedBlockCount == 0) {
            return Messages.text(StartGateMessage.TILE_NO_BLOCK);
        }
        return Messages.plural(StartGateMessage.TILE_BLOCKS, markedBlockCount);
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private static Component lore(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }
}

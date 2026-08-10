package de.simplepvparena.border;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.BorderColumnReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;

public final class SpectatorBorderService {

    public static final String BORDER_KEY_NAME = "spectator_border";

    private static final String ITEM_MARKER_VALUE = "spectator-border";

    private final NamespacedKey borderKey;
    private final BorderMarkIndex borderMarks;
    private final Runnable borderMarkChangeListener;

    public SpectatorBorderService(Plugin plugin, BorderMarkIndex borderMarks, Runnable borderMarkChangeListener) {
        this.borderKey = new NamespacedKey(plugin, BORDER_KEY_NAME);
        this.borderMarks = Objects.requireNonNull(borderMarks, "borderMarks");
        this.borderMarkChangeListener = Objects.requireNonNull(borderMarkChangeListener, "borderMarkChangeListener");
    }

    public NamespacedKey getBorderKey() {
        return borderKey;
    }

    public ItemStack createBorderItem(BorderItemRequest request) {
        ItemStack itemStack = new ItemStack(request.material(), request.amount());
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Spectator border").decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(List.of(Component.text("Blocks everybody who is not fighting")
                .decoration(TextDecoration.ITALIC, false)));
        itemMeta.getPersistentDataContainer().set(borderKey, PersistentDataType.STRING, ITEM_MARKER_VALUE);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isBorderItem(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        return ITEM_MARKER_VALUE.equals(itemStack.getItemMeta().getPersistentDataContainer()
                .get(borderKey, PersistentDataType.STRING));
    }

    public void markBlock(Block block) {
        if (borderMarks.mark(block.getWorld().getName(), block.getX(), block.getY(), block.getZ())) {
            borderMarkChangeListener.run();
        }
    }

    public void clearBlockMark(Block block) {
        if (borderMarks.clearMark(block.getWorld().getName(), block.getX(), block.getY(), block.getZ())) {
            borderMarkChangeListener.run();
        }
    }

    public boolean isBorderBlock(Block block) {
        return borderMarks.isMarkedBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public boolean isMovementAllowed(boolean activeFighter, boolean operator,
                                     Location origin, Location destination) {
        if (destination == null || destination.getWorld() == null) {
            return true;
        }
        String worldName = destination.getWorld().getName();
        int originBlockX = destination.getBlockX();
        int originBlockZ = destination.getBlockZ();
        if (origin != null && origin.getWorld() != null && origin.getWorld().getName().equals(worldName)) {
            originBlockX = origin.getBlockX();
            originBlockZ = origin.getBlockZ();
        }
        return SpectatorBorderMovementRule.isMovementAllowed(activeFighter, operator,
                borderMarks.lookupIn(worldName), originBlockX, originBlockZ,
                destination.getBlockX(), destination.getBlockZ());
    }

    public int clearMarksInShape(World world, ArenaShape shape) {
        if (!world.getName().equals(shape.getWorldName())) {
            return 0;
        }
        int clearedMarkCount = borderMarks.removeInsideShape(shape);
        if (clearedMarkCount > 0) {
            borderMarkChangeListener.run();
        }
        return clearedMarkCount;
    }

    public BorderColumnReader createColumnReader(World world) {
        return borderMarks.createColumnReader(world.getName());
    }
}

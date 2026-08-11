package de.simplepvparena.arena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.List;
import java.util.Map;

public final class ArenaGapCompass {

    public static final Material MATERIAL = Material.COMPASS;

    private static final double BLOCK_CENTER_OFFSET = 0.5D;

    private ArenaGapCompass() {
    }

    public static ItemStack create(World world, ArenaGapTarget target) {
        ItemStack compass = new ItemStack(MATERIAL);
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        compassMeta.displayName(Component.text("Border gap compass", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        compassMeta.lore(List.of(
                lore("Points to a possible gap in your border wall"),
                lore("Target " + target.describe()),
                lore(target.reason().getLoreLine()),
                lore("Close the wall there and calculate the fight area again")));
        compassMeta.setLodestone(new Location(world, target.blockX() + BLOCK_CENTER_OFFSET, target.blockY(),
                target.blockZ() + BLOCK_CENTER_OFFSET));
        compassMeta.setLodestoneTracked(false);
        compass.setItemMeta(compassMeta);
        return compass;
    }

    public static boolean giveTo(Player admin, ItemStack compass) {
        if (admin.getInventory().getItemInMainHand().getType().isAir()) {
            admin.getInventory().setItemInMainHand(compass);
            return true;
        }
        Map<Integer, ItemStack> itemsThatDidNotFit = admin.getInventory().addItem(compass);
        return itemsThatDidNotFit.isEmpty();
    }

    private static Component lore(String line) {
        return Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }
}

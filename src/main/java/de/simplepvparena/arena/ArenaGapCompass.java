package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaGapMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.Map;

public final class ArenaGapCompass {

    public static final Material MATERIAL = Material.COMPASS;

    private static final double BLOCK_CENTER_OFFSET = 0.5D;

    private ArenaGapCompass() {
    }

    public static ItemStack create(World world, ArenaGapTarget target) {
        return labelled(new ItemStack(MATERIAL), world, target);
    }

    static ItemStack labelled(ItemStack compass, World world, ArenaGapTarget target) {
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        compassMeta.displayName(Component.text(Messages.text(ArenaGapMessage.COMPASS_NAME), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        compassMeta.lore(Messages.lines(ArenaGapMessage.COMPASS_LORE, Placeholder.of("target", target.describe()),
                        Placeholder.of("reason", target.reason().getLoreLine())).stream()
                .map(ArenaGapCompass::lore)
                .toList());
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

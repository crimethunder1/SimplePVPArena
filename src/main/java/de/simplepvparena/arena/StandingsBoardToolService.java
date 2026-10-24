package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Function;

public final class StandingsBoardToolService {

    public static final Material TOOL_MATERIAL = Material.NETHER_STAR;

    public static final String TOOL_KEY_NAME = "standings_board_tool";

    private static final String TOOL_KEY_VALUE = "standings-board";

    private final NamespacedKey toolKey;
    private final ArenaSetupService arenaSetupService;
    private final Function<Material, ItemStack> itemStackFactory;

    public StandingsBoardToolService(Plugin plugin, ArenaSetupService arenaSetupService,
                                     Function<Material, ItemStack> itemStackFactory) {
        this.toolKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), TOOL_KEY_NAME);
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.itemStackFactory = Objects.requireNonNull(itemStackFactory, "itemStackFactory");
    }

    public ItemStack createTool() {
        ItemStack itemStack = itemStackFactory.apply(TOOL_MATERIAL);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(withoutItalics(Messages.text(ArenaMenuMessage.STANDINGS_BOARD_TOOL_NAME)));
        itemMeta.lore(Messages.lines(ArenaMenuMessage.STANDINGS_BOARD_TOOL_ITEM_LORE).stream()
                .map(StandingsBoardToolService::withoutItalics)
                .toList());
        itemMeta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, TOOL_KEY_VALUE);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isTool(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        return TOOL_KEY_VALUE.equals(itemStack.getItemMeta().getPersistentDataContainer()
                .get(toolKey, PersistentDataType.STRING));
    }

    public void useTool(Player admin, Block clickedBlock) {
        if (clickedBlock == null) {
            TournamentMessages.sendError(admin, Messages.text(ArenaMenuMessage.STANDINGS_BOARD_CLICK_A_BLOCK));
            return;
        }
        if (arenaSetupService.isStandingsBoardBlock(clickedBlock)) {
            arenaSetupService.removeStandingsBoard(admin);
            TournamentMessages.sendSuccess(admin, Messages.text(ArenaMenuMessage.STANDINGS_BOARD_REMOVED));
            return;
        }
        arenaSetupService.placeStandingsBoard(admin, clickedBlock);
        TournamentMessages.sendSuccess(admin, Messages.text(ArenaMenuMessage.STANDINGS_BOARD_PLACED));
    }

    public void takeToolAway(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isTool(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    private static Component withoutItalics(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }
}

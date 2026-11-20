package de.simplepvparena.placement;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.PlacementMessage;
import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@NullMarked
public final class BracketBoardService {

    public static final Material ITEM_MATERIAL = Material.PAINTING;

    private static final String ITEM_KEY_NAME = "bracket_board_item";
    private static final byte ITEM_KEY_VALUE = 1;

    private final NamespacedKey itemKey;
    private final BracketBoards boards;
    private final BracketBoardDisplays displays;
    private final FileBracketBoardStorage storage;
    private final Consumer<String> warningLogger;

    public BracketBoardService(Plugin plugin, BracketBoards boards, BracketBoardDisplays displays,
                               FileBracketBoardStorage storage, Consumer<String> warningLogger) {
        this.itemKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), ITEM_KEY_NAME);
        this.boards = Objects.requireNonNull(boards, "boards");
        this.displays = Objects.requireNonNull(displays, "displays");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.warningLogger = Objects.requireNonNull(warningLogger, "warningLogger");
    }

    public void load() {
        try {
            boards.replaceAll(storage.load());
        } catch (IOException failure) {
            warningLogger.accept(Messages.text(PlacementMessage.BRACKET_NOT_LOADED,
                    Placeholder.of("file", FileBracketBoardStorage.FILE_NAME)));
        }
    }

    public ItemStack createBoardItem() {
        ItemStack itemStack = new ItemStack(ITEM_MATERIAL);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(Messages.text(PlacementMessage.BRACKET_ITEM_NAME))
                .decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(Messages.lines(PlacementMessage.BRACKET_ITEM_LORE).stream()
                .map(line -> (Component) Component.text(line).decoration(TextDecoration.ITALIC, false))
                .toList());
        itemMeta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, ITEM_KEY_VALUE);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isBoardItem(@Nullable ItemStack itemStack) {
        return itemStack != null && itemStack.hasItemMeta()
                && itemStack.getItemMeta().getPersistentDataContainer().has(itemKey);
    }

    public void takeBoardItemsAway(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isBoardItem(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    public void placeBoard(Player admin, Block clickedBlock, BlockFace clickedFace) {
        Block anchor = clickedBlock.isReplaceable() ? clickedBlock : clickedBlock.getRelative(clickedFace);
        BlockFace facing = admin.getFacing().getOppositeFace();
        if (!BracketBoard.isHorizontal(facing)) {
            facing = BlockFace.SOUTH;
        }
        World world = anchor.getWorld();
        BracketBoard board = new BracketBoard(world.getName(), anchor.getX(), anchor.getY(), anchor.getZ(), facing);
        for (int[] position : board.blocks()) {
            Block block = world.getBlockAt(position[0], position[1], position[2]);
            if (!block.isReplaceable() || boards.overlapsAny(board)) {
                TournamentMessages.sendError(admin, Messages.text(PlacementMessage.BRACKET_NO_ROOM,
                        Placeholder.of("width", BracketBoard.WIDTH), Placeholder.of("height", BracketBoard.HEIGHT)));
                return;
            }
        }
        for (int[] position : board.blocks()) {
            world.getBlockAt(position[0], position[1], position[2]).setType(Material.BARRIER, false);
        }
        boards.add(board);
        save();
        displays.show(board);
        TournamentMessages.sendSuccess(admin, Messages.text(PlacementMessage.BRACKET_PLACED));
    }

    public Optional<BracketBoard> findBoardAt(Block block) {
        return boards.findAt(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public void removeBoard(Player admin, BracketBoard board) {
        if (!boards.remove(board)) {
            return;
        }
        World world = admin.getServer().getWorld(board.worldName());
        if (world != null) {
            for (int[] position : board.blocks()) {
                Block block = world.getBlockAt(position[0], position[1], position[2]);
                if (block.getType() == Material.BARRIER) {
                    block.setType(Material.AIR, false);
                }
            }
        }
        save();
        displays.hide(board);
        TournamentMessages.sendSuccess(admin, Messages.text(PlacementMessage.BRACKET_REMOVED));
    }

    private void save() {
        try {
            storage.save(boards.boards());
        } catch (IOException failure) {
            warningLogger.accept(Messages.text(PlacementMessage.BRACKET_NOT_SAVED,
                    Placeholder.of("file", FileBracketBoardStorage.FILE_NAME)));
        }
    }
}

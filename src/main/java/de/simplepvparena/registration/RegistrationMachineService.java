package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.RegistrationMessage;
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

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@NullMarked
public final class RegistrationMachineService {

    public static final Material ITEM_MATERIAL = Material.DISPENSER;

    private static final String ITEM_KEY_NAME = "registration_machine_item";
    private static final byte ITEM_KEY_VALUE = 1;

    private final NamespacedKey itemKey;
    private final RegistrationMachines machines;
    private final RegistrationMachineDisplays displays;
    private final RegistrationService registrationService;
    private final FileRegistrationMachineStorage storage;
    private final Consumer<String> warningLogger;

    public RegistrationMachineService(Plugin plugin, RegistrationMachines machines,
                                      RegistrationMachineDisplays displays, RegistrationService registrationService,
                                      FileRegistrationMachineStorage storage, Consumer<String> warningLogger) {
        this.itemKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), ITEM_KEY_NAME);
        this.machines = Objects.requireNonNull(machines, "machines");
        this.displays = Objects.requireNonNull(displays, "displays");
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.warningLogger = Objects.requireNonNull(warningLogger, "warningLogger");
    }

    public void load() {
        try {
            machines.replaceAll(storage.load());
        } catch (IOException failure) {
            warningLogger.accept(Messages.text(RegistrationMessage.MACHINES_NOT_LOADED,
                    Placeholder.of("file", FileRegistrationMachineStorage.FILE_NAME)));
        }
    }

    public ItemStack createMachineItem() {
        ItemStack itemStack = new ItemStack(ITEM_MATERIAL);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(Messages.text(RegistrationMessage.MACHINE_ITEM_NAME))
                .decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(Messages.lines(RegistrationMessage.MACHINE_ITEM_LORE).stream()
                .map(line -> (Component) Component.text(line).decoration(TextDecoration.ITALIC, false))
                .toList());
        itemMeta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, ITEM_KEY_VALUE);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isMachineItem(ItemStack itemStack) {
        return itemStack != null && itemStack.hasItemMeta()
                && itemStack.getItemMeta().getPersistentDataContainer().has(itemKey);
    }

    public void takeMachineItemsAway(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isMachineItem(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    public void placeMachine(Player admin, Block clickedBlock, BlockFace clickedFace) {
        Block anchor = clickedBlock.isReplaceable() ? clickedBlock : clickedBlock.getRelative(clickedFace);
        BlockFace facing = admin.getFacing().getOppositeFace();
        if (!RegistrationMachine.isHorizontal(facing)) {
            facing = BlockFace.SOUTH;
        }
        World world = anchor.getWorld();
        RegistrationMachine machine = new RegistrationMachine(world.getName(), anchor.getX(), anchor.getY(),
                anchor.getZ(), facing);
        for (int[] position : machine.blocks()) {
            Block block = world.getBlockAt(position[0], position[1], position[2]);
            if (!block.isReplaceable() || machines.overlapsAny(machine)) {
                TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.MACHINE_NO_ROOM));
                return;
            }
        }
        for (int[] position : machine.blocks()) {
            world.getBlockAt(position[0], position[1], position[2]).setType(Material.BARRIER, false);
        }
        machines.add(machine);
        save();
        displays.show(machine);
        TournamentMessages.sendSuccess(admin, Messages.text(RegistrationMessage.MACHINE_PLACED));
    }

    public void useMachine(Player player, RegistrationMachine machine) {
        if (player.isSneaking()) {
            registrationService.leave(player);
        } else {
            registrationService.join(player);
        }
        displays.press(machine);
        displays.updateCounters();
    }

    public Optional<RegistrationMachine> findMachineAt(Block block) {
        return machines.findAt(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public void removeMachine(Player admin, RegistrationMachine machine) {
        if (!machines.remove(machine)) {
            return;
        }
        World world = admin.getServer().getWorld(machine.worldName());
        if (world != null) {
            for (int[] position : machine.blocks()) {
                Block block = world.getBlockAt(position[0], position[1], position[2]);
                if (block.getType() == Material.BARRIER) {
                    block.setType(Material.AIR, false);
                }
            }
        }
        save();
        displays.hide(machine);
        TournamentMessages.sendSuccess(admin, Messages.text(RegistrationMessage.MACHINE_REMOVED));
    }

    private void save() {
        try {
            storage.save(machines.machines());
        } catch (IOException failure) {
            warningLogger.accept(Messages.text(RegistrationMessage.MACHINES_NOT_SAVED,
                    Placeholder.of("file", FileRegistrationMachineStorage.FILE_NAME)));
        }
    }
}

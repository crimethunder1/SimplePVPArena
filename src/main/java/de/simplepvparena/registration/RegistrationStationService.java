package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.RegistrationMessage;
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
import java.util.function.Predicate;

public final class RegistrationStationService {

    public static final Material TOOL_MATERIAL = Material.BLAZE_ROD;

    public static final String TOOL_KEY_NAME = "registration_station_tool";

    private static final String TOOL_KEY_VALUE = "registration-station";

    private final NamespacedKey toolKey;
    private final RegistrationStations stations;
    private final RegistrationService registrationService;
    private final Predicate<Material> buttonMaterials;
    private final Runnable stationSaver;
    private final Function<Material, ItemStack> itemStackFactory;

    public RegistrationStationService(Plugin plugin, RegistrationStations stations,
                                      RegistrationService registrationService, Predicate<Material> buttonMaterials,
                                      Runnable stationSaver, Function<Material, ItemStack> itemStackFactory) {
        this.toolKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), TOOL_KEY_NAME);
        this.stations = Objects.requireNonNull(stations, "stations");
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService");
        this.buttonMaterials = Objects.requireNonNull(buttonMaterials, "buttonMaterials");
        this.stationSaver = Objects.requireNonNull(stationSaver, "stationSaver");
        this.itemStackFactory = Objects.requireNonNull(itemStackFactory, "itemStackFactory");
    }

    public ItemStack createTool() {
        return labelled(itemStackFactory.apply(TOOL_MATERIAL));
    }

    private ItemStack labelled(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(Messages.text(RegistrationMessage.TOOL_NAME))
                .decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(Messages.lines(RegistrationMessage.TOOL_LORE).stream()
                .map(RegistrationStationService::lore)
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
            TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.CLICK_A_BUTTON));
            return;
        }
        RegistrationStation station = RegistrationStation.of(clickedBlock);
        if (stations.remove(station)) {
            stationSaver.run();
            TournamentMessages.sendSuccess(admin, Messages.text(RegistrationMessage.STATION_REMOVED));
            return;
        }
        if (!buttonMaterials.test(clickedBlock.getType())) {
            TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.CLICK_A_BUTTON));
            return;
        }
        stations.add(station);
        stationSaver.run();
        TournamentMessages.sendSuccess(admin, Messages.text(RegistrationMessage.STATION_ADDED));
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

    public boolean isStation(Block block) {
        return stations.isStation(RegistrationStation.of(block));
    }

    public void useStation(Player player, Block block) {
        if (!isStation(block) || !buttonMaterials.test(block.getType())) {
            return;
        }
        if (player.isSneaking()) {
            registrationService.leave(player);
            return;
        }
        registrationService.join(player);
    }

    public void refuseBreaking(Player player) {
        TournamentMessages.sendActionBarError(player, Messages.text(RegistrationMessage.STATION_PROTECTED));
    }

    public void removeBrokenStation(Player player, Block block) {
        if (!stations.remove(RegistrationStation.of(block))) {
            return;
        }
        stationSaver.run();
        TournamentMessages.sendSuccess(player, Messages.text(RegistrationMessage.STATION_REMOVED));
    }

    public RegistrationStations getStations() {
        return stations;
    }

    private static Component lore(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }
}

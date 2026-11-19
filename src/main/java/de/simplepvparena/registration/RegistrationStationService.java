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
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@NullMarked
public final class RegistrationStationService {

    public static final Material TOOL_MATERIAL = Material.BLAZE_ROD;

    public static final String TOOL_KEY_NAME = "registration_station_tool";

    public static final Material STATION_BUTTON_MATERIAL = Material.STONE_BUTTON;

    private static final String TOOL_KEY_VALUE = "registration-station";
    private static final String STATION_BUTTON_KEY_VALUE = "registration-station-button";

    private final NamespacedKey toolKey;
    private final RegistrationStations stations;
    private final RegistrationService registrationService;
    private final Predicate<Material> buttonMaterials;
    private final Runnable stationSaver;
    private final Function<Material, ItemStack> itemStackFactory;
    private final List<RegistrationStationObserver> observers = new ArrayList<>();

    private Supplier<Optional<ItemStack>> stationButtonLook = Optional::empty;
    private Supplier<ItemStack> handOutItemFactory = () -> createStationButton();

    public void setHandOutItemFactory(Supplier<ItemStack> handOutItemFactory) {
        this.handOutItemFactory = Objects.requireNonNull(handOutItemFactory, "handOutItemFactory");
    }

    public ItemStack createHandOutItem() {
        return handOutItemFactory.get();
    }

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

    public void addObserver(RegistrationStationObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    public void setStationButtonLook(Supplier<Optional<ItemStack>> stationButtonLook) {
        this.stationButtonLook = Objects.requireNonNull(stationButtonLook, "stationButtonLook");
    }

    public ItemStack createTool() {
        return labelled(itemStackFactory.apply(TOOL_MATERIAL), RegistrationMessage.TOOL_NAME,
                RegistrationMessage.TOOL_LORE, TOOL_KEY_VALUE);
    }

    public ItemStack createStationButton() {
        ItemStack button = itemStackFactory.apply(STATION_BUTTON_MATERIAL);
        stationButtonLook.get().filter(ItemStack::hasItemMeta).ifPresent(look -> {
            ItemMeta lookMeta = look.getItemMeta();
            ItemMeta buttonMeta = button.getItemMeta();
            if (lookMeta.hasItemModel()) {
                buttonMeta.setItemModel(lookMeta.getItemModel());
            }
            if (lookMeta.hasCustomModelDataComponent()) {
                buttonMeta.setCustomModelDataComponent(lookMeta.getCustomModelDataComponent());
            }
            button.setItemMeta(buttonMeta);
        });
        return labelled(button, RegistrationMessage.STATION_BUTTON_NAME, RegistrationMessage.STATION_BUTTON_LORE,
                STATION_BUTTON_KEY_VALUE);
    }

    private ItemStack labelled(ItemStack itemStack, RegistrationMessage name, RegistrationMessage lore,
                               String keyValue) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(Messages.text(name)).decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(Messages.lines(lore).stream()
                .map(RegistrationStationService::lore)
                .toList());
        itemMeta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, keyValue);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isTool(ItemStack itemStack) {
        return hasMarker(itemStack, TOOL_KEY_VALUE);
    }

    public boolean isStationButton(ItemStack itemStack) {
        return hasMarker(itemStack, STATION_BUTTON_KEY_VALUE);
    }

    private boolean hasMarker(ItemStack itemStack, String keyValue) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        return keyValue.equals(itemStack.getItemMeta().getPersistentDataContainer()
                .get(toolKey, PersistentDataType.STRING));
    }

    public void placeStationButton(Player admin, Block placedBlock) {
        RegistrationStation station = RegistrationStation.of(placedBlock);
        if (!buttonMaterials.test(placedBlock.getType()) || !stations.add(station)) {
            return;
        }
        stationSaver.run();
        observers.forEach(observer -> observer.stationAdded(station));
        TournamentMessages.sendSuccess(admin, Messages.text(RegistrationMessage.STATION_ADDED));
    }

    public void useTool(Player admin, Block clickedBlock) {
        if (clickedBlock == null) {
            TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.CLICK_A_BUTTON));
            return;
        }
        RegistrationStation station = RegistrationStation.of(clickedBlock);
        if (stations.remove(station)) {
            stationSaver.run();
            observers.forEach(observer -> observer.stationRemoved(station));
            TournamentMessages.sendSuccess(admin, Messages.text(RegistrationMessage.STATION_REMOVED));
            return;
        }
        if (!buttonMaterials.test(clickedBlock.getType())) {
            TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.CLICK_A_BUTTON));
            return;
        }
        stations.add(station);
        stationSaver.run();
        observers.forEach(observer -> observer.stationAdded(station));
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
        useStation(player, RegistrationStation.of(block));
    }

    public void useStation(Player player, RegistrationStation station) {
        if (!stations.isStation(station)) {
            return;
        }
        if (player.isSneaking()) {
            registrationService.leave(player);
        } else {
            registrationService.join(player);
        }
        observers.forEach(observer -> observer.stationUsed(station));
    }

    public void refuseBreaking(Player player) {
        TournamentMessages.sendActionBarError(player, Messages.text(RegistrationMessage.STATION_PROTECTED));
    }

    public void removeBrokenStation(Player player, Block block) {
        RegistrationStation station = RegistrationStation.of(block);
        if (!stations.remove(station)) {
            return;
        }
        stationSaver.run();
        observers.forEach(observer -> observer.stationRemoved(station));
        TournamentMessages.sendSuccess(player, Messages.text(RegistrationMessage.STATION_REMOVED));
    }

    public boolean removeStationWithoutButton(RegistrationStation station) {
        if (!stations.remove(station)) {
            return false;
        }
        stationSaver.run();
        observers.forEach(observer -> observer.stationRemoved(station));
        return true;
    }

    public RegistrationStations getStations() {
        return stations;
    }

    private static Component lore(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }
}

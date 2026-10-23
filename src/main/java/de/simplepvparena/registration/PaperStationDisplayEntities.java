package de.simplepvparena.registration;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class PaperStationDisplayEntities implements StationDisplayEntities {

    public static final String MARKER_KEY_NAME = "registration_station_display";

    private static final byte MARKER_VALUE = 1;

    private final NamespacedKey markerKey;

    public PaperStationDisplayEntities(Plugin plugin) {
        this.markerKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), MARKER_KEY_NAME);
    }

    @Override
    public Entity spawnItemDisplay(Location location, ItemStack itemStack) {
        return location.getWorld().spawn(location, ItemDisplay.class, display -> {
            mark(display);
            display.setBillboard(Display.Billboard.CENTER);
            display.setItemStack(itemStack);
        });
    }

    @Override
    public Entity spawnTextDisplay(Location location, Component text) {
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            mark(display);
            display.setBillboard(Display.Billboard.CENTER);
            display.text(text);
        });
    }

    @Override
    public boolean isStationDisplay(Entity entity) {
        return entity instanceof Display && entity.getPersistentDataContainer().has(markerKey);
    }

    private void mark(Display display) {
        display.setPersistent(false);
        display.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, MARKER_VALUE);
    }
}

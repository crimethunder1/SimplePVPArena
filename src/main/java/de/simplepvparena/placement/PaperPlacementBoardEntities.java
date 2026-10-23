package de.simplepvparena.placement;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class PaperPlacementBoardEntities implements PlacementBoardEntities {

    public static final String MARKER_KEY_NAME = "placement_board_display";
    public static final int LINE_WIDTH = 400;

    private static final byte MARKER_VALUE = 1;

    private final NamespacedKey markerKey;

    public PaperPlacementBoardEntities(Plugin plugin) {
        this.markerKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), MARKER_KEY_NAME);
    }

    @Override
    public Entity spawnBoard(Location location, Component text) {
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.setPersistent(false);
            display.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, MARKER_VALUE);
            display.setBillboard(Display.Billboard.CENTER);
            display.setLineWidth(LINE_WIDTH);
            display.text(text);
        });
    }

    @Override
    public void changeText(Entity board, Component text) {
        if (board instanceof TextDisplay display) {
            display.text(text);
        }
    }

    @Override
    public boolean isBoard(Entity entity) {
        return entity instanceof TextDisplay && entity.getPersistentDataContainer().has(markerKey);
    }
}

package de.simplepvparena.registration;

import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperStationDisplayEntitiesTest {

    private static final NamespacedKey MARKER_KEY = new NamespacedKey("simplepvparena",
            PaperStationDisplayEntities.MARKER_KEY_NAME);

    private final List<String> calls = new ArrayList<>();
    private final Map<Object, Object> persistentData = new LinkedHashMap<>();
    private final Plugin plugin = SharedBukkitServer.proxy(Plugin.class, (method, arguments) ->
            switch (method.getName()) {
                case "getName", "namespace" -> "simplepvparena";
                default -> throw new UnsupportedOperationException("Plugin." + method.getName());
            });
    private final PaperStationDisplayEntities displayEntities = new PaperStationDisplayEntities(plugin);

    @Test
    void theTextDisplayIsMarkedNotSavedAndTurnsToEveryViewer() {
        Component text = Component.text("Register");
        TextDisplay display = display(TextDisplay.class);
        Location location = new Location(world(display), 10.5D, 65.7D, 20.5D);

        Entity spawned = displayEntities.spawnTextDisplay(location, text);

        assertSame(display, spawned);
        assertEquals(List.of("spawn TextDisplay at 10.5 65.7 20.5", "setPersistent false", "setBillboard CENTER",
                "text " + text), calls);
        assertEquals(Map.of(MARKER_KEY, (byte) 1), persistentData);
    }

    @Test
    void theItemDisplayIsMarkedNotSavedAndTurnsToEveryViewer() {
        FakeItemStack stationItem = new FakeItemStack("station item");
        ItemDisplay display = display(ItemDisplay.class);
        Location location = new Location(world(display), 10.5D, 65.7D, 20.5D);

        Entity spawned = displayEntities.spawnItemDisplay(location, stationItem);

        assertSame(display, spawned);
        assertEquals(List.of("spawn ItemDisplay at 10.5 65.7 20.5", "setPersistent false", "setBillboard CENTER",
                "setItemStack " + stationItem), calls);
        assertEquals(Map.of(MARKER_KEY, (byte) 1), persistentData);
    }

    @Test
    void recognisesOnlyDisplaysWithItsMarker() {
        TextDisplay marked = display(TextDisplay.class);
        displayEntities.spawnTextDisplay(new Location(world(marked), 0.0D, 0.0D, 0.0D), Component.empty());
        Map<Object, Object> otherData = new LinkedHashMap<>();
        Display unmarked = SharedBukkitServer.proxy(Display.class, (method, arguments) ->
                switch (method.getName()) {
                    case "getPersistentDataContainer" -> SharedBukkitServer.createPersistentDataContainer(otherData);
                    default -> throw new UnsupportedOperationException("Display." + method.getName());
                });
        Entity otherEntity = SharedBukkitServer.proxy(Entity.class, (method, arguments) -> {
            throw new UnsupportedOperationException("Entity." + method.getName());
        });

        assertTrue(displayEntities.isStationDisplay(marked));
        assertFalse(displayEntities.isStationDisplay(unmarked));
        assertFalse(displayEntities.isStationDisplay(otherEntity));
    }

    private <T extends Display> T display(Class<T> type) {
        return SharedBukkitServer.proxy(type, (method, arguments) -> {
            switch (method.getName()) {
                case "getPersistentDataContainer" -> {
                    return SharedBukkitServer.createPersistentDataContainer(persistentData);
                }
                case "setPersistent", "setBillboard", "text", "setItemStack" -> {
                    calls.add(method.getName() + " " + arguments[0]);
                    return null;
                }
                default -> throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private World world(Display display) {
        return SharedBukkitServer.proxy(World.class, (method, arguments) -> {
            if (method.getName().equals("spawn") && arguments.length == 3) {
                Location location = (Location) arguments[0];
                calls.add("spawn " + ((Class<?>) arguments[1]).getSimpleName() + " at " + location.getX() + " "
                        + location.getY() + " " + location.getZ());
                ((Consumer<Object>) arguments[2]).accept(display);
                return display;
            }
            throw new UnsupportedOperationException("World." + method.getName());
        });
    }
}

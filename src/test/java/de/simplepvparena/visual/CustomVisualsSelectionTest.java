package de.simplepvparena.visual;

import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class CustomVisualsSelectionTest {

    private final List<String> infoLines = new ArrayList<>();
    private final List<String> askedPluginNames = new ArrayList<>();
    private final AtomicInteger factoryCalls = new AtomicInteger();
    private final RecordingCustomVisuals itemsAdderVisuals = new RecordingCustomVisuals();
    private final Supplier<CustomVisuals> itemsAdderVisualsFactory = () -> {
        factoryCalls.incrementAndGet();
        return itemsAdderVisuals;
    };

    @Test
    void withoutItemsAdderAlwaysChoosesTheVanillaVisualsAndNeverTouchesTheItemsAdderFactory() {
        for (boolean enabled : new boolean[] {true, false}) {
            CustomVisuals chosen = CustomVisualsSelection.select(pluginManager(false), enabled,
                    itemsAdderVisualsFactory, infoLines::add);

            assertInstanceOf(VanillaVisuals.class, chosen, "items-adder.enabled " + enabled);
        }
        assertEquals(0, factoryCalls.get());
        assertEquals(List.of(), infoLines, "nothing is logged without ItemsAdder");
        assertEquals(List.of("ItemsAdder", "ItemsAdder"), askedPluginNames);
    }

    @Test
    void withItemsAdderSwitchedOffInTheConfigurationChoosesTheVanillaVisualsAndSaysSo() {
        CustomVisuals chosen = CustomVisualsSelection.select(pluginManager(true), false, itemsAdderVisualsFactory,
                infoLines::add);

        assertInstanceOf(VanillaVisuals.class, chosen);
        assertEquals(0, factoryCalls.get());
        assertEquals(List.of("ItemsAdder was found, but items-adder.enabled in config.yml is false, the plugin "
                + "shows its own titles and effects until a restart with true"), infoLines);
    }

    @Test
    void withItemsAdderRunningAndSwitchedOnChoosesTheItemsAdderVisualsOnce() {
        CustomVisuals chosen = CustomVisualsSelection.select(pluginManager(true), true, itemsAdderVisualsFactory,
                infoLines::add);

        assertSame(itemsAdderVisuals, chosen);
        assertEquals(1, factoryCalls.get());
        assertEquals(List.of("ItemsAdder was found, the IDs of the section items-adder in config.yml are looked "
                + "up in ItemsAdder once it has loaded its content"), infoLines);
    }

    @Test
    @UseLanguage("de")
    void saysTheChoiceInGerman() {
        CustomVisualsSelection.select(pluginManager(true), true, itemsAdderVisualsFactory, infoLines::add);
        CustomVisualsSelection.select(pluginManager(true), false, itemsAdderVisualsFactory, infoLines::add);

        assertEquals(List.of("ItemsAdder wurde gefunden, die IDs aus dem Abschnitt items-adder in config.yml werden "
                        + "in ItemsAdder nachgeschlagen, sobald es seine Inhalte geladen hat",
                "ItemsAdder wurde gefunden, aber items-adder.enabled in config.yml ist false, das Plugin zeigt bis zu "
                        + "einem Neustart mit true seine eigenen Titel und Effekte"), infoLines);
    }

    @Test
    void asksForThePluginByItsExactName() {
        assertEquals("ItemsAdder", CustomVisualsSelection.ITEMS_ADDER_PLUGIN_NAME);
    }

    private PluginManager pluginManager(boolean itemsAdderEnabled) {
        return SharedBukkitServer.proxy(PluginManager.class, (method, arguments) -> switch (method.getName()) {
            case "isPluginEnabled" -> {
                askedPluginNames.add((String) arguments[0]);
                yield itemsAdderEnabled && "ItemsAdder".equals(arguments[0]);
            }
            default -> throw new UnsupportedOperationException("PluginManager." + method.getName());
        });
    }
}

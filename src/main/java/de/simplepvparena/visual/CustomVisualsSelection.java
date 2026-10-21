package de.simplepvparena.visual;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.VisualMessage;
import org.bukkit.plugin.PluginManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CustomVisualsSelection {

    public static final String ITEMS_ADDER_PLUGIN_NAME = "ItemsAdder";

    private CustomVisualsSelection() {
    }

    public static CustomVisuals select(PluginManager pluginManager, boolean itemsAdderEnabled,
                                       Supplier<CustomVisuals> itemsAdderVisualsFactory,
                                       Consumer<String> infoLogger) {
        if (!pluginManager.isPluginEnabled(ITEMS_ADDER_PLUGIN_NAME)) {
            return new VanillaVisuals();
        }
        if (!itemsAdderEnabled) {
            infoLogger.accept(Messages.text(VisualMessage.ITEMS_ADDER_SWITCHED_OFF,
                    Placeholder.of("path", CustomVisualSettings.ENABLED_CONFIGURATION_PATH)));
            return new VanillaVisuals();
        }
        infoLogger.accept(Messages.text(VisualMessage.ITEMS_ADDER_USED,
                Placeholder.of("section", CustomVisualSettings.SECTION_CONFIGURATION_PATH)));
        return itemsAdderVisualsFactory.get();
    }
}

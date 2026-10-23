package de.simplepvparena.border;

import de.simplepvparena.visual.CustomVisualsSelection;
import org.bukkit.plugin.PluginManager;

import java.util.logging.Logger;

public final class BorderVisualEffects {

    private BorderVisualEffects() {
    }

    public static BorderVisualEffect create(PluginManager pluginManager, Logger logger) {
        if (CustomVisualsSelection.isItemsAdderEnabled(pluginManager)) {
            logger.info(CustomVisualsSelection.ITEMS_ADDER_PLUGIN_NAME + " was found, the border blocks still use "
                    + "vanilla particles: this build ships no " + CustomVisualsSelection.ITEMS_ADDER_PLUGIN_NAME
                    + " implementation of " + BorderVisualEffect.class.getSimpleName());
        }
        return new VanillaBorderVisualEffect();
    }
}

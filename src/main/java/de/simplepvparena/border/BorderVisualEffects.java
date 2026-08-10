package de.simplepvparena.border;

import org.bukkit.plugin.PluginManager;

import java.util.logging.Logger;

public final class BorderVisualEffects {

    public static final String ITEMS_ADDER_PLUGIN_NAME = "ItemsAdder";

    private BorderVisualEffects() {
    }

    public static BorderVisualEffect create(PluginManager pluginManager, Logger logger) {
        if (pluginManager.isPluginEnabled(ITEMS_ADDER_PLUGIN_NAME)) {
            logger.info(ITEMS_ADDER_PLUGIN_NAME + " was found, the border blocks still use vanilla particles: "
                    + "this build ships no " + ITEMS_ADDER_PLUGIN_NAME + " implementation of "
                    + BorderVisualEffect.class.getSimpleName());
        }
        return new VanillaBorderVisualEffect();
    }
}

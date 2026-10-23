package de.simplepvparena.border;

import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BorderVisualEffectsTest {

    private final List<String> infoLines = new ArrayList<>();

    @Test
    void usesTheVanillaParticlesWithoutItemsAdderAndLogsNothing() {
        BorderVisualEffect effect = BorderVisualEffects.create(pluginManager(false), recordingLogger());

        assertInstanceOf(VanillaBorderVisualEffect.class, effect);
        assertEquals(List.of(), infoLines);
    }

    @Test
    void usesTheVanillaParticlesWithItemsAdderAndSaysSo() {
        BorderVisualEffect effect = BorderVisualEffects.create(pluginManager(true), recordingLogger());

        assertInstanceOf(VanillaBorderVisualEffect.class, effect);
        assertEquals(List.of("ItemsAdder was found, the border blocks still use vanilla particles: this build "
                + "ships no ItemsAdder implementation of BorderVisualEffect"), infoLines);
    }

    private Logger recordingLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                infoLines.add(logRecord.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }

    private static PluginManager pluginManager(boolean itemsAdderEnabled) {
        return SharedBukkitServer.proxy(PluginManager.class, (method, arguments) -> switch (method.getName()) {
            case "isPluginEnabled" -> itemsAdderEnabled && "ItemsAdder".equals(arguments[0]);
            default -> throw new UnsupportedOperationException("PluginManager." + method.getName());
        });
    }
}

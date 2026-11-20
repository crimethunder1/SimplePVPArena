package de.simplepvparena.voice;

import de.simplepvparena.config.PluginSettings;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.VoiceMessage;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.NullMarked;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@NullMarked
public final class VoiceChatHook {

    public static final String SIMPLE_VOICE_CHAT_PLUGIN_NAME = "voicechat";
    public static final long CONNECTION_CHECK_DELAY_TICKS = 100L;

    private VoiceChatHook() {
    }

    public static boolean hook(PluginManager pluginManager, BooleanSupplier registration, boolean enabled,
                               Consumer<String> infoLogger, Consumer<String> warningLogger) {
        if (!pluginManager.isPluginEnabled(SIMPLE_VOICE_CHAT_PLUGIN_NAME)) {
            return false;
        }
        if (!registration.getAsBoolean()) {
            warningLogger.accept(Messages.text(VoiceMessage.SIMPLE_VOICE_CHAT_SERVICE_MISSING));
            return false;
        }
        if (enabled) {
            infoLogger.accept(Messages.text(VoiceMessage.SIMPLE_VOICE_CHAT_USED,
                    Placeholder.of("path", PluginSettings.VOICE_CHAT_ENABLED_PATH),
                    Placeholder.of("permission", VoiceIsolation.BYPASS_PERMISSION_NAME)));
        } else {
            infoLogger.accept(Messages.text(VoiceMessage.SIMPLE_VOICE_CHAT_SWITCHED_OFF,
                    Placeholder.of("path", PluginSettings.VOICE_CHAT_ENABLED_PATH)));
        }
        return true;
    }

    public static void checkConnection(VoiceIsolation isolation, Consumer<String> warningLogger) {
        if (isolation.isAttached() && !isolation.isConnected()) {
            warningLogger.accept(Messages.text(VoiceMessage.SIMPLE_VOICE_CHAT_NOT_CONNECTED));
        }
    }
}

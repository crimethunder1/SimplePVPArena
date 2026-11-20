package de.simplepvparena.voice;

import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class VoiceChatHookTest {

    private static final String USED = "SimpleVoiceChat was found, during a match the fighters only hear the other "
            + "fighters and players with tournament.voice.bypass, voice-chat.enabled false in config.yml switches "
            + "this off";
    private static final String NOT_CONNECTED = "SimpleVoiceChat did not connect the plugin, the fighters keep "
            + "hearing every player in the voice chat. This happens when only SimplePVPArena was reloaded, restart "
            + "the server";

    private final List<String> infoLines = new ArrayList<>();
    private final List<String> warningLines = new ArrayList<>();
    private final List<String> askedPluginNames = new ArrayList<>();
    private final AtomicInteger registrations = new AtomicInteger();

    @Test
    void withoutSimpleVoiceChatNeverRegistersAndLogsNothing() {
        assertFalse(VoiceChatHook.hook(pluginManager(false), this::register, true, infoLines::add,
                warningLines::add));

        assertEquals(0, registrations.get());
        assertEquals(List.of(), infoLines);
        assertEquals(List.of(), warningLines);
        assertEquals(List.of("voicechat"), askedPluginNames);
    }

    @Test
    void registersOnceAndSaysThatTheFightersAreIsolated() {
        assertTrue(VoiceChatHook.hook(pluginManager(true), this::register, true, infoLines::add, warningLines::add));

        assertEquals(1, registrations.get());
        assertEquals(List.of(USED), infoLines);
        assertEquals(List.of(), warningLines);
    }

    @Test
    void registersAlsoWhenSwitchedOffAndSaysSo() {
        assertTrue(VoiceChatHook.hook(pluginManager(true), this::register, false, infoLines::add,
                warningLines::add));

        assertEquals(1, registrations.get());
        assertEquals(List.of("SimpleVoiceChat was found, but voice-chat.enabled in config.yml is false, the "
                + "fighters hear every player in the voice chat"), infoLines);
    }

    @Test
    void warnsWhenSimpleVoiceChatOffersNoInterface() {
        assertFalse(VoiceChatHook.hook(pluginManager(true), () -> false, true, infoLines::add, warningLines::add));

        assertEquals(List.of(), infoLines);
        assertEquals(List.of("SimpleVoiceChat runs but offers no interface for plugins, the fighters keep hearing "
                + "every player in the voice chat"), warningLines);
    }

    @Test
    @UseLanguage("de")
    void saysTheChoiceInGerman() {
        VoiceChatHook.hook(pluginManager(true), this::register, true, infoLines::add, warningLines::add);

        assertEquals(List.of("SimpleVoiceChat wurde gefunden, während eines Matches hören die Kämpfer nur die "
                + "anderen Kämpfer und Spieler mit tournament.voice.bypass, mit voice-chat.enabled false in "
                + "config.yml ist das aus"), infoLines);
    }

    @Test
    void warnsWhenSimpleVoiceChatDidNotConnectThePlugin() {
        VoiceIsolation isolation = new VoiceIsolation(() -> VoiceRoster.EMPTY, new SilentNotices());
        VoiceChatHook.checkConnection(isolation, warningLines::add);
        assertEquals(List.of(), warningLines, "nothing to warn about without SimpleVoiceChat");

        isolation.attach(groups(false));
        VoiceChatHook.checkConnection(isolation, warningLines::add);
        assertEquals(List.of(NOT_CONNECTED), warningLines);

        warningLines.clear();
        isolation.attach(groups(true));
        VoiceChatHook.checkConnection(isolation, warningLines::add);
        assertEquals(List.of(), warningLines);
    }

    @Test
    void asksForThePluginByItsExactName() {
        assertEquals("voicechat", VoiceChatHook.SIMPLE_VOICE_CHAT_PLUGIN_NAME);
    }

    private boolean register() {
        registrations.incrementAndGet();
        return true;
    }

    private PluginManager pluginManager(boolean simpleVoiceChatEnabled) {
        return SharedBukkitServer.proxy(PluginManager.class, (method, arguments) -> switch (method.getName()) {
            case "isPluginEnabled" -> {
                askedPluginNames.add((String) arguments[0]);
                yield simpleVoiceChatEnabled && "voicechat".equals(arguments[0]);
            }
            default -> throw new UnsupportedOperationException("PluginManager." + method.getName());
        });
    }

    private static VoiceGroups groups(boolean connected) {
        return new VoiceGroups() {
            @Override
            public boolean isConnected() {
                return connected;
            }

            @Override
            public boolean leaveGroup(UUID player) {
                return false;
            }
        };
    }

    private static final class SilentNotices implements VoiceNotices {

        @Override
        public void isolated(UUID fighter) {
        }

        @Override
        public void leftGroup(UUID fighter) {
        }

        @Override
        public void eliminated(UUID fighter) {
        }

        @Override
        public void backInTheFight(UUID fighter) {
        }

        @Override
        public void groupRefused(UUID fighter) {
        }
    }
}

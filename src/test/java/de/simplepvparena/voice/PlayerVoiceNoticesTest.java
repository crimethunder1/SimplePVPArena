package de.simplepvparena.voice;

import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerVoiceNoticesTest {

    private static final UUID ONLINE = UUID.randomUUID();
    private static final UUID OFFLINE = UUID.randomUUID();

    private final List<String> received = new ArrayList<>();
    private final Player onlinePlayer = SharedBukkitServer.proxy(Player.class, (method, arguments) -> {
        if (method.getName().equals("sendMessage") && arguments.length == 1
                && arguments[0] instanceof Component component) {
            received.add(PlainTextComponentSerializer.plainText().serialize(component));
            return null;
        }
        throw new UnsupportedOperationException("Player." + method.getName());
    });
    private final PlayerVoiceNotices notices = new PlayerVoiceNotices(
            player -> player.equals(ONLINE) ? onlinePlayer : null);

    @Test
    void tellsTheOnlineFighterEveryChangeOfWhatHeHears() {
        notices.isolated(ONLINE);
        notices.leftGroup(ONLINE);
        notices.eliminated(ONLINE);
        notices.backInTheFight(ONLINE);
        notices.groupRefused(ONLINE);

        assertEquals(5, received.size(), received::toString);
        assertTrue(received.get(0).endsWith("Voice chat: during the match you only hear the fighters, the spectators "
                + "are muted for you"), received.get(0));
        assertTrue(received.get(1).endsWith("Voice chat: you left your group for the match"));
        assertTrue(received.get(2).endsWith("Voice chat: you are out, you now hear your team and the spectators, "
                + "the opponents no longer hear you"));
        assertTrue(received.get(3).endsWith("Voice chat: you are back in the fight and only hear the fighters"));
        assertTrue(received.get(4).endsWith("Voice chat: you cannot join a group during your match"));
    }

    @Test
    void sendsNothingToAPlayerWhoIsOffline() {
        notices.isolated(OFFLINE);
        notices.groupRefused(OFFLINE);

        assertEquals(List.of(), received);
    }
}

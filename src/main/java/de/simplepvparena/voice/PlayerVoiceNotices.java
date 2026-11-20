package de.simplepvparena.voice;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.message.VoiceMessage;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@NullMarked
public final class PlayerVoiceNotices implements VoiceNotices {

    private final Function<UUID, @Nullable Player> onlinePlayers;

    public PlayerVoiceNotices(Function<UUID, @Nullable Player> onlinePlayers) {
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
    }

    @Override
    public void isolated(UUID fighter) {
        send(fighter, VoiceMessage.ISOLATED);
    }

    @Override
    public void leftGroup(UUID fighter) {
        send(fighter, VoiceMessage.LEFT_GROUP);
    }

    @Override
    public void eliminated(UUID fighter) {
        send(fighter, VoiceMessage.ELIMINATED);
    }

    @Override
    public void groupRefused(UUID fighter) {
        send(fighter, VoiceMessage.GROUP_REFUSED);
    }

    private void send(UUID receiver, VoiceMessage message) {
        Player player = onlinePlayers.apply(receiver);
        if (player != null) {
            TournamentMessages.sendInfo(player, Messages.text(message));
        }
    }
}

package de.simplepvparena.registration;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationObserver {

    void playerRegistered(Player player);

    default void playerRegisteredWithoutTeamChoice(Player player) {
        playerRegistered(player);
    }

    void playerUnregistered(UUID playerUniqueId);

    default Optional<String> findUnregistrationRejection(UUID playerUniqueId) {
        return Optional.empty();
    }
}

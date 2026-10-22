package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.RegistrationMessage;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class RegistrationService {

    private final RegistrationList registrationList;
    private final BooleanSupplier tournamentInProgress;
    private final Runnable registrationSaver;

    public RegistrationService(RegistrationList registrationList, BooleanSupplier tournamentInProgress,
                               Runnable registrationSaver) {
        this.registrationList = Objects.requireNonNull(registrationList, "registrationList");
        this.tournamentInProgress = Objects.requireNonNull(tournamentInProgress, "tournamentInProgress");
        this.registrationSaver = Objects.requireNonNull(registrationSaver, "registrationSaver");
    }

    public void join(Player player) {
        if (isClosed(player)) {
            return;
        }
        UUID uniqueId = player.getUniqueId();
        if (!registrationList.register(uniqueId, player.getName())) {
            if (registrationList.rememberName(uniqueId, player.getName())) {
                registrationSaver.run();
            }
            TournamentMessages.sendWarning(player, Messages.text(RegistrationMessage.ALREADY_REGISTERED));
            return;
        }
        registrationSaver.run();
        TournamentMessages.sendSuccess(player, Messages.text(RegistrationMessage.REGISTERED));
    }

    public void leave(Player player) {
        if (isClosed(player)) {
            return;
        }
        if (!registrationList.unregister(player.getUniqueId())) {
            TournamentMessages.sendWarning(player, Messages.text(RegistrationMessage.NOT_REGISTERED));
            return;
        }
        registrationSaver.run();
        TournamentMessages.sendSuccess(player, Messages.text(RegistrationMessage.UNREGISTERED));
    }

    public boolean removeRegistration(UUID uniqueId) {
        if (!registrationList.unregister(uniqueId)) {
            return false;
        }
        registrationSaver.run();
        return true;
    }

    public void clearRegistrations() {
        registrationList.clear();
        registrationSaver.run();
    }

    public List<RegistrationEntry> getEntries() {
        return registrationList.getEntries();
    }

    public int getCount() {
        return registrationList.getCount();
    }

    private boolean isClosed(Player player) {
        if (!tournamentInProgress.getAsBoolean()) {
            return false;
        }
        TournamentMessages.sendError(player, Messages.text(RegistrationMessage.CLOSED));
        return true;
    }
}

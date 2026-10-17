package de.simplepvparena.gui;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.tournament.KitChoicePresenter;
import de.simplepvparena.tournament.PendingKitChoice;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

final class KitChoiceAdminMenuPresenter implements KitChoicePresenter {

    private final AdminMenuNavigation navigation;
    private final Supplier<Collection<? extends Player>> onlinePlayers;

    KitChoiceAdminMenuPresenter(AdminMenuNavigation navigation,
                                Supplier<Collection<? extends Player>> onlinePlayers) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
    }

    @Override
    public void show(PendingKitChoice pendingKitChoice) {
        for (Player admin : findOnlineAdmins()) {
            navigation.open(admin, AdminMenuHolder.kitChoice(pendingKitChoice.matchId(), 0));
        }
    }

    @Override
    public void close(String message) {
        for (Player admin : findOnlineAdmins()) {
            if (navigation.isShowing(admin, AdminMenuType.KIT_CHOICE)) {
                navigation.close(admin);
            }
            TournamentMessages.sendInfo(admin, message);
        }
    }

    @Override
    public boolean hasAudience() {
        return !findOnlineAdmins().isEmpty();
    }

    private List<Player> findOnlineAdmins() {
        List<Player> onlineAdmins = new ArrayList<>();
        for (Player player : onlinePlayers.get()) {
            if (AdminPermission.isAdmin(player)) {
                onlineAdmins.add(player);
            }
        }
        return onlineAdmins;
    }
}

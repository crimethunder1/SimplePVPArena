package de.simplepvparena.gui;

import de.simplepvparena.registration.TeamChoicePresenter;
import de.simplepvparena.registration.TeamChoiceService;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class TeamChoiceMenuPresenter implements TeamChoicePresenter {

    private final AdminMenuNavigation navigation;
    private final TeamChoiceService teamChoiceService;
    private final Supplier<Collection<? extends Player>> onlinePlayers;
    private final Consumer<Runnable> nextTickScheduler;
    private final Map<UUID, AdminMenuHolder> openedHoldersByPlayer = new HashMap<>();

    TeamChoiceMenuPresenter(AdminMenuNavigation navigation, TeamChoiceService teamChoiceService,
                            Supplier<Collection<? extends Player>> onlinePlayers,
                            Consumer<Runnable> nextTickScheduler) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.teamChoiceService = Objects.requireNonNull(teamChoiceService, "teamChoiceService");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        this.nextTickScheduler = Objects.requireNonNull(nextTickScheduler, "nextTickScheduler");
    }

    @Override
    public void show(Player player) {
        UUID playerUniqueId = player.getUniqueId();
        nextTickScheduler.accept(() -> findOnlinePlayer(playerUniqueId)
                .filter(onlinePlayer -> teamChoiceService.findAccessRejection(playerUniqueId).isEmpty())
                .ifPresent(this::open));
    }

    @Override
    public void refresh() {
        for (Player player : onlinePlayers.get()) {
            AdminMenuHolder holder = openedHoldersByPlayer.get(player.getUniqueId());
            if (holder != null && navigation.isShowing(player, AdminMenuType.TEAM_CHOICE)) {
                navigation.refresh(player, holder);
            }
        }
    }

    void open(Player player) {
        AdminMenuHolder holder = AdminMenuHolder.teamChoice();
        openedHoldersByPlayer.put(player.getUniqueId(), holder);
        navigation.open(player, holder);
    }

    private Optional<Player> findOnlinePlayer(UUID playerUniqueId) {
        return onlinePlayers.get().stream()
                .filter(player -> player.getUniqueId().equals(playerUniqueId))
                .map(Player.class::cast)
                .findFirst();
    }
}

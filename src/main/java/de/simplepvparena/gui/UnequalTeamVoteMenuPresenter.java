package de.simplepvparena.gui;

import de.simplepvparena.match.PendingUnequalTeamVote;
import de.simplepvparena.match.UnequalTeamVotePresenter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

final class UnequalTeamVoteMenuPresenter implements UnequalTeamVotePresenter {

    private final AdminMenuNavigation navigation;
    private final Supplier<Collection<? extends Player>> onlinePlayers;
    private final Map<UUID, AdminMenuHolder> openedHoldersByPlayer = new HashMap<>();

    UnequalTeamVoteMenuPresenter(AdminMenuNavigation navigation,
                                 Supplier<Collection<? extends Player>> onlinePlayers) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
    }

    @Override
    public void show(PendingUnequalTeamVote vote) {
        for (Player voter : findOnlineVoters(vote)) {
            open(voter, vote);
        }
    }

    @Override
    public void refresh(PendingUnequalTeamVote vote) {
        for (Player voter : findOnlineVoters(vote)) {
            AdminMenuHolder holder = openedHoldersByPlayer.get(voter.getUniqueId());
            if (holder != null && navigation.isShowing(voter, AdminMenuType.UNEQUAL_TEAM_VOTE)) {
                navigation.refresh(voter, holder);
            }
        }
    }

    @Override
    public void close(PendingUnequalTeamVote vote) {
        for (Player player : onlinePlayers.get()) {
            if (openedHoldersByPlayer.containsKey(player.getUniqueId())
                    && navigation.isShowing(player, AdminMenuType.UNEQUAL_TEAM_VOTE)) {
                navigation.close(player);
            }
        }
        openedHoldersByPlayer.clear();
    }

    void open(Player voter, PendingUnequalTeamVote vote) {
        AdminMenuHolder holder = AdminMenuHolder.unequalTeamVote(vote.getMatchId());
        openedHoldersByPlayer.put(voter.getUniqueId(), holder);
        navigation.open(voter, holder);
    }

    private List<Player> findOnlineVoters(PendingUnequalTeamVote vote) {
        List<Player> onlineVoters = new ArrayList<>();
        for (Player player : onlinePlayers.get()) {
            if (vote.isVoter(player.getUniqueId())) {
                onlineVoters.add(player);
            }
        }
        return onlineVoters;
    }
}

package de.simplepvparena.gui;

import de.simplepvparena.match.PendingUnequalTeamVote;
import de.simplepvparena.match.UnequalTeamVoteService;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.message.UnequalTeamVoteMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class UnequalTeamVoteMenu implements AdminMenu {

    static final int FIRST_PLAYER_SLOT = 9;

    private final AdminMenuNavigation navigation;
    private final UnequalTeamVoteService voteService;

    UnequalTeamVoteMenu(AdminMenuNavigation navigation, UnequalTeamVoteService voteService) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.voteService = Objects.requireNonNull(voteService, "voteService");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(UnequalTeamVoteMessage.MENU_TITLE,
                Placeholder.of("match", holder.getSubject().orElseThrow()));
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        String matchId = holder.getSubject().orElseThrow();
        Optional<PendingUnequalTeamVote> vote = findVoteOfMenu(matchId);
        if (vote.isEmpty()) {
            canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(Messages.text(UnequalTeamVoteMessage.INFO_TITLE),
                    List.of(describeOverVote(matchId))));
            return;
        }
        List<UUID> voterUniqueIds = vote.get().getVoterUniqueIds();
        for (int voterIndex = 0; voterIndex < voterUniqueIds.size(); voterIndex++) {
            UUID voterUniqueId = voterUniqueIds.get(voterIndex);
            canvas.set(FIRST_PLAYER_SLOT + voterIndex, MenuItems.create(Material.PLAYER_HEAD,
                    vote.get().findVoterName(voterUniqueId),
                    Messages.plural(UnequalTeamVoteMessage.PLAYER_VOTES, vote.get().countVotesFor(voterUniqueId)),
                    Messages.text(UnequalTeamVoteMessage.PLAYER_LORE)));
        }
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(Messages.text(UnequalTeamVoteMessage.INFO_TITLE),
                List.of(Messages.plural(UnequalTeamVoteMessage.INFO_PLAYERS, vote.get().getFighterCount(),
                                Placeholder.of("online", vote.get().getVoterCount())),
                        Messages.plural(UnequalTeamVoteMessage.INFO_TIME_LEFT, vote.get().getSecondsLeft()),
                        Messages.text(UnequalTeamVoteMessage.INFO_HINT))));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        String matchId = holder.getSubject().orElseThrow();
        Optional<PendingUnequalTeamVote> vote = findVoteOfMenu(matchId);
        if (vote.isEmpty()) {
            navigation.close(player);
            TournamentMessages.sendWarning(player, describeOverVote(matchId));
            return;
        }
        int voterIndex = slot - FIRST_PLAYER_SLOT;
        List<UUID> voterUniqueIds = vote.get().getVoterUniqueIds();
        if (voterIndex < 0 || voterIndex >= voterUniqueIds.size()) {
            return;
        }
        voteService.vote(player.getUniqueId(), voterUniqueIds.get(voterIndex));
    }

    static String describeOverVote(String matchId) {
        return Messages.text(UnequalTeamVoteMessage.VOTE_OVER, Placeholder.of("match", matchId));
    }

    private Optional<PendingUnequalTeamVote> findVoteOfMenu(String matchId) {
        return voteService.getPending().filter(vote -> vote.getMatchId().equals(matchId));
    }
}

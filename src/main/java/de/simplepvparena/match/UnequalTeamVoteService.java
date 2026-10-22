package de.simplepvparena.match;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.message.UnequalTeamVoteMessage;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public final class UnequalTeamVoteService {

    private static final long TICKS_PER_SECOND = 20L;

    private final Plugin plugin;
    private final Function<UUID, Player> onlinePlayerLookup;
    private final RandomGenerator random;

    private UnequalTeamVotePresenter presenter = UnequalTeamVotePresenter.NONE;
    private PendingUnequalTeamVote pendingVote;
    private Match pendingMatch;
    private Team votingTeam;
    private Runnable pendingRoundStart;
    private BukkitTask secondTask;

    public UnequalTeamVoteService() {
        this.plugin = null;
        this.onlinePlayerLookup = null;
        this.random = null;
    }

    public UnequalTeamVoteService(Plugin plugin, Function<UUID, Player> onlinePlayerLookup, RandomGenerator random) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.onlinePlayerLookup = Objects.requireNonNull(onlinePlayerLookup, "onlinePlayerLookup");
        this.random = Objects.requireNonNull(random, "random");
    }

    public void setPresenter(UnequalTeamVotePresenter presenter) {
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    public void begin(Match match, UnequalTeamSettings settings, Runnable startRounds) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(startRounds, "startRounds");
        if (pendingVote != null) {
            throw new IllegalStateException("the players of match " + pendingVote.getMatchId()
                    + " are still voting, match " + match.getMatchId() + " cannot begin a second vote");
        }
        if (plugin == null || settings.mode() == UnequalTeamsMode.ALL) {
            startRounds.run();
            return;
        }
        Map<UUID, String> firstTeamOnline = collectOnlineMembers(match.getFirstTeam());
        Map<UUID, String> secondTeamOnline = collectOnlineMembers(match.getSecondTeam());
        if (firstTeamOnline.isEmpty() || secondTeamOnline.isEmpty()
                || firstTeamOnline.size() == secondTeamOnline.size()) {
            startRounds.run();
            return;
        }
        boolean firstTeamVotes = firstTeamOnline.size() > secondTeamOnline.size();
        Team largerTeam = firstTeamVotes ? match.getFirstTeam() : match.getSecondTeam();
        Team smallerTeam = match.getOpponentOf(largerTeam);
        Map<UUID, String> voters = firstTeamVotes ? firstTeamOnline : secondTeamOnline;
        int fighterCount = Math.min(firstTeamOnline.size(), secondTeamOnline.size());
        PendingUnequalTeamVote vote = new PendingUnequalTeamVote(match.getMatchId(), largerTeam.getTeamName(),
                smallerTeam.getTeamName(), fighterCount, voters, settings.voteSeconds());
        pendingVote = vote;
        pendingMatch = match;
        votingTeam = largerTeam;
        pendingRoundStart = startRounds;
        scheduleNextSecond(vote);
        String announcement = Messages.plural(UnequalTeamVoteMessage.BEGIN, vote.getSecondsLeft(),
                Placeholder.of("opponent", vote.getOpponentTeamName()));
        for (UUID voterUniqueId : vote.getVoterUniqueIds()) {
            findOnlinePlayer(voterUniqueId).ifPresent(voter -> TournamentMessages.sendInfo(voter, announcement));
        }
        plugin.getLogger().info("The players of " + vote.getVotingTeamName() + " vote which " + fighterCount
                + " of them fight in match " + vote.getMatchId());
        presenter.show(vote);
    }

    public boolean vote(UUID voterUniqueId, UUID candidateUniqueId) {
        PendingUnequalTeamVote vote = pendingVote;
        if (vote == null || !vote.castVote(voterUniqueId, candidateUniqueId)) {
            return false;
        }
        if (vote.hasEveryVoterVoted()) {
            finish(vote);
            return true;
        }
        presenter.refresh(vote);
        return true;
    }

    public void handlePlayerLeft(UUID playerUniqueId) {
        PendingUnequalTeamVote vote = pendingVote;
        if (vote == null || !vote.removeVoter(playerUniqueId)) {
            return;
        }
        if (vote.getVoterCount() <= vote.getFighterCount() || vote.hasEveryVoterVoted()) {
            finish(vote);
            return;
        }
        presenter.refresh(vote);
    }

    public void cancel() {
        PendingUnequalTeamVote vote = pendingVote;
        if (vote == null) {
            return;
        }
        clearPendingVote();
        presenter.close(vote);
    }

    public boolean isPending() {
        return pendingVote != null;
    }

    public Optional<PendingUnequalTeamVote> getPending() {
        return Optional.ofNullable(pendingVote);
    }

    public boolean isVoter(UUID playerUniqueId) {
        return pendingVote != null && pendingVote.isVoter(playerUniqueId);
    }

    private void countDown(PendingUnequalTeamVote vote) {
        if (pendingVote != vote) {
            return;
        }
        vote.countDownOneSecond();
        if (vote.getSecondsLeft() == 0) {
            finish(vote);
            return;
        }
        scheduleNextSecond(vote);
        presenter.refresh(vote);
    }

    private void finish(PendingUnequalTeamVote vote) {
        Match match = pendingMatch;
        Team team = votingTeam;
        Runnable startRounds = pendingRoundStart;
        match.chooseFighters(team, vote.chooseFighters(random));
        clearPendingVote();
        presenter.close(vote);
        startRounds.run();
    }

    private void clearPendingVote() {
        if (secondTask != null) {
            secondTask.cancel();
        }
        secondTask = null;
        pendingVote = null;
        pendingMatch = null;
        votingTeam = null;
        pendingRoundStart = null;
    }

    private void scheduleNextSecond(PendingUnequalTeamVote vote) {
        secondTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> countDown(vote),
                TICKS_PER_SECOND);
    }

    private Map<UUID, String> collectOnlineMembers(Team team) {
        Map<UUID, String> onlineMembers = new LinkedHashMap<>();
        for (TeamMember member : team.getMembers()) {
            findOnlinePlayer(member.uniqueId()).ifPresent(player -> onlineMembers.put(member.uniqueId(),
                    player.getName()));
        }
        return onlineMembers;
    }

    private Optional<Player> findOnlinePlayer(UUID playerUniqueId) {
        Player player = onlinePlayerLookup.apply(playerUniqueId);
        if (player == null || !player.isOnline()) {
            return Optional.empty();
        }
        return Optional.of(player);
    }
}

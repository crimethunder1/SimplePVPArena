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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.random.RandomGenerator;

@NullMarked
public final class UnequalTeamVoteService {

    private static final long TICKS_PER_SECOND = 20L;

    private final @Nullable Plugin plugin;
    private final @Nullable Function<UUID, Player> onlinePlayerLookup;
    private final @Nullable RandomGenerator random;

    private UnequalTeamVotePresenter presenter = UnequalTeamVotePresenter.NONE;
    private @Nullable PendingUnequalTeamVote pendingVote;
    private @Nullable Match pendingMatch;
    private @Nullable Team votingTeam;
    private @Nullable Runnable pendingRoundStart;
    private @Nullable BukkitTask secondTask;

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
        Plugin votePlugin = plugin;
        if (votePlugin == null || settings.mode() == UnequalTeamsMode.ALL) {
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
        votePlugin.getLogger().info("The players of " + vote.getVotingTeamName() + " vote which " + fighterCount
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

    public boolean isLeftOutOfThePendingVote(Match match, UUID playerUniqueId) {
        PendingUnequalTeamVote vote = pendingVote;
        Team team = votingTeam;
        return vote != null && team != null && pendingMatch == match && team.containsPlayer(playerUniqueId)
                && !vote.isVoter(playerUniqueId);
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
        Match match = Objects.requireNonNull(pendingMatch, "pendingMatch");
        Team team = Objects.requireNonNull(votingTeam, "votingTeam");
        Runnable startRounds = Objects.requireNonNull(pendingRoundStart, "pendingRoundStart");
        match.chooseFighters(team, vote.chooseFighters(Objects.requireNonNull(random, "random")));
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
        Plugin votePlugin = Objects.requireNonNull(plugin, "plugin");
        secondTask = votePlugin.getServer().getScheduler().runTaskLater(votePlugin, () -> countDown(vote),
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
        Player player = Objects.requireNonNull(onlinePlayerLookup, "onlinePlayerLookup").apply(playerUniqueId);
        if (player == null || !player.isOnline()) {
            return Optional.empty();
        }
        return Optional.of(player);
    }
}

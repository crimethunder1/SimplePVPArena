package de.simplepvparena.match;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.TickingScheduler;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnequalTeamVoteServiceTest {

    private static final UnequalTeamSettings VOTE_OF_THIRTY_SECONDS = UnequalTeamSettings.defaults();
    private static final String BEGIN_MESSAGE = "Your team has more players online than Bravo: vote within 30 "
            + "seconds who fights. /tournament vote opens the vote again";

    private final TickingScheduler scheduler = new TickingScheduler();
    private final Map<UUID, FakePlayer> playersByUniqueId = new LinkedHashMap<>();
    private final RecordingUnequalTeamVotePresenter presenter = new RecordingUnequalTeamVotePresenter();
    private final SwitchableRandom random = new SwitchableRandom();
    private final UnequalTeamVoteService voteService = new UnequalTeamVoteService(scheduler.plugin(),
            this::findPlayer, random);

    private int roundStartCount;

    UnequalTeamVoteServiceTest() {
        SharedBukkitServer.install();
        voteService.setPresenter(presenter);
    }

    @Test
    void letsTheLargerTeamOfTwoAgainstOneVoteForOneFighter() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        FakePlayer cleo = addPlayer("Cleo");
        Match match = createMatch(team("Alpha", anna, ben), team("Bravo", cleo));

        begin(match);

        PendingUnequalTeamVote vote = voteService.getPending().orElseThrow();
        assertEquals("Alpha", vote.getVotingTeamName());
        assertEquals("Bravo", vote.getOpponentTeamName());
        assertEquals("M1", vote.getMatchId());
        assertEquals(1, vote.getFighterCount());
        assertEquals(List.of(anna.uniqueId, ben.uniqueId), vote.getVoterUniqueIds());
        assertEquals(30, vote.getSecondsLeft());
        assertEquals(List.of(vote), presenter.shownVotes());
        assertEquals(0, roundStartCount, "the rounds wait for the vote");
        assertEquals(List.of(BEGIN_MESSAGE), anna.messages);
        assertEquals(List.of(BEGIN_MESSAGE), ben.messages);
        assertEquals(List.of(), cleo.messages, "the smaller team does not vote");
        assertTrue(voteService.isVoter(anna.uniqueId));
        assertFalse(voteService.isVoter(cleo.uniqueId));
    }

    @Test
    void letsTheSecondTeamVoteWhenItIsTheLargerOne() {
        FakePlayer anna = addPlayer("Anna");
        Match match = createMatch(team("Alpha", anna), team("Bravo", addPlayer("Ben"), addPlayer("Cleo"),
                addPlayer("Dora")));

        begin(match);

        PendingUnequalTeamVote vote = voteService.getPending().orElseThrow();
        assertEquals("Bravo", vote.getVotingTeamName());
        assertEquals("Alpha", vote.getOpponentTeamName());
        assertEquals(1, vote.getFighterCount());
        assertEquals(3, vote.getVoterCount());
        assertEquals(List.of(), anna.messages);
    }

    @Test
    void givesFourAgainstTwoTwoPlacesAndThreeAgainstTwoTwoPlaces() {
        begin(createMatch(team("Alpha", addPlayer("A1"), addPlayer("A2"), addPlayer("A3"), addPlayer("A4")),
                team("Bravo", addPlayer("B1"), addPlayer("B2"))));
        assertEquals(2, voteService.getPending().orElseThrow().getFighterCount());
        voteService.cancel();

        begin(createMatch(team("Charlie", addPlayer("C1"), addPlayer("C2"), addPlayer("C3")),
                team("Delta", addPlayer("D1"), addPlayer("D2"))));
        assertEquals(2, voteService.getPending().orElseThrow().getFighterCount());
        assertEquals(3, voteService.getPending().orElseThrow().getVoterCount());
    }

    @Test
    void countsOnlyTheOnlineMembersOfBothTeams() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        FakePlayer cleo = addPlayer("Cleo");
        FakePlayer dora = addPlayer("Dora");
        ben.online = false;
        Match match = createMatch(team("Alpha", anna, ben, cleo), team("Bravo", dora));

        begin(match);

        assertEquals(List.of(anna.uniqueId, cleo.uniqueId), voteService.getPending().orElseThrow()
                .getVoterUniqueIds());
    }

    @Test
    void startsTheRoundsAtOnceWhenBothTeamsHaveAsManyPlayersOnline() {
        FakePlayer offline = addPlayer("Offline");
        offline.online = false;
        Match match = createMatch(team("Alpha", addPlayer("Anna"), addPlayer("Ben"), offline),
                team("Bravo", addPlayer("Cleo"), addPlayer("Dora")));

        begin(match);

        assertEquals(1, roundStartCount);
        assertFalse(voteService.isPending());
        assertEquals(List.of(), presenter.shownVotes());
        assertTrue(match.findTeamWithChosenFighters().isEmpty());
        assertEquals(0, scheduler.pendingTaskCount());
    }

    @Test
    void startsTheRoundsAtOnceWhenOneTeamHasNobodyOnline() {
        FakePlayer offline = addPlayer("Offline");
        offline.online = false;
        Match match = createMatch(team("Alpha", addPlayer("Anna"), addPlayer("Ben")), team("Bravo", offline));

        begin(match);

        assertEquals(1, roundStartCount);
        assertFalse(voteService.isPending());
    }

    @Test
    void startsTheRoundsAtOnceInTheModeAll() {
        FakePlayer anna = addPlayer("Anna");
        Match match = createMatch(team("Alpha", anna, addPlayer("Ben")), team("Bravo", addPlayer("Cleo")));

        voteService.begin(match, new UnequalTeamSettings(UnequalTeamsMode.ALL, 30), () -> roundStartCount++);

        assertEquals(1, roundStartCount);
        assertFalse(voteService.isPending());
        assertEquals(List.of(), anna.messages);
        assertTrue(match.findTeamWithChosenFighters().isEmpty());
    }

    @Test
    void startsTheRoundsAtOnceWithoutAPluginToVoteIn() {
        UnequalTeamVoteService serviceWithoutPlugin = new UnequalTeamVoteService();
        Match match = createMatch(team("Alpha", addPlayer("Anna"), addPlayer("Ben")), team("Bravo",
                addPlayer("Cleo")));

        serviceWithoutPlugin.begin(match, VOTE_OF_THIRTY_SECONDS, () -> roundStartCount++);

        assertEquals(1, roundStartCount);
        assertFalse(serviceWithoutPlugin.isPending());
    }

    @Test
    void refusesASecondVoteWhileOneIsRunning() {
        begin(createMatch(team("Alpha", addPlayer("Anna"), addPlayer("Ben")), team("Bravo", addPlayer("Cleo"))));
        Match secondMatch = createMatch(team("Charlie", addPlayer("Dora"), addPlayer("Emil")),
                team("Delta", addPlayer("Finn")));

        assertThrows(IllegalStateException.class, () -> begin(secondMatch));
    }

    @Test
    void countsTheVotesAndLetsEveryVoterChangeTheVoteUntilTheEnd() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        FakePlayer cleo = addPlayer("Cleo");
        begin(createMatch(team("Alpha", anna, ben, cleo), team("Bravo", addPlayer("Dora"))));
        PendingUnequalTeamVote vote = voteService.getPending().orElseThrow();

        assertTrue(voteService.vote(anna.uniqueId, anna.uniqueId));
        assertTrue(voteService.vote(ben.uniqueId, anna.uniqueId));

        assertEquals(2, vote.countVotesFor(anna.uniqueId));
        assertEquals(0, vote.countVotesFor(ben.uniqueId));

        assertTrue(voteService.vote(ben.uniqueId, cleo.uniqueId));

        assertEquals(1, vote.countVotesFor(anna.uniqueId));
        assertEquals(1, vote.countVotesFor(cleo.uniqueId));
        assertEquals(cleo.uniqueId, vote.findVoteOf(ben.uniqueId).orElseThrow());
        assertTrue(vote.findVoteOf(cleo.uniqueId).isEmpty());
        assertEquals(3, presenter.refreshCount(), "every vote refreshes the open menus");
        assertEquals(0, roundStartCount);
    }

    @Test
    void refusesAVoteOfAPlayerOutsideOfTheVotingTeamAndAVoteForSuchAPlayer() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer cleo = addPlayer("Cleo");
        begin(createMatch(team("Alpha", anna, addPlayer("Ben")), team("Bravo", cleo)));

        assertFalse(voteService.vote(cleo.uniqueId, anna.uniqueId));
        assertFalse(voteService.vote(anna.uniqueId, cleo.uniqueId));
        assertFalse(voteService.vote(UUID.randomUUID(), anna.uniqueId));

        assertEquals(0, voteService.getPending().orElseThrow().countVotesFor(anna.uniqueId));
        assertTrue(voteService.getPending().orElseThrow().findVoteOf(anna.uniqueId).isEmpty());
    }

    @Test
    void endsAsSoonAsEveryVoterHasVotedAndChoosesThePlayerWithTheMostVotes() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        FakePlayer cleo = addPlayer("Cleo");
        Team alpha = team("Alpha", anna, ben, cleo);
        Match match = createMatch(alpha, team("Bravo", addPlayer("Dora")));
        begin(match);
        PendingUnequalTeamVote vote = voteService.getPending().orElseThrow();

        voteService.vote(anna.uniqueId, ben.uniqueId);
        voteService.vote(ben.uniqueId, ben.uniqueId);
        voteService.vote(cleo.uniqueId, anna.uniqueId);

        assertEquals(1, roundStartCount);
        assertFalse(voteService.isPending());
        assertEquals(List.of(ben.uniqueId), match.findChosenFighters(alpha).orElseThrow());
        assertSame(alpha, match.findTeamWithChosenFighters().orElseThrow());
        assertEquals(List.of(vote), presenter.closedVotes());
        assertEquals(0, scheduler.pendingTaskCount(), "the countdown of the vote is stopped");
        assertFalse(voteService.vote(anna.uniqueId, anna.uniqueId), "the vote is over");
    }

    @Test
    void endsAfterTheVoteSecondsAndLetsChanceFillThePlacesNobodyVotedFor() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        FakePlayer cleo = addPlayer("Cleo");
        FakePlayer dora = addPlayer("Dora");
        Team alpha = team("Alpha", anna, ben, cleo, dora);
        Match match = createMatch(alpha, team("Bravo", addPlayer("Emil"), addPlayer("Finn")));
        begin(match);
        voteService.vote(anna.uniqueId, cleo.uniqueId);
        random.drawZero();

        scheduler.advanceSeconds(29);

        assertEquals(0, roundStartCount);
        assertEquals(1, voteService.getPending().orElseThrow().getSecondsLeft());

        scheduler.advanceSeconds(1);

        assertEquals(1, roundStartCount);
        assertFalse(voteService.isPending());
        assertEquals(List.of(cleo.uniqueId, ben.uniqueId), match.findChosenFighters(alpha).orElseThrow(),
                "Cleo has the most votes, chance draws one of the three players without a vote");
    }

    @Test
    void letsChanceDecideATieThroughTheReplaceableSourceOfChance() {
        assertEquals("Ben", decideTieOfTwoWith(true));
        assertEquals("Anna", decideTieOfTwoWith(false));
    }

    @Test
    void takesEveryPlayerOfATieThatFitsIntoThePlacesWithoutChance() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        FakePlayer cleo = addPlayer("Cleo");
        FakePlayer dora = addPlayer("Dora");
        Team alpha = team("Alpha", anna, ben, cleo, dora);
        Match match = createMatch(alpha, team("Bravo", addPlayer("Emil"), addPlayer("Finn")));
        begin(match);
        random.drawZero();

        voteService.vote(anna.uniqueId, dora.uniqueId);
        voteService.vote(ben.uniqueId, dora.uniqueId);
        voteService.vote(cleo.uniqueId, ben.uniqueId);
        voteService.vote(dora.uniqueId, ben.uniqueId);

        assertEquals(List.of(ben.uniqueId, dora.uniqueId), match.findChosenFighters(alpha).orElseThrow());
    }

    @Test
    void refreshesTheMenusOnceEverySecondOfTheVote() {
        begin(createMatch(team("Alpha", addPlayer("Anna"), addPlayer("Ben")), team("Bravo", addPlayer("Cleo"))));

        scheduler.advanceSeconds(3);

        assertEquals(3, presenter.refreshCount());
        assertEquals(27, voteService.getPending().orElseThrow().getSecondsLeft());
    }

    @Test
    void endsAfterTheConfiguredVoteSeconds() {
        begin(createMatch(team("Alpha", addPlayer("Anna"), addPlayer("Ben")), team("Bravo", addPlayer("Cleo"))),
                new UnequalTeamSettings(UnequalTeamsMode.VOTE, 5));

        scheduler.advanceSeconds(4);
        assertTrue(voteService.isPending());

        scheduler.advanceSeconds(1);
        assertFalse(voteService.isPending());
        assertEquals(1, roundStartCount);
    }

    @Test
    void forgetsAVoterWhoLeavesTogetherWithTheVoteOfAndForThatPlayer() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        FakePlayer cleo = addPlayer("Cleo");
        FakePlayer dora = addPlayer("Dora");
        Team alpha = team("Alpha", anna, ben, cleo, dora);
        Match match = createMatch(alpha, team("Bravo", addPlayer("Emil")));
        begin(match);
        PendingUnequalTeamVote vote = voteService.getPending().orElseThrow();
        voteService.vote(anna.uniqueId, dora.uniqueId);
        voteService.vote(dora.uniqueId, anna.uniqueId);
        voteService.vote(ben.uniqueId, cleo.uniqueId);

        voteService.handlePlayerLeft(dora.uniqueId);

        assertTrue(voteService.isPending());
        assertFalse(vote.isVoter(dora.uniqueId));
        assertEquals(3, vote.getVoterCount());
        assertEquals(1, vote.getFighterCount(), "the number of fighters stays");
        assertEquals(0, vote.countVotesFor(anna.uniqueId), "the vote of Dora no longer counts");
        assertTrue(vote.findVoteOf(anna.uniqueId).isEmpty(), "Anna voted for Dora and may vote again");
        assertFalse(voteService.vote(anna.uniqueId, dora.uniqueId));

        voteService.vote(anna.uniqueId, cleo.uniqueId);
        voteService.vote(cleo.uniqueId, anna.uniqueId);

        assertEquals(1, roundStartCount, "every remaining voter has voted");
        assertEquals(List.of(cleo.uniqueId), match.findChosenFighters(alpha).orElseThrow());
    }

    @Test
    void endsWhenEveryRemainingVoterHasVotedOnceTheLastMissingVoterLeaves() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        FakePlayer cleo = addPlayer("Cleo");
        Team alpha = team("Alpha", anna, ben, cleo);
        Match match = createMatch(alpha, team("Bravo", addPlayer("Dora")));
        begin(match);
        voteService.vote(anna.uniqueId, ben.uniqueId);
        voteService.vote(ben.uniqueId, ben.uniqueId);

        voteService.handlePlayerLeft(cleo.uniqueId);

        assertEquals(1, roundStartCount);
        assertEquals(List.of(ben.uniqueId), match.findChosenFighters(alpha).orElseThrow());
    }

    @Test
    void letsEveryRemainingPlayerFightOnceNoMoreThanThePlacesAreLeft() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        FakePlayer cleo = addPlayer("Cleo");
        Team alpha = team("Alpha", anna, ben, cleo);
        Match match = createMatch(alpha, team("Bravo", addPlayer("Dora"), addPlayer("Emil")));
        begin(match);
        voteService.vote(anna.uniqueId, cleo.uniqueId);

        voteService.handlePlayerLeft(cleo.uniqueId);

        assertEquals(1, roundStartCount);
        assertEquals(List.of(anna.uniqueId, ben.uniqueId), match.findChosenFighters(alpha).orElseThrow());
    }

    @Test
    void endsAtOnceWhenOnlyAsManyVotersAsPlacesAreLeft() {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        Team alpha = team("Alpha", anna, ben);
        Match match = createMatch(alpha, team("Bravo", addPlayer("Cleo")));
        begin(match);

        voteService.handlePlayerLeft(anna.uniqueId);

        assertEquals(1, roundStartCount);
        assertEquals(List.of(ben.uniqueId), match.findChosenFighters(alpha).orElseThrow());
    }

    @Test
    void keepsTheVoteAndItsPlacesWhenAPlayerOfTheSmallerTeamLeaves() {
        FakePlayer cleo = addPlayer("Cleo");
        begin(createMatch(team("Alpha", addPlayer("Anna"), addPlayer("Ben"), addPlayer("Dora")),
                team("Bravo", cleo, addPlayer("Emil"))));
        int refreshesBefore = presenter.refreshCount();

        voteService.handlePlayerLeft(cleo.uniqueId);

        PendingUnequalTeamVote vote = voteService.getPending().orElseThrow();
        assertEquals(2, vote.getFighterCount());
        assertEquals(3, vote.getVoterCount());
        assertEquals(refreshesBefore, presenter.refreshCount());
        assertEquals(0, roundStartCount);
    }

    @Test
    void cancelsTheVoteWithoutStartingTheRounds() {
        FakePlayer anna = addPlayer("Anna");
        Match match = createMatch(team("Alpha", anna, addPlayer("Ben")), team("Bravo", addPlayer("Cleo")));
        begin(match);
        PendingUnequalTeamVote vote = voteService.getPending().orElseThrow();

        voteService.cancel();
        scheduler.advanceSeconds(30);

        assertFalse(voteService.isPending());
        assertEquals(0, roundStartCount);
        assertEquals(List.of(vote), presenter.closedVotes());
        assertTrue(match.findTeamWithChosenFighters().isEmpty());
        assertEquals(0, scheduler.pendingTaskCount());
        assertFalse(voteService.vote(anna.uniqueId, anna.uniqueId));
    }

    @Test
    void cancelsNothingWithoutARunningVote() {
        voteService.cancel();

        assertEquals(List.of(), presenter.closedVotes());
    }

    @Test
    @UseLanguage("de")
    void announcesTheVoteInGerman() {
        FakePlayer anna = addPlayer("Anna");
        begin(createMatch(team("Alpha", anna, addPlayer("Ben")), team("Bravo", addPlayer("Cleo"))),
                new UnequalTeamSettings(UnequalTeamsMode.VOTE, 30));

        assertEquals(List.of("Dein Team hat mehr Spieler online als Bravo: Stimme innerhalb von 30 Sekunden ab, wer "
                + "kämpft. /tournament vote öffnet die Abstimmung wieder"), anna.messages);
    }

    private String decideTieOfTwoWith(boolean drawZero) {
        FakePlayer anna = addPlayer("Anna");
        FakePlayer ben = addPlayer("Ben");
        Team alpha = team("Alpha", anna, ben);
        Match match = createMatch(alpha, team("Bravo", addPlayer("Cleo")));
        if (drawZero) {
            random.drawZero();
        } else {
            random.keepOrder();
        }
        begin(match);

        voteService.vote(anna.uniqueId, ben.uniqueId);
        voteService.vote(ben.uniqueId, anna.uniqueId);

        UUID fighter = match.findChosenFighters(alpha).orElseThrow().getFirst();
        return playersByUniqueId.get(fighter).name;
    }

    private void begin(Match match) {
        begin(match, VOTE_OF_THIRTY_SECONDS);
    }

    private void begin(Match match, UnequalTeamSettings settings) {
        voteService.begin(match, settings, () -> roundStartCount++);
    }

    private Match createMatch(Team firstTeam, Team secondTeam) {
        return Match.regularMatch("M1", firstTeam, secondTeam, new ArenaRegistry().getArena());
    }

    private static Team team(String teamName, FakePlayer... players) {
        List<TeamMember> members = new ArrayList<>();
        for (FakePlayer player : players) {
            members.add(new TeamMember(player.uniqueId, player.name));
        }
        return new Team(teamName, members);
    }

    private FakePlayer addPlayer(String playerName) {
        FakePlayer player = new FakePlayer(playerName);
        playersByUniqueId.put(player.uniqueId, player);
        return player;
    }

    private Player findPlayer(UUID playerUniqueId) {
        FakePlayer player = playersByUniqueId.get(playerUniqueId);
        return player == null ? null : player.player;
    }

    private static final class FakePlayer {

        private final UUID uniqueId = UUID.randomUUID();
        private final String name;
        private final List<String> messages = new ArrayList<>();
        private final Player player;

        private boolean online = true;

        private FakePlayer(String name) {
            this.name = name;
            this.player = SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
                case "getUniqueId" -> uniqueId;
                case "getName" -> name;
                case "isOnline" -> online;
                case "sendMessage" -> {
                    messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                    yield null;
                }
                default -> throw new UnsupportedOperationException("Player." + method.getName());
            });
        }
    }

    private static final class SwitchableRandom implements RandomGenerator {

        private boolean drawingZero;

        private void drawZero() {
            drawingZero = true;
        }

        private void keepOrder() {
            drawingZero = false;
        }

        @Override
        public int nextInt(int bound) {
            return drawingZero ? 0 : bound - 1;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("only bounded integers are drawn");
        }
    }
}

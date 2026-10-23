package de.simplepvparena.listener;

import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.match.Match;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.team.Team;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnequalTeamRejoinTest {

    private final UnequalTeamVoteHarness harness = new UnequalTeamVoteHarness();
    private final PlayerConnectionListener listener = new PlayerConnectionListener(harness.matchService(),
            harness.arena().getSpectatorService(), harness.arena().getArenaRegistry(),
            new BorderSelectorModeRegistry(), harness.arena().getKitService(), harness.arena().getPlugin().getLogger());

    @Test
    void aPlayerOfTheLargerTeamWhoWasNotChosenHearsThatTheTeamFightsWithTheChosenPlayers() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben", "Cleo");
        harness.addTeam("Bravo", "Dora", "Emil");
        harness.startTheFinalOfTwoTeams();
        harness.vote(alpha.get(0), alpha.get(0));
        harness.vote(alpha.get(1), alpha.get(1));
        harness.vote(alpha.get(2), alpha.get(0));
        ArenaMatchHarness.FakePlayer cleo = alpha.get(2);
        leave(cleo);

        rejoin(cleo);

        assertEquals(List.of("You came back during match " + harness.runningMatch().getMatchId()
                + ", your team fights with the chosen players"), cleo.messages());
    }

    @Test
    void aPlayerWhoMissedTheBeginningOfTheVoteIsNotChosenEither() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben", "Cleo");
        harness.addTeam("Bravo", "Dora");
        ArenaMatchHarness.FakePlayer cleo = alpha.get(2);
        cleo.setOnline(false);
        harness.startTheFinalOfTwoTeams();
        assertTrue(harness.voteService().isPending());

        rejoin(cleo);

        assertEquals(List.of("You came back during match " + harness.runningMatch().getMatchId()
                + ", your team fights with the chosen players"), cleo.messages());
    }

    @Test
    @UseLanguage("de")
    void tellsTheReturningPlayerInGermanThatTheTeamFightsWithTheChosenPlayers() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben", "Cleo");
        harness.addTeam("Bravo", "Dora");
        ArenaMatchHarness.FakePlayer cleo = alpha.get(2);
        cleo.setOnline(false);
        harness.startTheFinalOfTwoTeams();

        rejoin(cleo);

        assertEquals(List.of("Du bist während Match " + harness.runningMatch().getMatchId()
                + " zurückgekehrt, dein Team kämpft mit den gewählten Spielern"), cleo.messages());
    }

    @Test
    void aPlayerOfTheSmallerTeamStillHearsThatTheyTakePartFromTheNextRoundOn() {
        harness.addTeam("Alpha", "Anna", "Ben");
        List<ArenaMatchHarness.FakePlayer> bravo = harness.addTeam("Bravo", "Cleo", "Dora");
        ArenaMatchHarness.FakePlayer dora = bravo.get(1);
        dora.setOnline(false);
        harness.startTheFinalOfTwoTeams();

        rejoin(dora);

        assertEquals(List.of("You rejoined during match " + harness.runningMatch().getMatchId()
                + ", you take part again from the next round on"), dora.messages());
    }

    @Test
    void anEmptySelectionLetsEveryOnlinePlayerOfTheLargerTeamFightFromTheNextRoundOn() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        List<ArenaMatchHarness.FakePlayer> bravo = harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();
        harness.vote(alpha.get(0), alpha.get(1));
        harness.vote(alpha.get(1), alpha.get(1));
        Match match = harness.runningMatch();
        Team alphaTeam = match.findTeamByName("Alpha").orElseThrow();
        assertEquals(Set.of(alpha.get(1).uniqueId()), harness.activePlayersOf("Alpha"));

        match.chooseFighters(alphaTeam, List.of());
        harness.eliminate(bravo.get(0));

        assertTrue(match.findTeamWithChosenFighters().isEmpty());
        assertEquals(Set.of(alpha.get(0).uniqueId(), alpha.get(1).uniqueId()), harness.activePlayersOf("Alpha"));
        assertFalse(harness.matchService().isLeftOutOfTheChosenFighters(match, alpha.get(0).uniqueId()));
    }

    private void leave(ArenaMatchHarness.FakePlayer player) {
        player.setOnline(false);
        harness.matchService().handlePlayerDisconnected(player.player());
    }

    private void rejoin(ArenaMatchHarness.FakePlayer player) {
        player.setOnline(true);
        player.forgetReceivedTexts();
        listener.onPlayerJoin(new PlayerJoinEvent(player.player(), Component.empty()));
    }
}

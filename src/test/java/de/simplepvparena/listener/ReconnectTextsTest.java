package de.simplepvparena.listener;

import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.MatchMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconnectTextsTest {

    private static final double FAR_AWAY = TournamentAudienceHarness.FAR_AWAY;

    private ArenaMatchHarness harness;
    private MatchService matchService;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;
    private ArenaMatchHarness.FakePlayer bystander;
    private ArenaMatchHarness.FakePlayer outsider;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        harness.useArenaAudience();
        matchService = harness.getMatchService();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
        bystander = harness.addPlayer("Bystander");
        outsider = harness.addPlayer("Outsider");
        outsider.moveTo(harness.at(FAR_AWAY, FAR_AWAY));
        harness.startMatch(fighter, mate, opponent, opponentMate);
        bystander.forgetReceivedTexts();
    }

    @Test
    void thePlayersInTheAreaReadTheLossTheReturnAndTheEliminationInEnglish() {
        disconnect(fighter);
        fighter.setOnline(true);
        matchService.handlePlayerReturned(fighter.player());
        disconnect(mate);
        harness.advanceSeconds(60);

        assertEquals(List.of(
                "Fighter lost the connection, 60 seconds to come back",
                "Fighter is back",
                "Mate lost the connection, 60 seconds to come back",
                "Mate did not come back and is eliminated"), bystander.messages());
        assertEquals(List.of(), outsider.messages());
    }

    @Test
    void writesOneSecondInTheSingular() {
        matchService.setReconnectSeconds(1);

        disconnect(fighter);

        assertEquals(List.of("Fighter lost the connection, 1 second to come back"), bystander.messages());
        assertEquals("Fighter lost the connection, 2 seconds to come back",
                Messages.plural(MatchMessage.RECONNECT_CONNECTION_LOST, 2, Placeholder.of("player", "Fighter")));
    }

    private void disconnect(ArenaMatchHarness.FakePlayer player) {
        matchService.handlePlayerDisconnected(player.player());
        player.setOnline(false);
    }
}

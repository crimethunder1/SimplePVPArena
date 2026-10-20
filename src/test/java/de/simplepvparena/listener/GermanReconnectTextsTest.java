package de.simplepvparena.listener;

import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.MatchMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanReconnectTextsTest {

    private ArenaMatchHarness harness;
    private MatchService matchService;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer bystander;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        harness.useArenaAudience();
        matchService = harness.getMatchService();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        bystander = harness.addPlayer("Bystander");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        bystander.forgetReceivedTexts();
    }

    @Test
    void thePlayersInTheAreaReadTheLossTheReturnAndTheEliminationInGerman() {
        disconnect(fighter);
        fighter.setOnline(true);
        matchService.handlePlayerReturned(fighter.player());
        disconnect(mate);
        harness.advanceSeconds(60);

        assertEquals(List.of(
                "Fighter hat die Verbindung verloren, 60 Sekunden zum Zurückkommen",
                "Fighter ist zurück",
                "Mate hat die Verbindung verloren, 60 Sekunden zum Zurückkommen",
                "Mate ist nicht zurückgekommen und scheidet aus"), bystander.messages());
    }

    @Test
    void writesOneSecondInTheSingularInGerman() {
        matchService.setReconnectSeconds(1);

        disconnect(fighter);

        assertEquals(List.of("Fighter hat die Verbindung verloren, 1 Sekunde zum Zurückkommen"),
                bystander.messages());
        assertEquals("Fighter hat die Verbindung verloren, 2 Sekunden zum Zurückkommen",
                Messages.plural(MatchMessage.RECONNECT_CONNECTION_LOST, 2, Placeholder.of("player", "Fighter")));
    }

    private void disconnect(ArenaMatchHarness.FakePlayer player) {
        matchService.handlePlayerDisconnected(player.player());
        player.setOnline(false);
    }
}

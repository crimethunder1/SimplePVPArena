package de.simplepvparena.listener;

import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.AutoMatchMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoMatchTextsTest {

    @Test
    void writesTheAnnouncementInTheSingularAndThePlural() {
        assertEquals("Auto match: Alpha against Bravo begins in 1 second", announcementOf(1));
        assertEquals("Auto match: Alpha against Bravo begins in 3 seconds", announcementOf(3));
    }

    @Test
    void namesEveryReasonOfACancellationInAWholeSentence() {
        assertEquals(List.of("Auto match cancelled: a team has no player online any more",
                "Auto match cancelled: the mode was switched off",
                "Auto match cancelled: a tournament is starting",
                "Auto match cancelled: the arena is not ready"), cancellations());
    }

    @Test
    @UseLanguage("de")
    void writesTheAnnouncementAndTheCancellationsInGerman() {
        assertEquals("Auto-Match: Alpha gegen Bravo beginnt in 1 Sekunde", announcementOf(1));
        assertEquals("Auto-Match: Alpha gegen Bravo beginnt in 10 Sekunden", announcementOf(10));
        assertEquals(List.of("Auto-Match abgebrochen: ein Team hat keinen Spieler mehr online",
                "Auto-Match abgebrochen: der Modus wurde ausgeschaltet",
                "Auto-Match abgebrochen: ein Turnier beginnt",
                "Auto-Match abgebrochen: die Arena ist nicht bereit"), cancellations());
        assertEquals("Ein Auto-Match beginnt gleich, die Arena ist dafür reserviert",
                Messages.text(ArenaSetupMessage.ARENA_RESERVED_FOR_AUTO_MATCH));
    }

    @Test
    @UseLanguage("de")
    void announcesAndRefusesInGermanThroughTheWholeFlow() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();

        harness.advanceSeconds(1);

        assertEquals(List.of("Auto-Match: Alpha gegen Bravo beginnt in 10 Sekunden"), harness.messagesOf("Alpha"));
        assertEquals(Optional.of("Team Alpha spielt gleich ein Auto-Match, es kann erst nach dem Ende des Matches "
                + "geändert werden"), harness.tournamentManager().findTeamChangeRejection("Alpha"));

        harness.advanceSeconds(AutoMatchHarness.START_DELAY_SECONDS);
        TournamentOperationResult start = harness.tournamentManager().startTournament(false);

        assertEquals("Ein Auto-Match läuft, starte das Turnier, wenn es vorbei ist", start.message());
    }

    private static String announcementOf(int seconds) {
        return Messages.plural(AutoMatchMessage.ANNOUNCEMENT, seconds, Placeholder.of("firstTeam", "Alpha"),
                Placeholder.of("secondTeam", "Bravo"));
    }

    private static List<String> cancellations() {
        return List.of(Messages.text(AutoMatchMessage.CANCELLED_TEAM_OFFLINE),
                Messages.text(AutoMatchMessage.CANCELLED_MODE_SWITCHED_OFF),
                Messages.text(AutoMatchMessage.CANCELLED_TOURNAMENT_STARTING),
                Messages.text(AutoMatchMessage.CANCELLED_ARENA_NOT_READY));
    }
}

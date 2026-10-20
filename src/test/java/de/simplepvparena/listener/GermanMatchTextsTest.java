package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.message.MatchMenuMessage;
import de.simplepvparena.message.MatchMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UseLanguage("de")
class GermanMatchTextsTest {

    private static final RoundTimeoutSettings TWO_SECOND_ROUND_WITH_A_WARNING =
            new RoundTimeoutSettings(2, 1, 0.0D, 0.0D);
    private static final RoundTimeoutSettings ONE_SECOND_ROUND = new RoundTimeoutSettings(1, 0, 2.0D, 1.0D);

    @Test
    void announcesTheMatchesTheRoundsTheResultsAndTheWinnerOfTheTournament() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        Match firstMatch = tournament.runningMatch();
        String first = tournament.firstTeamName();
        String second = tournament.secondTeamName();
        BracketMatch secondMatch = tournament.tournamentManager().getBracket().orElseThrow()
                .findMatch("round1-match2").orElseThrow();
        String third = secondMatch.getFirstTeamName().orElseThrow();
        String fourth = secondMatch.getSecondTeamName().orElseThrow();

        tournament.winRound(first);
        tournament.winRound(first);
        tournament.finishCleaning();
        tournament.winMatch(third);
        tournament.finishCleaning();
        tournament.startFinal();
        tournament.winMatch(first);

        assertEquals(List.of(
                "Match round1-match1: " + first + " gegen " + second,
                "Runde 1 von Match round1-match1 hat begonnen (" + first + " 0 : 0 " + second + "), Zeitlimit 300 "
                        + "Sekunden",
                first + " holt einen Punkt (" + first + " 1 : 0 " + second + ")",
                "Runde 2 von Match round1-match1 hat begonnen (" + first + " 1 : 0 " + second + "), Zeitlimit 300 "
                        + "Sekunden",
                first + " holt einen Punkt (" + first + " 2 : 0 " + second + ")",
                "Match round1-match1 ist vorbei, " + first + " gewinnt (" + first + " 2 : 0 " + second + ")",
                "Als Nächstes: " + third + " gegen " + fourth + " (round1-match2)",
                "Match round1-match2: " + third + " gegen " + fourth),
                bystander.messages().subList(0, 8));
        assertTrue(bystander.messages().contains("Das Finale spielen " + first + " und " + third
                + ", ein Admin startet es"), bystander.messages().toString());
        assertTrue(bystander.messages().contains("Match round2-match1: " + first + " gegen " + third),
                bystander.messages().toString());
        assertEquals(List.of(first + " 1 : 0 " + second + " / " + first + " gewinnt Runde 1",
                first + " gewinnt / round1-match1, " + first + " 2 : 0 " + second),
                bystander.titleTexts().subList(0, 2));
        assertEquals(first + " gewinnt das Finale / round2-match1, " + first + " 2 : 0 " + third,
                bystander.titleTexts().get(bystander.titleTexts().size() - 1));
        assertEquals(List.of("Das Turnier hat mit 4 Teams in 2 Turnierrunden begonnen",
                "Das Turnier ist vorbei, " + first + " gewinnt"), tournament.broadcasts());
        assertEquals("round1-match1", firstMatch.getMatchId());
    }

    @Test
    void showsAResultThatAnAdminDecided() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        String second = tournament.secondTeamName();

        tournament.tournamentManager().forceMatchResult("round1-match1", second);

        assertEquals(List.of(second + " gewinnt / round1-match1, von einem Admin entschieden"),
                bystander.titleTexts());
    }

    @Test
    void announcesPauseContinuationAndCancellationToTheWholeServer() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");

        tournament.startTournament();
        tournament.tournamentManager().togglePause();
        tournament.tournamentManager().togglePause();
        tournament.tournamentManager().cancelTournament();

        assertEquals(List.of("Das Turnier hat mit 4 Teams in 2 Turnierrunden begonnen",
                "Das Turnier ist pausiert, bis zur Fortsetzung startet kein weiteres Match\n"
                        + "Die Rundenzeit behält ihre verbleibende Zeit",
                "Das Turnier geht weiter",
                "Das Turnier wurde abgebrochen"), tournament.broadcasts());
    }

    @Test
    void announcesTheTeamWithAByeAndAWalkover() {
        TournamentAudienceHarness byes = TournamentAudienceHarness.withArenaAudience("Alpha", "Bravo", "Charlie");
        assertTrue(byes.tournamentManager().startTournament(true).successful());
        String byeTeamName = byes.tournamentManager().getBracket().orElseThrow()
                .getTeamNamesWithFirstRoundBye().iterator().next();

        TournamentAudienceHarness walkover = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = walkover.addPlayerInTheArea("Bystander");
        walkover.startTournament();
        BracketMatch secondMatch = walkover.tournamentManager().getBracket().orElseThrow()
                .findMatch("round1-match2").orElseThrow();
        String advancingTeamName = secondMatch.getFirstTeamName().orElseThrow();
        walkover.teamRegistry().removeTeam(secondMatch.getSecondTeamName().orElseThrow());
        walkover.winMatch(walkover.firstTeamName());
        walkover.finishCleaning();

        assertEquals(List.of("Das Turnier hat mit 3 Teams in 2 Turnierrunden begonnen",
                "Team ohne Gegner in der ersten Turnierrunde, es rückt kampflos vor: " + byeTeamName),
                byes.broadcasts());
        assertEquals(1, TournamentAudienceHarness.countMessages(bystander, advancingTeamName
                + " rückt in Match round1-match2 kampflos vor, weil das gegnerische Team nicht mehr registriert ist"));
    }

    @Test
    void warnsAboutSuddenDeathInTheActionBarTheTitleAndTheChat() {
        ArenaMatchHarness harness = new ArenaMatchHarness();
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        harness.useArenaAudience();
        harness.getMatchService().setRoundTimeoutSettings(TWO_SECOND_ROUND_WITH_A_WARNING);

        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.runRepeatingTasks();
        harness.runRepeatingTasks();

        assertEquals(List.of("Sudden Death in 1 Sekunde"), fighter.actionBars());
        assertEquals(List.of("Sudden Death / Beide Teams nehmen Schaden, bis eines von ihnen fällt"),
                fighter.titleTexts());
        assertEquals(1, TournamentAudienceHarness.countMessages(fighter, "Das Zeitlimit von Runde 1 in Match M1 ist "
                + "abgelaufen, der Sudden Death hat begonnen"));
        assertEquals(1, TournamentAudienceHarness.countMessages(fighter, "Runde 1 von Match M1 hat begonnen "
                + "(Alpha 0 : 0 Beta), Zeitlimit 2 Sekunden"));
    }

    @Test
    void tellsTheAudienceOfAHaltedMatchHowToResolveIt() {
        assertEquals("Match M1 ist angehalten, kein Teilnehmer ist online\nNutze \"Ergebnis erzwingen\" im Match-Menü "
                + "von /tournament, um es zu entscheiden", Messages.text(MatchMessage.HALTED,
                Placeholder.of("match", "M1"),
                Placeholder.of("action", MatchService.describeMatchMenuTile(MatchMenuMessage.FORCE_RESULT_TITLE))));
    }

    @Test
    void reportsADrawInTheChatAndTheTitle() {
        ArenaMatchHarness harness = new ArenaMatchHarness();
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        harness.useArenaAudience();
        harness.getMatchService().setRoundTimeoutSettings(ONE_SECOND_ROUND);
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            participant.reactToDamageWith(() -> harness.getMatchService()
                    .handlePlayerEliminated(participant.uniqueId()));
        }

        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.runTick();
        harness.runRepeatingTasks();

        assertEquals(1, TournamentAudienceHarness.countMessages(fighter,
                "Runde 1 von Match M1 endete ohne überlebendes Team, es gibt keinen Punkt"));
        assertEquals(1, TournamentAudienceHarness.countMessages(fighter, "Beide Teams sind in derselben Sekunde des "
                + "Sudden Death gefallen, Runde 1 von Match M1 wird wiederholt"));
        assertTrue(fighter.titleTexts().contains("Alpha 0 : 0 Beta / Runde 1 ist unentschieden"),
                fighter.titleTexts().toString());
    }
}

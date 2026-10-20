package de.simplepvparena.tournament;

import de.simplepvparena.match.Match;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentMessage;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UseLanguage("de")
class GermanTournamentTextsTest {

    private static final List<String> FOUR_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta");

    @Test
    void refusesAStartWithTooFewOrAnUnevenNumberOfTeams() {
        TournamentManagerFixture oneTeam = TournamentManagerFixture.withoutStorage(List.of("Alpha"));
        TournamentManagerFixture threeTeams = TournamentManagerFixture.withoutStorage(
                List.of("Alpha", "Bravo", "Charlie"));

        assertEquals("Mindestens 2 Teams sind nötig",
                oneTeam.tournamentManager().startTournament(false).message());
        assertEquals("Die Anzahl der Teams ist ungerade, nutze \"Start erzwingen\" im Match-Menü von /tournament, "
                + "um trotzdem zu starten", threeTeams.tournamentManager().startTournament(false).message());
    }

    @Test
    void answersStartPauseResumeAndCancellation() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        TournamentManager tournamentManager = fixture.tournamentManager();

        assertEquals("Turnier gestartet", tournamentManager.startTournament(false).message());
        assertEquals("Es läuft bereits ein Turnier", tournamentManager.startTournament(false).message());
        assertEquals("Turnier pausiert", tournamentManager.togglePause().message());
        assertEquals("Das Turnier ist pausiert\nSetze es mit \"Turnier fortsetzen\" im Match-Menü von /tournament "
                + "fort, bevor du das Finale startest", tournamentManager.startFinalMatch().message());
        assertEquals("Turnier fortgesetzt", tournamentManager.togglePause().message());
        assertEquals("Turnier abgebrochen", tournamentManager.cancelTournament().message());
        assertEquals("Es läuft kein Turnier", tournamentManager.togglePause().message());
        assertEquals("Es läuft kein Turnier", tournamentManager.cancelTournament().message());
        assertEquals("Es läuft kein Turnier", tournamentManager.forceMatchResult("round1-match1", "Alpha").message());
        assertEquals("Es läuft kein Turnier", tournamentManager.startFinalMatch().message());
    }

    @Test
    void answersAForcedResult() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        TournamentManager tournamentManager = fixture.tournamentManager();
        Match runningMatch = fixture.matchService().getActiveMatch().orElseThrow();
        String runningMatchId = runningMatch.getMatchId();
        String winnerTeamName = runningMatch.getFirstTeam().getTeamName();

        assertEquals("Unbekanntes Match round9-match9",
                tournamentManager.forceMatchResult("round9-match9", "Alpha").message());
        assertEquals("Zulu nimmt nicht an Match " + runningMatchId + " teil",
                tournamentManager.forceMatchResult(runningMatchId, "Zulu").message());
        assertEquals("Match " + runningMatchId + " wurde für " + winnerTeamName + " entschieden",
                tournamentManager.forceMatchResult(runningMatchId, winnerTeamName).message());
        assertEquals("Match " + runningMatchId + " ist bereits entschieden",
                tournamentManager.forceMatchResult(runningMatchId, winnerTeamName).message());
    }

    @Test
    void refusesToChangeATeamDuringTheTournament() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        Match runningMatch = fixture.matchService().getActiveMatch().orElseThrow();
        String teamName = runningMatch.getFirstTeam().getTeamName();

        assertEquals(Optional.of("Ein Team kann nicht umbenannt werden, während ein Turnier läuft, der Turnierbaum "
                + "behält seinen Namen"), fixture.tournamentManager().findTeamRenameRejection(teamName));
        assertEquals(Optional.of("Team " + teamName + " spielt in Match " + runningMatch.getMatchId()
                + ", es kann erst nach dem Ende des Matches geändert werden"),
                fixture.tournamentManager().findTeamChangeRejection(teamName));
    }

    @Test
    void refusesWhileAKitIsChosenOrTheArenaIsNotReady() {
        assertEquals("Für Match round1-match2 wird gerade ein Kit gewählt, warte darauf oder pausiere das Turnier",
                TournamentManager.buildKitChoiceRejection("round1-match2"));
        assertEquals("Die Arena ist durch Match round2-match1 belegt",
                TournamentManager.findArenaRejection(true, Optional.of("round2-match1")).orElseThrow());
        assertEquals("Die Arena ist nicht vollständig eingerichtet\nBerechne ihre Kampffläche mit "
                + "\"Kampffläche berechnen\" im Arena-Menü von /tournament und setze den Tribünen-Spawn mit "
                + "\"Tribünen-Spawn setzen\" im Arena-Menü von /tournament",
                TournamentManager.findArenaRejection(false, Optional.empty()).orElseThrow());
        assertEquals("Die Arena wird noch gereinigt. Versuche es gleich noch einmal.",
                TournamentManager.describeArenaBeingCleaned());
    }

    @Test
    void refusesTheFinalBeforeBothFinalistsAreKnown() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();

        assertEquals("Das Finale ist noch nicht bereit, zuerst müssen beide Finalisten feststehen",
                fixture.tournamentManager().startFinalMatch().message());
    }

    @Test
    void tellsTheAdminsAboutARestoredTournament() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture before = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        before.startTournament();
        String removedTeamName = before.forceRunningMatchForTheFirstTeam();
        List<String> remainingTeamNames = FOUR_TEAMS.stream()
                .filter(teamName -> !teamName.equals(removedTeamName))
                .toList();

        TournamentManagerFixture after = TournamentManagerFixture.withStorage(storage, remainingTeamNames);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertEquals(List.of("Das Turnier wurde aus memory.yml wiederhergestellt (entschiedene Matches: 1, "
                        + "Turnierrunden: 2). Es ist pausiert, ein Match, das vor dem Neustart lief, beginnt von vorn. "
                        + "Setze es mit \"Turnier fortsetzen\" im Match-Menü von /tournament fort",
                "Team des wiederhergestellten Turniers, das nicht mehr registriert ist: " + removedTeamName
                        + ". Seine entschiedenen Matches behalten den Namen, ein offenes Match von ihm geht beim "
                        + "Fortsetzen des Turniers kampflos an den Gegner"), after.adminMessages());
    }

    @Test
    void tellsTheAdminsWhenTheRestoredTournamentHasNoBaseline() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture.withStorage(storage, FOUR_TEAMS).startTournament();

        TournamentManagerFixture after = TournamentManagerFixture.withStorageWithoutBaseline(storage, FOUR_TEAMS);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertEquals("Nach dem Neustart ist kein Ausgangszustand der Kampffläche gespeichert. Falls der Server "
                + "abgestürzt ist, repariere zuerst die Arena, führe dann \"Kampffläche berechnen\" im Arena-Menü "
                + "von /tournament erneut aus, das nimmt die Arena, wie sie jetzt ist, als neuen Ausgangszustand auf, "
                + "und setze erst danach das Turnier fort", after.adminMessages().get(1));
    }

    @Test
    void tellsTheAdminsWhyNoTournamentWasRestored(@TempDir Path pluginFolder) throws IOException {
        Path stateFile = pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME);
        Files.writeString(stateFile, "format-version: 7\n", StandardCharsets.UTF_8);
        TournamentManagerFixture broken = TournamentManagerFixture.withStorage(
                new FileTournamentStateStorage(stateFile), FOUR_TEAMS);
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        storage.store(new SavedTournament(Instant.EPOCH, Instant.EPOCH, TournamentState.RUNNING,
                List.of(new SavedFirstRoundMatch("round1-match1", Optional.of("Alpha"), Optional.of("Bravo")),
                        new SavedFirstRoundMatch("round1-match2", Optional.of("Charlie"), Optional.of("Delta"))),
                List.of(new SavedMatchResult("round1-match1", "Delta", Optional.empty()))));
        TournamentManagerFixture inconsistent = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);

        assertFalse(broken.tournamentManager().restoreSavedTournament());
        assertFalse(inconsistent.tournamentManager().restoreSavedTournament());

        assertEquals(List.of("Das gespeicherte Turnier in tournament.yml.broken konnte nicht gelesen werden: "
                + "unknown format-version 7. Es wurde kein Turnier wiederhergestellt"), broken.adminMessages());
        assertEquals(List.of("Das gespeicherte Turnier in memory.yml passt nicht zusammen: Delta does not take part "
                + "in match round1-match1. Es wurde kein Turnier wiederhergestellt"), inconsistent.adminMessages());
    }

    @Test
    void tellsTheAdminsWhenTheStateCannotBeSaved() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        storage.failSaves(true);

        fixture.startTournament();

        assertEquals(List.of("Der Turnierstand konnte nicht gespeichert werden, das Turnier läuft weiter und die "
                + "nächste Entscheidung versucht es erneut: the disk is full"),
                fixture.adminMessagesContaining("gespeichert werden"));
    }

    @Test
    void tellsTheAdminsAboutTeamsOfAnotherSize() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(List.of());
        fixture.registerTeamOf("Alpha", 1);
        fixture.registerTeamOf("Bravo", 2);
        fixture.registerTeamOf("Charlie", 4);
        fixture.registerTeamOf("Delta", 3);

        fixture.startTournament();

        assertEquals(List.of("1 Team hat weniger als 2 Spieler: Alpha (1). Das Turnier startet trotzdem, ein "
                        + "fehlender Spieler zählt als abwesend",
                "2 Teams haben mehr als 2 Spieler: Charlie (4) und Delta (3). Das Turnier startet trotzdem, jeder "
                        + "Spieler dieser Teams, der online ist, kämpft"), fixture.adminMessages());
    }

    @Test
    void tellsTheAdminsAboutTheArenaThatIsNotCleaned() {
        assertEquals("Es ist kein Ausgangszustand der Kampffläche gespeichert, deshalb wird die Arena zwischen den "
                + "Matches nicht gereinigt. Einen Ausgangszustand nimmt \"Kampffläche berechnen\" im Arena-Menü von "
                + "/tournament auf", TournamentManager.describeNoBaselineWarning());
        assertEquals("Die Arena wurde nach Match round1-match1 nicht gereinigt: die Welt arena ist nicht geladen. "
                + "Das nächste Match beginnt in der Arena, wie sie jetzt ist",
                TournamentManager.buildSkippedCleaningWarning("round1-match1", "die Welt arena ist nicht geladen",
                        false));
        assertEquals("Die Arena wurde nach Match round2-match1 nicht gereinigt: die Welt arena ist nicht geladen. "
                + "Führe \"Kampffläche berechnen\" im Arena-Menü von /tournament vor dem nächsten Turnier "
                + "erneut aus",
                TournamentManager.buildSkippedCleaningWarning("round2-match1", "die Welt arena ist nicht geladen\n"
                        + "zweite Zeile", true));
    }

    @Test
    void countsTeamsRoundsByesAndRestoredMatchesWithTheRightForm() {
        assertEquals(List.of("1 Team", "2 Teams", "1 Turnierrunde", "2 Turnierrunden"),
                List.of(Messages.plural(TournamentMessage.TEAM_COUNT, 1),
                        Messages.plural(TournamentMessage.TEAM_COUNT, 2),
                        Messages.plural(TournamentMessage.ROUND_COUNT, 1),
                        Messages.plural(TournamentMessage.ROUND_COUNT, 2)));
        assertEquals("Das Turnier hat mit 2 Teams in 1 Turnierrunde begonnen", Messages.text(TournamentMessage.STARTED,
                Placeholder.of("teams", Messages.plural(TournamentMessage.TEAM_COUNT, 2)),
                Placeholder.of("rounds", Messages.plural(TournamentMessage.ROUND_COUNT, 1))));
        assertEquals("Team ohne Gegner in der ersten Turnierrunde, es rückt kampflos vor: Alpha",
                Messages.plural(TournamentMessage.BYES, 1, Placeholder.of("teams", "Alpha")));
        assertEquals("Teams ohne Gegner in der ersten Turnierrunde, sie rücken kampflos vor: Alpha und Bravo",
                Messages.plural(TournamentMessage.BYES, 2,
                        Placeholder.of("teams", Messages.join(List.of("Alpha", "Bravo")))));
        assertEquals("Teams des wiederhergestellten Turniers, die nicht mehr registriert sind: Alpha und Bravo. "
                + "Ihre entschiedenen Matches behalten den Namen, ein offenes Match von ihnen geht beim Fortsetzen "
                + "des Turniers kampflos an den Gegner", Messages.plural(TournamentMessage.UNREGISTERED_TEAMS, 2,
                Placeholder.of("teams", Messages.join(List.of("Alpha", "Bravo")))));
        assertEquals("x. Das Turnier startet trotzdem, jeder Spieler dieses Teams, der online ist, kämpft",
                Messages.plural(TournamentMessage.LARGER_TEAMS, 1, Placeholder.of("teams", "x")));
        assertTrue(Messages.plural(TournamentMessage.RESTORED, 2, Placeholder.of("file", "f"),
                Placeholder.of("rounds", 2), Placeholder.of("action", "a"))
                .contains("(entschiedene Matches: 2, Turnierrunden: 2)"));
    }

    @Test
    @UseLanguage("en")
    void countsTeamsRoundsByesAndRestoredMatchesWithTheRightFormInEnglish() {
        assertEquals(List.of("1 team", "2 teams", "1 round", "2 rounds"),
                List.of(Messages.plural(TournamentMessage.TEAM_COUNT, 1),
                        Messages.plural(TournamentMessage.TEAM_COUNT, 2),
                        Messages.plural(TournamentMessage.ROUND_COUNT, 1),
                        Messages.plural(TournamentMessage.ROUND_COUNT, 2)));
        assertEquals("The tournament has started with 2 teams in 1 round", Messages.text(TournamentMessage.STARTED,
                Placeholder.of("teams", Messages.plural(TournamentMessage.TEAM_COUNT, 2)),
                Placeholder.of("rounds", Messages.plural(TournamentMessage.ROUND_COUNT, 1))));
        assertEquals("Team advancing without a first round opponent: Alpha",
                Messages.plural(TournamentMessage.BYES, 1, Placeholder.of("teams", "Alpha")));
        assertEquals("Teams advancing without a first round opponent: Alpha, Bravo and Charlie",
                Messages.plural(TournamentMessage.BYES, 3,
                        Placeholder.of("teams", Messages.join(List.of("Alpha", "Bravo", "Charlie")))));
        assertEquals("Team of the restored tournament that is no longer registered: Alpha. Their decided matches "
                + "keep the name, an open match of theirs goes to the opponent by walkover when the tournament "
                + "continues", Messages.plural(TournamentMessage.UNREGISTERED_TEAMS, 1,
                Placeholder.of("teams", "Alpha")));
        assertEquals("Teams of the restored tournament that are no longer registered: Alpha and Bravo. Their decided "
                + "matches keep the name, an open match of theirs goes to the opponent by walkover when the "
                + "tournament continues", Messages.plural(TournamentMessage.UNREGISTERED_TEAMS, 2,
                Placeholder.of("teams", Messages.join(List.of("Alpha", "Bravo")))));
        assertEquals("x. The tournament starts anyway, every online player of this team fights",
                Messages.plural(TournamentMessage.LARGER_TEAMS, 1, Placeholder.of("teams", "x")));
        assertEquals("x. The tournament starts anyway, every online player of these teams fights",
                Messages.plural(TournamentMessage.LARGER_TEAMS, 2, Placeholder.of("teams", "x")));
        assertTrue(Messages.plural(TournamentMessage.RESTORED, 1, Placeholder.of("file", "f"),
                Placeholder.of("rounds", 1), Placeholder.of("action", "a"))
                .contains("(matches decided: 1, rounds: 1)"));
        assertTrue(Messages.plural(TournamentMessage.RESTORED, 2, Placeholder.of("file", "f"),
                Placeholder.of("rounds", 2), Placeholder.of("action", "a"))
                .contains("(matches decided: 2, rounds: 2)"));
    }

    @Test
    void describesTheStatusAndTheBracketBeforeTheTournament() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);

        assertEquals("Turnierstatus: kein Turnier", fixture.tournamentManager().describeStatus());
        assertEquals(List.of("Es läuft kein Turnier"), fixture.tournamentManager().describeBracket());
    }

    @Test
    void describesTheStatusAndTheBracketOfARunningTournament() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        BracketMatch firstMatch = fixture.bracketMatch("round1-match1");
        BracketMatch secondMatch = fixture.bracketMatch("round1-match2");
        String firstTeam = firstMatch.getFirstTeamName().orElseThrow();
        String secondTeam = firstMatch.getSecondTeamName().orElseThrow();

        assertEquals("Turnierstatus: läuft | Turnierrunden: 2 | laufendes Match: round1-match1 | Finale: wartet auf "
                + "Finalisten", fixture.tournamentManager().describeStatus());
        fixture.forceRunningMatchForTheFirstTeam();

        assertEquals(List.of("Turnierrunde 1:",
                "  round1-match1 " + firstTeam + " gegen " + secondTeam + " -> " + firstTeam,
                "  round1-match2 " + secondMatch.getFirstTeamName().orElseThrow() + " gegen "
                        + secondMatch.getSecondTeamName().orElseThrow(),
                "Turnierrunde 2:",
                "  round2-match1 " + firstTeam + " gegen offen"), fixture.tournamentManager().describeBracket());
    }

    @Test
    void describesTheStatusOfAFinishedTournamentWithItsChampion() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        fixture.forceRunningMatchForTheFirstTeam();
        fixture.forceRunningMatchForTheFirstTeam();
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());
        String championTeamName = fixture.forceRunningMatchForTheFirstTeam();

        assertEquals("Turnierstatus: beendet | Turnierrunden: 2 | laufendes Match: keins | Finale: vorbei | Sieger: "
                + championTeamName, fixture.tournamentManager().describeStatus());
    }

    @Test
    void describesThePausedTournamentWithItsRunningMatch() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        fixture.tournamentManager().togglePause();

        assertEquals("Turnierstatus: pausiert | Turnierrunden: 2 | laufendes Match: round1-match1 | Finale: wartet "
                + "auf Finalisten", fixture.tournamentManager().describeStatus());
    }

    @Test
    void namesTheSmallestNumberOfTeamsWithTheRightForm() {
        assertEquals("Mindestens 1 Team ist nötig", Messages.plural(TournamentMessage.AT_LEAST_TEAMS_REQUIRED, 1));
        assertEquals("Mindestens 2 Teams sind nötig", Messages.plural(TournamentMessage.AT_LEAST_TEAMS_REQUIRED, 2));
    }

    @Test
    @UseLanguage("en")
    void namesTheSmallestNumberOfTeamsWithTheRightFormInEnglish() {
        assertEquals("At least 1 team is required", Messages.plural(TournamentMessage.AT_LEAST_TEAMS_REQUIRED, 1));
        assertEquals("At least 2 teams are required", Messages.plural(TournamentMessage.AT_LEAST_TEAMS_REQUIRED, 2));
    }
}

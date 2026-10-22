package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.BracketMatch;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UseLanguage("de")
class GermanMenuSnapshotTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MAIN_BORDER_SELECTOR_SLOT = 13;
    private static final int MAIN_FINAL_SLOT = 14;
    private static final int MAIN_BRACKET_SLOT = 16;
    private static final int MAIN_RELOAD_SLOT = 20;
    private static final int MAIN_KIT_CHOICE_SLOT = 23;
    private static final int MAIN_KIT_SLOT = 24;
    private static final int MATCH_CANCEL_SLOT = 13;
    private static final int MATCH_DECIDE_WAITING_MATCH_SLOT = 14;
    private static final int MATCH_FORCE_RESULT_SLOT = 16;
    private static final int FIRST_WAITING_MATCH_SLOT = 0;
    private static final int WAITING_MATCH_RESULT_FIRST_TEAM_SLOT = 11;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheMainMenuInGerman() {
        fixture.openMainMenu();

        assertEquals("""
                Turnierverwaltung | 27
                4 | BOOK | Turnier | Turnierstatus: kein Turnier / Registrierte Teams: 0 / Arena: unvollständig / \
                Laufendes Match: keins
                10 | GRASS_BLOCK | Arena | Kampffläche, Spawns, Reinigung und Zurücksetzen / Arena unvollständig
                11 | PLAYER_HEAD | Teams | Registrierte Teams: 0 / \
                Zeigt alle Teams, fügt eines hinzu und entfernt eines
                12 | DIAMOND_SWORD | Match und Turnier | Starten, pausieren, abbrechen und ein Ergebnis erzwingen / \
                Laufendes Match: keins
                13 | BRUSH | Wandblöcke per Klick markieren | \
                Macht jeden deiner Rechtsklicks zu einer Wandmarkierung / \
                Ein Klick markiert einen Block, ein Klick auf einen / \
                markierten Block nimmt die Markierung wieder weg / \
                Kein Wand-Item nötig: Treppen, Zäune und jeder / \
                andere Block, der schon dort steht, lässt sich markieren / \
                Truhen, Türen und alles andere bleiben zu, solange er an ist / \
                Das Menü schließt sich, damit du die Blöcke anklicken kannst / \
                Der Modus ist aus, diese Kachel schaltet ihn an
                14 | GRAY_DYE | Finale starten | Es läuft kein Turnier
                15 | PAPER | Turnierstatus | Schreibt den Stand, die Turnierrunden und das laufende Match in den Chat
                16 | WRITTEN_BOOK | Turnierbaum | Zeigt jede Turnierrunde und jedes Match
                18 | OBSERVER | Probelauf des Turnierablaufs | Geht den ganzen Ablauf einmal durch, ohne Event / \
                Registriert zwei Testteams, baut ihren Turnierbaum, / \
                entscheidet ein simuliertes Match und entfernt sie wieder / \
                Prüft die Arena und ihre Spawns, das Kit, den / Countdown des Starttors und ob die / \
                Reinigung nach einem Match starten würde / \
                Nichts wird gestartet, geschrieben oder gereinigt: / \
                kein Block, keine Datei und kein Kämpfer wird berührt / \
                Für das Kit und das echte Ankommen nach dem / Teleport bleibt ein echter Testspieler nötig / \
                Gesperrt, solange ein Turnier oder ein Match läuft / \
                Das Menü schließt sich, damit du die Antwort lesen kannst
                20 | COMPARATOR | config.yml neu laden | Liest config.yml erneut und wendet sie sofort an / \
                Rundenzeitlimit, Blockmengen der Reinigung, Zellgrenze, / Schutzzone und Wandpartikel / \
                Die Arena, die Teams und ein laufendes Match bleiben unberührt / \
                Das Menü schließt sich, damit du die Antwort lesen kannst
                22 | BARRIER | Schließen | Schließt das Menü
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 0 / Standard-Kit: keins / \
                Die Items, die jeder Kämpfer zum Start einer Runde bekommt / Kits anlegen, ersetzen und löschen
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheRunningMatchTheReloadNoteAndTheDefaultKitOfTheMainMenuInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.createKit("Archer");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.openMainMenu();

        String runningMatch = "Laufendes Match: " + fixture.runningMatch().describe();
        assertEquals(List.of("Turnierstatus: kein Turnier", "Registrierte Teams: 2", "Arena: bereit", runningMatch),
                fixture.lastMenu().findTile(AdminMenuLayout.INFO_SLOT).orElseThrow().loreLines());
        assertEquals(List.of("Starten, pausieren, abbrechen und ein Ergebnis erzwingen", runningMatch),
                fixture.lastMenu().findTile(MAIN_MATCH_SLOT).orElseThrow().loreLines());
        assertEquals(List.of("Liest config.yml erneut und wendet sie sofort an",
                "Rundenzeitlimit, Blockmengen der Reinigung, Zellgrenze,", "Schutzzone und Wandpartikel",
                "Die Arena, die Teams und ein laufendes Match bleiben unberührt",
                "Ein Match läuft: das neue Rundenzeitlimit", "gilt ab der nächsten Runde",
                "Das Menü schließt sich, damit du die Antwort lesen kannst"),
                fixture.lastMenu().findTile(MAIN_RELOAD_SLOT).orElseThrow().loreLines());
        assertEquals(new MenuTile(Material.DIAMOND_CHESTPLATE, "Kits", List.of("Kits: 1",
                "Standard-Kit: Archer", "Die Items, die jeder Kämpfer zum Start einer Runde bekommt",
                "Kits anlegen, ersetzen und löschen")), fixture.lastMenu().findTile(MAIN_KIT_SLOT).orElseThrow());
    }

    @Test
    void showsTheKitChoiceTileOfTheMainMenuInGerman() {
        AdminMenuFixture kitChoiceFixture = AdminMenuFixture.withKitChoice();
        kitChoiceFixture.createKit("Archer");
        kitChoiceFixture.createKit("Knight");
        kitChoiceFixture.kitChoiceService().begin("round1-match1", "Alpha", "Bravo", () -> { });

        kitChoiceFixture.openMainMenu();

        assertTrue(kitChoiceFixture.describeLastMenu().contains("""
                23 | CHEST | Kit für das nächste Match wählen | Match round1-match1: Alpha gegen Bravo / \
                Das Menü schließt sich, wenn die Zeit um ist oder ein Kit gewählt wurde
                """), kitChoiceFixture.describeLastMenu());
        assertTrue(kitChoiceFixture.lastMenu().findTile(MAIN_KIT_CHOICE_SLOT).isPresent());
    }

    @Test
    void theFinalTileNamesEveryStageInAWholeGermanSentence() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        Map<String, List<String>> finalTiles = new LinkedHashMap<>();

        finalTiles.put("pending", finalTileOfTheMainMenu());
        fixture.startTournament();
        finalTiles.put("ready", finalTileOfTheMainMenu());
        fixture.pauseTournament();
        finalTiles.put("ready while paused", finalTileOfTheMainMenu());
        fixture.pauseTournament();
        fixture.startFinal();
        finalTiles.put("running", finalTileOfTheMainMenu());
        fixture.decideRunningMatchFor("Alpha");
        finalTiles.put("over", finalTileOfTheMainMenu());

        assertEquals(Map.of(
                "pending", List.of("GRAY_DYE", "Finale starten", "Es läuft kein Turnier"),
                "ready", List.of("NETHER_STAR", "Finale starten", "Beide Finalisten stehen fest",
                        "Startet das Finale in der Arena"),
                "ready while paused", List.of("GRAY_DYE", "Finale starten",
                        "Das Finale ist bereit, setze das Turnier fort, um es zu starten"),
                "running", List.of("GRAY_DYE", "Finale starten", "Das Finale läuft"),
                "over", List.of("GRAY_DYE", "Finale starten", "Das Finale ist vorbei")), finalTiles);
    }

    @Test
    @UseLanguage("en")
    void theFinalTileKeepsTheEnglishSentenceOfTheReadyStageWhileTheTournamentIsPaused() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.pauseTournament();

        assertEquals(List.of("GRAY_DYE", "Start the final", "The final is ready, continue the tournament to start it"),
                finalTileOfTheMainMenu());
    }

    @Test
    void answersTheTilesOfTheMainMenuInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMainMenu();
        fixture.click(MAIN_BORDER_SELECTOR_SLOT);
        fixture.openMainMenu();
        fixture.click(MAIN_BORDER_SELECTOR_SLOT);
        fixture.openMainMenu();
        fixture.click(MAIN_BRACKET_SLOT);

        assertEquals(String.join("\n",
                "Der Wandauswahlmodus ist an",
                "Klicke mit rechts auf einen beliebigen Block der Welt, um ihn als Wandblock zu markieren, "
                        + "ein Rechtsklick auf einen markierten Block nimmt die Markierung wieder weg",
                "Kein Wand-Item nötig: Treppen, Zäune und alles andere, was schon dort steht, kann Teil der Wand "
                        + "werden",
                "Jeder andere Rechtsklick ist gesperrt, solange der Modus an ist, auch bei Truhen und Türen",
                "Öffne das Turniermenü und klicke die Kachel erneut an, um ihn auszuschalten"),
                fixture.messages().get(0));
        assertEquals(String.join("\n", "Der Wandauswahlmodus ist aus",
                "Rechtsklicks funktionieren wieder wie immer, jede gesetzte Markierung bleibt"),
                fixture.messages().get(1));
        assertEquals(2, fixture.messages().size());
        assertEquals("Turnierbaum (Turnierrunde 1)", fixture.lastMenu().title());
    }

    @Test
    void warnsInGermanAfterTheReloadWhileAMatchRuns() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.openMainMenu();

        fixture.click(MAIN_RELOAD_SLOT);

        List<String> messages = fixture.messages();
        assertEquals(String.join("\n", "Ein Match läuft, die neuen Einstellungen gelten ab der nächsten Runde",
                "Die laufende Runde behält ihre Rundenzeit und ihre Kämpfer behalten ihr Kit"),
                messages.get(messages.size() - 1));
    }

    @Test
    void showsTheMatchMenuBeforeTheTournamentInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match und Turnier | 27
                4 | BOOK | Turnier | Turnierstatus: kein Turnier / Laufendes Match: keins / Rundenzeit: läuft
                10 | LIME_WOOL | Turnier starten | Braucht eine gerade Anzahl Teams
                11 | YELLOW_WOOL | Start erzwingen | Startet das Turnier mit einer ungeraden Anzahl Teams
                12 | GRAY_DYE | Turnier pausieren | Es läuft kein Turnier
                13 | GRAY_DYE | Turnier abbrechen | Es läuft kein Turnier
                14 | GRAY_DYE | Wartendes Match entscheiden | Es läuft kein Turnier
                15 | GRAY_DYE | Finale starten | Es läuft kein Turnier
                16 | GRAY_DYE | Ergebnis erzwingen | Es läuft kein Match
                17 | COMPARATOR | Auto-Match-Modus | Der Modus ist aus / \
                Zwei Teams mit je einem Spieler online bekommen ein Match ohne Turnier / \
                Ein laufendes Turnier hat Vorrang / Klicke, um ihn einzuschalten
                19 | GRAY_DYE | Match neu starten | Es läuft kein Match
                20 | GRAY_DYE | Ergebnis zurücknehmen | Es läuft kein Turnier
                22 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMatchMenuWhileTheTournamentRunsAMatchInGerman() {
        startATournamentOfFourTeams();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match und Turnier | 27
                4 | BOOK | Turnier | Turnierstatus: läuft | Turnierrunden: 2 | laufendes Match: round1-match1 | \
                Finale: wartet auf Finalisten / Laufendes Match: round1-match1 (%1$s 0 : 0 %2$s) / Rundenzeit: läuft
                10 | GRAY_DYE | Turnier starten | Es läuft bereits ein Turnier
                11 | GRAY_DYE | Start erzwingen | Es läuft bereits ein Turnier
                12 | CLOCK | Turnier pausieren | Bis zur Fortsetzung startet kein weiteres Match / \
                Die Rundenzeit behält ihre verbleibende Zeit
                13 | RED_WOOL | Turnier abbrechen | Stoppt das laufende Match und den Turnierbaum / \
                Fragt vorher nach einer Bestätigung
                14 | LECTERN | Wartendes Match entscheiden | \
                Entscheidet ein Match, das nicht läuft, für eines seiner zwei Teams
                15 | GRAY_DYE | Finale starten | Das Finale ist nicht bereit, seine Finalisten stehen noch nicht fest
                16 | ANVIL | Ergebnis erzwingen | Entscheidet das laufende Match für eines seiner zwei Teams
                17 | COMPARATOR | Auto-Match-Modus | Der Modus ist aus / \
                Zwei Teams mit je einem Spieler online bekommen ein Match ohne Turnier / \
                Ein laufendes Turnier hat Vorrang / \
                Ein Turnier läuft, der Modus wirkt, wenn es vorbei ist / \
                Klicke, um ihn einzuschalten
                19 | REPEATER | Match neu starten | \
                Stoppt das laufende Match und beginnt es mit denselben zwei Teams neu / \
                Der Spielstand ist wieder 0 : 0, Kit-Wahl und Countdown laufen erneut / \
                Fragt vorher nach einer Bestätigung
                20 | BARRIER | Ergebnis zurücknehmen | \
                Öffnet die entschiedenen Matches, deren Folgematch noch nicht begonnen hat
                22 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName()),
                fixture.describeLastMenu());
    }

    @Test
    void showsTheMatchMenuWhileTheTournamentIsPausedInGerman() {
        startATournamentOfFourTeams();
        fixture.pauseTournament();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertTrue(fixture.describeLastMenu().contains("""
                wartet auf Finalisten / Laufendes Match: round1-match1 (%1$s 0 : 0 %2$s) / Rundenzeit: pausiert
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName())),
                fixture.describeLastMenu());
        assertTrue(fixture.describeLastMenu().contains("""
                12 | CLOCK | Turnier fortsetzen | Die Rundenzeit läuft mit ihrer verbleibenden Zeit weiter
                """), fixture.describeLastMenu());
    }

    @Test
    void refusesTheWaitingMatchesAndTheForcedResultWithoutATournamentInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);
        fixture.click(MATCH_FORCE_RESULT_SLOT);

        assertEquals(List.of("Es läuft kein Turnier", "Es läuft kein Match"), fixture.messages());
    }

    @Test
    void showsTheForcedResultInGerman() {
        startATournamentOfFourTeams();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(MATCH_FORCE_RESULT_SLOT);
        String runningMatchMenu = fixture.describeLastMenu();
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        fixture.refillLastMenu();

        assertEquals("""
                Ergebnis von round1-match1 | 27
                4 | BOOK | Ergebnis erzwingen | Match round1-match1 (%1$s 0 : 0 %2$s) / Runde 1 / \
                Ein Klick entscheidet das Match für dieses Team
                11 | PLAYER_HEAD | %1$s | %1$s-first / %1$s-second / Entscheidet das Match für dieses Team
                15 | PLAYER_HEAD | %2$s | %2$s-first / %2$s-second / Entscheidet das Match für dieses Team
                22 | OAK_DOOR | Zurück | Zurück zum Match-Menü
                """.formatted(firstTeamOf("round1-match1"), secondTeamOf("round1-match1")), runningMatchMenu);
        assertEquals("""
                Ergebnis von round1-match1 | 27
                4 | BOOK | Ergebnis erzwingen | Match round1-match1 läuft nicht mehr
                22 | OAK_DOOR | Zurück | Zurück zum Match-Menü
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheWaitingMatchesInGerman() {
        startATournamentOfFourTeams();
        assertTrue(fixture.teamRegistry().removeTeam(secondTeamOf("round1-match2")));

        openTheWaitingMatches();

        assertEquals("""
                Wartende Matches (Seite 1) | 54
                0 | PLAYER_HEAD | round1-match2 | %1$s gegen %2$s / Turnierrunde 1 / %2$s ist nicht mehr registriert
                47 | BOOK | Wartende Matches | Wartende Matches: 1 / Seite 1 von 1 / \
                Ein Klick auf ein Match entscheidet es für eines seiner Teams
                49 | OAK_DOOR | Zurück | Zurück zum Match-Menü
                """.formatted(firstTeamOf("round1-match2"), secondTeamOf("round1-match2")),
                fixture.describeLastMenu());
    }

    @Test
    void showsThatNoMatchIsWaitingInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.startFinal();

        openTheWaitingMatches();

        assertEquals("""
                Wartende Matches (Seite 1) | 54
                47 | BOOK | Wartende Matches | Kein Match wartet
                49 | OAK_DOOR | Zurück | Zurück zum Match-Menü
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheResultOfAWaitingMatchInGerman() {
        startATournamentOfFourTeams();
        String firstTeam = firstTeamOf("round1-match2");
        String secondTeam = secondTeamOf("round1-match2");
        assertTrue(fixture.teamRegistry().removeTeam(firstTeam));
        openTheWaitingMatches();

        fixture.click(FIRST_WAITING_MATCH_SLOT);

        assertEquals("""
                Ergebnis von round1-match2 | 27
                4 | BOOK | Wartendes Match entscheiden | Match round1-match2 (%1$s gegen %2$s) / Turnierrunde 1 / \
                Ein Klick entscheidet das Match für dieses Team
                11 | PLAYER_HEAD | %1$s | Nicht mehr registriert / Entscheidet das Match für dieses Team
                15 | PLAYER_HEAD | %2$s | %2$s-first / %2$s-second / Entscheidet das Match für dieses Team
                22 | OAK_DOOR | Zurück | Zurück zu den wartenden Matches
                """.formatted(firstTeam, secondTeam), fixture.describeLastMenu());
    }

    @Test
    void warnsInGermanWhenTheWaitingMatchIsDecidedMeanwhile() {
        startATournamentOfFourTeams();
        openTheWaitingMatches();
        fixture.click(FIRST_WAITING_MATCH_SLOT);
        assertTrue(fixture.tournamentManager().forceMatchResult("round1-match2", firstTeamOf("round1-match2"))
                .successful());
        fixture.messages().clear();

        fixture.click(WAITING_MATCH_RESULT_FIRST_TEAM_SLOT);

        assertEquals(List.of("Match round1-match2 wartet nicht mehr"), fixture.messages());
    }

    @Test
    void showsTheConfirmationOfTheCancellationInGerman() {
        startATournamentOfFourTeams();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(MATCH_CANCEL_SLOT);

        assertEquals("""
                Turnier abbrechen? | 27
                4 | BOOK | Turnier abbrechen? | Das laufende Turnier wird abgebrochen / \
                Das laufende Match wird gestoppt und sein Turnierbaum verworfen
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());
    }

    @Test
    void asksEveryConfirmationQuestionInGerman() {
        Map<ConfirmationRequest, String> questions = new EnumMap<>(ConfirmationRequest.class);
        for (ConfirmationRequest request : ConfirmationRequest.values()) {
            questions.put(request, request.getTitle(Optional.of("round1-match2")));
        }

        assertEquals(Map.of(
                ConfirmationRequest.CLEAN_ARENA, "Arena reinigen?",
                ConfirmationRequest.RESET_ARENA, "Arena zurücksetzen?",
                ConfirmationRequest.REPLACE_KIT, "Kit durch dein Inventar ersetzen?",
                ConfirmationRequest.DELETE_KIT, "Kit löschen?",
                ConfirmationRequest.REMOVE_TEAM, "Team entfernen?",
                ConfirmationRequest.CANCEL_TOURNAMENT, "Turnier abbrechen?",
                ConfirmationRequest.RESTART_MATCH, "Match neu starten?",
                ConfirmationRequest.FORM_RANDOM_TEAMS, "Zufallsteams bilden?",
                ConfirmationRequest.CLEAR_REGISTRATIONS, "Anmeldungen leeren?",
                ConfirmationRequest.REVOKE_RESULT, "Ergebnis von round1-match2 zurücknehmen?"), questions);
    }

    private List<String> finalTileOfTheMainMenu() {
        fixture.openMainMenu();
        MenuTile finalTile = fixture.lastMenu().findTile(MAIN_FINAL_SLOT).orElseThrow();
        List<String> description = new ArrayList<>(List.of(finalTile.material().name(),
                finalTile.displayName()));
        description.addAll(finalTile.loreLines());
        return description;
    }

    private void startATournamentOfFourTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
    }

    private void openTheWaitingMatches() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);
    }

    private BracketMatch bracketMatch(String matchId) {
        return fixture.tournamentManager().getBracket().orElseThrow().findMatch(matchId).orElseThrow();
    }

    private String firstTeamOf(String matchId) {
        return bracketMatch(matchId).getFirstTeamName().orElseThrow();
    }

    private String secondTeamOf(String matchId) {
        return bracketMatch(matchId).getSecondTeamName().orElseThrow();
    }
}

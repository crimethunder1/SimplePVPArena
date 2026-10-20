package de.simplepvparena.diagnostics;

import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanDryRunTextsTest {

    private final DryRunFixture fixture = new DryRunFixture();

    @Test
    void reportsAnArenaThatIsNotSetUpYet() {
        assertEquals(List.of(
                "Probelauf des Turnierablaufs: 3 von 7 Schritten bestanden, 3 gescheitert, 1 übersprungen",
                "  [ok] Zwei Testteams registriert",
                "    Dry-Run-Alpha mit Dry-Run-Alpha-1, Dry-Run-Alpha-2",
                "    Dry-Run-Bravo mit Dry-Run-Bravo-1, Dry-Run-Bravo-2",
                "    Beide sind über den Namen zu finden und beide Platzhalter-Spieler über ihre eindeutige ID",
                "    Die Mitglieder sind Platzhalter und kein Konto dieses Servers, und in teams.yml wurde nichts "
                        + "geschrieben",
                "  [GESCHEITERT] Arena, Kampffläche und Spawns",
                "    Die Arena kann noch kein Match austragen, ihr fehlt noch:",
                "      die berechnete Kampffläche (\"Kampffläche berechnen\" im Arena-Menü von /tournament)",
                "      der Tribünen-Spawn (\"Tribünen-Spawn setzen\" im Arena-Menü von /tournament)",
                "    Der Probelauf berechnet die Kampffläche nie selbst: eine Berechnung ersetzt die Form, den "
                        + "Ausgangszustand und die markierten Höhen",
                "  [GESCHEITERT] Das Kit, das ein Rundenstart ausgibt",
                "    Das Kit ist leer, jeder Kämpfer würde die Runde mit bloßen Händen beginnen",
                "    Pack das Kit in dein eigenes Inventar, lege es mit \"Kit aus deinem Inventar anlegen\" im "
                        + "Kit-Menü von /tournament an und mach es zum Standard-Kit",
                "  [übersprungen] Countdown des Starttors",
                "    Kein Starttor-Block ist markiert, die Runden beginnen ohne Starttor",
                "    Das ist eine funktionierende Einrichtung, deshalb wird nichts abgelehnt",
                "    Gib dir mit \"Starttor-Blöcke geben\" im Arena-Menü von /tournament Starttor-Blöcke, um eines "
                        + "zu bauen",
                "  [ok] Turnierbaum und ein simuliertes Match",
                "    Turnierbaum über 2 Teams gebaut: 1 Turnierrunde, 1 Match darin",
                "    Match round1-match1: Dry-Run-Alpha gegen Dry-Run-Bravo, beide Teams sind in der Teamliste zu "
                        + "finden",
                "    Die Reihenfolge der Matches lässt es aus: das einzige Match eines Turnierbaums mit zwei Teams "
                        + "ist das Finale, und das Finale ist das eine Match, das ein Admin startet",
                "    Das Finale wechselt auf \"bereit\" und meldet sich einmal: Das Finale Dry-Run-Alpha gegen "
                        + "Dry-Run-Bravo ist bereit, ein Admin startet es mit \"Finale starten\" in /tournament",
                "    Simuliertes Ergebnis für Dry-Run-Alpha: der Turnierbaum ist vollständig, Sieger ist "
                        + "Dry-Run-Alpha und das Finale steht auf \"vorbei\"",
                "    Nichts davon hat die Arena erreicht: keine Runde wurde begonnen, kein Starttor geöffnet und "
                        + "kein Kämpfer teleportiert oder ausgerüstet",
                "  [GESCHEITERT] Die Reinigung nach dem Match",
                "    Es ist kein Ausgangszustand der Kampffläche gespeichert, deshalb wird zwischen zwei Matches "
                        + "nichts gereinigt",
                "    Eine Reinigung, um die niemand gebeten hat, leert nie die Arena, sie wird stattdessen "
                        + "übersprungen",
                "    Der Ausgangszustand liegt nur im Speicher: führe \"Kampffläche berechnen\" im Arena-Menü von "
                        + "/tournament einmal aus",
                "  [ok] Testteams wieder entfernt",
                "    Beide Testteams sind weg, die Teamliste enthält wieder 0 eigene Teams",
                "    teams.yml wurde nie geschrieben, also könnte auch ein Neustart sie nicht zurückbringen",
                "Für die Teile, die an einem Körper wirken, bleibt ein echter Testspieler nötig:",
                "  das Kit, das wirklich in einem Inventar landet, und das alte, das abgeräumt wird",
                "  der Teleport auf die beiden Team-Spawns hinter dem Starttor",
                "  wer als aktiver Kämpfer zählt, was davon abhängt, wer online ist",
                "  der Countdown des Starttors und die Titel, die wirklich über der Arena laufen",
                "Der Probelauf prüft, dass all das eingerichtet ist, aber nie, dass es angekommen ist"),
                fixture.dryRun().run().describe());
    }

    @Test
    void reportsAServerThatIsReadyForItsEvent() {
        fixture.makeTheServerReadyForItsEvent();

        assertEquals(List.of(
                "Probelauf des Turnierablaufs: 7 von 7 Schritten bestanden",
                "  [ok] Zwei Testteams registriert",
                "    Dry-Run-Alpha mit Dry-Run-Alpha-1, Dry-Run-Alpha-2",
                "    Dry-Run-Bravo mit Dry-Run-Bravo-1, Dry-Run-Bravo-2",
                "    Beide sind über den Namen zu finden und beide Platzhalter-Spieler über ihre eindeutige ID",
                "    Die Mitglieder sind Platzhalter und kein Konto dieses Servers, und in teams.yml wurde nichts "
                        + "geschrieben",
                "  [ok] Arena, Kampffläche und Spawns",
                "    Kampffläche: event mit 4 Zellen zwischen 0/0 und 1/1, von y 64 bis y 67, berechnet auf y 64",
                "    Eine Reinigung schreibt 60 Blöcke zwischen y 53 und y 67",
                "    Beide Team-Spawns und der Tribünen-Spawn sind gesetzt",
                "  [ok] Das Kit, das ein Rundenstart ausgibt",
                "    Getragene Items: 2 von 5",
                "    Items im Inventar: 3 von 36 Plätzen",
                "    Jeder Kämpfer bekommt bei jedem Rundenstart diese 5 Items, jedes als eigene Kopie",
                "  [ok] Countdown des Starttors",
                "    Der Countdown zählt von 10 bis 1 und zeigt dann \"Kämpft!\", geprüft, ohne einen Tick zu warten",
                "    event: 15 Blöcke in 5 Schichten, die letzte fällt in Tick 200 von 200",
                "  [ok] Turnierbaum und ein simuliertes Match",
                "    Turnierbaum über 2 Teams gebaut: 1 Turnierrunde, 1 Match darin",
                "    Match round1-match1: Dry-Run-Alpha gegen Dry-Run-Bravo, beide Teams sind in der Teamliste zu "
                        + "finden",
                "    Die Reihenfolge der Matches lässt es aus: das einzige Match eines Turnierbaums mit zwei Teams "
                        + "ist das Finale, und das Finale ist das eine Match, das ein Admin startet",
                "    Das Finale wechselt auf \"bereit\" und meldet sich einmal: Das Finale Dry-Run-Alpha gegen "
                        + "Dry-Run-Bravo ist bereit, ein Admin startet es mit \"Finale starten\" in /tournament",
                "    Simuliertes Ergebnis für Dry-Run-Alpha: der Turnierbaum ist vollständig, Sieger ist "
                        + "Dry-Run-Alpha und das Finale steht auf \"vorbei\"",
                "    Nichts davon hat die Arena erreicht: keine Runde wurde begonnen, kein Starttor geöffnet und "
                        + "kein Kämpfer teleportiert oder ausgerüstet",
                "  [ok] Die Reinigung nach dem Match",
                "    Eine Reinigung würde sofort beginnen und den Ausgangszustand der letzten Berechnung "
                        + "wiederherstellen",
                "    60 Blöcke zwischen y 53 und y 67",
                "    Nichts wurde geschrieben: der Probelauf prüft den Auslöser, er reinigt die Arena nie",
                "  [ok] Testteams wieder entfernt",
                "    Beide Testteams sind weg, die Teamliste enthält wieder 0 eigene Teams",
                "    teams.yml wurde nie geschrieben, also könnte auch ein Neustart sie nicht zurückbringen",
                "Für die Teile, die an einem Körper wirken, bleibt ein echter Testspieler nötig:",
                "  das Kit, das wirklich in einem Inventar landet, und das alte, das abgeräumt wird",
                "  der Teleport auf die beiden Team-Spawns hinter dem Starttor",
                "  wer als aktiver Kämpfer zählt, was davon abhängt, wer online ist",
                "  der Countdown des Starttors und die Titel, die wirklich über der Arena laufen",
                "Der Probelauf prüft, dass all das eingerichtet ist, aber nie, dass es angekommen ist"),
                fixture.dryRun().run().describe());
    }

    @Test
    void reportsAReadyArenaWithoutAKitAndWithoutAStartGate() {
        fixture.makeTheServerReadyForItsEvent();
        fixture.kitService.setKitDefinition(KitDefinition.empty());
        fixture.gateMarks.clear();

        assertEquals(List.of(
                "Probelauf des Turnierablaufs: 5 von 7 Schritten bestanden, 1 gescheitert, 1 übersprungen",
                "  [ok] Zwei Testteams registriert",
                "    Dry-Run-Alpha mit Dry-Run-Alpha-1, Dry-Run-Alpha-2",
                "    Dry-Run-Bravo mit Dry-Run-Bravo-1, Dry-Run-Bravo-2",
                "    Beide sind über den Namen zu finden und beide Platzhalter-Spieler über ihre eindeutige ID",
                "    Die Mitglieder sind Platzhalter und kein Konto dieses Servers, und in teams.yml wurde nichts "
                        + "geschrieben",
                "  [ok] Arena, Kampffläche und Spawns",
                "    Kampffläche: event mit 4 Zellen zwischen 0/0 und 1/1, von y 64 bis y 67, berechnet auf y 64",
                "    Eine Reinigung schreibt 60 Blöcke zwischen y 53 und y 67",
                "    Beide Team-Spawns und der Tribünen-Spawn sind gesetzt",
                "  [GESCHEITERT] Das Kit, das ein Rundenstart ausgibt",
                "    Das Kit ist leer, jeder Kämpfer würde die Runde mit bloßen Händen beginnen",
                "    Pack das Kit in dein eigenes Inventar, lege es mit \"Kit aus deinem Inventar anlegen\" im "
                        + "Kit-Menü von /tournament an und mach es zum Standard-Kit",
                "  [übersprungen] Countdown des Starttors",
                "    Kein Starttor-Block ist markiert, die Runden beginnen ohne Starttor",
                "    Das ist eine funktionierende Einrichtung, deshalb wird nichts abgelehnt",
                "    Gib dir mit \"Starttor-Blöcke geben\" im Arena-Menü von /tournament Starttor-Blöcke, um eines "
                        + "zu bauen",
                "  [ok] Turnierbaum und ein simuliertes Match",
                "    Turnierbaum über 2 Teams gebaut: 1 Turnierrunde, 1 Match darin",
                "    Match round1-match1: Dry-Run-Alpha gegen Dry-Run-Bravo, beide Teams sind in der Teamliste zu "
                        + "finden",
                "    Die Reihenfolge der Matches lässt es aus: das einzige Match eines Turnierbaums mit zwei Teams "
                        + "ist das Finale, und das Finale ist das eine Match, das ein Admin startet",
                "    Das Finale wechselt auf \"bereit\" und meldet sich einmal: Das Finale Dry-Run-Alpha gegen "
                        + "Dry-Run-Bravo ist bereit, ein Admin startet es mit \"Finale starten\" in /tournament",
                "    Simuliertes Ergebnis für Dry-Run-Alpha: der Turnierbaum ist vollständig, Sieger ist "
                        + "Dry-Run-Alpha und das Finale steht auf \"vorbei\"",
                "    Nichts davon hat die Arena erreicht: keine Runde wurde begonnen, kein Starttor geöffnet und "
                        + "kein Kämpfer teleportiert oder ausgerüstet",
                "  [ok] Die Reinigung nach dem Match",
                "    Eine Reinigung würde sofort beginnen und den Ausgangszustand der letzten Berechnung "
                        + "wiederherstellen",
                "    60 Blöcke zwischen y 53 und y 67",
                "    Nichts wurde geschrieben: der Probelauf prüft den Auslöser, er reinigt die Arena nie",
                "  [ok] Testteams wieder entfernt",
                "    Beide Testteams sind weg, die Teamliste enthält wieder 0 eigene Teams",
                "    teams.yml wurde nie geschrieben, also könnte auch ein Neustart sie nicht zurückbringen",
                "Für die Teile, die an einem Körper wirken, bleibt ein echter Testspieler nötig:",
                "  das Kit, das wirklich in einem Inventar landet, und das alte, das abgeräumt wird",
                "  der Teleport auf die beiden Team-Spawns hinter dem Starttor",
                "  wer als aktiver Kämpfer zählt, was davon abhängt, wer online ist",
                "  der Countdown des Starttors und die Titel, die wirklich über der Arena laufen",
                "Der Probelauf prüft, dass all das eingerichtet ist, aber nie, dass es angekommen ist"),
                fixture.dryRun().run().describe());
    }

    @Test
    void refusesToRunWhileATournamentIsInProgressOrAMatchStandsOnTheArena() {
        fixture.tournamentState = TournamentState.RUNNING;

        assertEquals(List.of(
                "Der Probelauf wurde nicht gestartet",
                "Ein Turnier läuft, der Probelauf ist eine Prüfung für die Zeit vor einem Event",
                "Er registriert zwei Testteams und würde sie in die Teamliste eines laufenden Turnierbaums setzen",
                "Warte, bis das Event vorbei ist, oder brich es im Match-Menü von /tournament ab"),
                fixture.dryRun().run().describe());

        fixture.tournamentState = TournamentState.IDLE;
        fixture.matchRunning = true;

        assertEquals(List.of(
                "Der Probelauf wurde nicht gestartet",
                "Auf der Arena läuft ein Match, der Probelauf ist eine Prüfung für die Zeit vor einem Event",
                "Entscheide es mit \"Ergebnis erzwingen\" im Match-Menü von /tournament oder warte darauf"),
                fixture.dryRun().run().describe());
    }

    @Test
    void namesBothMissingTeamSpawnsAndAStartGateThatIsTallerThanTheCountdown() {
        fixture.makeTheServerReadyForItsEvent();
        fixture.arena.firstTeamSpawn = false;
        fixture.arena.secondTeamSpawn = false;
        fixture.gateDuration.setSeconds(1);
        fixture.markGate(30, 1);

        assertEquals(List.of(
                "Die Arena ist bereit, aber der Spawn des ersten Teams und der Spawn des zweiten Teams sind nicht "
                        + "gesetzt",
                "Ein nicht gesetzter Team-Spawn fällt auf die Mitte der Kampffläche zurück, dann würden beide "
                        + "Teams auf demselben Block starten",
                "Setze sie im Arena-Menü von /tournament, jeweils während du darauf stehst"),
                fixture.findStep(TournamentDryRun.ARENA_STEP).details());
        assertEquals(List.of(
                "Der Countdown zählt von 1 bis 1 und zeigt dann \"Kämpft!\", geprüft, ohne einen Tick zu warten",
                "event: 30 Blöcke in 30 Schichten, die letzte fällt in Tick 30 von 20",
                "  Dieses Starttor ist höher, als der Countdown Ticks hat, deshalb wird es noch 10 Ticks nach "
                        + "\"Kämpft!\" abgebaut"),
                fixture.findStep(TournamentDryRun.GATE_STEP).details());
    }
}

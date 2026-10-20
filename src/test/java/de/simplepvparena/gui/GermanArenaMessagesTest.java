package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaSpawnType;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UseLanguage("de")
class GermanArenaMessagesTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int ARENA_DIAGNOSE_SLOT = 11;
    private static final int ARENA_FIRST_TEAM_SPAWN_SLOT = 12;
    private static final int ARENA_TRIBUNE_SPAWN_SLOT = 14;
    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int ARENA_START_GATE_SLOT = 20;
    private static final int CONFIRM_YES_SLOT = 11;
    private static final double CENTRE_Z = 3.5D;
    private static final String MATCH_RUNNING_CLEANING = "Ein Match läuft, die Arena lässt sich nur zwischen den "
            + "Matches reinigen";
    private static final String MATCH_RUNNING_RESET = "Ein Match läuft, die Arena lässt sich nur zwischen den "
            + "Matches zurücksetzen";
    private static final String RESERVED = "Das Kit für das nächste Match wird gerade gewählt, die Arena ist bis zu "
            + "dessen Start für dieses Match reserviert";
    private static final String CALCULATED = "Die Kampffläche wurde aus den Wandblöcken berechnet\n"
            + "admin-menu mit 25 Zellen zwischen 1/1 und 5/5, von y 64 bis y 67, berechnet auf y 64";
    private static final String BASELINE_READING = "Die 375 Blöcke von y 53 bis y 67, die Kampffläche und die "
            + "Schichten unter ihrem Boden, werden als der Ausgangszustand eingelesen, den \"Arena reinigen\" im "
            + "Arena-Menü von /tournament wiederherstellt\n"
            + "In Portionen von 16384 pro Tick, bis dahin lässt sich die Arena nicht reinigen.\n"
            + "Der Ausgangszustand ist weg, wenn der Server neu startet";
    private static final String RECOGNIZED_WALL = "Die erkannte Wand wird ein paar Sekunden lang angezeigt\n"
            + "96 Wandblöcke, verbunden durch 184 Linien";

    private final AdminMenuFixture fixture = AdminMenuFixture.withKitChoice();

    @Test
    void setsTheFirstTeamSpawnOfAnIncompleteArenaAndNamesWhatIsMissingInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.standInsideTheFightArea();

        fixture.click(ARENA_FIRST_TEAM_SPAWN_SLOT);

        assertEquals(List.of(
                "Der Spawn des ersten Teams wurde auf admin-menu 3.5/64.0/3.5 gesetzt",
                "Der Arena fehlt noch:",
                "die berechnete Kampffläche (\"Kampffläche berechnen\" im Arena-Menü von /tournament)",
                "der Tribünen-Spawn (\"Tribünen-Spawn setzen\" im Arena-Menü von /tournament)"),
                fixture.messages());
    }

    @Test
    void refusesATribuneSpawnInsideTheFightAreaInGerman() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.standInsideTheFightArea();

        fixture.click(ARENA_TRIBUNE_SPAWN_SLOT);

        assertEquals(List.of("Der Tribünen-Spawn muss außerhalb der berechneten Kampffläche liegen, "
                + "admin-menu 3.5/64.0/3.5 liegt darin\n"
                + "Steh außerhalb der Arenawand, wo die Zuschauer warten, dann setze ihn erneut. "
                + "Es wurde nichts gespeichert"), fixture.messages());
    }

    @Test
    void setsATribuneInsideTheAreaOfEffectAndRefusesOneOutsideOfItInGerman() {
        fixture.configureArena();
        fixture.standAt(50.5D, CENTRE_Z);
        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.TRIBUNE);
        fixture.standAt(200.5D, CENTRE_Z);

        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.TRIBUNE);

        assertEquals(List.of("Der Tribünen-Spawn wurde auf admin-menu 50.5/64.0/3.5 gesetzt",
                "Der Tribünen-Spawn muss innerhalb des Wirkungsbereichs liegen, admin-menu 200.5/64.0/3.5 liegt "
                        + "197 Blöcke entfernt von seiner Mitte 3.5/3.5, Radius 99.5 Blöcke\n"
                        + "Stell dich näher an die Kampffläche, dann setze ihn erneut. Es wurde nichts gespeichert\n"
                        + "Der Rand um die Kampffläche wird in config.yml unter arena.effect-margin eingestellt"),
                fixture.messages());
    }

    @Test
    void calculatesTheFightAreaAndWarnsAboutATribuneOutsideOfTheAreaOfEffectInGerman() {
        fixture.configureArena();
        fixture.arenaRegistry().getArena().setTribuneSpawn(
                new PositionSnapshot("admin-menu", 300.5D, 64.0D, CENTRE_Z, 0F, 0F));
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertEquals(List.of(CALCULATED, BASELINE_READING, RECOGNIZED_WALL,
                "Der Tribünen-Spawn admin-menu 300.5/64.0/3.5 liegt außerhalb des Wirkungsbereichs, 297 Blöcke "
                        + "entfernt von seiner Mitte 3.5/3.5, Radius 99.5 Blöcke\n"
                        + "Die Berechnung bleibt gültig. Setze den Tribünen-Spawn näher an der Kampffläche erneut, "
                        + "der Rand um sie wird in config.yml unter arena.effect-margin eingestellt"),
                fixture.messages());
    }

    @Test
    void refusesTheCalculationWhileAMatchRunsInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertEquals(List.of("Ein Match läuft, die Kampffläche lässt sich nur zwischen den Matches berechnen"),
                fixture.messages());
    }

    @Test
    void refusesTheCleaningOfAnArenaWithoutAFightAreaInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_CLEAN_SLOT);

        assertEquals(List.of("Die Kampffläche ist nicht berechnet, es gibt nichts zu reinigen\n"
                + "Nutze zuerst \"Kampffläche berechnen\" im Arena-Menü von /tournament, während du innerhalb der "
                + "Arenawand stehst"), fixture.messages());
    }

    @Test
    void refusesTheCleaningAndTheResetWhileAMatchRunsInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_CLEAN_SLOT);
        fixture.click(ARENA_RESET_SLOT);

        assertEquals(List.of(MATCH_RUNNING_CLEANING, MATCH_RUNNING_RESET), fixture.messages());
    }

    @Test
    void refusesTheCleaningTheResetAndTheCalculationWhileTheKitIsChosenInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.startTournament();
        assertTrue(fixture.tournamentManager().isKitChoicePending());
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.messages().clear();

        fixture.click(ARENA_CLEAN_SLOT);
        fixture.click(ARENA_RESET_SLOT);
        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertEquals(List.of(RESERVED, RESERVED, RESERVED), fixture.messages());
    }

    @Test
    void cleansTheArenaAfterTheConfirmationInGerman() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(ARENA_CLEAN_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Es ist kein Ausgangszustand der Kampffläche gespeichert, stattdessen wird jeder ihrer "
                        + "Blöcke auf Luft gesetzt\n"
                        + "Einen Ausgangszustand nimmt \"Kampffläche berechnen\" im Arena-Menü von /tournament auf, "
                        + "er übersteht keinen Neustart des Servers",
                "Die Arena wird gereinigt, 100 Blöcke zwischen y 64 und y 67\n"
                        + "Verarbeitet in Portionen von 4096 pro Tick\n"
                        + "Schicht für Schicht vom Boden aufwärts: Die rote Flut kündigt das Volumen an, die grüne "
                        + "Räumung schreibt es, 6.0 Sekunden für beide zusammen"), fixture.messages());
        assertEquals(Optional.of("Die Arena wird bereits gereinigt"),
                fixture.arenaSetupService().findCleaningRejection());
    }

    @Test
    void asksForTheDiagnosisCellLimitAndCancelsItWithTheGermanWordInGerman() {
        askForTheDiagnosisCellLimit();

        assertEquals(List.of(
                "Gib im Chat ein: <Zellenlimit>",
                "Eine Zahl zwischen 1 und 5000000",
                "Steh innerhalb der Arenawand, die Berechnung läuft ab der Stelle, an der du stehst",
                "config.yml wird dadurch nicht geändert",
                "Gib abbrechen ein, um abzubrechen, die Anfrage läuft in 60 Sekunden ab"), fixture.messages());

        fixture.messages().clear();
        assertTrue(fixture.typeInChat("abbrechen"));
        fixture.runNextTick();

        assertEquals(List.of("Die Eingabe für die Diagnose mit einem höheren Zellenlimit wurde abgebrochen, "
                + "nichts wurde geändert"), fixture.messages());
    }

    @Test
    void runsTheDiagnosisWithTheTypedCellLimitInGerman() {
        askForTheDiagnosisCellLimit();
        fixture.messages().clear();

        fixture.typeInChat("60000");
        fixture.runNextTick();

        assertEquals(List.of("Einmalige Berechnung mit einem Limit von 60000 Zellen\n"
                        + "statt der 50000 aus arena.shape-maximum-cells, config.yml wird nicht geändert",
                CALCULATED, BASELINE_READING, RECOGNIZED_WALL), fixture.messages());
    }

    @Test
    void refusesTwoValuesForTheDiagnosisCellLimitInGerman() {
        askForTheDiagnosisCellLimit();
        fixture.messages().clear();

        fixture.typeInChat("500 Zellen");
        fixture.runNextTick();

        assertEquals(List.of("Erwartet wird 1 Wert (<Zellenlimit>), eingegeben wurden 2: 500 Zellen"),
                fixture.messages());
    }

    @Test
    void refusesACellLimitThatIsNoNumberInGerman() {
        askForTheDiagnosisCellLimit();
        fixture.messages().clear();

        fixture.typeInChat("viele");
        fixture.runNextTick();

        assertEquals(List.of("Das Zellenlimit viele ist keine Zahl"), fixture.messages());
    }

    @Test
    void asksForTheStartGateBlocksAndCancelsItWithTheGermanWordInGerman() {
        askForTheStartGateBlocks();

        assertEquals(List.of(
                "Gib im Chat ein: <Material> <Menge>",
                "Beispiel: barrier 32",
                "Die Menge muss zwischen 1 und 64 liegen",
                "Gib abbrechen ein, um abzubrechen, die Anfrage läuft in 60 Sekunden ab"), fixture.messages());

        fixture.messages().clear();
        assertTrue(fixture.typeInChat(" Abbrechen "));
        fixture.runNextTick();

        assertEquals(List.of("Die Eingabe für die Starttor-Blöcke wurde abgebrochen, nichts wurde geändert"),
                fixture.messages());
    }

    @Test
    void namesTheHandedOutItemsInGerman() {
        assertEquals("Du hast die Höhenmarke der Kampffläche bekommen\n"
                + "Setze damit einen Block am tiefsten Punkt der Fläche, das Item wechselt von selbst zum höchsten\n"
                + "Der gesetzte Block verschwindet wieder, nur seine Höhe wird gemerkt",
                Messages.text(ArenaMenuMessage.HEIGHT_MARKER_HANDED_OUT));
        assertEquals("Dein Inventar ist voll, die Höhenmarke konnte nicht ausgegeben werden",
                Messages.text(ArenaMenuMessage.HEIGHT_MARKER_INVENTORY_FULL));
        assertEquals("Dein Inventar ist voll, nicht jeder Starttor-Block konnte ausgegeben werden",
                Messages.text(ArenaMenuMessage.START_GATE_INVENTORY_FULL));
    }

    @Test
    void resetsTheArenaAfterTheConfirmationInGerman() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(ARENA_RESET_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Die Konfiguration der Arena wurde entfernt\n"
                + "Ihre Blöcke und die Wandblöcke bleiben unberührt"), fixture.messages());
    }

    private void askForTheDiagnosisCellLimit() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.standInsideTheFightArea();
        fixture.messages().clear();
        fixture.click(ARENA_DIAGNOSE_SLOT);
    }

    private void askForTheStartGateBlocks() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.messages().clear();
        fixture.click(ARENA_START_GATE_SLOT);
    }
}

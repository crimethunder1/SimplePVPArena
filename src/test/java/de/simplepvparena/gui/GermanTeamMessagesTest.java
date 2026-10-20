package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanTeamMessagesTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_SMALLER_SLOT = 46;
    private static final int TEAMS_LARGER_SLOT = 48;
    private static final int TEAMS_ADD_SLOT = 51;
    private static final int TEAM_RENAME_SLOT = 10;
    private static final int TEAM_ADD_PLAYER_SLOT = 11;
    private static final int TEAM_REMOVE_PLAYER_SLOT = 12;
    private static final int TEAM_REMOVE_SLOT = 13;
    private static final int CONFIRMATION_YES_SLOT = 11;
    private static final int FIRST_MEMBER_SLOT = 9;
    private static final String GERMAN_NAME_RULE = "1 bis 16 Buchstaben, Ziffern, Bindestriche oder Unterstriche";
    private static final String GERMAN_PROMPT_FOOTER = "Gib abbrechen ein, um abzubrechen, die Anfrage läuft in 60 "
            + "Sekunden ab";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void asksForTheTeamToAddInGerman() {
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(TEAMS_ADD_SLOT);

        assertEquals(List.of(
                "Gib im Chat ein: <Teamname> <Spieler> [<Spieler> ...]",
                "Ein Team hat 1 bis 2 Spieler, ein Teamname hat " + GERMAN_NAME_RULE,
                "Beispiel: Alpha Steve Alex",
                "Beispiel: Bravo Notch Jeb_ Dinnerbone",
                "Jeder Spieler muss online oder dem Server bekannt sein",
                GERMAN_PROMPT_FOOTER), fixture.messages());
    }

    @Test
    void registersATeamInGerman() {
        askForTheTeamToAdd();

        typeAndRunTheNextTick("Bravo Steve Alex");

        assertEquals(List.of("Team Bravo mit Steve, Alex registriert"), fixture.messages());
    }

    @Test
    void refusesADuplicateTeamNameInGerman() {
        askForTheTeamToAdd();

        typeAndRunTheNextTick("ALPHA Steve Alex");

        assertEquals(List.of("Team ALPHA konnte nicht registriert werden\nEin Team namens ALPHA gibt es bereits"),
                fixture.messages());
    }

    @Test
    void refusesAPlayerOfAnotherTeamInGerman() {
        askForTheTeamToAdd();
        fixture.teamRegistry().registerTeam("Bravo", List.of(fixture.knownPlayer("Steve")));

        typeAndRunTheNextTick("Charlie Steve Alex");

        assertEquals(List.of("Team Charlie konnte nicht registriert werden\n"
                + "Steve ist bereits in Team Bravo registriert"), fixture.messages());
    }

    @Test
    void refusesAnInvalidTeamNameInGerman() {
        askForTheTeamToAdd();

        typeAndRunTheNextTick("Team.One Steve Alex");

        assertEquals(List.of("Team Team.One konnte nicht registriert werden\n"
                + "Der Teamname Team.One ist nicht erlaubt, ein Teamname hat " + GERMAN_NAME_RULE), fixture.messages());
    }

    @Test
    void refusesAnUnknownPlayerInGerman() {
        askForTheTeamToAdd();

        typeAndRunTheNextTick("Bravo Steve Herobrine");

        assertEquals(List.of("Unbekannter Spieler Herobrine\nDer Spieler muss online oder dem Server bekannt sein"),
                fixture.messages());
    }

    @Test
    void refusesATeamLineWithTooFewValuesWithTheGermanUsage() {
        askForTheTeamToAdd();

        typeAndRunTheNextTick("Bravo");

        assertEquals(List.of("Erwartet werden 2 bis 3 durch Leerzeichen getrennte Werte "
                + "(<Teamname> <Spieler> [<Spieler> ...]), eingegeben: 1 (Bravo)"), fixture.messages());
    }

    @Test
    void namesTheTeamToAddInGermanWhenTheInputIsCancelledWithEitherWord() {
        List<String> cancellations = new ArrayList<>();
        for (String cancelWord : List.of("abbrechen", "cancel")) {
            askForTheTeamToAdd();
            typeAndRunTheNextTick(cancelWord);
            cancellations.addAll(fixture.messages());
        }

        assertEquals(List.of(
                "Die Eingabe für das neue Team wurde abgebrochen, nichts wurde geändert",
                "Die Eingabe für das neue Team wurde abgebrochen, nichts wurde geändert"), cancellations);
    }

    @Test
    void asksForTheNewNameAndNamesItInGermanWhenTheInputIsCancelledWithEitherWord() {
        fixture.registerTeams("Alpha");
        openTheOptionsOf("Alpha");
        fixture.click(TEAM_RENAME_SLOT);
        List<String> prompt = List.copyOf(fixture.messages());
        fixture.messages().clear();
        typeAndRunTheNextTick("abbrechen");
        String germanWord = fixture.messages().get(0);
        openTheOptionsOf("Alpha");
        fixture.click(TEAM_RENAME_SLOT);
        fixture.messages().clear();
        typeAndRunTheNextTick("cancel");

        assertEquals(List.of(
                "Gib im Chat ein: <Neuer Name>",
                "Ein Teamname hat " + GERMAN_NAME_RULE,
                "Beispiel: Phoenix",
                "Das Team behält seine Spieler und seinen Platz in der Team-Liste",
                GERMAN_PROMPT_FOOTER), prompt);
        assertEquals("Die Eingabe für den neuen Namen von Team Alpha wurde abgebrochen, nichts wurde geändert",
                germanWord);
        assertEquals(List.of("Die Eingabe für den neuen Namen von Team Alpha wurde abgebrochen, nichts wurde "
                + "geändert"), fixture.messages());
    }

    @Test
    void renamesATeamAndRefusesTheNameOfAnotherTeamInGerman() {
        fixture.registerTeams("Alpha", "Bravo");
        openTheOptionsOf("Alpha");
        fixture.click(TEAM_RENAME_SLOT);
        fixture.messages().clear();
        typeAndRunTheNextTick("BRAVO");
        List<String> refusal = List.copyOf(fixture.messages());
        fixture.click(TEAM_RENAME_SLOT);
        fixture.messages().clear();

        typeAndRunTheNextTick("Phoenix");

        assertEquals(List.of("Team Alpha konnte nicht umbenannt werden\nEin Team namens BRAVO gibt es bereits"),
                refusal);
        assertEquals(List.of("Team Alpha heißt jetzt Phoenix"), fixture.messages());
    }

    @Test
    void asksForThePlayerToAddAndNamesItInGermanWhenTheInputIsCancelledWithEitherWord() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha");
        openTheOptionsOf("Alpha");
        fixture.click(TEAM_ADD_PLAYER_SLOT);
        List<String> prompt = List.copyOf(fixture.messages());
        fixture.messages().clear();
        typeAndRunTheNextTick("abbrechen");
        String germanWord = fixture.messages().get(0);
        openTheOptionsOf("Alpha");
        fixture.click(TEAM_ADD_PLAYER_SLOT);
        fixture.messages().clear();
        typeAndRunTheNextTick("cancel");

        assertEquals(List.of(
                "Gib im Chat ein: <Spieler>",
                "Team Alpha hat 2 von 3 Spielern",
                "Beispiel: Steve",
                "Jeder Spieler muss online oder dem Server bekannt sein",
                GERMAN_PROMPT_FOOTER), prompt);
        assertEquals("Die Eingabe für den neuen Spieler von Team Alpha wurde abgebrochen, nichts wurde geändert",
                germanWord);
        assertEquals(List.of("Die Eingabe für den neuen Spieler von Team Alpha wurde abgebrochen, nichts wurde "
                + "geändert"), fixture.messages());
    }

    @Test
    void addsAPlayerAndRefusesAPlayerOfAnotherTeamInGerman() {
        fixture.setTeamSize(4);
        fixture.makePlayersKnown("Steve", "Alex");
        fixture.registerTeams("Alpha");
        fixture.teamRegistry().registerTeam("Bravo", List.of(fixture.knownPlayer("Alex")));
        openTheOptionsOf("Alpha");
        fixture.click(TEAM_ADD_PLAYER_SLOT);
        fixture.messages().clear();
        typeAndRunTheNextTick("Steve");
        List<String> added = List.copyOf(fixture.messages());
        fixture.click(TEAM_ADD_PLAYER_SLOT);
        fixture.messages().clear();

        typeAndRunTheNextTick("Alex");

        assertEquals(List.of("Steve ist Team Alpha beigetreten"), added);
        assertEquals(List.of("Alex konnte Team Alpha nicht beitreten\nAlex ist bereits in Team Bravo registriert"),
                fixture.messages());
    }

    @Test
    void refusesAPlayerForAFullTeamInGerman() {
        fixture.registerTeams("Alpha");
        openTheOptionsOf("Alpha");

        fixture.click(TEAM_ADD_PLAYER_SLOT);

        assertEquals(List.of("Team Alpha hat bereits 2 Spieler, die Teamgröße ist 2"), fixture.messages());
    }

    @Test
    void removesAPlayerAndRefusesToRemoveTheLastOneInGerman() {
        fixture.registerTeamOf("Duo", "Steve", "Alex");
        openTheOptionsOf("Duo");
        fixture.click(TEAM_REMOVE_PLAYER_SLOT);
        fixture.messages().clear();
        fixture.click(FIRST_MEMBER_SLOT);
        List<String> removed = List.copyOf(fixture.messages());
        fixture.messages().clear();

        fixture.click(FIRST_MEMBER_SLOT);

        assertEquals(List.of("Steve hat Team Duo verlassen"), removed);
        assertEquals(List.of("Ein Team behält mindestens 1 Spieler, entferne stattdessen das Team"),
                fixture.messages());
    }

    @Test
    void removesATeamInGerman() {
        fixture.registerTeams("Alpha", "Bravo");
        openTheOptionsOf("Alpha");
        fixture.click(TEAM_REMOVE_SLOT);
        fixture.messages().clear();

        fixture.click(CONFIRMATION_YES_SLOT);

        assertEquals(List.of("Team Alpha entfernt"), fixture.messages());
    }

    @Test
    void refusesASmallerTeamSizeAndNamesTheLargerTeamsWithUnd() {
        fixture.setTeamSize(3);
        fixture.registerTeamOf("Alpha", "Steve", "Alex", "Notch");
        fixture.registerTeamOf("Bravo", "Jeb_", "Dinnerbone", "Grumm");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(TEAMS_SMALLER_SLOT);

        assertEquals(List.of("2 Teams haben mehr als 2 Spieler: Alpha (Steve, Alex, Notch) und "
                + "Bravo (Jeb_, Dinnerbone, Grumm). Entferne sie zuerst"), fixture.messages());
    }

    @Test
    void announcesTheNewTeamSizeAndAFailedWriteInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_LARGER_SLOT);
        List<String> written = List.copyOf(fixture.messages());
        fixture.messages().clear();
        fixture.failTeamSizeWrites(new IOException("Datenträger voll"));

        fixture.click(TEAMS_LARGER_SLOT);

        assertEquals(List.of("Die Teamgröße ist jetzt 3 (3 gegen 3)"), written);
        assertEquals(List.of("Die Teamgröße ist jetzt 4 (4 gegen 4), aber config.yml konnte nicht geschrieben "
                + "werden: Datenträger voll"), fixture.messages());
    }

    @Test
    void refusesATeamSizeBeyondTheLimitsInGerman() {
        fixture.setTeamSize(1);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_SMALLER_SLOT);
        List<String> smallest = List.copyOf(fixture.messages());
        fixture.messages().clear();
        fixture.setTeamSize(8);

        fixture.click(TEAMS_LARGER_SLOT);

        assertEquals(List.of("Die kleinste Teamgröße ist 1"), smallest);
        assertEquals(List.of("Die größte Teamgröße ist 8"), fixture.messages());
    }

    private void askForTheTeamToAdd() {
        fixture.makePlayersKnown("Steve", "Alex");
        if (fixture.teamRegistry().findTeam("Alpha").isEmpty()) {
            fixture.registerTeams("Alpha");
        }
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_ADD_SLOT);
        fixture.messages().clear();
    }

    private void openTheOptionsOf(String teamName) {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(fixture.teamRegistry().getTeamNames().indexOf(teamName));
        fixture.messages().clear();
    }

    private void typeAndRunTheNextTick(String line) {
        fixture.typeInChat(line);
        fixture.runNextTick();
    }
}

package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UseLanguage("de")
class GermanTeamMenuSnapshotTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;
    private static final int TEAM_REMOVE_PLAYER_SLOT = 12;
    private static final int TEAM_REMOVE_SLOT = 13;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheFirstPageOfSeveralTeamsInGerman() {
        fixture.registerNumberedTeams(46);

        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertEquals("Teams (Seite 1) | 54\n" + describeTeamSlots(1, 45) + """
                46 | RED_DYE | Kleinere Teams | Klicke, um Teams aus 1 Spieler zu bilden
                47 | BOOK | Teams | Registrierte Teams: 46 / Seite 1 von 2 / Teamgröße: 2 (2 gegen 2) / \
                Ein Klick auf ein Team öffnet seine Optionen
                48 | LIME_DYE | Größere Teams | Klicke, um Teams aus 3 Spielern zu bilden
                49 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
                51 | LIME_WOOL | Team hinzufügen | Fragt im Chat nach <Teamname> <Spieler> [<Spieler> ...] / \
                Spielernamen lassen sich nicht in ein Inventar tippen / \
                Das Menü schließt sich, damit du tippen kannst
                53 | ARROW | Nächste Seite | Seite 2 von 2
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheSecondPageOfSeveralTeamsInGerman() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(TEAMS_NEXT_PAGE_SLOT);

        assertEquals("""
                Teams (Seite 2) | 54
                0 | PLAYER_HEAD | Team46 | Mitglieder: Team46-first, Team46-second / \
                Klicke für die Optionen dieses Teams
                45 | ARROW | Vorherige Seite | Seite 1 von 2
                46 | RED_DYE | Kleinere Teams | Klicke, um Teams aus 1 Spieler zu bilden
                47 | BOOK | Teams | Registrierte Teams: 46 / Seite 2 von 2 / Teamgröße: 2 (2 gegen 2) / \
                Ein Klick auf ein Team öffnet seine Optionen
                48 | LIME_DYE | Größere Teams | Klicke, um Teams aus 3 Spielern zu bilden
                49 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
                51 | LIME_WOOL | Team hinzufügen | Fragt im Chat nach <Teamname> <Spieler> [<Spieler> ...] / \
                Spielernamen lassen sich nicht in ein Inventar tippen / \
                Das Menü schließt sich, damit du tippen kannst
                """, fixture.describeLastMenu());
    }

    @Test
    void namesTheSmallestAndTheLargestTeamSizeOnTheGreyTilesInGerman() {
        fixture.setTeamSize(1);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        String smallestSize = fixture.describeLastMenu();
        fixture.setTeamSize(8);
        fixture.refillLastMenu();
        String largestSize = fixture.describeLastMenu();

        assertTrue(smallestSize.contains("46 | GRAY_DYE | Kleinere Teams | Die kleinste Teamgröße ist 1\n"),
                smallestSize);
        assertTrue(smallestSize.contains(
                "48 | LIME_DYE | Größere Teams | Klicke, um Teams aus 2 Spielern zu bilden\n"), smallestSize);
        assertTrue(largestSize.contains(
                "46 | RED_DYE | Kleinere Teams | Klicke, um Teams aus 7 Spielern zu bilden\n"), largestSize);
        assertTrue(largestSize.contains("48 | GRAY_DYE | Größere Teams | Die größte Teamgröße ist 8\n"), largestSize);
    }

    @Test
    void showsTheOptionsOfAFullTeamInGerman() {
        fixture.registerTeams("Alpha", "Bravo");

        openTheOptionsOf("Alpha");

        assertEquals("""
                Team Alpha | 27
                4 | BOOK | Alpha | Mitglieder: Alpha-first, Alpha-second / Spieler: 2 von 2
                10 | NAME_TAG | Team umbenennen | Fragt im Chat nach dem neuen Namen / \
                Das Team behält seinen Platz in der Team-Liste / Das Menü schließt sich, damit du tippen kannst
                11 | GRAY_DYE | Spieler hinzufügen | Das Team hat bereits 2 Spieler, die Teamgröße ist 2
                12 | RED_DYE | Spieler entfernen | Zeigt die Spieler von Alpha / \
                Ein Klick auf einen Spieler entfernt ihn
                13 | RED_WOOL | Team entfernen | Entfernt Alpha aus den registrierten Teams / \
                Fragt vorher nach einer Bestätigung
                22 | OAK_DOOR | Zurück | Zurück zur Team-Liste
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheOptionsOfATeamOfOnePlayerBelowTheTeamSizeInGerman() {
        fixture.setTeamSize(3);
        fixture.registerTeamOf("Solo", "Steve");

        openTheOptionsOf("Solo");

        assertEquals("""
                Team Solo | 27
                4 | BOOK | Solo | Mitglieder: Steve / Spieler: 1 von 3
                10 | NAME_TAG | Team umbenennen | Fragt im Chat nach dem neuen Namen / \
                Das Team behält seinen Platz in der Team-Liste / Das Menü schließt sich, damit du tippen kannst
                11 | LIME_DYE | Spieler hinzufügen | Fragt im Chat nach dem Namen des Spielers / \
                Das Menü schließt sich, damit du tippen kannst
                12 | GRAY_DYE | Spieler entfernen | Ein Team behält mindestens 1 Spieler, entferne stattdessen das Team
                13 | RED_WOOL | Team entfernen | Entfernt Solo aus den registrierten Teams / \
                Fragt vorher nach einer Bestätigung
                22 | OAK_DOOR | Zurück | Zurück zur Team-Liste
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheOptionsOfARemovedTeamInGerman() {
        fixture.registerTeams("Alpha", "Bravo");
        openTheOptionsOf("Alpha");
        assertTrue(fixture.teamRegistry().removeTeam("Alpha"));

        fixture.refillLastMenu();

        assertEquals("""
                Team Alpha | 27
                4 | BOOK | Alpha | Dieses Team ist nicht mehr registriert
                10 | GRAY_DYE | Team umbenennen | Dieses Team ist nicht mehr registriert
                11 | GRAY_DYE | Spieler hinzufügen | Dieses Team ist nicht mehr registriert
                12 | GRAY_DYE | Spieler entfernen | Dieses Team ist nicht mehr registriert
                13 | RED_WOOL | Team entfernen | Entfernt Alpha aus den registrierten Teams / \
                Fragt vorher nach einer Bestätigung
                22 | OAK_DOOR | Zurück | Zurück zur Team-Liste
                """, fixture.describeLastMenu());
    }

    @Test
    void asksForTheRemovalOfATeamInGerman() {
        fixture.registerTeams("Alpha", "Bravo");
        openTheOptionsOf("Alpha");

        fixture.click(TEAM_REMOVE_SLOT);

        assertEquals("""
                Team entfernen? | 27
                4 | BOOK | Team entfernen? | Team Alpha wird aus den registrierten Teams entfernt / \
                Ein Team, das ein Match spielt, lässt sich nicht entfernen
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());
    }

    @Test
    void showsThePlayersOfATeamInGerman() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha");
        fixture.registerTeamOf("Trio", "Steve", "Alex", "Notch");
        openTheOptionsOf("Trio");

        fixture.click(TEAM_REMOVE_PLAYER_SLOT);

        assertEquals("""
                Spieler von Trio | 27
                4 | BOOK | Trio | Spieler: 3 von 3 / Ein Klick auf einen Spieler entfernt ihn
                9 | PLAYER_HEAD | Steve | Klicke, um den Spieler aus dem Team zu entfernen
                10 | PLAYER_HEAD | Alex | Klicke, um den Spieler aus dem Team zu entfernen
                11 | PLAYER_HEAD | Notch | Klicke, um den Spieler aus dem Team zu entfernen
                22 | OAK_DOOR | Zurück | Zurück zu den Optionen von Team Trio
                """, fixture.describeLastMenu());
    }

    @Test
    void showsThePlayersOfARemovedTeamInGerman() {
        fixture.registerTeams("Alpha");
        openTheOptionsOf("Alpha");
        fixture.click(TEAM_REMOVE_PLAYER_SLOT);
        assertTrue(fixture.teamRegistry().removeTeam("Alpha"));

        fixture.refillLastMenu();

        assertEquals("""
                Spieler von Alpha | 27
                4 | BOOK | Alpha | Dieses Team ist nicht mehr registriert
                22 | OAK_DOOR | Zurück | Zurück zu den Optionen von Team Alpha
                """, fixture.describeLastMenu());
    }

    private void openTheOptionsOf(String teamName) {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(fixture.teamRegistry().getTeamNames().indexOf(teamName));
    }

    private static String describeTeamSlots(int firstTeamNumber, int lastTeamNumber) {
        StringBuilder description = new StringBuilder();
        for (int teamNumber = firstTeamNumber; teamNumber <= lastTeamNumber; teamNumber++) {
            String teamName = AdminMenuFixture.numberedTeamName(teamNumber);
            description.append(teamNumber - firstTeamNumber).append(" | PLAYER_HEAD | ").append(teamName)
                    .append(" | Mitglieder: ").append(teamName).append("-first, ").append(teamName)
                    .append("-second / Klicke für die Optionen dieses Teams\n");
        }
        return description.toString();
    }
}

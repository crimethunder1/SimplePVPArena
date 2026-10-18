package de.simplepvparena.gui;

import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UseLanguage("de")
class GermanTextsTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_ADD_SLOT = 51;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;

    @Test
    void theBackTileNamesItsTargetInAWholeGermanSentence() {
        assertEquals(new MenuTile(Material.OAK_DOOR, "Zurück", List.of("Zurück zum Hauptmenü")),
                MenuItems.back(MenuMessage.BACK_TO_MAIN_MENU));
        assertEquals(new MenuTile(Material.OAK_DOOR, "Zurück", List.of("Zurück zur Kit-Liste")),
                MenuItems.back(MenuMessage.BACK_TO_KIT_LIST));
        assertEquals(new MenuTile(Material.OAK_DOOR, "Zurück", List.of("Zurück zu den Optionen von Team Alpha")),
                MenuItems.back(MenuMessage.BACK_TO_TEAM_OPTIONS, Placeholder.of("team", "Alpha")));
    }

    @Test
    void thePageTilesNameTheirTargetPageInGerman() {
        MenuPagination pagination = new MenuPagination(100, MenuPagination.DEFAULT_ITEMS_PER_PAGE, 1);

        assertEquals(new MenuTile(Material.ARROW, "Vorherige Seite", List.of("Seite 1 von 3")),
                MenuItems.previousPage(pagination));
        assertEquals(new MenuTile(Material.ARROW, "Nächste Seite", List.of("Seite 3 von 3")),
                MenuItems.nextPage(pagination));
    }

    @Test
    void thePrefixIsGermanAndStaysOutOfTheBody() {
        assertEquals("Turnier", TournamentMessages.prefixName());
        assertEquals("[Turnier] Gespeichert",
                TournamentMessages.toPlainText(TournamentMessages.success("Gespeichert")));
        assertEquals("Gespeichert", TournamentMessages.toPlainBody(TournamentMessages.success("Gespeichert")));
        assertEquals("[Turnier] ", TournamentMessages.plainPrefix());
    }

    @Test
    void theTeamMenuShowsTheGermanBackAndPageTiles() {
        AdminMenuFixture fixture = new AdminMenuFixture();
        fixture.registerNumberedTeams(46);

        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        String firstPage = fixture.describeLastMenu();
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        String secondPage = fixture.describeLastMenu();

        assertTrue(firstPage.contains("49 | OAK_DOOR | Zurück | Zurück zum Hauptmenü\n"), firstPage);
        assertTrue(firstPage.contains("53 | ARROW | Nächste Seite | Seite 2 von 2\n"), firstPage);
        assertTrue(secondPage.contains("45 | ARROW | Vorherige Seite | Seite 1 von 2\n"), secondPage);
    }

    @Test
    void aRefusedMenuAnswersInGermanWithTheGermanPrefix() {
        AdminMenuFixture fixture = new AdminMenuFixture();
        fixture.makePlayersKnown("Steve", "Alex");
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_ADD_SLOT);
        fixture.messages().clear();
        fixture.revokeAdminPermission();

        fixture.typeInChat("Bravo Steve Alex");
        fixture.runNextTick();

        assertEquals(List.of("Du darfst das Turniermenü nicht benutzen"), fixture.messages());
        assertEquals("[Turnier] Du darfst das Turniermenü nicht benutzen",
                TournamentMessages.toPlainText(fixture.lastSentMessage()));
    }
}

package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatInputCancelWordTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_ADD_SLOT = 51;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    @UseLanguage("de")
    void theGermanPromptNamesTheGermanCancelWord() {
        fixture.makePlayersKnown("Steve", "Alex");
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(TEAMS_ADD_SLOT);

        List<String> messages = fixture.messages();
        assertEquals("Gib abbrechen ein, um abzubrechen, die Anfrage läuft in 60 Sekunden ab",
                messages.get(messages.size() - 1));
    }

    @Test
    @UseLanguage("de")
    void theGermanCancelWordCancelsInGerman() {
        askForTheTeamToAdd();

        assertTrue(fixture.typeInChat(" Abbrechen "));
        fixture.runNextTick();

        assertCancelledInGerman();
    }

    @Test
    @UseLanguage("de")
    void theEnglishCancelWordCancelsInGermanAsWell() {
        askForTheTeamToAdd();

        assertTrue(fixture.typeInChat("cancel"));
        fixture.runNextTick();

        assertCancelledInGerman();
    }

    @Test
    void theGermanCancelWordIsAnOrdinaryAnswerInEnglish() {
        askForTheTeamToAdd();

        assertTrue(fixture.typeInChat("abbrechen"));
        fixture.runNextTick();

        assertFalse(fixture.messages().stream().anyMatch(message -> message.contains("was cancelled")),
                fixture.messages().toString());
        assertEquals(0, fixture.pendingChatInputCount());
    }

    private void askForTheTeamToAdd() {
        fixture.makePlayersKnown("Steve", "Alex");
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_ADD_SLOT);
        fixture.messages().clear();
    }

    private void assertCancelledInGerman() {
        assertEquals(1, fixture.messages().size(), fixture.messages().toString());
        String message = fixture.messages().get(0);
        assertTrue(message.startsWith("Die Eingabe für "), message);
        assertTrue(message.endsWith(" wurde abgebrochen, nichts wurde geändert"), message);
        assertEquals(List.of("Alpha"), fixture.teamRegistry().getTeamNames());
    }
}

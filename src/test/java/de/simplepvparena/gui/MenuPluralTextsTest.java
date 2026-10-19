package de.simplepvparena.gui;

import de.simplepvparena.message.MainMenuMessage;
import de.simplepvparena.message.MessageCatalog;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.WaitingMatchMessage;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuPluralTextsTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MAIN_KIT_SLOT = 24;
    private static final int MATCH_DECIDE_WAITING_MATCH_SLOT = 14;
    private static final int WAITING_MATCHES_INFO_SLOT = 47;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    void theRegisteredTeamsTakeTheSingularFormForOneTeam() {
        Messages.use(catalogWithDistinctPluralForms());
        fixture.registerTeams("Alpha");

        fixture.openMainMenu();

        assertEquals("One registered team: 1", infoLore().get(1));
        assertEquals("One registered team: 1", loreOf(MAIN_TEAMS_SLOT).get(0));
    }

    @Test
    void theRegisteredTeamsTakeThePluralFormForTwoTeams() {
        Messages.use(catalogWithDistinctPluralForms());
        fixture.registerTeams("Alpha", "Bravo");

        fixture.openMainMenu();

        assertEquals("Several registered teams: 2", infoLore().get(1));
        assertEquals("Several registered teams: 2", loreOf(MAIN_TEAMS_SLOT).get(0));
    }

    @Test
    void theKitCountTakesTheSingularFormForOneKit() {
        Messages.use(catalogWithDistinctPluralForms());
        fixture.createKit("Archer");

        fixture.openMainMenu();

        assertEquals("One kit: 1", loreOf(MAIN_KIT_SLOT).get(0));
    }

    @Test
    void theKitCountTakesThePluralFormForTwoKits() {
        Messages.use(catalogWithDistinctPluralForms());
        fixture.createKit("Archer");
        fixture.createKit("Knight");

        fixture.openMainMenu();

        assertEquals("Several kits: 2", loreOf(MAIN_KIT_SLOT).get(0));
    }

    @Test
    void theWaitingMatchesTakeTheSingularFormForOneMatch() {
        Messages.use(catalogWithDistinctPluralForms());
        startATournamentWithOneWaitingMatch();

        openTheWaitingMatches();

        assertEquals("One waiting match: 1", loreOf(WAITING_MATCHES_INFO_SLOT).get(0));
    }

    @Test
    void theWaitingMatchesTakeThePluralFormForTwoMatches() {
        Messages.use(catalogWithDistinctPluralForms());
        startATournamentWithTwoWaitingMatches();

        openTheWaitingMatches();

        assertEquals("Several waiting matches: 2", loreOf(WAITING_MATCHES_INFO_SLOT).get(0));
    }

    @Test
    void theShippedEnglishTextsCountOneAndTwo() {
        fixture.registerTeams("Alpha");
        fixture.createKit("Archer");
        fixture.openMainMenu();
        List<String> oneOfEach = List.of(infoLore().get(1), loreOf(MAIN_KIT_SLOT).get(0));

        fixture.registerTeams("Bravo");
        fixture.createKit("Knight");
        fixture.openMainMenu();
        List<String> twoOfEach = List.of(infoLore().get(1), loreOf(MAIN_KIT_SLOT).get(0));

        assertEquals(List.of("Registered teams: 1", "Kits: 1"), oneOfEach);
        assertEquals(List.of("Registered teams: 2", "Kits: 2"), twoOfEach);
    }

    @Test
    @UseLanguage("de")
    void theShippedGermanTextsCountOneAndTwo() {
        fixture.registerTeams("Alpha");
        fixture.createKit("Archer");
        fixture.openMainMenu();
        List<String> oneOfEach = List.of(infoLore().get(1), loreOf(MAIN_KIT_SLOT).get(0));

        fixture.registerTeams("Bravo");
        fixture.createKit("Knight");
        fixture.openMainMenu();
        List<String> twoOfEach = List.of(infoLore().get(1), loreOf(MAIN_KIT_SLOT).get(0));

        assertEquals(List.of("Registrierte Teams: 1", "Kits: 1"), oneOfEach);
        assertEquals(List.of("Registrierte Teams: 2", "Kits: 2"), twoOfEach);
    }

    @Test
    @UseLanguage("de")
    void theShippedGermanTextCountsOneWaitingMatch() {
        startATournamentWithOneWaitingMatch();

        openTheWaitingMatches();

        assertEquals("Wartende Matches: 1", loreOf(WAITING_MATCHES_INFO_SLOT).get(0));
    }

    @Test
    @UseLanguage("de")
    void theShippedGermanTextCountsTwoWaitingMatches() {
        startATournamentWithTwoWaitingMatches();

        openTheWaitingMatches();

        assertEquals("Wartende Matches: 2", loreOf(WAITING_MATCHES_INFO_SLOT).get(0));
    }

    private List<String> infoLore() {
        return loreOf(AdminMenuLayout.INFO_SLOT);
    }

    private List<String> loreOf(int slot) {
        return fixture.lastMenu().findTile(slot).orElseThrow().loreLines();
    }

    private void startATournamentWithOneWaitingMatch() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
    }

    private void startATournamentWithTwoWaitingMatches() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        fixture.startTournament();
        decideForTheFirstTeam("round1-match4");
        decideForTheFirstTeam("round1-match3");
    }

    private void decideForTheFirstTeam(String matchId) {
        String firstTeamName = fixture.tournamentManager().getBracket().orElseThrow().findMatch(matchId)
                .orElseThrow().getFirstTeamName().orElseThrow();
        assertTrue(fixture.tournamentManager().forceMatchResult(matchId, firstTeamName).successful());
    }

    private void openTheWaitingMatches() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);
    }

    private static MessageCatalog catalogWithDistinctPluralForms() {
        MessageCatalog shippedEnglish = MessageCatalogLoader.shippedCatalog(MessageCatalogLoader.DEFAULT_LANGUAGE);
        Map<String, List<String>> pluralForms = Map.of(
                singularPath(MainMenuMessage.REGISTERED_TEAMS), List.of("One registered team: {count}"),
                pluralPath(MainMenuMessage.REGISTERED_TEAMS), List.of("Several registered teams: {count}"),
                singularPath(MainMenuMessage.KIT_COUNT), List.of("One kit: {count}"),
                pluralPath(MainMenuMessage.KIT_COUNT), List.of("Several kits: {count}"),
                singularPath(WaitingMatchMessage.WAITING_MATCH_COUNT), List.of("One waiting match: {count}"),
                pluralPath(WaitingMatchMessage.WAITING_MATCH_COUNT), List.of("Several waiting matches: {count}"));
        return new MessageCatalog() {
            @Override
            public Optional<List<String>> find(String path) {
                return Optional.ofNullable(pluralForms.get(path)).or(() -> shippedEnglish.find(path));
            }

            @Override
            public Set<String> paths() {
                return shippedEnglish.paths();
            }
        };
    }

    private static String singularPath(MessageKey key) {
        return key.path() + Messages.SINGULAR_SUFFIX;
    }

    private static String pluralPath(MessageKey key) {
        return key.path() + Messages.PLURAL_SUFFIX;
    }
}

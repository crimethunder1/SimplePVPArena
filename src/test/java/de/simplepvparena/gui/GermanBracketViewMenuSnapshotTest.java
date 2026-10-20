package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanBracketViewMenuSnapshotTest {

    private static final int MAIN_BRACKET_SLOT = 16;
    private static final int NEXT_ROUND_SLOT = 53;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsOnlyTheInfoTileWithoutATournamentInGerman() {
        fixture.openBracketView();

        assertEquals("""
                Turnierbaum (Turnierrunde 1) | 54
                49 | BOOK | Turnierbaum | Es läuft kein Turnier
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheFirstRoundOfSixTeamsInGerman() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot");

        fixture.openBracketView();

        assertEquals(withSeats("""
                Turnierbaum (Turnierrunde 1) | 54
                0 | GREEN_CONCRETE | round1-match1 | {1} rückt ohne Gegner vor
                1 | LIME_CONCRETE | round1-match2 | {2} gegen {3} / Läuft gerade
                2 | GREEN_CONCRETE | round1-match3 | {4} rückt ohne Gegner vor
                3 | YELLOW_CONCRETE | round1-match4 | {5} gegen {6} / Bereit zum Spielen
                49 | BOOK | Turnierbaum | Turnierstatus: läuft / Turnierrunde 1 von 3
                53 | ARROW | Nächste Turnierrunde | Turnierrunde 2 von 3
                """), fixture.describeLastMenu());
    }

    @Test
    void showsTheSecondRoundWithOpenTeamsAndTheBackTileInGerman() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot");
        fixture.openMenuFromTheMainMenu(MAIN_BRACKET_SLOT);

        fixture.click(NEXT_ROUND_SLOT);

        assertEquals(withSeats("""
                Turnierbaum (Turnierrunde 2) | 54
                0 | GRAY_CONCRETE | round2-match1 | {1} gegen offen / Wartet auf seine Teams
                1 | GRAY_CONCRETE | round2-match2 | {4} gegen offen / Wartet auf seine Teams
                45 | ARROW | Vorherige Turnierrunde | Turnierrunde 1 von 3
                48 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
                49 | BOOK | Turnierbaum | Turnierstatus: läuft / Turnierrunde 2 von 3
                53 | ARROW | Nächste Turnierrunde | Turnierrunde 3 von 3
                """), fixture.describeLastMenu());
    }

    @Test
    void showsTheChampionInGerman() {
        startATournamentOf("Alpha", "Bravo");
        fixture.startFinal();
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());

        fixture.openBracketView();

        assertEquals(withSeats("""
                Turnierbaum (Turnierrunde 1) | 54
                0 | GREEN_CONCRETE | round1-match1 | {1} gegen {2} / Gewonnen von {1}
                49 | BOOK | Turnierbaum | Turnierstatus: beendet / Turnierrunde 1 von 1 / Sieger: {1}
                """), fixture.describeLastMenu());
    }

    private void startATournamentOf(String... teamNames) {
        fixture.configureArena();
        fixture.registerTeams(teamNames);
        fixture.startTournament();
    }

    private String withSeats(String template) {
        List<String> seats = new ArrayList<>();
        for (BracketMatch match : fixture.tournamentManager().getBracket().orElseThrow().getRounds().get(0)) {
            match.getFirstTeamName().ifPresent(seats::add);
            match.getSecondTeamName().ifPresent(seats::add);
        }
        String text = template;
        for (int seat = 1; seat <= seats.size(); seat++) {
            text = text.replace("{" + seat + "}", seats.get(seat - 1));
        }
        return text;
    }
}

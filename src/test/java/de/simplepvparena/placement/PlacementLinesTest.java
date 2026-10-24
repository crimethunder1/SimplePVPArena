package de.simplepvparena.placement;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementLinesTest {

    private static final PlacementTeam ALPHA = new PlacementTeam("Alpha", NamedTextColor.RED);
    private static final PlacementTeam BRAVO = new PlacementTeam("Bravo", NamedTextColor.WHITE);

    @Test
    void writesTheRunningTournament() {
        PlacementStandings standings = tournament(PlacementPhase.TOURNAMENT_RUNNING,
                Optional.of(new PlacementMatch(ALPHA, 1, BRAVO, 0)), List.of(ALPHA, BRAVO));

        assertEquals("Tournament", plain(PlacementLines.title(standings)));
        assertEquals(List.of("Round 2 of 3", "Alpha 1 : 0 Bravo", "Still in: 2 teams", "Alpha", "Bravo"),
                plain(PlacementLines.sidebarLines(standings)));
    }

    @Test
    void writesAPausedTournamentWithoutARunningMatchAndASingleTeam() {
        PlacementStandings standings = tournament(PlacementPhase.TOURNAMENT_PAUSED, Optional.empty(),
                List.of(ALPHA));

        assertEquals(List.of("Paused", "No match is running", "Still in: 1 team", "Alpha"),
                plain(PlacementLines.sidebarLines(standings)));
    }

    @Test
    void showsEightTeamsAndCountsTheOthers() {
        PlacementStandings standings = tournament(PlacementPhase.TOURNAMENT_RUNNING, Optional.empty(),
                teams(12));

        List<String> lines = plain(PlacementLines.sidebarLines(standings));

        assertEquals(12, lines.size());
        assertEquals(List.of("Still in: 12 teams", "T01", "T02", "T03", "T04", "T05", "T06", "T07", "T08",
                "and 4 more"), lines.subList(2, 12));
    }

    @Test
    void showsTheFirstPlacesOfAnOverTournament() {
        List<PlacementRank> ranks = new ArrayList<>();
        int[] places = {1, 2, 3, 3, 5, 5, 5, 5, 9, 9};
        List<PlacementTeam> teams = teams(10);
        for (int index = 0; index < places.length; index++) {
            ranks.add(new PlacementRank(places[index], teams.get(index)));
        }
        PlacementStandings standings = PlacementStandings.tournament(PlacementPhase.TOURNAMENT_OVER, 4, 4,
                Optional.empty(), List.of(teams.get(0)), List.of(), ranks);

        assertEquals(List.of("Over", "1. T01", "2. T02", "3. T03", "3. T04", "5. T05", "5. T06", "5. T07",
                "5. T08", "and 2 more"), plain(PlacementLines.sidebarLines(standings)));
    }

    @Test
    void nothingToShowGivesNoLine() {
        assertEquals(List.of(), PlacementLines.sidebarLines(PlacementStandings.none()));
    }

    @Test
    void neverWritesMoreThanFifteenLines() {
        assertEquals(15, PlacementLines.SIDEBAR_LINE_LIMIT);
        assertEquals(8, PlacementLines.TEAM_LINE_LIMIT);
        for (PlacementStandings standings : List.of(
                tournament(PlacementPhase.TOURNAMENT_RUNNING, Optional.empty(), teams(64)),
                tournament(PlacementPhase.TOURNAMENT_PAUSED, Optional.of(new PlacementMatch(ALPHA, 0, BRAVO, 1)),
                        teams(30)))) {
            assertTrue(PlacementLines.sidebarLines(standings).size() <= PlacementLines.SIDEBAR_LINE_LIMIT);
        }
    }

    @Test
    void coloursTheTeamNamesInTheirColourAndTheRestInTheTextColours() {
        PlacementStandings standings = tournament(PlacementPhase.TOURNAMENT_RUNNING,
                Optional.of(new PlacementMatch(ALPHA, 1, BRAVO, 0)), List.of(ALPHA));

        List<Component> lines = PlacementLines.sidebarLines(standings);

        assertEquals(NamedTextColor.YELLOW, lines.get(0).color());
        Component matchLine = lines.get(1);
        assertEquals(NamedTextColor.WHITE, matchLine.color());
        assertEquals(List.of("Alpha", " 1 : 0 ", "Bravo"), matchLine.children().stream()
                .map(child -> ((TextComponent) child).content()).toList());
        assertEquals(NamedTextColor.RED, matchLine.children().get(0).color());
        assertEquals(NamedTextColor.WHITE, matchLine.children().get(2).color());
        assertEquals(TournamentMessages.DETAIL_COLOUR, lines.get(2).color());
        assertEquals(NamedTextColor.RED, lines.get(3).color());
        assertEquals(TournamentMessages.PREFIX_COLOUR, PlacementLines.title(standings).color());
    }

    @Test
    @UseLanguage("de")
    void writesEveryLineInGerman() {
        PlacementStandings running = tournament(PlacementPhase.TOURNAMENT_RUNNING,
                Optional.of(new PlacementMatch(ALPHA, 1, BRAVO, 0)), teams(9));
        PlacementStandings paused = tournament(PlacementPhase.TOURNAMENT_PAUSED, Optional.empty(), List.of(ALPHA));
        PlacementStandings over = PlacementStandings.tournament(PlacementPhase.TOURNAMENT_OVER, 1, 1,
                Optional.empty(), List.of(ALPHA), List.of(), List.of(new PlacementRank(1, ALPHA),
                        new PlacementRank(2, BRAVO)));

        assertEquals("Turnier", plain(PlacementLines.title(running)));
        assertEquals(List.of("Turnierrunde 2 von 3", "Alpha 1 : 0 Bravo", "Noch dabei: 9 Teams", "T01", "T02",
                "T03", "T04", "T05", "T06", "T07", "T08", "und 1 weiteres"),
                plain(PlacementLines.sidebarLines(running)));
        assertEquals(List.of("Pausiert", "Es läuft kein Match", "Noch dabei: 1 Team", "Alpha"),
                plain(PlacementLines.sidebarLines(paused)));
        assertEquals(List.of("Vorbei", "1. Alpha", "2. Bravo"), plain(PlacementLines.sidebarLines(over)));
    }

    @Test
    @UseLanguage("de")
    void countsSeveralFurtherTeamsInGerman() {
        PlacementStandings standings = tournament(PlacementPhase.TOURNAMENT_RUNNING, Optional.empty(), teams(11));

        List<String> lines = plain(PlacementLines.sidebarLines(standings));

        assertEquals("und 3 weitere", lines.get(lines.size() - 1));
    }

    static List<PlacementTeam> teams(int count) {
        List<PlacementTeam> teams = new ArrayList<>();
        for (int number = 1; number <= count; number++) {
            teams.add(new PlacementTeam(String.format("T%02d", number), NamedTextColor.WHITE));
        }
        return teams;
    }

    static String plain(Component component) {
        return TournamentMessages.toPlainText(component);
    }

    static List<String> plain(List<Component> components) {
        return components.stream().map(PlacementLinesTest::plain).toList();
    }

    private static PlacementStandings tournament(PlacementPhase phase, Optional<PlacementMatch> runningMatch,
                                                 List<PlacementTeam> remainingTeams) {
        return PlacementStandings.tournament(phase, 2, 3, runningMatch, remainingTeams, List.of(), List.of());
    }
}

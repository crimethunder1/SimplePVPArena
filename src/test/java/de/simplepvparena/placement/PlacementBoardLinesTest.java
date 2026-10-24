package de.simplepvparena.placement;

import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.simplepvparena.placement.PlacementLinesTest.plain;
import static de.simplepvparena.placement.PlacementLinesTest.teams;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlacementBoardLinesTest {

    private static final PlacementTeam ALPHA = new PlacementTeam("Alpha", NamedTextColor.RED);
    private static final PlacementTeam BRAVO = new PlacementTeam("Bravo", NamedTextColor.WHITE);
    private static final PlacementTeam CHARLIE = new PlacementTeam("Charlie", NamedTextColor.AQUA);
    private static final PlacementTeam DELTA = new PlacementTeam("Delta", NamedTextColor.WHITE);

    @Test
    void addsWhoIsOutInWhichRoundToTheLinesOfTheSidebar() {
        PlacementStandings standings = PlacementStandings.tournament(PlacementPhase.TOURNAMENT_RUNNING, 3, 3,
                Optional.of(new PlacementMatch(ALPHA, 0, BRAVO, 0)), List.of(ALPHA, BRAVO),
                List.of(new PlacementElimination(1, List.of(CHARLIE, DELTA, teams(1).get(0), teams(2).get(1))),
                        new PlacementElimination(2, List.of(CHARLIE))), List.of());

        List<Component> lines = PlacementLines.boardLines(standings);

        assertEquals(List.of("Round 3 of 3", "Alpha 0 : 0 Bravo", "Still in: 2 teams", "Alpha", "Bravo",
                "Out in round 1: Charlie, Delta, T01 and T02", "Out in round 2: Charlie"), plain(lines));
        Component firstRound = lines.get(5);
        assertEquals(NamedTextColor.AQUA, firstRound.children().get(1).color());
        assertEquals(NamedTextColor.WHITE, firstRound.children().get(3).color());
    }

    @Test
    void showsTheWholePlacementOfAnOverTournamentWithSharedPlacesOnOneLine() {
        List<PlacementTeam> teams = teams(12);
        int[] places = {1, 2, 3, 3, 5, 5, 5, 5, 9, 9, 9, 9};
        List<PlacementRank> ranks = new ArrayList<>();
        for (int index = 0; index < places.length; index++) {
            ranks.add(new PlacementRank(places[index], teams.get(index)));
        }
        PlacementStandings standings = PlacementStandings.tournament(PlacementPhase.TOURNAMENT_OVER, 4, 4,
                Optional.empty(), List.of(teams.get(0)), List.of(new PlacementElimination(1, teams.subList(8, 12))),
                ranks);

        assertEquals(List.of("Over", "1. T01", "2. T02", "3. T03 and T04", "5. T05, T06, T07 and T08",
                "9. T09, T10, T11 and T12"), plain(PlacementLines.boardLines(standings)));
    }

    @Test
    void neverWritesMoreThanTwentyLines() {
        List<PlacementElimination> eliminations = new ArrayList<>();
        for (int roundNumber = 1; roundNumber <= 12; roundNumber++) {
            eliminations.add(new PlacementElimination(roundNumber, List.of(ALPHA)));
        }
        PlacementStandings standings = PlacementStandings.tournament(PlacementPhase.TOURNAMENT_RUNNING, 13, 14,
                Optional.empty(), teams(9), eliminations, List.of());

        List<String> lines = plain(PlacementLines.boardLines(standings));

        assertEquals(20, PlacementLines.BOARD_LINE_LIMIT);
        assertEquals(20, lines.size());
        assertEquals("Out in round 8: Alpha", lines.get(19));
    }

    @Test
    void nothingToShowLeavesOnlyTheTitle() {
        assertEquals(List.of(), PlacementLines.boardLines(PlacementStandings.none()));
    }

    @Test
    @UseLanguage("de")
    void writesTheBoardInGerman() {
        PlacementStandings running = PlacementStandings.tournament(PlacementPhase.TOURNAMENT_PAUSED, 2, 2,
                Optional.empty(), List.of(ALPHA, BRAVO), List.of(new PlacementElimination(1, List.of(CHARLIE,
                        DELTA))), List.of());
        PlacementStandings over = PlacementStandings.tournament(PlacementPhase.TOURNAMENT_OVER, 2, 2,
                Optional.empty(), List.of(ALPHA), List.of(), List.of(new PlacementRank(1, ALPHA),
                        new PlacementRank(2, BRAVO), new PlacementRank(3, CHARLIE), new PlacementRank(3, DELTA)));

        assertEquals("Turnier\nPausiert\nEs läuft kein Match\nNoch dabei: 2 Teams\nAlpha\nBravo\n"
                + "Ausgeschieden in Turnierrunde 1: Charlie und Delta", plain(PlacementLines.boardText(running)));
        assertEquals(List.of("Vorbei", "1. Alpha", "2. Bravo", "3. Charlie und Delta"),
                plain(PlacementLines.boardLines(over)));
    }
}

package de.simplepvparena.gui;

import de.simplepvparena.tournament.Bracket;
import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketViewMenuSnapshotTest {

    private static final int NEXT_ROUND_SLOT = 53;
    private static final int INFO_SLOT = 49;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsOnlyTheInfoTileWithoutATournament() {
        fixture.openBracketView();

        assertEquals("""
                Bracket (round 1) | 54
                49 | BOOK | Bracket | No tournament is in progress
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheFirstRoundOfEightTeamsWithDecidedRunningAndReadyMatches() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        decideRunningMatches(2);

        fixture.openBracketView();

        assertEquals(withSeats("""
                Bracket (round 1) | 54
                0 | GREEN_CONCRETE | round1-match1 | {1} against {2} / Won by {1}
                1 | GREEN_CONCRETE | round1-match2 | {3} against {4} / Won by {3}
                2 | LIME_CONCRETE | round1-match3 | {5} against {6} / Running now
                3 | YELLOW_CONCRETE | round1-match4 | {7} against {8} / Ready to be played
                49 | BOOK | Bracket | Tournament state: RUNNING / Round 1 of 3
                53 | ARROW | Next round | Round 2 of 3
                """), fixture.describeLastMenu());
    }

    @Test
    void showsTheSecondRoundWithAReadyMatchAndAMatchWhoseTeamsAreStillOpen() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        decideRunningMatches(2);
        fixture.openBracketView();

        fixture.click(NEXT_ROUND_SLOT);

        assertEquals(withSeats("""
                Bracket (round 2) | 54
                0 | YELLOW_CONCRETE | round2-match1 | {1} against {3} / Ready to be played
                1 | GRAY_CONCRETE | round2-match2 | open against open / Waiting for its teams
                45 | ARROW | Previous round | Round 1 of 3
                49 | BOOK | Bracket | Tournament state: RUNNING / Round 2 of 3
                53 | ARROW | Next round | Round 3 of 3
                """), fixture.describeLastMenu());
    }

    @Test
    void showsASecondRoundMatchThatKnowsOneOfItsTeams() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        decideRunningMatches(3);
        fixture.openBracketView();

        fixture.click(NEXT_ROUND_SLOT);

        assertEquals(withSeats("""
                Bracket (round 2) | 54
                0 | YELLOW_CONCRETE | round2-match1 | {1} against {3} / Ready to be played
                1 | GRAY_CONCRETE | round2-match2 | {5} against open / Waiting for its teams
                45 | ARROW | Previous round | Round 1 of 3
                49 | BOOK | Bracket | Tournament state: RUNNING / Round 2 of 3
                53 | ARROW | Next round | Round 3 of 3
                """), fixture.describeLastMenu());
    }

    @Test
    void showsTheFinalAsTheLastRoundBeforeItsTeamsAreKnown() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        fixture.openBracketView();

        fixture.click(NEXT_ROUND_SLOT);
        fixture.click(NEXT_ROUND_SLOT);

        assertEquals("""
                Bracket (round 3) | 54
                0 | GRAY_CONCRETE | round3-match1 | open against open / Waiting for its teams
                45 | ARROW | Previous round | Round 2 of 3
                49 | BOOK | Bracket | Tournament state: RUNNING / Round 3 of 3
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheFinalReadyToBePlayed() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        decideRunningMatches(6);

        fixture.openBracketView();

        assertEquals(withSeats("""
                Bracket (round 3) | 54
                0 | YELLOW_CONCRETE | round3-match1 | {1} against {5} / Ready to be played
                45 | ARROW | Previous round | Round 2 of 3
                49 | BOOK | Bracket | Tournament state: RUNNING / Round 3 of 3
                """), fixture.describeLastMenu());
    }

    @Test
    void showsTheRunningFinal() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        decideRunningMatches(6);
        fixture.startFinal();

        fixture.openBracketView();

        assertEquals(withSeats("""
                Bracket (round 3) | 54
                0 | LIME_CONCRETE | round3-match1 | {1} against {5} / Running now
                45 | ARROW | Previous round | Round 2 of 3
                49 | BOOK | Bracket | Tournament state: RUNNING / Round 3 of 3
                """), fixture.describeLastMenu());
    }

    @Test
    void showsTheChampionWhenTheTournamentIsOver() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        decideRunningMatches(6);
        fixture.startFinal();
        decideRunningMatches(1);

        fixture.openBracketView();

        assertEquals(withSeats("""
                Bracket (round 3) | 54
                0 | GREEN_CONCRETE | round3-match1 | {1} against {5} / Won by {1}
                45 | ARROW | Previous round | Round 2 of 3
                49 | BOOK | Bracket | Tournament state: FINISHED / Round 3 of 3 / Champion: {1}
                """), fixture.describeLastMenu());
    }

    @Test
    void showsTheFirstRoundOfSixTeamsWithTwoTeamsAdvancingWithoutAnOpponent() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot");

        fixture.openBracketView();

        assertEquals(withSeats("""
                Bracket (round 1) | 54
                0 | GREEN_CONCRETE | round1-match1 | {1} advances without an opponent
                1 | LIME_CONCRETE | round1-match2 | {2} against {3} / Running now
                2 | GREEN_CONCRETE | round1-match3 | {4} advances without an opponent
                3 | YELLOW_CONCRETE | round1-match4 | {5} against {6} / Ready to be played
                49 | BOOK | Bracket | Tournament state: RUNNING / Round 1 of 3
                53 | ARROW | Next round | Round 2 of 3
                """), fixture.describeLastMenu());
    }

    @Test
    void showsTheTeamsThatAdvancedWithoutAnOpponentInTheSecondRound() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot");
        fixture.openBracketView();

        fixture.click(NEXT_ROUND_SLOT);

        assertEquals(withSeats("""
                Bracket (round 2) | 54
                0 | GRAY_CONCRETE | round2-match1 | {1} against open / Waiting for its teams
                1 | GRAY_CONCRETE | round2-match2 | {4} against open / Waiting for its teams
                45 | ARROW | Previous round | Round 1 of 3
                49 | BOOK | Bracket | Tournament state: RUNNING / Round 2 of 3
                53 | ARROW | Next round | Round 3 of 3
                """), fixture.describeLastMenu());
    }

    @Test
    void namesThePausedTournament() {
        startATournamentOf("Alpha", "Bravo", "Charlie", "Delta");
        fixture.pauseTournament();

        fixture.openBracketView();

        assertEquals(List.of("Tournament state: PAUSED", "Round 1 of 2"),
                fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines());
    }

    @Test
    void showsTheFirstFortyFiveMatchesOfARoundAndSaysHowManyThereAre() {
        fixture.configureArena();
        fixture.registerNumberedTeams(92);
        fixture.startTournament();

        fixture.openBracketView();

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        for (int slot = 0; slot < BracketViewMenu.MATCH_SLOT_COUNT; slot++) {
            assertEquals("round1-match" + (slot + 1), menu.findTile(slot).orElseThrow().displayName());
        }
        for (int slot : List.of(45, 46, 47, 48, 50, 51, 52)) {
            assertFalse(menu.findTile(slot).isPresent(), "slot " + slot);
        }
        assertTrue(menu.findTile(NEXT_ROUND_SLOT).isPresent());
        assertEquals(List.of("Tournament state: RUNNING", "Round 1 of 7",
                        "Shows the first 45 of 64 matches of this round"),
                menu.findTile(INFO_SLOT).orElseThrow().loreLines());
    }

    private void startATournamentOf(String... teamNames) {
        fixture.configureArena();
        fixture.registerTeams(teamNames);
        fixture.startTournament();
    }

    private void decideRunningMatches(int matchCount) {
        for (int decidedMatches = 0; decidedMatches < matchCount; decidedMatches++) {
            fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        }
    }

    private String withSeats(String template) {
        Bracket bracket = fixture.tournamentManager().getBracket().orElseThrow();
        List<String> seats = new ArrayList<>();
        for (BracketMatch match : bracket.getRounds().get(0)) {
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

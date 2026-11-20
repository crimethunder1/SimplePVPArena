package de.simplepvparena.placement;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.Bracket;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BracketBoardTextTest {

    private TournamentState state = TournamentState.RUNNING;
    private Optional<Bracket> bracket = Optional.empty();
    private Optional<String> runningMatchId = Optional.empty();
    private Optional<String> titleImage = Optional.empty();
    private final BracketBoardText boardText = new BracketBoardText(() -> state, () -> bracket,
            () -> runningMatchId, teamName -> Optional.empty(), () -> titleImage);

    @Test
    @UseLanguage("de")
    void showsAHintWhileNoTournamentRuns() {
        state = TournamentState.IDLE;
        bracket = Optional.of(Bracket.createSingleElimination(List.of("Alpha", "Bravo")));

        BracketBoardView view = boardText.build();

        assertEquals("Turnierbaum\nNoch kein Turnier\nDer Baum erscheint, sobald das Turnier startet",
                plain(view.header()));
        assertEquals(List.of(), view.columns());
    }

    @Test
    @UseLanguage("de")
    void showsTheRunningAndTheNextMatchAndTheWholeTree() {
        bracket = Optional.of(Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta")));
        runningMatchId = Optional.of("round1-match1");

        BracketBoardView view = boardText.build();

        assertEquals("Turnierbaum\nJetzt: Alpha gegen Delta\nDanach: Bravo gegen Charlie", plain(view.header()));
        assertEquals(3, view.headerLineCount());
        assertEquals(6, view.columnLineCount());
        assertEquals(List.of(
                "Halbfinale\n \n┌ Alpha\n└ Delta\n┌ Bravo\n└ Charlie",
                "Finale\n \n \n┌ ???\n└ ???\n ",
                "Sieger\n \n \n???\n \n "), view.columns().stream().map(BracketBoardTextTest::plain).toList());
    }

    @Test
    @UseLanguage("de")
    void movesTheWinnersOnAndWaitsForTheFinalOpponent() {
        Bracket fourTeams = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta"));
        fourTeams.recordWinner("round1-match1", "Alpha");
        bracket = Optional.of(fourTeams);
        runningMatchId = Optional.of("round1-match2");

        BracketBoardView view = boardText.build();

        assertEquals("Turnierbaum\nJetzt: Bravo gegen Charlie\nDanach: steht noch nicht fest", plain(view.header()));
        assertEquals("Finale\n \n \n┌ Alpha\n└ ???\n ", plain(view.columns().get(1)));
    }

    @Test
    @UseLanguage("de")
    void namesTheChampionOnceTheFinalIsDecided() {
        Bracket fourTeams = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta"));
        fourTeams.recordWinner("round1-match1", "Alpha");
        fourTeams.recordWinner("round1-match2", "Bravo");
        fourTeams.recordWinner("round2-match1", "Bravo");
        bracket = Optional.of(fourTeams);
        state = TournamentState.FINISHED;

        BracketBoardView view = boardText.build();

        assertEquals("Turnierbaum\nSieger: Bravo\nVorbei", plain(view.header()));
        assertEquals("Sieger\n \n \n★ Bravo\n \n ", plain(view.columns().get(2)));
    }

    @Test
    @UseLanguage("en")
    void showsThePauseAndTheTitleImage() {
        bracket = Optional.of(Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie")));
        state = TournamentState.PAUSED;
        titleImage = Optional.of("");

        BracketBoardView view = boardText.build();

        assertEquals("\nTournament bracket\nPaused\nNow: no fight at the moment\n"
                + "Next: Bravo against Charlie", plain(view.header()));
        assertEquals("Semi final\n \n┌ Alpha\n└ Bye\n┌ Bravo\n└ Charlie", plain(view.columns().get(0)));
    }

    @Test
    @UseLanguage("de")
    void namesTheLastThreeRoundsAndNumbersTheOthers() {
        assertEquals(List.of("Runde 1", "Viertelfinale", "Halbfinale", "Finale"), List.of(
                BracketBoardText.roundName(0, 4), BracketBoardText.roundName(1, 4),
                BracketBoardText.roundName(2, 4), BracketBoardText.roundName(3, 4)));
    }

    @Test
    void shortensLongTeamNames() {
        assertEquals("Alpha", BracketBoardText.shorten("Alpha", 5));
        assertEquals("Alph…", BracketBoardText.shorten("Alphabet", 5));
    }

    @Test
    void fitsEveryColumnIntoTheTreeArea() {
        int lineCount = 66;
        float scale = BracketBoardGeometry.treeScale(lineCount);

        assertEquals(BracketBoardGeometry.TREE_TOP - BracketBoardGeometry.TREE_BOTTOM,
                BracketBoardGeometry.textHeight(lineCount, scale), 0.0001F);
        assertEquals(BracketBoardGeometry.MAXIMUM_TREE_SCALE, BracketBoardGeometry.treeScale(6));
    }

    private static String plain(net.kyori.adventure.text.Component text) {
        return TournamentMessages.toPlainText(text);
    }
}

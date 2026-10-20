package de.simplepvparena.tournament;

import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.MatchMenuMessage;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TournamentStageTextsTest {

    private static final Map<TournamentState, String> GERMAN_STATES = Map.of(
            TournamentState.IDLE, "ohne Turnier",
            TournamentState.RUNNING, "läuft",
            TournamentState.PAUSED, "pausiert",
            TournamentState.CANCELLED, "abgebrochen",
            TournamentState.FINISHED, "beendet");

    private static final Map<FinalMatchStage, String> GERMAN_STAGES = Map.of(
            FinalMatchStage.PENDING, "wartet auf Finalisten",
            FinalMatchStage.READY, "bereit",
            FinalMatchStage.RUNNING, "läuft",
            FinalMatchStage.FINISHED, "vorbei");

    @Test
    @UseLanguage("en")
    void namesEveryTournamentStateAndFinalStageInEnglishByItsName() {
        for (TournamentState state : TournamentState.values()) {
            assertEquals(state.name(), state.describe());
        }
        for (FinalMatchStage stage : FinalMatchStage.values()) {
            assertEquals(stage.name(), stage.describe());
        }
    }

    @Test
    @UseLanguage("de")
    void namesEveryTournamentStateAndFinalStageInGerman() {
        assertEquals(Arrays.asList(TournamentState.values()).stream().map(GERMAN_STATES::get).toList(),
                Arrays.stream(TournamentState.values()).map(TournamentState::describe).toList());
        assertEquals(Arrays.asList(FinalMatchStage.values()).stream().map(GERMAN_STAGES::get).toList(),
                Arrays.stream(FinalMatchStage.values()).map(FinalMatchStage::describe).toList());
    }

    @Test
    void everyTournamentStateAndFinalStageHasAKeyInBothLanguageFiles() {
        List<String> missing = new ArrayList<>();
        for (String language : MessageCatalogLoader.SHIPPED_LANGUAGES) {
            Messages.use(MessageCatalogLoader.shippedCatalog(language));
            try {
                for (TournamentState state : TournamentState.values()) {
                    if (state.describe().startsWith("tournament-status.")) {
                        missing.add(language + " " + state);
                    }
                }
                for (FinalMatchStage stage : FinalMatchStage.values()) {
                    if (stage.describe().startsWith("tournament-status.")) {
                        missing.add(language + " " + stage);
                    }
                }
            } finally {
                Messages.reset();
            }
        }

        assertEquals(List.of(), missing);
        assertEquals(GERMAN_STATES.keySet().size(), TournamentState.values().length,
                "a new tournament state needs a German name in this test and a key in both files");
        assertEquals(GERMAN_STAGES.keySet().size(), FinalMatchStage.values().length,
                "a new final stage needs a German name in this test and a key in both files");
    }

    @Test
    @UseLanguage("de")
    void tellsTheAdminsAndRefusesTheStartOfTheFinalInGerman() {
        List<String> notices = new ArrayList<>();
        FinalMatchGate gate = new FinalMatchGate(notices::add);
        Bracket bracket = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta"));

        gate.synchronizeWith(bracket.getFinalMatch());
        String pendingRejection = gate.findStartRejection().orElseThrow();
        bracket.recordWinner("round1-match1", "Alpha");
        bracket.recordWinner("round1-match2", "Charlie");
        gate.synchronizeWith(bracket.getFinalMatch());
        gate.markRunning();
        String runningRejection = gate.findStartRejection().orElseThrow();
        bracket.recordWinner(bracket.getFinalMatch().getMatchId(), "Alpha");
        gate.synchronizeWith(bracket.getFinalMatch());

        assertEquals("Das Finale ist noch nicht bereit, zuerst müssen beide Finalisten feststehen", pendingRejection);
        assertEquals("Das Finale läuft bereits", runningRejection);
        assertEquals("Das Finale ist bereits entschieden", gate.findStartRejection().orElseThrow());
        assertEquals(List.of("Das Finale Alpha gegen Charlie ist bereit, ein Admin startet es mit "
                + "\"Finale starten\" in /tournament"), notices);
    }

    @Test
    @UseLanguage("en")
    void namesTheTilesOfTheMatchMenuInEnglish() {
        assertEquals("\"Start the final\" in /tournament", FinalMatchGate.describeFinalStartAction());
        assertEquals("\"Force start\" in the match menu of /tournament",
                MatchService.describeMatchMenuTile(MatchMenuMessage.FORCE_START_TITLE));
        assertEquals("\"Continue the tournament\" in the match menu of /tournament",
                MatchService.describeMatchMenuTile(MatchMenuMessage.CONTINUE_TITLE));
        assertEquals("\"Force a result\" in the match menu of /tournament",
                MatchService.describeMatchMenuTile(MatchMenuMessage.FORCE_RESULT_TITLE));
    }

    @Test
    @UseLanguage("de")
    void namesTheTilesOfTheMatchMenuInGerman() {
        assertEquals("\"Finale starten\" in /tournament", FinalMatchGate.describeFinalStartAction());
        assertEquals("\"Start erzwingen\" im Match-Menü von /tournament",
                MatchService.describeMatchMenuTile(MatchMenuMessage.FORCE_START_TITLE));
        assertEquals("\"Turnier fortsetzen\" im Match-Menü von /tournament",
                MatchService.describeMatchMenuTile(MatchMenuMessage.CONTINUE_TITLE));
        assertEquals("\"Ergebnis erzwingen\" im Match-Menü von /tournament",
                MatchService.describeMatchMenuTile(MatchMenuMessage.FORCE_RESULT_TITLE));
    }

    @Test
    void buildsTheReferenceInTheLanguageThatIsSetWhenItIsAskedFor() {
        Messages.use(MessageCatalogLoader.shippedCatalog("de"));
        String german = FinalMatchGate.describeFinalStartAction();
        Messages.reset();

        assertEquals("\"Finale starten\" in /tournament", german);
        assertFalse(FinalMatchGate.describeFinalStartAction().equals(german));
    }
}

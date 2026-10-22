package de.simplepvparena.gui;

import de.simplepvparena.match.Match;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.FinalMatchStage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchAdminMenuRestartTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int RESTART_SLOT = 19;
    private static final int FINAL_SLOT = 15;
    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;

    private static final String OFFERED_TILE = "19 | REPEATER | Restart the match | "
            + "Stops the running match and begins it again with the same two teams / "
            + "The score is back at 0 : 0, the kit choice and the countdown run again / "
            + "Asks for a confirmation first";

    private AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void offersTheRestartWhileATournamentMatchRuns() {
        startATournamentOfFourTeams();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals(OFFERED_TILE, describeRestartTile());
    }

    @Test
    void offersTheRestartWhileTheTournamentIsPausedAndItsMatchStillRuns() {
        startATournamentOfFourTeams();
        fixture.pauseTournament();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals(OFFERED_TILE, describeRestartTile());
    }

    @Test
    void greysTheTileOutWithoutARunningMatch() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("19 | GRAY_DYE | Restart the match | No match is running", describeRestartTile());
    }

    @Test
    void greysTheTileOutWhileAKitIsBeingChosen() {
        fixture = AdminMenuFixture.withKitChoice();
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        startATournamentOfFourTeams();
        assertTrue(fixture.tournamentManager().isKitChoicePending());

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("19 | GRAY_DYE | Restart the match | A kit is being chosen for match round1-match1, wait for "
                + "it or pause the tournament", describeRestartTile());
    }

    @Test
    void showsTheWholeMatchMenuWithAGreyTileWhileAnAutoMatchRuns() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        startAnAutoMatch();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match and tournament | 27
                4 | BOOK | Tournament | Tournament state: IDLE / Running match: auto-1 (Alpha 0 : 0 Bravo) / \
                Round timer: running
                10 | LIME_WOOL | Start the tournament | Needs an even number of teams
                11 | YELLOW_WOOL | Force start | Starts the tournament with an uneven number of teams
                12 | GRAY_DYE | Pause the tournament | No tournament is in progress
                13 | GRAY_DYE | Cancel the tournament | No tournament is in progress
                14 | GRAY_DYE | Decide a waiting match | No tournament is in progress
                15 | GRAY_DYE | Start the final | No tournament is in progress
                16 | ANVIL | Force a result | Decides the running match for one of its two teams
                17 | COMPARATOR | Auto match mode | The mode is off / \
                Two teams with a player each online get a match without a tournament / \
                A running tournament has priority / Click to switch it on
                19 | GRAY_DYE | Restart the match | An auto match cannot be restarted
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }

    @Test
    void asksForTheConfirmationOfTheRunningMatchAndRestartsNothingBeforeIt() {
        startATournamentOfFourTeams();
        Match runningMatch = fixture.runningMatch();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(RESTART_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of(ConfirmationRequest.RESTART_MATCH), menu.holder().getConfirmationRequest());
        assertEquals(Optional.of("round1-match1"), menu.holder().getSubject());
        assertEquals("Restart the match?", menu.title());
        assertEquals("""
                Restart the match? | 27
                4 | BOOK | Restart the match? | The running match is stopped and begins again / Its score is lost
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertSame(runningMatch, fixture.runningMatch(), "nothing is restarted before the confirmation");
    }

    @Test
    void reportsTheReasonOfTheGreyTileAndOpensNoConfirmation() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(RESTART_SLOT);

        assertEquals(List.of("No match is running"), fixture.messages());
        assertEquals(TournamentMessages.error("No match is running"), fixture.lastSentMessage());
        assertSame(matchHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
    }

    @Test
    void reportsThatAnAutoMatchCannotBeRestartedAndLeavesItRunning() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        Match autoMatch = startAnAutoMatch();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(RESTART_SLOT);

        assertEquals(List.of("An auto match cannot be restarted"), fixture.messages());
        assertSame(matchHolder, fixture.lastMenu().holder());
        assertSame(autoMatch, fixture.runningMatch());
    }

    @Test
    void theConfirmationRestartsTheMatchWithAFreshScoreAndReturnsToTheMatchMenu() {
        startATournamentOfFourTeams();
        Match oldMatch = fixture.runningMatch();
        oldMatch.getScore().awardPoint(oldMatch.getFirstTeam().getTeamName());
        openTheRestartConfirmation();

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Match round1-match1 was stopped and begins again"), fixture.messages());
        assertEquals(TournamentMessages.success("Match round1-match1 was stopped and begins again"),
                fixture.lastSentMessage());
        Match newMatch = fixture.runningMatch();
        assertNotSame(oldMatch, newMatch);
        assertEquals("round1-match1", newMatch.getMatchId());
        assertEquals(0, newMatch.getScore().getPoints(newMatch.getFirstTeam().getTeamName()));
        assertFalse(fixture.tournamentManager().getBracket().orElseThrow().findMatch("round1-match1")
                .orElseThrow().isDecided());
        assertEquals(AdminMenuType.MATCH, fixture.lastMenu().holder().getMenuType());
        assertEquals(OFFERED_TILE, describeRestartTile());
    }

    @Test
    void theConfirmationOfTheFinalNamesTheTileThatStartsItAgain() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.startFinal();
        openTheRestartConfirmation();

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("The final was stopped, start it again with \"Start the final\" in the match menu "
                + "of /tournament"), fixture.messages());
        assertFalse(fixture.matchService().hasActiveMatch());
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
        assertEquals(AdminMenuType.MATCH, fixture.lastMenu().holder().getMenuType());
        assertEquals(Material.NETHER_STAR, fixture.lastMenu().findTile(FINAL_SLOT).orElseThrow().material());
        assertEquals("19 | GRAY_DYE | Restart the match | No match is running", describeRestartTile());
    }

    @Test
    void refusingTheConfirmationChangesNothing() {
        startATournamentOfFourTeams();
        Match runningMatch = fixture.runningMatch();
        openTheRestartConfirmation();

        fixture.click(CONFIRM_NO_SLOT);

        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertSame(runningMatch, fixture.runningMatch());
        assertEquals(AdminMenuType.MATCH, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void theConfirmationRestartsNothingWhenTheMatchIsOverMeanwhile() {
        startATournamentOfFourTeams();
        openTheRestartConfirmation();
        AdminMenuHolder confirmationHolder = fixture.lastMenu().holder();
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        Match nextMatch = fixture.runningMatch();
        assertEquals("round1-match2", nextMatch.getMatchId());
        fixture.messages().clear();

        fixture.clickIn(confirmationHolder, CONFIRM_YES_SLOT);

        assertEquals(List.of("Match round1-match1 is not running any more"), fixture.messages());
        assertEquals(TournamentMessages.error("Match round1-match1 is not running any more"),
                fixture.lastSentMessage());
        assertSame(nextMatch, fixture.runningMatch());
        assertTrue(fixture.tournamentManager().getBracket().orElseThrow().findMatch("round1-match1")
                .orElseThrow().isDecided());
        assertEquals(AdminMenuType.MATCH, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void theConfirmationRestartsNothingWhenNoMatchRunsAnyMore() {
        startATournamentOfFourTeams();
        openTheRestartConfirmation();
        AdminMenuHolder confirmationHolder = fixture.lastMenu().holder();
        fixture.matchService().cancelActiveMatch();
        fixture.messages().clear();

        fixture.clickIn(confirmationHolder, CONFIRM_YES_SLOT);

        assertEquals(List.of("Match round1-match1 is not running any more"), fixture.messages());
        assertFalse(fixture.matchService().hasActiveMatch());
    }

    @Test
    void namesTheRestartConfirmationWhenItIsNotRegistered() {
        ConfirmationAdminMenu confirmationAdminMenu = new ConfirmationAdminMenu(
                new AdminMenuNavigator(new RecordingAdminMenuWindows()));
        for (ConfirmationRequest request : EnumSet.complementOf(EnumSet.of(ConfirmationRequest.RESTART_MATCH))) {
            confirmationAdminMenu.register(request, new SilentConfirmation());
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                confirmationAdminMenu::requireEveryConfirmationRegistered);

        assertEquals("no confirmation is registered for [RESTART_MATCH]", failure.getMessage());
    }

    @Test
    @UseLanguage("de")
    void showsTheTileTheConfirmationAndTheReplyInGerman() {
        startATournamentOfFourTeams();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("19 | REPEATER | Match neu starten | "
                + "Stoppt das laufende Match und beginnt es mit denselben zwei Teams neu / "
                + "Der Spielstand ist wieder 0 : 0, Kit-Wahl und Countdown laufen erneut / "
                + "Fragt vorher nach einer Bestätigung", describeRestartTile());

        fixture.click(RESTART_SLOT);

        assertEquals("""
                Match neu starten? | 27
                4 | BOOK | Match neu starten? | Das laufende Match wird gestoppt und beginnt neu / \
                Sein Spielstand geht verloren
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Match round1-match1 wurde gestoppt und beginnt neu"), fixture.messages());
    }

    @Test
    @UseLanguage("de")
    void namesTheGreyReasonsTheFinalAndAMatchThatIsOverInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        startAnAutoMatch();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        assertEquals("19 | GRAY_DYE | Match neu starten | Ein Auto-Match lässt sich nicht neu starten",
                describeRestartTile());
        fixture.matchService().cancelActiveMatch();
        fixture.refillLastMenu();
        assertEquals("19 | GRAY_DYE | Match neu starten | Es läuft kein Match", describeRestartTile());

        fixture.startTournament();
        fixture.startFinal();
        openTheRestartConfirmation();
        AdminMenuHolder confirmationHolder = fixture.lastMenu().holder();
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Das Finale wurde gestoppt, starte es erneut mit \"Finale starten\" im Match-Menü "
                + "von /tournament"), fixture.messages());

        fixture.messages().clear();
        fixture.clickIn(confirmationHolder, CONFIRM_YES_SLOT);

        assertEquals(List.of("Match round1-match1 läuft nicht mehr"), fixture.messages());
    }

    private void startATournamentOfFourTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
    }

    private Match startAnAutoMatch() {
        Match autoMatch = Match.autoMatch("auto-1", fixture.teamRegistry().findTeam("Alpha").orElseThrow(),
                fixture.teamRegistry().findTeam("Bravo").orElseThrow(), fixture.arenaRegistry().getArena());
        fixture.matchService().startMatch(autoMatch);
        return autoMatch;
    }

    private void openTheRestartConfirmation() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(RESTART_SLOT);
        assertEquals(Optional.of(ConfirmationRequest.RESTART_MATCH),
                fixture.lastMenu().holder().getConfirmationRequest());
        fixture.messages().clear();
    }

    private String describeRestartTile() {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(RESTART_SLOT + " | "))
                .findFirst()
                .orElseThrow();
    }

    private static final class SilentConfirmation implements AdminMenuConfirmation {

        @Override
        public List<String> describe(AdminMenuHolder holder) {
            return List.of();
        }

        @Override
        public void carryOut(Player player, AdminMenuHolder holder) {
        }

        @Override
        public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
            return AdminMenuHolder.main();
        }
    }
}

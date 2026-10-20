package de.simplepvparena.diagnostics;

import de.simplepvparena.message.DryRunCheckMessage;
import de.simplepvparena.message.DryRunMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DryRunTextsTest {

    private final DryRunFixture fixture = new DryRunFixture();

    @Test
    void namesBothMissingTeamSpawnsInThePlural() {
        fixture.makeTheServerReadyForItsEvent();
        fixture.arena.firstTeamSpawn = false;
        fixture.arena.secondTeamSpawn = false;

        assertEquals(List.of("The arena is ready but the first team spawn and the second team spawn are not set",
                        "An unset team spawn falls back to the middle of the fight area, so both teams would start "
                                + "on the same block",
                        "Set them in the arena menu of /tournament, each while standing on it"),
                fixture.findStep(TournamentDryRun.ARENA_STEP).details());
    }

    @Test
    void namesASingleMissingTeamSpawnAsBefore() {
        fixture.makeTheServerReadyForItsEvent();
        fixture.arena.firstTeamSpawn = false;

        assertEquals(List.of("The arena is ready but the first team spawn is not set",
                        "An unset team spawn falls back to the middle of the fight area, so both teams would start "
                                + "on the same block",
                        "Set it in the arena menu of /tournament while standing on it"),
                fixture.findStep(TournamentDryRun.ARENA_STEP).details());
    }

    @Test
    void namesAGateOfOneBlockInTheSingular() {
        fixture.makeTheServerReadyForItsEvent();
        fixture.markGate(1, 1);

        List<String> details = fixture.findStep(TournamentDryRun.GATE_STEP).details();

        assertTrue(details.stream().anyMatch(line -> line.startsWith(DryRunFixture.WORLD + ": 1 block in 1 layer, "
                + "the last one falls on tick ")), details.toString());
    }

    @Test
    void namesAGateOfSeveralBlocksAndLayersInThePlural() {
        fixture.makeTheServerReadyForItsEvent();
        fixture.markGate(2, 3);

        List<String> details = fixture.findStep(TournamentDryRun.GATE_STEP).details();

        assertTrue(details.stream().anyMatch(line -> line.startsWith(DryRunFixture.WORLD + ": 6 blocks in 2 layers, "
                + "the last one falls on tick ")), details.toString());
    }

    @Test
    void namesAKitOfOneItemInTheSingular() {
        fixture.makeTheServerReadyForItsEvent();
        fixture.giveKit(0, 1);

        assertEquals("Every fighter is given this 1 item at every round start, as a copy of its own",
                lastLine(fixture.findStep(TournamentDryRun.KIT_STEP).details()));
    }

    @Test
    void namesAKitOfSeveralItemsAsBefore() {
        fixture.makeTheServerReadyForItsEvent();

        assertEquals("Every fighter is given these 5 items at every round start, each as a copy of its own",
                lastLine(fixture.findStep(TournamentDryRun.KIT_STEP).details()));
    }

    @Test
    void namesOneTakenTestTeamNameInTheSingularAndTwoInThePlural() {
        fixture.registerRealTeam(DryRunTeam.FIRST_TEAM_NAME);

        assertEquals(List.of("A registered team is already called Dry-Run-Alpha",
                        "The dry run registers exactly those names and never replaces a team of yours",
                        "Rename or remove that team in the team menu of /tournament and click again"),
                fixture.findStep(TournamentDryRun.TEAM_STEP).details());
        assertEquals(List.of("No test team was registered, so there is nothing to remove",
                        "The registry still holds its own 1 team"),
                fixture.findStep(TournamentDryRun.CLEANUP_STEP).details());

        fixture.registerRealTeam(DryRunTeam.SECOND_TEAM_NAME);

        assertEquals(List.of("Registered teams are already called Dry-Run-Alpha and Dry-Run-Bravo",
                        "The dry run registers exactly those names and never replaces a team of yours",
                        "Rename or remove those teams in the team menu of /tournament and click again"),
                fixture.findStep(TournamentDryRun.TEAM_STEP).details());
        assertEquals(List.of("No test team was registered, so there is nothing to remove",
                        "The registry still holds its own 2 teams"),
                fixture.findStep(TournamentDryRun.CLEANUP_STEP).details());
    }

    @Test
    void countsTheOwnTeamsAfterTheCleanupInTheSingularAndThePlural() {
        fixture.registerRealTeam("Alpha");

        assertEquals(List.of("Both test teams are gone, the registry holds its 1 own team again",
                        "teams.yml was never written, so a restart could not bring them back either"),
                fixture.findStep(TournamentDryRun.CLEANUP_STEP).details());

        fixture.registerRealTeam("Bravo");

        assertEquals(List.of("Both test teams are gone, the registry holds its 2 own teams again",
                        "teams.yml was never written, so a restart could not bring them back either"),
                fixture.findStep(TournamentDryRun.CLEANUP_STEP).details());
    }

    @Test
    void writesTheCleanupWarningsInTheSingularAndThePlural() {
        assertEquals("This test team could not be removed: Dry-Run-Alpha\nRemove it by hand in the team menu of "
                        + "/tournament before the event starts",
                Messages.plural(DryRunMessage.TEAMS_LEFT_BEHIND, 1, Placeholder.of("teams", "Dry-Run-Alpha")));
        assertEquals("These test teams could not be removed: Dry-Run-Alpha and Dry-Run-Bravo\nRemove them by hand in "
                        + "the team menu of /tournament before the event starts",
                Messages.plural(DryRunMessage.TEAMS_LEFT_BEHIND, 2,
                        Placeholder.of("teams", Messages.join(List.of("Dry-Run-Alpha", "Dry-Run-Bravo")))));
        assertEquals("The registry held 1 team before the dry run and holds 3 now\nCheck the team menu of "
                        + "/tournament before the event starts",
                Messages.plural(DryRunMessage.TEAM_COUNT_CHANGED, 1, Placeholder.of("after", 3)));
        assertEquals("The registry held 2 teams before the dry run and holds 4 now\nCheck the team menu of "
                        + "/tournament before the event starts",
                Messages.plural(DryRunMessage.TEAM_COUNT_CHANGED, 2, Placeholder.of("after", 4)));
    }

    @Test
    void writesTheCountsOfTheGateTheBracketAndTheCleaningInTheSingularAndThePlural() {
        Placeholder release = Placeholder.of("release", "Fight!");
        assertEquals("  That gate is taller than the countdown has ticks, so it keeps coming down for 1 tick after "
                + "\"Fight!\"", Messages.plural(DryRunCheckMessage.GATE_TALLER_THAN_COUNTDOWN, 1, release));
        assertEquals("  That gate is taller than the countdown has ticks, so it keeps coming down for 2 ticks after "
                + "\"Fight!\"", Messages.plural(DryRunCheckMessage.GATE_TALLER_THAN_COUNTDOWN, 2, release));
        assertEquals("1 team, 1 round, 1 match", String.join(", ",
                Messages.plural(DryRunCheckMessage.BRACKET_TEAM_COUNT, 1),
                Messages.plural(DryRunCheckMessage.BRACKET_ROUND_COUNT, 1),
                Messages.plural(DryRunCheckMessage.BRACKET_MATCH_COUNT, 1)));
        assertEquals("2 teams, 2 rounds, 2 matches", String.join(", ",
                Messages.plural(DryRunCheckMessage.BRACKET_TEAM_COUNT, 2),
                Messages.plural(DryRunCheckMessage.BRACKET_ROUND_COUNT, 2),
                Messages.plural(DryRunCheckMessage.BRACKET_MATCH_COUNT, 2)));
        Placeholder floor = Placeholder.of("floorY", 53);
        Placeholder top = Placeholder.of("topY", 67);
        assertEquals("A cleaning writes 1 block between y 53 and y 67",
                Messages.plural(DryRunCheckMessage.CLEANING_WRITES_BLOCKS, 1, floor, top));
        assertEquals("1 block between y 53 and y 67",
                Messages.plural(DryRunCheckMessage.CLEANED_BLOCKS, 1, floor, top));
        assertEquals("2 blocks between y 53 and y 67",
                Messages.plural(DryRunCheckMessage.CLEANED_BLOCKS, 2, floor, top));
        assertEquals("The ready final announced itself 2 times to the admins instead of exactly once",
                Messages.plural(DryRunCheckMessage.FINAL_ANNOUNCED_TIMES, 2));
    }

    @Test
    void writesTheBracketOfTheTwoTestTeamsAsBefore() {
        fixture.makeTheServerReadyForItsEvent();

        List<String> details = fixture.findStep(TournamentDryRun.MATCH_STEP).details();

        assertEquals("Bracket built over 2 teams: 1 round, 1 match in it", details.get(0));
        assertEquals("Match round1-match1: Dry-Run-Alpha against Dry-Run-Bravo, both teams resolve in the registry",
                details.get(1));
        assertTrue(details.get(3).startsWith("The final gate turns READY and announces once: The final "
                + "Dry-Run-Alpha against Dry-Run-Bravo is ready, an admin starts it with \"Start the final\" in "
                + "/tournament"), details.get(3));
        assertEquals("Simulated result for Dry-Run-Alpha: the bracket is complete, the champion is Dry-Run-Alpha "
                + "and the final gate is FINISHED", details.get(4));
    }

    @Test
    void summarizesARunOfOneStepInTheSingular() {
        assertEquals("Dry run of the tournament pipeline: 1 of 1 step passed",
                DryRunReport.of(List.of(DryRunStep.passed("First", "one"))).describe().get(0));
        assertEquals("Dry run of the tournament pipeline: 0 of 1 step passed, 1 failed",
                DryRunReport.of(List.of(DryRunStep.failed("First", "one"))).describe().get(0));
        assertEquals("Dry run of the tournament pipeline: 0 of 1 step passed, 1 skipped",
                DryRunReport.of(List.of(DryRunStep.skipped("First", "one"))).describe().get(0));
    }

    @Test
    void refusesWithTheTileOfTheMatchMenuWhileAMatchStandsOnTheArena() {
        fixture.matchRunning = true;

        assertEquals("A match stands on the arena, the dry run is a check for before an event\nDecide it with "
                + "\"Force a result\" in the match menu of /tournament, or wait for it",
                fixture.dryRun().findRejection().orElseThrow());
    }

    @Test
    @UseLanguage("de")
    void followsTheLanguageForTheCountsAndTheStatusMarkers() {
        fixture.registerRealTeam(DryRunTeam.FIRST_TEAM_NAME);
        fixture.tournamentState = TournamentState.FINISHED;

        assertEquals(List.of("Ein registriertes Team heißt bereits Dry-Run-Alpha",
                        "Der Probelauf registriert genau diese Namen und ersetzt nie eines deiner Teams",
                        "Benenne dieses Team im Team-Menü von /tournament um oder entferne es und klicke noch einmal"),
                fixture.findStep(TournamentDryRun.TEAM_STEP).details());
        assertEquals(List.of("Es wurde kein Testteam registriert, also gibt es nichts zu entfernen",
                        "Die Teamliste enthält weiterhin 1 eigenes Team"),
                fixture.findStep(TournamentDryRun.CLEANUP_STEP).details());
        assertEquals("Probelauf des Turnierablaufs: 1 von 1 Schritt bestanden",
                DryRunReport.of(List.of(DryRunStep.passed("Erster", "eins"))).describe().get(0));
        assertEquals(List.of("ok", "übersprungen", "GESCHEITERT"),
                List.of(DryRunStatus.PASSED.getMarker(), DryRunStatus.SKIPPED.getMarker(),
                        DryRunStatus.FAILED.getMarker()));
        assertEquals("Jeder Kämpfer bekommt bei jedem Rundenstart dieses 1 Item als eigene Kopie",
                Messages.plural(DryRunCheckMessage.KIT_ITEMS, 1));
        assertEquals("Jeder Kämpfer bekommt bei jedem Rundenstart diese 2 Items, jedes als eigene Kopie",
                Messages.plural(DryRunCheckMessage.KIT_ITEMS, 2));
        assertEquals("1 Team, 1 Turnierrunde, 1 Match, 1 Block, 1 Schicht", String.join(", ",
                Messages.plural(DryRunCheckMessage.BRACKET_TEAM_COUNT, 1),
                Messages.plural(DryRunCheckMessage.BRACKET_ROUND_COUNT, 1),
                Messages.plural(DryRunCheckMessage.BRACKET_MATCH_COUNT, 1),
                Messages.plural(DryRunCheckMessage.GATE_BLOCK_COUNT, 1),
                Messages.plural(DryRunCheckMessage.GATE_LAYER_COUNT, 1)));
        assertEquals("2 Teams, 2 Turnierrunden, 2 Matches, 2 Blöcke, 2 Schichten", String.join(", ",
                Messages.plural(DryRunCheckMessage.BRACKET_TEAM_COUNT, 2),
                Messages.plural(DryRunCheckMessage.BRACKET_ROUND_COUNT, 2),
                Messages.plural(DryRunCheckMessage.BRACKET_MATCH_COUNT, 2),
                Messages.plural(DryRunCheckMessage.GATE_BLOCK_COUNT, 2),
                Messages.plural(DryRunCheckMessage.GATE_LAYER_COUNT, 2)));
    }

    private static String lastLine(List<String> lines) {
        return lines.get(lines.size() - 1);
    }
}

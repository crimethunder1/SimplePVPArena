package de.simplepvparena.placement;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchTeamColours;
import de.simplepvparena.match.ScoreboardFighterColours;
import de.simplepvparena.match.TeamColourSettings;
import de.simplepvparena.support.InMemoryScoreboard;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementSidebarTest {

    private static final PlacementTeam ALPHA = new PlacementTeam("Alpha", NamedTextColor.RED);
    private static final PlacementTeam BRAVO = new PlacementTeam("Bravo", NamedTextColor.WHITE);

    private final SidebarFixture fixture = new SidebarFixture();
    private final PlacementSidebar sidebar = fixture.createSidebar();

    @Test
    void showsTheSidebarOnlyToThePlayersInsideTheAreaOfEffectOnAScoreboardOfTheirOwn() {
        SidebarFixture.FakePlayer insider = fixture.addPlayer("Insider", fixture.inside());
        SidebarFixture.FakePlayer outsider = fixture.addPlayer("Outsider", fixture.outside());
        fixture.showStandings(runningTournament());

        sidebar.refresh();

        InMemoryScoreboard ownScoreboard = fixture.scoreboardOf(insider);
        assertEquals(Optional.of("Tournament"), ownScoreboard.sidebarTitle());
        assertEquals(List.of("Round 1 of 2", "Alpha 1 : 0 Bravo", "Still in: 2 teams", "Alpha", "Bravo"),
                ownScoreboard.sidebarLines());
        assertSame(fixture.mainScoreboard().scoreboard(), outsider.scoreboard());
        assertEquals(List.of(), fixture.mainScoreboard().sidebarLines(), "the main scoreboard shows no sidebar");
        assertEquals(1, fixture.newScoreboards().size());
        assertTrue(sidebar.isShownTo(insider.player()));
        assertFalse(sidebar.isShownTo(outsider.player()));
    }

    @Test
    void hidesTheNumbersAtTheEdge() {
        SidebarFixture.FakePlayer insider = fixture.addPlayer("Insider", fixture.inside());
        fixture.showStandings(runningTournament());

        sidebar.refresh();

        assertSame(NumberFormat.blank(), fixture.scoreboardOf(insider).sidebarNumberFormat().orElseThrow());
    }

    @Test
    void leavingTheAreaGivesThePreviousScoreboardBackAndEnteringItAgainShowsTheSidebarAgain() {
        InMemoryScoreboard scoreboardOfAnotherPlugin = new InMemoryScoreboard();
        SidebarFixture.FakePlayer walker = fixture.addPlayer("Walker", fixture.outside());
        walker.useScoreboard(scoreboardOfAnotherPlugin.scoreboard());
        fixture.showStandings(runningTournament());
        sidebar.refresh();
        assertSame(scoreboardOfAnotherPlugin.scoreboard(), walker.scoreboard());

        walker.moveTo(fixture.inside());
        sidebar.reconcile(walker.player(), walker.location());
        assertEquals(List.of("Round 1 of 2", "Alpha 1 : 0 Bravo", "Still in: 2 teams", "Alpha", "Bravo"),
                fixture.scoreboardOf(walker).sidebarLines());

        walker.moveTo(fixture.outside());
        sidebar.reconcile(walker.player(), walker.location());
        assertSame(scoreboardOfAnotherPlugin.scoreboard(), walker.scoreboard());

        walker.moveTo(fixture.inside());
        sidebar.reconcile(walker.player(), walker.location());
        assertTrue(sidebar.isShownTo(walker.player()));
        assertEquals(2, fixture.newScoreboards().size(), "every new stay gets a fresh scoreboard");
    }

    @Test
    void switchingTheSidebarOffOrLosingTheContentGivesTheMainScoreboardBack() {
        SidebarFixture.FakePlayer insider = fixture.addPlayer("Insider", fixture.inside());
        fixture.showStandings(runningTournament());
        sidebar.refresh();

        sidebar.setEnabled(false);
        assertSame(fixture.mainScoreboard().scoreboard(), insider.scoreboard());
        assertFalse(sidebar.isEnabled());

        sidebar.setEnabled(true);
        assertTrue(sidebar.isShownTo(insider.player()));

        fixture.showStandings(PlacementStandings.none());
        sidebar.refresh();
        assertSame(fixture.mainScoreboard().scoreboard(), insider.scoreboard());
    }

    @Test
    void givesEveryScoreboardBackWhenThePluginStops() {
        SidebarFixture.FakePlayer first = fixture.addPlayer("First", fixture.inside());
        SidebarFixture.FakePlayer second = fixture.addPlayer("Second", fixture.inside());
        fixture.showStandings(runningTournament());
        sidebar.refresh();

        sidebar.giveEveryScoreboardBack();

        assertSame(fixture.mainScoreboard().scoreboard(), first.scoreboard());
        assertSame(fixture.mainScoreboard().scoreboard(), second.scoreboard());
        assertFalse(sidebar.isShownTo(first.player()));
    }

    @Test
    void leavesAScoreboardAloneThatSomebodyElseSetMeanwhileAndShowsTheSidebarOnTopOfItAgain() {
        SidebarFixture.FakePlayer insider = fixture.addPlayer("Insider", fixture.inside());
        fixture.showStandings(runningTournament());
        sidebar.refresh();
        InMemoryScoreboard scoreboardOfAnotherPlugin = new InMemoryScoreboard();
        insider.useScoreboard(scoreboardOfAnotherPlugin.scoreboard());

        sidebar.refresh();
        assertTrue(sidebar.isShownTo(insider.player()));
        insider.moveTo(fixture.outside());
        sidebar.reconcile(insider.player(), insider.location());

        assertSame(scoreboardOfAnotherPlugin.scoreboard(), insider.scoreboard());
    }

    @Test
    void forgetsAPlayerWhoLeftTheServer() {
        SidebarFixture.FakePlayer leaver = fixture.addPlayer("Leaver", fixture.inside());
        fixture.showStandings(runningTournament());
        sidebar.refresh();

        leaver.goOffline();
        sidebar.refresh();
        int changesWhileOffline = leaver.scoreboardChangeCount();
        sidebar.giveEveryScoreboardBack();

        assertEquals(changesWhileOffline, leaver.scoreboardChangeCount());
        assertFalse(sidebar.isShownTo(leaver.player()));
    }

    @Test
    void updatesTheLinesAndDropsTheLinesThatAreNoLongerNeeded() {
        SidebarFixture.FakePlayer insider = fixture.addPlayer("Insider", fixture.inside());
        fixture.showStandings(runningTournament());
        sidebar.refresh();

        fixture.showStandings(PlacementStandings.tournament(PlacementPhase.TOURNAMENT_OVER, 1, 1, Optional.empty(),
                List.of(BRAVO), List.of(), List.of(new PlacementRank(1, BRAVO))));
        sidebar.refresh();

        InMemoryScoreboard ownScoreboard = fixture.scoreboardOf(insider);
        assertEquals(Optional.of("Tournament"), ownScoreboard.sidebarTitle());
        assertEquals(List.of("Over", "1. Bravo"), ownScoreboard.sidebarLines());
        assertEquals(1, fixture.newScoreboards().size());
    }

    @Test
    void waitsForTheScoreboardsOfTheServer() {
        SidebarFixture.FakePlayer insider = fixture.addPlayer("Insider", fixture.inside());
        fixture.showStandings(runningTournament());
        fixture.withholdTheScoreboardManager();

        sidebar.refresh();

        assertSame(fixture.mainScoreboard().scoreboard(), insider.scoreboard());
        assertFalse(sidebar.isShownTo(insider.player()));
    }

    @Test
    void startsOneRefreshEverySecondAndStopsIt() {
        SidebarFixture.FakePlayer insider = fixture.addPlayer("Insider", fixture.inside());
        fixture.showStandings(runningTournament());

        sidebar.start();
        sidebar.start();
        assertTrue(sidebar.isRunning());
        assertEquals(List.of(PlacementSidebar.REFRESH_PERIOD_TICKS), fixture.repeatingPeriods());
        fixture.repeatingTasks().get(0).run();
        assertTrue(sidebar.isShownTo(insider.player()));

        sidebar.stop();
        assertFalse(sidebar.isRunning());
        assertTrue(fixture.isTaskCancelled());
        assertEquals(20L, PlacementSidebar.REFRESH_PERIOD_TICKS);
    }

    @Test
    void theNameColoursOfTheFightersAreTheSameOnTheOwnScoreboardAsOnTheMainScoreboard() {
        ScoreboardFighterColours fighterColours = new ScoreboardFighterColours(fixture.server(), problem -> { });
        SidebarFixture.FakePlayer spectator = fixture.addPlayer("Spectator", fixture.inside());
        TeamMember steve = new TeamMember(UUID.randomUUID(), "Steve");
        TeamMember alex = new TeamMember(UUID.randomUUID(), "Alex");
        Match match = Match.regularMatch("round1-match1", new Team("Alpha", List.of(steve), TeamColour.GREEN),
                new Team("Bravo", List.of(alex)), new Arena());
        match.beginRound(Set.of(steve.uniqueId()), Set.of(alex.uniqueId()));
        fixture.showStandings(runningTournament());

        fighterColours.colourFighters(match);
        sidebar.refresh();
        List<String> colouredTeams = fixture.mainScoreboard().describeTeams();
        assertEquals(List.of(
                MatchTeamColours.FIRST_TEAM_SCOREBOARD_NAME + " | dark_green | Alpha | friendly fire true | "
                        + "invisibles false | ALWAYS | [Steve]",
                MatchTeamColours.SECOND_TEAM_SCOREBOARD_NAME + " | aqua | Bravo | friendly fire true | "
                        + "invisibles false | ALWAYS | [Alex]"), colouredTeams);
        assertEquals(colouredTeams, fixture.scoreboardOf(spectator).describeTeams());

        fighterColours.applySettings(new TeamColourSettings(NamedTextColor.GOLD, NamedTextColor.WHITE));
        sidebar.refresh();
        assertEquals(fixture.mainScoreboard().describeTeams(), fixture.scoreboardOf(spectator).describeTeams());

        fighterColours.clearColours();
        sidebar.refresh();
        assertEquals(fixture.mainScoreboard().describeTeams(), fixture.scoreboardOf(spectator).describeTeams());
        assertTrue(fixture.scoreboardOf(spectator).describeTeams().stream().allMatch(team -> team.endsWith("[]")));
    }

    @Test
    void aRefreshWithoutAChangeWritesNothingToTheTeamsOfTheOwnScoreboard() {
        ScoreboardFighterColours fighterColours = new ScoreboardFighterColours(fixture.server(), problem -> { });
        SidebarFixture.FakePlayer spectator = fixture.addPlayer("Spectator", fixture.inside());
        TeamMember steve = new TeamMember(UUID.randomUUID(), "Steve");
        TeamMember alex = new TeamMember(UUID.randomUUID(), "Alex");
        Match match = Match.regularMatch("round1-match1", new Team("Alpha", List.of(steve)),
                new Team("Bravo", List.of(alex)), new Arena());
        match.beginRound(Set.of(steve.uniqueId()), Set.of(alex.uniqueId()));
        fixture.showStandings(runningTournament());
        fighterColours.colourFighters(match);
        sidebar.refresh();
        int writesAfterTheFirstRefresh = fixture.scoreboardOf(spectator).teamWriteCount();

        sidebar.refresh();
        sidebar.refresh();

        assertEquals(writesAfterTheFirstRefresh, fixture.scoreboardOf(spectator).teamWriteCount());
    }

    @Test
    void keepsThePrefixesOfAnotherPluginOnTheOwnScoreboardInTheArea() {
        SidebarFixture.FakePlayer spectator = fixture.addPlayer("Spectator", fixture.inside());
        org.bukkit.scoreboard.Team rankTeam = fixture.mainScoreboard().scoreboard().registerNewTeam("rank_vip");
        rankTeam.prefix(Component.text("[VIP] "));
        rankTeam.addEntry("Spectator");
        fixture.showStandings(runningTournament());

        sidebar.refresh();

        assertEquals(fixture.mainScoreboard().describeTeams(), fixture.scoreboardOf(spectator).describeTeams());
        assertEquals(fixture.mainScoreboard().describeTeamDetails(),
                fixture.scoreboardOf(spectator).describeTeamDetails());
        assertTrue(fixture.scoreboardOf(spectator).describeTeamDetails().getFirst().startsWith(
                "rank_vip | prefix [VIP] "));
    }

    @Test
    void mirroringDropsATeamThatIsGoneFromTheMainScoreboard() {
        InMemoryScoreboard source = new InMemoryScoreboard();
        InMemoryScoreboard target = new InMemoryScoreboard();
        target.scoreboard().registerNewTeam(MatchTeamColours.FIRST_TEAM_SCOREBOARD_NAME).addEntry("Steve");

        ScoreboardTeamMirror.mirrorTeams(source.scoreboard(), target.scoreboard());

        assertEquals(List.of(), target.describeTeams());
    }

    private static PlacementStandings runningTournament() {
        List<PlacementTeam> remainingTeams = new ArrayList<>(List.of(ALPHA, BRAVO));
        return PlacementStandings.tournament(PlacementPhase.TOURNAMENT_RUNNING, 1, 2,
                Optional.of(new PlacementMatch(ALPHA, 1, BRAVO, 0)), remainingTeams, List.of(), List.of());
    }
}

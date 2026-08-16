package de.simplepvparena.match;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTeamColoursTest {

    private final TeamMember firstFighter = new TeamMember(UUID.randomUUID(), "Alpha1");
    private final TeamMember firstMate = new TeamMember(UUID.randomUUID(), "Alpha2");
    private final TeamMember secondFighter = new TeamMember(UUID.randomUUID(), "Beta1");
    private final TeamMember secondMate = new TeamMember(UUID.randomUUID(), "Beta2");
    private final Team firstTeam = new Team("Alpha", List.of(firstFighter, firstMate));
    private final Team secondTeam = new Team("Beta", List.of(secondFighter, secondMate));

    @Test
    void paintsTheFirstTeamOfTheMatchInTheFirstColour() {
        List<MatchTeamColour> colours = MatchTeamColours.of(runningMatch(), TeamColourSettings.defaults());

        assertEquals(2, colours.size(), "a match has exactly two sides");
        assertEquals("Alpha", colours.get(0).teamName());
        assertEquals(NamedTextColor.RED, colours.get(0).colour());
        assertEquals("Beta", colours.get(1).teamName());
        assertEquals(NamedTextColor.AQUA, colours.get(1).colour());
    }

    @Test
    void followsTheConfiguredColours() {
        TeamColourSettings settings = new TeamColourSettings(NamedTextColor.GOLD, NamedTextColor.DARK_PURPLE);

        List<MatchTeamColour> colours = MatchTeamColours.of(runningMatch(), settings);

        assertEquals(NamedTextColor.GOLD, colours.get(0).colour());
        assertEquals(NamedTextColor.DARK_PURPLE, colours.get(1).colour());
    }

    @Test
    void writesTheTwoSidesIntoTheTwoScoreboardTeamsOfThePlugin() {
        List<MatchTeamColour> colours = MatchTeamColours.of(runningMatch(), TeamColourSettings.defaults());

        assertEquals(MatchTeamColours.FIRST_TEAM_SCOREBOARD_NAME, colours.get(0).scoreboardTeamName());
        assertEquals(MatchTeamColours.SECOND_TEAM_SCOREBOARD_NAME, colours.get(1).scoreboardTeamName());
        assertNotEquals(MatchTeamColours.FIRST_TEAM_SCOREBOARD_NAME, MatchTeamColours.SECOND_TEAM_SCOREBOARD_NAME);
        assertEquals(List.of(MatchTeamColours.FIRST_TEAM_SCOREBOARD_NAME,
                MatchTeamColours.SECOND_TEAM_SCOREBOARD_NAME), MatchTeamColours.SCOREBOARD_TEAM_NAMES);
        for (String scoreboardTeamName : MatchTeamColours.SCOREBOARD_TEAM_NAMES) {
            assertTrue(scoreboardTeamName.length() <= 16,
                    "a scoreboard team name has to stay inside the length the game allows");
        }
    }

    @Test
    void namesEveryFighterOfTheRunningRound() {
        List<MatchTeamColour> colours = MatchTeamColours.of(runningMatch(), TeamColourSettings.defaults());

        assertEquals(List.of(firstFighter, firstMate), colours.get(0).fighters());
        assertEquals(List.of("Alpha1", "Alpha2"), colours.get(0).fighterNames());
        assertEquals(List.of("Beta1", "Beta2"), colours.get(1).fighterNames());
    }

    @Test
    void leavesEverybodyWhoIsNotFightingTheRoundUncoloured() {
        Match match = new Match("M1", firstTeam, secondTeam, new Arena());
        match.beginRound(Set.of(firstFighter.uniqueId()),
                Set.of(secondFighter.uniqueId(), secondMate.uniqueId()));

        List<MatchTeamColour> colours = MatchTeamColours.of(match, TeamColourSettings.defaults());

        assertEquals(List.of("Alpha1"), colours.get(0).fighterNames(), "the mate who is offline stays out");
        assertEquals(List.of("Beta1", "Beta2"), colours.get(1).fighterNames());
    }

    @Test
    void coloursNobodyOfAMatchThatHasNotStartedARoundYet() {
        Match match = new Match("M1", firstTeam, secondTeam, new Arena());

        List<MatchTeamColour> colours = MatchTeamColours.of(match, TeamColourSettings.defaults());

        assertEquals(List.of(), colours.get(0).fighters());
        assertEquals(List.of(), colours.get(1).fighters());
        assertEquals("Alpha", colours.get(0).teamName(), "the side itself is still part of the answer");
    }

    @Test
    void keepsBothSidesInTheAnswerWhenOneOfThemIsWipedOut() {
        Match match = runningMatch();
        match.getRoundParticipants().orElseThrow().eliminate(secondFighter.uniqueId());
        match.getRoundParticipants().orElseThrow().eliminate(secondMate.uniqueId());

        List<MatchTeamColour> colours = MatchTeamColours.of(match, TeamColourSettings.defaults());

        assertEquals(List.of("Alpha1", "Alpha2"), colours.get(0).fighterNames());
        assertEquals(List.of(), colours.get(1).fighterNames());
        assertEquals(MatchTeamColours.SECOND_TEAM_SCOREBOARD_NAME, colours.get(1).scoreboardTeamName());
    }

    private Match runningMatch() {
        Match match = new Match("M1", firstTeam, secondTeam, new Arena());
        match.beginRound(Set.of(firstFighter.uniqueId(), firstMate.uniqueId()),
                Set.of(secondFighter.uniqueId(), secondMate.uniqueId()));
        return match;
    }
}

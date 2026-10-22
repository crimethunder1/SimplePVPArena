package de.simplepvparena.match;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchTeamColoursOfColouredTeamsTest {

    private final TeamMember firstFighter = new TeamMember(UUID.randomUUID(), "First");
    private final TeamMember secondFighter = new TeamMember(UUID.randomUUID(), "Second");

    @Test
    void paintsBothTeamsInTheirOwnColours() {
        List<NamedTextColor> colours = coloursOf(TeamColour.GREEN, TeamColour.YELLOW);

        assertEquals(List.of(NamedTextColor.DARK_GREEN, NamedTextColor.YELLOW), colours);
    }

    @Test
    void paintsATeamWithoutColourInTheColourOfItsSide() {
        assertEquals(List.of(NamedTextColor.BLUE, NamedTextColor.AQUA), coloursOf(TeamColour.BLUE, null));
        assertEquals(List.of(NamedTextColor.RED, NamedTextColor.GOLD), coloursOf(null, TeamColour.ORANGE));
    }

    @Test
    void usesTheSideColoursWhenBothTeamsHaveTheSameColour() {
        assertEquals(List.of(NamedTextColor.RED, NamedTextColor.AQUA), coloursOf(TeamColour.PINK, TeamColour.PINK));
    }

    @Test
    void usesTheSideColoursWhenTwoColoursShowTheSameTextColour() {
        assertEquals(List.of(NamedTextColor.RED, NamedTextColor.AQUA), coloursOf(TeamColour.GRAY, TeamColour.BLACK));
        assertEquals(List.of(NamedTextColor.RED, NamedTextColor.AQUA),
                coloursOf(TeamColour.PINK, TeamColour.MAGENTA));
    }

    @Test
    void usesTheSideColoursWhenTheColourOfOneTeamMatchesTheSideColourOfTheOther() {
        assertEquals(List.of(NamedTextColor.RED, NamedTextColor.AQUA), coloursOf(TeamColour.LIGHT_BLUE, null));
    }

    @Test
    void usesTheConfiguredSideColoursForTeamsWithoutColour() {
        TeamColourSettings settings = new TeamColourSettings(NamedTextColor.GOLD, NamedTextColor.DARK_PURPLE);

        List<MatchTeamColour> colours = MatchTeamColours.of(match(null, TeamColour.LIME), settings);

        assertEquals(NamedTextColor.GOLD, colours.get(0).colour());
        assertEquals(NamedTextColor.GREEN, colours.get(1).colour());
    }

    private List<NamedTextColor> coloursOf(TeamColour firstColour, TeamColour secondColour) {
        return MatchTeamColours.of(match(firstColour, secondColour), TeamColourSettings.defaults()).stream()
                .map(MatchTeamColour::colour)
                .toList();
    }

    private Match match(TeamColour firstColour, TeamColour secondColour) {
        Match match = Match.regularMatch("M1", new Team("First", List.of(firstFighter), firstColour),
                new Team("Second", List.of(secondFighter), secondColour), new Arena());
        match.beginRound(Set.of(firstFighter.uniqueId()), Set.of(secondFighter.uniqueId()));
        return match;
    }
}

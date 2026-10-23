package de.simplepvparena.team;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.match.Match;
import de.simplepvparena.tournament.Bracket;
import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamNameComparisonTest {

    private final TeamMember steve = new TeamMember(UUID.randomUUID(), "Steve");
    private final TeamMember alex = new TeamMember(UUID.randomUUID(), "Alex");

    @Test
    void comparesTeamNamesLikeTheTeamRegistryFindsThem() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", List.of(steve));

        for (String writtenName : List.of("Alpha", "alpha", "ALPHA", "Bravo", "Alph")) {
            assertEquals(registry.findTeam(writtenName).isPresent(), TeamRegistry.isSameTeamName(writtenName,
                    "Alpha"), writtenName);
        }
        assertFalse(TeamRegistry.isSameTeamName(null, "Alpha"));
        assertFalse(TeamRegistry.isSameTeamName("Alpha", null));
    }

    @Test
    void theOpponentInAMatchIsFoundByTheNameOfTheTeam() {
        Team alpha = new Team("Alpha", List.of(steve));
        Team bravo = new Team("Bravo", List.of(alex));
        Match match = Match.regularMatch("M1", alpha, bravo, new ArenaRegistry().getArena());

        assertSame(bravo, match.getOpponentOf(new Team("ALPHA", List.of(steve))));
        assertSame(alpha, match.getOpponentOf(new Team("bravo", List.of(alex))));
        assertSame(bravo, match.getOpponentOf(alpha));
    }

    @Test
    void aBracketMatchFindsItsTeamsWithoutRegardToTheCase() {
        BracketMatch match = Bracket.createSingleElimination(List.of("alpha", "bravo")).getFinalMatch();

        assertTrue(match.containsTeam("ALPHA"));
        assertTrue(match.containsTeam("Bravo"));
        assertFalse(match.containsTeam("Charlie"));
        assertFalse(match.containsTeam(null));
        assertEquals(Optional.of("bravo"), match.getOpponentOf("Alpha"));
        assertEquals(Optional.of("alpha"), match.getOpponentOf("BRAVO"));
        assertEquals(Optional.empty(), match.getOpponentOf("Charlie"));
        assertEquals(Optional.empty(), match.getOpponentOf(null));
    }

    @Test
    void aWinnerWrittenInAnotherCaseKeepsTheStoredName() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));

        bracket.recordWinner("round1-match1", "ALPHA");

        BracketMatch decidedMatch = bracket.findMatch("round1-match1").orElseThrow();
        assertEquals(Optional.of("alpha"), decidedMatch.getWinnerTeamName());
        assertTrue(bracket.getFinalMatch().getFirstTeamName().filter("alpha"::equals).isPresent()
                || bracket.getFinalMatch().getSecondTeamName().filter("alpha"::equals).isPresent(),
                bracket.getFinalMatch().toString());
    }
}

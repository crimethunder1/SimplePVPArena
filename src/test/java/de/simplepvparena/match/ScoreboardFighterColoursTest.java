package de.simplepvparena.match;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreboardFighterColoursTest {

    private final Map<String, NamedTextColor> coloursByScoreboardTeam = new HashMap<>();
    private final Map<String, Set<String>> entriesByScoreboardTeam = new HashMap<>();
    private final Map<String, org.bukkit.scoreboard.Team> scoreboardTeams = new HashMap<>();
    private final ScoreboardFighterColours fighterColours = new ScoreboardFighterColours(createServer(),
            problem -> { });
    private final TeamMember steve = new TeamMember(UUID.randomUUID(), "Steve");
    private final TeamMember alex = new TeamMember(UUID.randomUUID(), "Alex");

    @Test
    void coloursTheFightersInTheColoursOfTheirTeams() {
        fighterColours.colourFighters(match(TeamColour.GREEN, TeamColour.YELLOW));

        assertEquals(NamedTextColor.DARK_GREEN, firstSideColour());
        assertEquals(NamedTextColor.YELLOW, secondSideColour());
        assertEquals(Set.of("Steve"), entriesByScoreboardTeam.get(MatchTeamColours.FIRST_TEAM_SCOREBOARD_NAME));
        assertEquals(Set.of("Alex"), entriesByScoreboardTeam.get(MatchTeamColours.SECOND_TEAM_SCOREBOARD_NAME));
    }

    @Test
    void keepsTheTeamColoursWhenTheSettingsChangeDuringAMatch() {
        fighterColours.colourFighters(match(TeamColour.GREEN, null));

        fighterColours.applySettings(new TeamColourSettings(NamedTextColor.GOLD, NamedTextColor.WHITE));

        assertEquals(NamedTextColor.DARK_GREEN, firstSideColour());
        assertEquals(NamedTextColor.WHITE, secondSideColour());
    }

    @Test
    void usesTheSideColoursAgainOnceTheColoursAreCleared() {
        fighterColours.colourFighters(match(TeamColour.GREEN, TeamColour.YELLOW));
        fighterColours.clearColours();

        fighterColours.applySettings(new TeamColourSettings(NamedTextColor.GOLD, NamedTextColor.WHITE));

        assertEquals(NamedTextColor.GOLD, firstSideColour());
        assertEquals(NamedTextColor.WHITE, secondSideColour());
        assertEquals(Set.of(), entriesByScoreboardTeam.get(MatchTeamColours.FIRST_TEAM_SCOREBOARD_NAME));
    }

    private NamedTextColor firstSideColour() {
        return coloursByScoreboardTeam.get(MatchTeamColours.FIRST_TEAM_SCOREBOARD_NAME);
    }

    private NamedTextColor secondSideColour() {
        return coloursByScoreboardTeam.get(MatchTeamColours.SECOND_TEAM_SCOREBOARD_NAME);
    }

    private Match match(TeamColour firstColour, TeamColour secondColour) {
        Match match = Match.regularMatch("M1", new Team("First", List.of(steve), firstColour),
                new Team("Second", List.of(alex), secondColour), new Arena());
        match.beginRound(Set.of(steve.uniqueId()), Set.of(alex.uniqueId()));
        return match;
    }

    private Server createServer() {
        Scoreboard scoreboard = SharedBukkitServer.proxy(Scoreboard.class, (method, arguments) ->
                switch (method.getName()) {
                    case "getTeam" -> scoreboardTeams.get((String) arguments[0]);
                    case "registerNewTeam" -> registerTeam((String) arguments[0]);
                    case "getEntryTeam" -> scoreboardTeams.entrySet().stream()
                            .filter(entry -> entriesByScoreboardTeam.get(entry.getKey()).contains(arguments[0]))
                            .map(Map.Entry::getValue)
                            .findFirst()
                            .orElse(null);
                    default -> throw new UnsupportedOperationException("Scoreboard." + method.getName());
                });
        ScoreboardManager scoreboardManager = SharedBukkitServer.proxy(ScoreboardManager.class,
                (method, arguments) -> switch (method.getName()) {
                    case "getMainScoreboard" -> scoreboard;
                    default -> throw new UnsupportedOperationException("ScoreboardManager." + method.getName());
                });
        return SharedBukkitServer.proxy(Server.class, (method, arguments) -> switch (method.getName()) {
            case "getScoreboardManager" -> scoreboardManager;
            default -> throw new UnsupportedOperationException("Server." + method.getName());
        });
    }

    private org.bukkit.scoreboard.Team registerTeam(String scoreboardTeamName) {
        Set<String> entries = new LinkedHashSet<>();
        entriesByScoreboardTeam.put(scoreboardTeamName, entries);
        org.bukkit.scoreboard.Team scoreboardTeam = SharedBukkitServer.proxy(org.bukkit.scoreboard.Team.class,
                (method, arguments) -> switch (method.getName()) {
                    case "getName" -> scoreboardTeamName;
                    case "color" -> {
                        coloursByScoreboardTeam.put(scoreboardTeamName, (NamedTextColor) arguments[0]);
                        yield null;
                    }
                    case "addEntry" -> {
                        entries.add((String) arguments[0]);
                        yield null;
                    }
                    case "removeEntry" -> entries.remove((String) arguments[0]);
                    case "getEntries" -> Set.copyOf(entries);
                    case "displayName", "setAllowFriendlyFire", "setCanSeeFriendlyInvisibles", "setOption" -> null;
                    default -> throw new UnsupportedOperationException("Team." + method.getName());
                });
        scoreboardTeams.put(scoreboardTeamName, scoreboardTeam);
        return scoreboardTeam;
    }
}

package de.simplepvparena.placement;

import de.simplepvparena.support.InMemoryScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ScoreboardTeamMirrorTest {

    private final InMemoryScoreboard source = new InMemoryScoreboard();
    private final InMemoryScoreboard target = new InMemoryScoreboard();

    @Test
    void mirrorsEveryTeamWithItsNameDisplayNamePrefixSuffixColourOptionsAndMembers() {
        Team admins = rankTeam("rank_admin", "[Admin] ", NamedTextColor.RED, "Notch");
        admins.suffix(Component.text(" *"));
        admins.setAllowFriendlyFire(false);
        admins.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        admins.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
        rankTeam("rank_guest", "[Guest] ", null, "Steve", "Alex");

        ScoreboardTeamMirror.mirrorTeams(source.scoreboard(), target.scoreboard());

        assertEquals(source.describeTeams(), target.describeTeams());
        assertEquals(source.describeTeamDetails(), target.describeTeamDetails());
        assertEquals(List.of("rank_admin | red | Admins | friendly fire false | invisibles true | ALWAYS | [Notch]",
                "rank_guest | null | Admins | friendly fire true | invisibles true | ALWAYS | [Alex, Steve]"),
                target.describeTeams());
    }

    @Test
    void followsNewChangedAndRemovedTeams() {
        Team admins = rankTeam("rank_admin", "[Admin] ", NamedTextColor.RED, "Notch");
        Team guests = rankTeam("rank_guest", "[Guest] ", NamedTextColor.GRAY, "Steve");
        ScoreboardTeamMirror.mirrorTeams(source.scoreboard(), target.scoreboard());

        admins.prefix(Component.text("[Owner] "));
        admins.color(null);
        admins.addEntry("Steve");
        guests.unregister();
        rankTeam("rank_moderator", "[Mod] ", NamedTextColor.BLUE, "Alex");
        ScoreboardTeamMirror.mirrorTeams(source.scoreboard(), target.scoreboard());

        assertEquals(source.describeTeams(), target.describeTeams());
        assertEquals(source.describeTeamDetails(), target.describeTeamDetails());
        assertEquals(List.of("rank_admin", "rank_moderator"), teamNamesOf(target));
    }

    @Test
    void writesNothingWhenNothingChanged() {
        rankTeam("rank_admin", "[Admin] ", NamedTextColor.RED, "Notch");
        ScoreboardTeamMirror.mirrorTeams(source.scoreboard(), target.scoreboard());
        int writesAfterTheFirstMirror = target.teamWriteCount();

        ScoreboardTeamMirror.mirrorTeams(source.scoreboard(), target.scoreboard());

        assertEquals(writesAfterTheFirstMirror, target.teamWriteCount());
    }

    @Test
    void leavesTheObjectivesOfTheMainScoreboardOut() {
        source.scoreboard().registerNewObjective("kills", Criteria.DUMMY, Component.text("Kills"));

        ScoreboardTeamMirror.mirrorTeams(source.scoreboard(), target.scoreboard());

        assertFalse(target.objectiveNames().contains("kills"));
    }

    private Team rankTeam(String teamName, String prefix, NamedTextColor colour, String... members) {
        Team team = source.scoreboard().registerNewTeam(teamName);
        team.displayName(Component.text("Admins"));
        team.prefix(Component.text(prefix));
        if (colour != null) {
            team.color(colour);
        }
        for (String member : members) {
            team.addEntry(member);
        }
        return team;
    }

    private static List<String> teamNamesOf(InMemoryScoreboard scoreboard) {
        return scoreboard.describeTeams().stream()
                .map(description -> description.substring(0, description.indexOf(" | ")))
                .toList();
    }
}

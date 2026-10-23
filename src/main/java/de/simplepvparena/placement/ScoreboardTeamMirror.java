package de.simplepvparena.placement;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ScoreboardTeamMirror {

    private ScoreboardTeamMirror() {
    }

    public static void mirrorTeams(Scoreboard source, Scoreboard target) {
        Set<String> sourceTeamNames = new HashSet<>();
        for (Team sourceTeam : List.copyOf(source.getTeams())) {
            String teamName = sourceTeam.getName();
            sourceTeamNames.add(teamName);
            Team targetTeam = target.getTeam(teamName);
            mirrorTeam(sourceTeam, targetTeam == null ? target.registerNewTeam(teamName) : targetTeam);
        }
        for (Team targetTeam : List.copyOf(target.getTeams())) {
            if (!sourceTeamNames.contains(targetTeam.getName())) {
                targetTeam.unregister();
            }
        }
    }

    private static void mirrorTeam(Team sourceTeam, Team targetTeam) {
        mirrorColour(sourceTeam, targetTeam);
        if (!Objects.equals(sourceTeam.displayName(), targetTeam.displayName())) {
            targetTeam.displayName(sourceTeam.displayName());
        }
        if (!Objects.equals(sourceTeam.prefix(), targetTeam.prefix())) {
            targetTeam.prefix(sourceTeam.prefix());
        }
        if (!Objects.equals(sourceTeam.suffix(), targetTeam.suffix())) {
            targetTeam.suffix(sourceTeam.suffix());
        }
        if (sourceTeam.allowFriendlyFire() != targetTeam.allowFriendlyFire()) {
            targetTeam.setAllowFriendlyFire(sourceTeam.allowFriendlyFire());
        }
        if (sourceTeam.canSeeFriendlyInvisibles() != targetTeam.canSeeFriendlyInvisibles()) {
            targetTeam.setCanSeeFriendlyInvisibles(sourceTeam.canSeeFriendlyInvisibles());
        }
        for (Team.Option option : Team.Option.values()) {
            Team.OptionStatus status = sourceTeam.getOption(option);
            if (status != targetTeam.getOption(option)) {
                targetTeam.setOption(option, status);
            }
        }
        mirrorEntries(sourceTeam, targetTeam);
    }

    private static void mirrorColour(Team sourceTeam, Team targetTeam) {
        if (!sourceTeam.hasColor()) {
            if (targetTeam.hasColor()) {
                targetTeam.color(null);
            }
            return;
        }
        NamedTextColor colour = NamedTextColor.nearestTo(sourceTeam.color());
        if (!targetTeam.hasColor() || !colour.equals(targetTeam.color())) {
            targetTeam.color(colour);
        }
    }

    private static void mirrorEntries(Team sourceTeam, Team targetTeam) {
        Set<String> sourceEntries = sourceTeam.getEntries();
        for (String entry : Set.copyOf(targetTeam.getEntries())) {
            if (!sourceEntries.contains(entry)) {
                targetTeam.removeEntry(entry);
            }
        }
        for (String entry : sourceEntries) {
            if (!targetTeam.hasEntry(entry)) {
                targetTeam.addEntry(entry);
            }
        }
    }
}

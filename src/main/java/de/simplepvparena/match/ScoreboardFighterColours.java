package de.simplepvparena.match;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class ScoreboardFighterColours implements FighterColours {

    private final Server server;
    private final Consumer<String> problemReporter;

    private final Map<String, String> previousTeamNameByFighter = new LinkedHashMap<>();

    private TeamColourSettings settings = TeamColourSettings.defaults();
    private Match colouredMatch;
    private boolean missingScoreboardReported;

    public ScoreboardFighterColours(Server server, Consumer<String> problemReporter) {
        this.server = Objects.requireNonNull(server, "server");
        this.problemReporter = Objects.requireNonNull(problemReporter, "problemReporter");
    }

    @Override
    public void applySettings(TeamColourSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        findMainScoreboard().ifPresent(scoreboard -> {
            if (colouredMatch != null) {
                for (MatchTeamColour matchTeamColour : MatchTeamColours.of(colouredMatch, settings)) {
                    applyColour(scoreboard, matchTeamColour.scoreboardTeamName(), matchTeamColour.colour());
                }
                return;
            }
            applyColour(scoreboard, MatchTeamColours.FIRST_TEAM_SCOREBOARD_NAME, settings.firstTeamColour());
            applyColour(scoreboard, MatchTeamColours.SECOND_TEAM_SCOREBOARD_NAME, settings.secondTeamColour());
        });
    }

    @Override
    public void colourFighters(Match match) {
        findMainScoreboard().ifPresent(scoreboard -> {
            releaseEveryFighter(scoreboard);
            colouredMatch = match;
            for (MatchTeamColour matchTeamColour : MatchTeamColours.of(match, settings)) {
                Team scoreboardTeam = findOrRegisterTeam(scoreboard, matchTeamColour.scoreboardTeamName());
                decorate(scoreboardTeam, matchTeamColour);
                for (String fighterName : matchTeamColour.fighterNames()) {
                    rememberPreviousTeamOf(scoreboard, fighterName);
                    scoreboardTeam.addEntry(fighterName);
                }
            }
        });
    }

    @Override
    public void clearColours() {
        findMainScoreboard().ifPresent(this::releaseEveryFighter);
    }

    public static void mirrorFighterTeams(Scoreboard source, Scoreboard target) {
        for (String scoreboardTeamName : MatchTeamColours.SCOREBOARD_TEAM_NAMES) {
            Team sourceTeam = source.getTeam(scoreboardTeamName);
            Team targetTeam = target.getTeam(scoreboardTeamName);
            if (sourceTeam == null) {
                if (targetTeam != null) {
                    targetTeam.unregister();
                }
                continue;
            }
            mirrorTeam(sourceTeam, targetTeam == null ? target.registerNewTeam(scoreboardTeamName) : targetTeam);
        }
    }

    private static void mirrorTeam(Team sourceTeam, Team targetTeam) {
        if (sourceTeam.hasColor() && !(targetTeam.hasColor() && sourceTeam.color().equals(targetTeam.color()))) {
            targetTeam.color(NamedTextColor.nearestTo(sourceTeam.color()));
        }
        if (!Objects.equals(sourceTeam.displayName(), targetTeam.displayName())) {
            targetTeam.displayName(sourceTeam.displayName());
        }
        if (sourceTeam.allowFriendlyFire() != targetTeam.allowFriendlyFire()) {
            targetTeam.setAllowFriendlyFire(sourceTeam.allowFriendlyFire());
        }
        if (sourceTeam.canSeeFriendlyInvisibles() != targetTeam.canSeeFriendlyInvisibles()) {
            targetTeam.setCanSeeFriendlyInvisibles(sourceTeam.canSeeFriendlyInvisibles());
        }
        Team.OptionStatus nameTagVisibility = sourceTeam.getOption(Team.Option.NAME_TAG_VISIBILITY);
        if (nameTagVisibility != targetTeam.getOption(Team.Option.NAME_TAG_VISIBILITY)) {
            targetTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, nameTagVisibility);
        }
        Set<String> fighterNames = sourceTeam.getEntries();
        for (String entry : Set.copyOf(targetTeam.getEntries())) {
            if (!fighterNames.contains(entry)) {
                targetTeam.removeEntry(entry);
            }
        }
        for (String fighterName : fighterNames) {
            if (!targetTeam.hasEntry(fighterName)) {
                targetTeam.addEntry(fighterName);
            }
        }
    }

    private void releaseEveryFighter(Scoreboard scoreboard) {
        for (String scoreboardTeamName : MatchTeamColours.SCOREBOARD_TEAM_NAMES) {
            Team scoreboardTeam = scoreboard.getTeam(scoreboardTeamName);
            if (scoreboardTeam == null) {
                continue;
            }
            for (String fighterName : Set.copyOf(scoreboardTeam.getEntries())) {
                scoreboardTeam.removeEntry(fighterName);
                restorePreviousTeamOf(scoreboard, fighterName);
            }
        }
        previousTeamNameByFighter.clear();
        colouredMatch = null;
    }

    private void decorate(Team scoreboardTeam, MatchTeamColour matchTeamColour) {
        scoreboardTeam.color(matchTeamColour.colour());
        scoreboardTeam.displayName(Component.text(matchTeamColour.teamName(), matchTeamColour.colour()));
        scoreboardTeam.setAllowFriendlyFire(true);
        scoreboardTeam.setCanSeeFriendlyInvisibles(false);
        scoreboardTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
    }

    private void applyColour(Scoreboard scoreboard, String scoreboardTeamName, NamedTextColor colour) {
        Team scoreboardTeam = scoreboard.getTeam(scoreboardTeamName);
        if (scoreboardTeam != null) {
            scoreboardTeam.color(colour);
        }
    }

    private Team findOrRegisterTeam(Scoreboard scoreboard, String scoreboardTeamName) {
        Team scoreboardTeam = scoreboard.getTeam(scoreboardTeamName);
        return scoreboardTeam == null ? scoreboard.registerNewTeam(scoreboardTeamName) : scoreboardTeam;
    }

    private void rememberPreviousTeamOf(Scoreboard scoreboard, String fighterName) {
        Team currentTeam = scoreboard.getEntryTeam(fighterName);
        if (currentTeam == null || MatchTeamColours.SCOREBOARD_TEAM_NAMES.contains(currentTeam.getName())) {
            return;
        }
        previousTeamNameByFighter.put(fighterName, currentTeam.getName());
    }

    private void restorePreviousTeamOf(Scoreboard scoreboard, String fighterName) {
        String previousTeamName = previousTeamNameByFighter.remove(fighterName);
        if (previousTeamName == null) {
            return;
        }
        Team previousTeam = scoreboard.getTeam(previousTeamName);
        if (previousTeam != null) {
            previousTeam.addEntry(fighterName);
        }
    }

    private Optional<Scoreboard> findMainScoreboard() {
        ScoreboardManager scoreboardManager = server.getScoreboardManager();
        if (scoreboardManager != null) {
            return Optional.of(scoreboardManager.getMainScoreboard());
        }
        if (!missingScoreboardReported) {
            missingScoreboardReported = true;
            problemReporter.accept("The server hands out no scoreboard, the fighters of a match are not "
                    + "coloured in the tab list, in the chat and above their head");
        }
        return Optional.empty();
    }
}

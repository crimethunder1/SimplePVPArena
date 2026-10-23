package de.simplepvparena.placement;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.PlacementMessage;
import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlacementLines {

    public static final int SIDEBAR_LINE_LIMIT = 15;
    public static final int BOARD_LINE_LIMIT = 20;
    public static final int TEAM_LINE_LIMIT = 8;
    public static final int AUTO_MATCH_TABLE_LIMIT = 10;

    private static final TextColor TITLE_COLOUR = TournamentMessages.PREFIX_COLOUR;
    private static final TextColor STATE_COLOUR = NamedTextColor.YELLOW;
    private static final TextColor TEXT_COLOUR = NamedTextColor.WHITE;
    private static final TextColor DETAIL_COLOUR = TournamentMessages.DETAIL_COLOUR;
    private static final String MARKER = "\u0000";
    private static final Pattern MARKED_TEAM = Pattern.compile(MARKER + "(\\d+)" + MARKER);

    private PlacementLines() {
    }

    public static Component title(PlacementStandings standings) {
        PlacementMessage title = standings.phase() == PlacementPhase.AUTO_MATCH
                ? PlacementMessage.AUTO_MATCH_TITLE
                : PlacementMessage.TOURNAMENT_TITLE;
        return Component.text(Messages.text(title), TITLE_COLOUR, TextDecoration.BOLD);
    }

    public static List<Component> sidebarLines(PlacementStandings standings) {
        List<Component> lines = switch (standings.phase()) {
            case TOURNAMENT_RUNNING, TOURNAMENT_PAUSED -> tournamentLines(standings);
            case TOURNAMENT_OVER -> firstPlacesLines(standings);
            case AUTO_MATCH -> autoMatchLines(standings);
            case NONE -> List.of();
        };
        return lines.size() <= SIDEBAR_LINE_LIMIT ? lines : List.copyOf(lines.subList(0, SIDEBAR_LINE_LIMIT));
    }

    public static List<Component> boardLines(PlacementStandings standings) {
        List<Component> lines = switch (standings.phase()) {
            case TOURNAMENT_RUNNING, TOURNAMENT_PAUSED -> withEliminations(standings);
            case TOURNAMENT_OVER -> fullPlacementLines(standings);
            case AUTO_MATCH -> autoMatchLines(standings);
            case NONE -> List.of();
        };
        return lines.size() <= BOARD_LINE_LIMIT ? lines : List.copyOf(lines.subList(0, BOARD_LINE_LIMIT));
    }

    public static Component boardText(PlacementStandings standings) {
        TextComponent.Builder text = Component.text().append(title(standings));
        for (Component line : boardLines(standings)) {
            text.append(Component.newline()).append(line);
        }
        return text.build();
    }

    static List<Component> tournamentLines(PlacementStandings standings) {
        List<Component> lines = new ArrayList<>();
        lines.add(stateLine(standings));
        lines.add(standings.runningMatch()
                .map(PlacementLines::matchLine)
                .orElseGet(() -> detailLine(Messages.text(PlacementMessage.NO_MATCH))));
        List<PlacementTeam> remainingTeams = standings.remainingTeams();
        lines.add(detailLine(Messages.plural(PlacementMessage.STILL_IN, remainingTeams.size())));
        addLimited(lines, remainingTeams.stream().map(PlacementLines::teamName).toList());
        return lines;
    }

    static List<Component> autoMatchLines(PlacementStandings standings) {
        List<Component> lines = new ArrayList<>();
        lines.add(standings.runningMatch()
                .map(PlacementLines::matchLine)
                .orElseGet(() -> detailLine(Messages.text(PlacementMessage.WAITING_FOR_AUTO_MATCH))));
        standings.autoMatchWinCounts().stream()
                .limit(AUTO_MATCH_TABLE_LIMIT)
                .map(winCount -> compose(Messages.plural(PlacementMessage.WINS, winCount.wins(),
                        Placeholder.of("team", marker(0))), TEXT_COLOUR, List.of(winCount.team())))
                .forEach(lines::add);
        return lines;
    }

    static Component stateLine(PlacementStandings standings) {
        String state = switch (standings.phase()) {
            case TOURNAMENT_PAUSED -> Messages.text(PlacementMessage.PAUSED);
            case TOURNAMENT_OVER -> Messages.text(PlacementMessage.OVER);
            default -> Messages.text(PlacementMessage.ROUND, Placeholder.of("round", standings.roundNumber()),
                    Placeholder.of("rounds", standings.roundCount()));
        };
        return Component.text(state, STATE_COLOUR);
    }

    static Component placeLine(int place, List<PlacementTeam> teams) {
        return compose(Messages.text(PlacementMessage.PLACE, Placeholder.of("place", place),
                Placeholder.of("teams", Messages.join(markers(teams.size())))), TEXT_COLOUR, teams);
    }

    private static List<String> markers(int count) {
        List<String> markers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            markers.add(marker(index));
        }
        return markers;
    }

    static Component detailLine(String text) {
        return Component.text(text, DETAIL_COLOUR);
    }

    static Component compose(String markedText, TextColor colour, List<PlacementTeam> teams) {
        TextComponent.Builder line = Component.text().color(colour);
        Matcher markedTeam = MARKED_TEAM.matcher(markedText);
        int textStart = 0;
        while (markedTeam.find()) {
            if (markedTeam.start() > textStart) {
                line.append(Component.text(markedText.substring(textStart, markedTeam.start())));
            }
            line.append(teamName(teams.get(Integer.parseInt(markedTeam.group(1)))));
            textStart = markedTeam.end();
        }
        if (textStart < markedText.length()) {
            line.append(Component.text(markedText.substring(textStart)));
        }
        return line.build();
    }

    static String marker(int index) {
        return MARKER + index + MARKER;
    }

    private static List<Component> withEliminations(PlacementStandings standings) {
        List<Component> lines = new ArrayList<>(tournamentLines(standings));
        for (PlacementElimination elimination : standings.eliminations()) {
            lines.add(compose(Messages.text(PlacementMessage.OUT_IN_ROUND,
                    Placeholder.of("round", elimination.roundNumber()),
                    Placeholder.of("teams", Messages.join(markers(elimination.teams().size())))),
                    DETAIL_COLOUR, elimination.teams()));
        }
        return lines;
    }

    private static List<Component> fullPlacementLines(PlacementStandings standings) {
        List<Component> lines = new ArrayList<>();
        lines.add(stateLine(standings));
        Map<Integer, List<PlacementTeam>> teamsByPlace = new LinkedHashMap<>();
        for (PlacementRank rank : standings.ranks()) {
            teamsByPlace.computeIfAbsent(rank.place(), unused -> new ArrayList<>()).add(rank.team());
        }
        teamsByPlace.forEach((place, teams) -> lines.add(placeLine(place, teams)));
        return lines;
    }

    private static List<Component> firstPlacesLines(PlacementStandings standings) {
        List<Component> lines = new ArrayList<>();
        lines.add(stateLine(standings));
        addLimited(lines, standings.ranks().stream()
                .map(rank -> placeLine(rank.place(), List.of(rank.team())))
                .toList());
        return lines;
    }

    private static void addLimited(List<Component> lines, List<Component> entries) {
        if (entries.size() <= TEAM_LINE_LIMIT) {
            lines.addAll(entries);
            return;
        }
        lines.addAll(entries.subList(0, TEAM_LINE_LIMIT));
        lines.add(detailLine(Messages.plural(PlacementMessage.MORE, entries.size() - TEAM_LINE_LIMIT)));
    }

    private static Component matchLine(PlacementMatch match) {
        return compose(Messages.text(PlacementMessage.MATCH, Placeholder.of("firstTeam", marker(0)),
                        Placeholder.of("firstPoints", match.firstTeamPoints()),
                        Placeholder.of("secondPoints", match.secondTeamPoints()),
                        Placeholder.of("secondTeam", marker(1))),
                TEXT_COLOUR, List.of(match.firstTeam(), match.secondTeam()));
    }

    private static Component teamName(PlacementTeam team) {
        return Component.text(team.name(), team.colour());
    }
}

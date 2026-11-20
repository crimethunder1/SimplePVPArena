package de.simplepvparena.placement;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.PlacementMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.tournament.Bracket;
import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.TournamentState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

@NullMarked
public final class BracketBoardText {

    static final String FIRST_TEAM_CONNECTOR = "┌ ";
    static final String SECOND_TEAM_CONNECTOR = "└ ";
    static final String CHAMPION_MARK = "★ ";
    static final String SHORTENED_MARK = "…";
    static final String BLANK_LINE = " ";

    private static final Set<TournamentState> SHOWN_STATES = Set.of(TournamentState.RUNNING,
            TournamentState.PAUSED, TournamentState.FINISHED);
    private static final int COLUMN_HEADER_LINES = 2;
    private static final int LINES_PER_FIRST_ROUND_MATCH = 2;
    private static final int ROUNDS_WITH_A_NAME = 3;

    private static final TextColor TITLE_COLOUR = TournamentMessages.PREFIX_COLOUR;
    private static final TextColor DETAIL_COLOUR = TournamentMessages.DETAIL_COLOUR;
    private static final TextColor TEXT_COLOUR = NamedTextColor.WHITE;
    private static final TextColor STATE_COLOUR = NamedTextColor.YELLOW;
    private static final TextColor RUNNING_COLOUR = NamedTextColor.GREEN;
    private static final TextColor NEXT_COLOUR = NamedTextColor.YELLOW;
    private static final TextColor CONNECTOR_COLOUR = NamedTextColor.DARK_GRAY;
    private static final TextColor LOSER_COLOUR = NamedTextColor.DARK_GRAY;
    private static final TextColor OPEN_COLOUR = NamedTextColor.GRAY;
    private static final TextColor CHAMPION_COLOUR = TextColor.color(0xFFC83D);

    private final Supplier<TournamentState> stateSource;
    private final Supplier<Optional<Bracket>> bracketSource;
    private final Supplier<Optional<String>> runningMatchIdSource;
    private final Function<String, Optional<Team>> teamLookup;
    private final Supplier<Optional<String>> titleImageSource;

    public BracketBoardText(Supplier<TournamentState> stateSource, Supplier<Optional<Bracket>> bracketSource,
                            Supplier<Optional<String>> runningMatchIdSource,
                            Function<String, Optional<Team>> teamLookup,
                            Supplier<Optional<String>> titleImageSource) {
        this.stateSource = Objects.requireNonNull(stateSource, "stateSource");
        this.bracketSource = Objects.requireNonNull(bracketSource, "bracketSource");
        this.runningMatchIdSource = Objects.requireNonNull(runningMatchIdSource, "runningMatchIdSource");
        this.teamLookup = Objects.requireNonNull(teamLookup, "teamLookup");
        this.titleImageSource = Objects.requireNonNull(titleImageSource, "titleImageSource");
    }

    public BracketBoardView build() {
        TournamentState state = stateSource.get();
        Optional<Bracket> shownBracket = SHOWN_STATES.contains(state) ? bracketSource.get() : Optional.empty();
        List<Component> header = new ArrayList<>();
        titleImageSource.get().ifPresent(image -> header.add(Component.text(image, NamedTextColor.WHITE)));
        header.add(Component.text(Messages.text(PlacementMessage.BRACKET_TITLE), TITLE_COLOUR, TextDecoration.BOLD));
        if (shownBracket.isEmpty()) {
            header.add(Component.text(Messages.text(PlacementMessage.NO_TOURNAMENT_YET), TEXT_COLOUR));
            header.add(Component.text(Messages.text(PlacementMessage.BRACKET_WAITING_HINT), DETAIL_COLOUR));
            return new BracketBoardView(joinLines(header), header.size(), List.of(), 0);
        }
        Bracket bracket = shownBracket.get();
        Optional<BracketMatch> running = runningMatchIdSource.get().flatMap(bracket::findMatch)
                .filter(match -> !match.isDecided());
        Optional<BracketMatch> next = findNextMatch(bracket, running);
        Optional<String> champion = bracket.getChampionTeamName();
        if (champion.isPresent()) {
            header.add(PlacementLines.compose(Messages.text(PlacementMessage.BRACKET_CHAMPION,
                    Placeholder.of("team", PlacementLines.marker(0))), CHAMPION_COLOUR,
                    List.of(toTeam(champion.get()))));
            header.add(Component.text(Messages.text(PlacementMessage.OVER), DETAIL_COLOUR));
        } else {
            if (state == TournamentState.PAUSED) {
                header.add(Component.text(Messages.text(PlacementMessage.PAUSED), STATE_COLOUR));
            }
            header.add(running.map(match -> matchLine(PlacementMessage.BRACKET_NOW, match, RUNNING_COLOUR))
                    .orElseGet(() -> Component.text(Messages.text(PlacementMessage.BRACKET_NOW_NONE),
                            DETAIL_COLOUR)));
            header.add(next.map(match -> matchLine(PlacementMessage.BRACKET_NEXT, match, NEXT_COLOUR))
                    .orElseGet(() -> Component.text(Messages.text(PlacementMessage.BRACKET_NEXT_OPEN),
                            DETAIL_COLOUR)));
        }
        return buildTree(bracket, joinLines(header), header.size(), running, next);
    }

    static Optional<BracketMatch> findNextMatch(Bracket bracket, Optional<BracketMatch> running) {
        List<BracketMatch> waiting = bracket.getPlayableMatches().stream()
                .filter(match -> running.filter(runningMatch -> runningMatch.getMatchId()
                        .equals(match.getMatchId())).isEmpty())
                .toList();
        return waiting.stream().filter(match -> !bracket.isFinalMatch(match)).findFirst()
                .or(() -> waiting.stream().findFirst());
    }

    private BracketBoardView buildTree(Bracket bracket, Component header, int headerLineCount,
                                       Optional<BracketMatch> running, Optional<BracketMatch> next) {
        List<List<BracketMatch>> rounds = bracket.getRounds();
        int bodyLineCount = rounds.get(0).size() * LINES_PER_FIRST_ROUND_MATCH;
        int lineCount = bodyLineCount + COLUMN_HEADER_LINES;
        int columnCount = rounds.size() + 1;
        int nameLength = BracketBoardGeometry.maximumNameLength(columnCount, lineCount);
        List<Component> columns = new ArrayList<>();
        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            Component[] body = blankBody(bodyLineCount);
            int span = 1 << roundIndex;
            List<BracketMatch> round = rounds.get(roundIndex);
            for (int position = 0; position < round.size(); position++) {
                BracketMatch match = round.get(position);
                int firstLine = position * 2 * span + span - 1;
                TextColor connectorColour = connectorColour(match, running, next);
                body[firstLine] = teamLine(match, BracketMatch.FIRST_SLOT_INDEX, connectorColour, nameLength);
                body[firstLine + 1] = teamLine(match, BracketMatch.SECOND_SLOT_INDEX, connectorColour, nameLength);
            }
            columns.add(column(roundName(roundIndex, rounds.size()), body));
        }
        Component[] championBody = blankBody(bodyLineCount);
        championBody[rounds.get(0).size() - 1] = championLine(bracket.getChampionTeamName(), nameLength);
        columns.add(column(Messages.text(PlacementMessage.BRACKET_WINNER_COLUMN), championBody));
        return new BracketBoardView(header, headerLineCount, columns, lineCount);
    }

    private static TextColor connectorColour(BracketMatch match, Optional<BracketMatch> running,
                                             Optional<BracketMatch> next) {
        if (running.filter(runningMatch -> runningMatch.getMatchId().equals(match.getMatchId())).isPresent()) {
            return RUNNING_COLOUR;
        }
        if (next.filter(nextMatch -> nextMatch.getMatchId().equals(match.getMatchId())).isPresent()) {
            return NEXT_COLOUR;
        }
        return CONNECTOR_COLOUR;
    }

    static String roundName(int roundIndex, int roundCount) {
        int roundsToGo = roundCount - roundIndex;
        if (roundsToGo > ROUNDS_WITH_A_NAME) {
            return Messages.text(PlacementMessage.BRACKET_ROUND, Placeholder.of("round", roundIndex + 1));
        }
        return Messages.text(switch (roundsToGo) {
            case 1 -> PlacementMessage.BRACKET_FINAL;
            case 2 -> PlacementMessage.BRACKET_SEMI_FINAL;
            default -> PlacementMessage.BRACKET_QUARTER_FINAL;
        });
    }

    private Component teamLine(BracketMatch match, int slotIndex, TextColor connectorColour, int nameLength) {
        Optional<String> teamName = slotIndex == BracketMatch.FIRST_SLOT_INDEX ? match.getFirstTeamName()
                : match.getSecondTeamName();
        Component connector = Component.text(slotIndex == BracketMatch.FIRST_SLOT_INDEX ? FIRST_TEAM_CONNECTOR
                : SECOND_TEAM_CONNECTOR, connectorColour);
        return Component.text().append(connector).append(teamName.map(name -> teamName(match, name, nameLength))
                .orElseGet(() -> openSlot(match))).build();
    }

    private Component teamName(BracketMatch match, String name, int nameLength) {
        PlacementTeam team = toTeam(name);
        String shownName = shorten(team.name(), nameLength);
        Optional<String> winner = match.getWinnerTeamName();
        if (winner.isEmpty()) {
            return Component.text(shownName, team.colour());
        }
        if (winner.get().equals(name)) {
            return Component.text(shownName, team.colour(), TextDecoration.BOLD);
        }
        return Component.text(shownName, LOSER_COLOUR, TextDecoration.STRIKETHROUGH);
    }

    private static Component openSlot(BracketMatch match) {
        if (match.isFirstRoundBye()) {
            return Component.text(Messages.text(PlacementMessage.BRACKET_BYE), LOSER_COLOUR, TextDecoration.ITALIC);
        }
        return Component.text(Messages.text(PlacementMessage.BRACKET_OPEN_TEAM), OPEN_COLOUR);
    }

    private Component championLine(Optional<String> champion, int nameLength) {
        if (champion.isEmpty()) {
            return Component.text(Messages.text(PlacementMessage.BRACKET_OPEN_TEAM), OPEN_COLOUR);
        }
        PlacementTeam team = toTeam(champion.get());
        return Component.text()
                .append(Component.text(CHAMPION_MARK, CHAMPION_COLOUR))
                .append(Component.text(shorten(team.name(), nameLength), team.colour(), TextDecoration.BOLD))
                .build();
    }

    private Component matchLine(PlacementMessage key, BracketMatch match, TextColor colour) {
        return PlacementLines.compose(Messages.text(key, Placeholder.of("firstTeam", PlacementLines.marker(0)),
                        Placeholder.of("secondTeam", PlacementLines.marker(1))), colour,
                List.of(toTeam(match.getFirstTeamName().orElseThrow()),
                        toTeam(match.getSecondTeamName().orElseThrow())));
    }

    private static Component column(String title, Component[] body) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text(title, TITLE_COLOUR, TextDecoration.BOLD));
        lines.add(Component.text(BLANK_LINE));
        lines.addAll(Arrays.asList(body));
        return joinLines(lines);
    }

    private static Component[] blankBody(int lineCount) {
        Component[] body = new Component[lineCount];
        Arrays.fill(body, Component.text(BLANK_LINE));
        return body;
    }

    static String shorten(String name, int maximumLength) {
        if (name.length() <= maximumLength) {
            return name;
        }
        return name.substring(0, Math.max(1, maximumLength - 1)) + SHORTENED_MARK;
    }

    private static Component joinLines(List<Component> lines) {
        TextComponent.Builder text = Component.text();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                text.append(Component.newline());
            }
            text.append(lines.get(index));
        }
        return text.build();
    }

    private PlacementTeam toTeam(String teamName) {
        return PlacementTeam.of(teamName, teamLookup.apply(teamName));
    }
}

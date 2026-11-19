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
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@NullMarked
public final class PlacementBoardText {

    public static final int LEADERBOARD_LIMIT = 10;

    static final String SEPARATOR = "━".repeat(11);

    private static final TextColor SEPARATOR_COLOUR = NamedTextColor.DARK_GRAY;
    private static final TextColor TITLE_COLOUR = TournamentMessages.PREFIX_COLOUR;
    private static final TextColor FIRST_PLACE_COLOUR = TextColor.color(0xFFC83D);
    private static final TextColor SECOND_PLACE_COLOUR = TextColor.color(0xD7DCE1);
    private static final TextColor THIRD_PLACE_COLOUR = TextColor.color(0xD08A4E);
    private static final TextColor OTHER_PLACE_COLOUR = NamedTextColor.WHITE;
    private static final TextColor HINT_COLOUR = TournamentMessages.DETAIL_COLOUR;

    private final Supplier<List<AutoMatchWins>> leaderboardSource;
    private final Supplier<Optional<String>> titleImageSource;

    public PlacementBoardText(Supplier<List<AutoMatchWins>> leaderboardSource,
                              Supplier<Optional<String>> titleImageSource) {
        this.leaderboardSource = Objects.requireNonNull(leaderboardSource, "leaderboardSource");
        this.titleImageSource = Objects.requireNonNull(titleImageSource, "titleImageSource");
    }

    public Optional<Component> build(PlacementStandings standings) {
        List<Component> lines = new ArrayList<>();
        Optional<String> titleImage = titleImageSource.get();
        titleImage.ifPresent(image -> lines.add(Component.text(image, NamedTextColor.WHITE)));
        lines.add(Component.text(titleOf(standings), TITLE_COLOUR, TextDecoration.BOLD));
        lines.add(separator());
        lines.addAll(bodyOf(standings));
        lines.add(separator());
        TextComponent.Builder text = Component.text();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                text.append(Component.newline());
            }
            text.append(lines.get(index));
        }
        return Optional.of(text.build());
    }

    private String titleOf(PlacementStandings standings) {
        if (standings.hasContent()) {
            return Messages.text(PlacementMessage.TOURNAMENT_TITLE);
        }
        return Messages.text(PlacementMessage.AUTO_MATCH_TITLE);
    }

    private List<Component> bodyOf(PlacementStandings standings) {
        if (standings.hasContent()) {
            return PlacementLines.boardLines(standings);
        }
        List<AutoMatchWins> leaderboard = leaderboardSource.get();
        if (leaderboard.isEmpty()) {
            return List.of(Component.text(Messages.text(PlacementMessage.NO_GAMES_YET), NamedTextColor.WHITE),
                    Component.text(Messages.text(PlacementMessage.NO_GAMES_HINT), HINT_COLOUR));
        }
        List<Component> lines = new ArrayList<>();
        int shown = Math.min(LEADERBOARD_LIMIT, leaderboard.size());
        for (int index = 0; index < shown; index++) {
            AutoMatchWins entry = leaderboard.get(index);
            int place = index + 1;
            lines.add(Component.text(Messages.plural(PlacementMessage.WINS_ENTRY, entry.wins(),
                    Placeholder.of("place", place), Placeholder.of("player", entry.playerName())),
                    colourOf(place)));
        }
        return lines;
    }

    static TextColor colourOf(int place) {
        return switch (place) {
            case 1 -> FIRST_PLACE_COLOUR;
            case 2 -> SECOND_PLACE_COLOUR;
            case 3 -> THIRD_PLACE_COLOUR;
            default -> OTHER_PLACE_COLOUR;
        };
    }

    private static Component separator() {
        return Component.text(SEPARATOR, SEPARATOR_COLOUR);
    }
}

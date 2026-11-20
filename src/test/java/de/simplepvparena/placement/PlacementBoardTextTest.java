package de.simplepvparena.placement;

import de.simplepvparena.config.ServerMode;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlacementBoardTextTest {

    private static final String LINE = PlacementBoardText.SEPARATOR;
    private static final PlacementTeam ALPHA = new PlacementTeam("Alpha", NamedTextColor.RED);
    private static final PlacementTeam BRAVO = new PlacementTeam("Bravo", NamedTextColor.WHITE);

    private final List<AutoMatchWins> leaderboard = new ArrayList<>();
    private Optional<String> titleImage = Optional.empty();
    private ServerMode serverMode = ServerMode.AUTO;
    private final PlacementBoardText boardText = new PlacementBoardText(() -> leaderboard, () -> titleImage,
            () -> serverMode);

    @Test
    @UseLanguage("en")
    void showsAnInvitationWhileNothingWasPlayed() {
        assertEquals("Auto match leaderboard\n" + LINE + "\nNo games played yet\n"
                + "Register and fight for the first place!\n" + LINE, plain(PlacementStandings.none()));
    }

    @Test
    @UseLanguage("de")
    void showsTheAutoMatchWinsWithTheTitleImageInGerman() {
        titleImage = Optional.of("");
        leaderboard.add(new AutoMatchWins(UUID.randomUUID(), "Bot3", 2));
        leaderboard.add(new AutoMatchWins(UUID.randomUUID(), "Bot1", 1));

        assertEquals("\nAuto-Match-Rangliste\n" + LINE + "\n1. Bot3 - 2 Siege\n2. Bot1 - 1 Sieg\n" + LINE,
                plain(PlacementStandings.none()));
    }

    @Test
    @UseLanguage("de")
    void showsNoAutoMatchLeaderboardInTheEventMode() {
        serverMode = ServerMode.EVENT;
        leaderboard.add(new AutoMatchWins(UUID.randomUUID(), "Bot3", 2));

        assertEquals("Turnier\n" + LINE + "\nNoch kein Turnier\nMelde dich an, das Turnier startet bald!\n" + LINE,
                plain(PlacementStandings.none()));
    }

    @Test
    @UseLanguage("en")
    void showsTheTournamentWhileOneRuns() {
        leaderboard.add(new AutoMatchWins(UUID.randomUUID(), "Bot3", 2));
        PlacementStandings tournament = PlacementStandings.tournament(PlacementPhase.TOURNAMENT_RUNNING, 1, 2,
                Optional.of(new PlacementMatch(ALPHA, 1, BRAVO, 0)), List.of(ALPHA, BRAVO), List.of(), List.of());

        assertEquals("Tournament\n" + LINE + "\nRound 1 of 2\nAlpha 1 : 0 Bravo\nStill in: 2 teams\nAlpha\nBravo\n"
                + LINE, plain(tournament));
    }

    @Test
    void coloursTheFirstThreePlacesLikeMedals() {
        assertEquals(PlacementBoardText.colourOf(4), PlacementBoardText.colourOf(10));
        assertEquals(3, List.of(PlacementBoardText.colourOf(1), PlacementBoardText.colourOf(2),
                PlacementBoardText.colourOf(3)).stream().distinct().count());
    }

    private String plain(PlacementStandings standings) {
        Component text = boardText.build(standings).orElseThrow();
        return TournamentMessages.toPlainText(text);
    }
}

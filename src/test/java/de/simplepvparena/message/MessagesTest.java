package de.simplepvparena.message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    private final List<String> warnings = new ArrayList<>();

    @BeforeEach
    void collectWarnings() {
        Messages.useWarningLogger(warnings::add);
    }

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    void aKeyIsReplacedByItsText() {
        Messages.use(new MapMessageCatalog().with("menu.back.name", "Back"));

        assertEquals("Back", Messages.text(TestMessageKey.of("menu.back.name")));
        assertEquals(List.of(), warnings);
    }

    @Test
    void severalPlaceholdersAreFilledIn() {
        Messages.use(new MapMessageCatalog().with("match.won", "{winner} beats {loser} in {round}"));

        assertEquals("Alpha beats Beta in round 2", Messages.text(TestMessageKey.of("match.won"),
                Placeholder.of("winner", "Alpha"), Placeholder.of("loser", "Beta"),
                Placeholder.of("round", "round 2")));
    }

    @Test
    void aPlaceholderThatStandsTwiceIsFilledBothTimes() {
        Messages.use(new MapMessageCatalog().with("team.twice", "{team} against {team}"));

        assertEquals("Alpha against Alpha", Messages.text(TestMessageKey.of("team.twice"),
                Placeholder.of("team", "Alpha")));
    }

    @Test
    void aPlaceholderGivenTwiceTakesTheLastValue() {
        Messages.use(new MapMessageCatalog().with("team.name", "Team {team}"));

        assertEquals("Team Beta", Messages.text(TestMessageKey.of("team.name"),
                Placeholder.of("team", "Alpha"), Placeholder.of("team", "Beta")));
    }

    @Test
    void aPlaceholderWithoutAValueStaysInTheTextAndIsWarnedAboutOnce() {
        Messages.use(new MapMessageCatalog().with("team.renamed", "Team {old} is now called {new}"));

        String first = Messages.text(TestMessageKey.of("team.renamed"), Placeholder.of("old", "Alpha"));
        String second = Messages.text(TestMessageKey.of("team.renamed"), Placeholder.of("old", "Alpha"));

        assertEquals("Team Alpha is now called {new}", first);
        assertEquals(first, second);
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("team.renamed") && warnings.get(0).contains("{new}"), warnings.get(0));
    }

    @Test
    void aValueThatLooksLikeAPlaceholderIsNeverReadAsOne() {
        Messages.use(new MapMessageCatalog().with("team.joined", "{player} joined, {count} players"));

        assertEquals("{count} joined, 3 players", Messages.text(TestMessageKey.of("team.joined"),
                Placeholder.of("player", "{count}"), Placeholder.of("count", 3)));
    }

    @Test
    void dollarSignsAndBackslashesOfAValueAreKept() {
        Messages.use(new MapMessageCatalog().with("team.name", "Team {team}"));

        assertEquals("Team $1\\Alpha", Messages.text(TestMessageKey.of("team.name"),
                Placeholder.of("team", "$1\\Alpha")));
    }

    @Test
    void aValueIsAnyObjectWrittenAsText() {
        Messages.use(new MapMessageCatalog().with("arena.radius", "Radius {radius}, ready {ready}"));

        assertEquals("Radius 12, ready true", Messages.text(TestMessageKey.of("arena.radius"),
                Placeholder.of("radius", 12), Placeholder.of("ready", true)));
    }

    @Test
    void aPlaceholderNameHoldsOnlyLettersDigitsHyphensAndUnderscores() {
        assertEquals("page-count_2", Placeholder.of("page-count_2", 1).name());
        assertThrows(IllegalArgumentException.class, () -> Placeholder.of("page count", 1));
        assertThrows(IllegalArgumentException.class, () -> Placeholder.of("{page}", 1));
    }

    @Test
    void aMissingKeyShowsItsPathAndIsWarnedAboutOnlyOnce() {
        Messages.use(new MapMessageCatalog());

        assertEquals("menu.unknown", Messages.text(TestMessageKey.of("menu.unknown")));
        assertEquals("menu.unknown", Messages.text(TestMessageKey.of("menu.unknown")));
        assertEquals(List.of("menu.unknown"), Messages.lines(TestMessageKey.of("menu.unknown")));

        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("menu.unknown"), warnings.get(0));
    }

    @Test
    void aNewCatalogWarnsAboutAMissingKeyAgain() {
        Messages.use(new MapMessageCatalog());
        Messages.text(TestMessageKey.of("menu.unknown"));

        Messages.use(new MapMessageCatalog());
        Messages.text(TestMessageKey.of("menu.unknown"));

        assertEquals(2, warnings.size(), warnings.toString());
    }

    @Test
    void aTextOfSeveralLinesIsJoinedWithTheLineBreak() {
        Messages.use(new MapMessageCatalog().with("player.unknown", "Unknown player {player}",
                "The player has to be online"));

        assertEquals("Unknown player Steve" + TournamentMessages.LINE_BREAK + "The player has to be online",
                Messages.text(TestMessageKey.of("player.unknown"), Placeholder.of("player", "Steve")));
        assertEquals(List.of("Unknown player Steve", "The player has to be online"),
                Messages.lines(TestMessageKey.of("player.unknown"), Placeholder.of("player", "Steve")));
    }

    @Test
    void aTextOfOneLineIsASingleLine() {
        Messages.use(new MapMessageCatalog().with("menu.back.name", "Back"));

        assertEquals(List.of("Back"), Messages.lines(TestMessageKey.of("menu.back.name")));
    }

    @Test
    void thePluralTakesTheSingularFormForExactlyOne() {
        Messages.use(pluralCatalog());

        assertEquals("1 team has more than 2 players",
                Messages.plural(TestMessageKey.of("team.larger", "count", "size"), 1, Placeholder.of("size", 2)));
    }

    @Test
    void thePluralTakesTheOtherFormForEveryOtherCount() {
        Messages.use(pluralCatalog());
        TestMessageKey key = TestMessageKey.of("team.larger", "count", "size");

        assertEquals("0 teams have more than 2 players", Messages.plural(key, 0, Placeholder.of("size", 2)));
        assertEquals("2 teams have more than 2 players", Messages.plural(key, 2, Placeholder.of("size", 2)));
        assertEquals("12 teams have more than 4 players", Messages.plural(key, 12, Placeholder.of("size", 4)));
    }

    @Test
    void aMissingPluralFormShowsItsPath() {
        Messages.use(new MapMessageCatalog().with("team.larger.other", "{count} teams"));

        assertEquals("team.larger.one", Messages.plural(TestMessageKey.of("team.larger", "count"), 1));
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void anEnumerationNamesTheLastItemWithAnd() {
        assertEquals("", Messages.join(List.of()));
        assertEquals("Alpha", Messages.join(List.of("Alpha")));
        assertEquals("Alpha and Beta", Messages.join(List.of("Alpha", "Beta")));
        assertEquals("Alpha, Beta and Gamma", Messages.join(List.of("Alpha", "Beta", "Gamma")));
    }

    @Test
    void aLimitedEnumerationCountsTheItemsBeyondTheLimit() {
        List<String> teams = List.of("A", "B", "C", "D", "E", "F", "G");

        assertEquals("A, B, C, D, E and 2 more", Messages.listLimited(teams, 5));
        assertEquals("A and 6 more", Messages.listLimited(teams, 1));
        assertEquals("A, B, C, D, E, F and G", Messages.listLimited(teams, 7));
        assertEquals("A, B and C", Messages.listLimited(List.of("A", "B", "C"), 5));
        assertThrows(IllegalArgumentException.class, () -> Messages.listLimited(teams, 0));
    }

    @Test
    void enumerationsFollowTheLanguageOfTheCatalog() {
        Messages.use(MessageCatalogLoader.shippedCatalog("de"));

        assertEquals("Alpha, Beta und Gamma", Messages.join(List.of("Alpha", "Beta", "Gamma")));
        assertEquals("A, B und 2 mehr", Messages.listLimited(List.of("A", "B", "C", "D"), 2));
    }

    @Test
    void withoutAnyCatalogSetTheShippedEnglishFileIsUsed() {
        Messages.use(MessageCatalogLoader.shippedCatalog("de"));

        Messages.reset();

        assertEquals("Alpha and Beta", Messages.join(List.of("Alpha", "Beta")));
        assertEquals(List.of("{items} and {last}"),
                Messages.catalog().find(CommonMessage.LIST_AND.path()).orElseThrow());
    }

    private static MapMessageCatalog pluralCatalog() {
        return new MapMessageCatalog()
                .with("team.larger.one", "{count} team has more than {size} players")
                .with("team.larger.other", "{count} teams have more than {size} players");
    }
}

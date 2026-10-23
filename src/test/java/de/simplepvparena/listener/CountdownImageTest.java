package de.simplepvparena.listener;

import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.visual.ConfiguredVisuals;
import de.simplepvparena.visual.CustomVisualSettings;
import de.simplepvparena.visual.RecordingCustomVisuals;
import de.simplepvparena.visual.VanillaVisuals;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CountdownImageTest {

    private static final long SECOND = ArenaMatchHarness.TICKS_PER_SECOND;
    private static final int GATE_X = 2;
    private static final int GATE_Z = 3;
    private static final String THREE_IMAGE = "";
    private static final String TWO_IMAGE = "";
    private static final String ONE_IMAGE = "";
    private static final String FIGHT_IMAGE = "";
    private static final CustomVisualSettings EVERY_IMAGE = new CustomVisualSettings(true,
            "simplepvparena:countdown_3", "simplepvparena:countdown_2", "simplepvparena:countdown_1",
            "simplepvparena:countdown_fight", "", "");
    private static final Title.Times SECOND_TITLE_TIMES = Title.Times.times(Duration.ZERO, Duration.ofMillis(800L),
            Duration.ofMillis(200L));
    private static final Title.Times RELEASE_TITLE_TIMES = Title.Times.times(Duration.ZERO,
            Duration.ofMillis(1200L), Duration.ofMillis(400L));

    private final List<String> warnings = new ArrayList<>();
    private final RecordingCustomVisuals customVisuals = new RecordingCustomVisuals()
            .withFontImage("simplepvparena:countdown_3", THREE_IMAGE)
            .withFontImage("simplepvparena:countdown_2", TWO_IMAGE)
            .withFontImage("simplepvparena:countdown_1", ONE_IMAGE)
            .withFontImage("simplepvparena:countdown_fight", FIGHT_IMAGE);
    private final ConfiguredVisuals configuredVisuals = new ConfiguredVisuals(customVisuals, warnings::add);

    private ArenaMatchHarness harness;
    private MatchService matchService;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        matchService = harness.getMatchService();
        matchService.setCountdownSeconds(3);
        matchService.setCountdownImages(configuredVisuals);
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
    }

    @Test
    void withAnImageForEverySecondTheCountdownShowsTheImagesInsteadOfTheNumbersAndTheWord() {
        configuredVisuals.applySettings(EVERY_IMAGE);

        start();
        harness.advanceSeconds(3);

        assertEquals(List.of(THREE_IMAGE + " / ", TWO_IMAGE + " / ", ONE_IMAGE + " / ", FIGHT_IMAGE + " / "),
                fighter.titleTexts());
        assertEquals(fighter.titleTexts(), opponentMate.titleTexts(), "every fighter sees the same images");
        assertEquals(List.of(), warnings);
    }

    @Test
    void anImageKeepsTheTimesOfTheTitleAndIsNotTinted() {
        configuredVisuals.applySettings(EVERY_IMAGE);

        start();
        harness.advanceSeconds(3);

        List<Title> titles = fighter.titles();
        assertEquals(4, titles.size());
        for (Title secondTitle : titles.subList(0, 3)) {
            assertEquals(SECOND_TITLE_TIMES, secondTitle.times());
            assertNull(secondTitle.title().color());
            assertEquals(Component.empty(), secondTitle.subtitle());
        }
        assertEquals(RELEASE_TITLE_TIMES, titles.get(3).times());
        assertNull(titles.get(3).title().color());
    }

    @Test
    void aSecondWithoutAnIdentifierKeepsItsOwnTitle() {
        configuredVisuals.applySettings(new CustomVisualSettings(true, "simplepvparena:countdown_3", "", "",
                "simplepvparena:countdown_fight", "", ""));

        start();
        harness.advanceSeconds(3);

        assertEquals(List.of(THREE_IMAGE + " / ", "2 / ", "1 / ", FIGHT_IMAGE + " / "), fighter.titleTexts());
        assertEquals(TournamentMessages.WARNING_COLOUR, fighter.titles().get(1).title().color());
        assertEquals(TournamentMessages.WARNING_COLOUR, fighter.titles().get(2).title().color());
    }

    @Test
    void onlyTheWordOfTheFightCanBeAnImage() {
        configuredVisuals.applySettings(new CustomVisualSettings(true, "", "", "", "simplepvparena:countdown_fight",
                "", ""));

        start();
        harness.advanceSeconds(3);

        assertEquals(List.of("3 / ", "2 / ", "1 / ", FIGHT_IMAGE + " / "), fighter.titleTexts());
    }

    @Test
    void secondsAboveThreeKeepTheirNumbers() {
        matchService.setCountdownSeconds(5);
        configuredVisuals.applySettings(EVERY_IMAGE);

        start();
        harness.advanceSeconds(5);

        assertEquals(List.of("5 / ", "4 / ", THREE_IMAGE + " / ", TWO_IMAGE + " / ", ONE_IMAGE + " / ",
                FIGHT_IMAGE + " / "), fighter.titleTexts());
        assertEquals(TournamentMessages.WARNING_COLOUR, fighter.titles().get(0).title().color());
    }

    @Test
    void theStartGateCountdownShowsTheImagesWithItsOwnSubtitles() {
        configuredVisuals.applySettings(EVERY_IMAGE);
        markGateBlock();

        start();
        harness.advanceTicksWithRepeatingTasks(10 * SECOND);

        List<String> titleTexts = fighter.titleTexts();
        assertEquals(11, titleTexts.size(), titleTexts.toString());
        for (int index = 0; index < 7; index++) {
            assertEquals((10 - index) + " / The gate comes down layer by layer", titleTexts.get(index));
        }
        assertEquals(List.of(THREE_IMAGE + " / The gate comes down layer by layer",
                TWO_IMAGE + " / The gate comes down layer by layer",
                ONE_IMAGE + " / The gate comes down layer by layer", FIGHT_IMAGE + " / The gate is open"),
                titleTexts.subList(7, 11));
    }

    @Test
    void anUnknownIdentifierKeepsTheOwnTitleAndIsWarnedAboutOnce() {
        configuredVisuals.applySettings(new CustomVisualSettings(true, "simplepvparena:missing",
                "simplepvparena:missing", "simplepvparena:missing", "simplepvparena:missing", "", ""));

        start();
        harness.advanceSeconds(3);

        assertEquals(List.of("3 / ", "2 / ", "1 / ", "Fight! / "), fighter.titleTexts());
        assertEquals(List.of("ItemsAdder has no font image simplepvparena:missing, set in "
                + "items-adder.countdown-images.three of config.yml, the own title of the plugin is shown instead"),
                warnings);
    }

    @Test
    void aFailingLookupKeepsTheOwnTitle() {
        customVisuals.failingFor("simplepvparena:countdown_2");
        configuredVisuals.applySettings(EVERY_IMAGE);

        start();
        harness.advanceSeconds(3);

        assertEquals(List.of(THREE_IMAGE + " / ", "2 / ", ONE_IMAGE + " / ", FIGHT_IMAGE + " / "),
                fighter.titleTexts());
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void theSwitchedOffSettingKeepsEveryOwnTitle() {
        configuredVisuals.applySettings(new CustomVisualSettings(false, "simplepvparena:countdown_3",
                "simplepvparena:countdown_2", "simplepvparena:countdown_1", "simplepvparena:countdown_fight", "", ""));

        start();
        harness.advanceSeconds(3);

        assertEquals(List.of("3 / ", "2 / ", "1 / ", "Fight! / "), fighter.titleTexts());
        assertEquals(List.of(), customVisuals.lookups());
    }

    @Test
    void aReloadDuringTheCountdownAppliesFromTheNextSecondOn() {
        start();
        configuredVisuals.applySettings(EVERY_IMAGE);
        harness.advanceSeconds(3);

        assertEquals(List.of("3 / ", TWO_IMAGE + " / ", ONE_IMAGE + " / ", FIGHT_IMAGE + " / "),
                fighter.titleTexts());
    }

    @Test
    void withoutItemsAdderTheCountdownIsTheSameAsWithoutAnyImage() {
        ConfiguredVisuals vanilla = new ConfiguredVisuals(new VanillaVisuals(), warnings::add);
        vanilla.applySettings(EVERY_IMAGE);
        matchService.setCountdownImages(vanilla);
        markGateBlock();

        start();
        harness.advanceTicksWithRepeatingTasks(10 * SECOND);

        assertEquals(List.of("10", "9", "8", "7", "6", "5", "4", "3", "2", "1", "Fight!"), titleHeadlines(fighter));
        assertEquals(TournamentMessages.SUCCESS_COLOUR, fighter.titles().get(10).title().color());
        assertEquals(List.of(), warnings, "without ItemsAdder no identifier is looked up or warned about");
    }

    private void start() {
        harness.startMatch(fighter, mate, opponent, opponentMate);
    }

    private void markGateBlock() {
        harness.setBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z, Material.BARRIER);
        harness.getGateMarks().mark(ArenaMatchHarness.WORLD_NAME, GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z,
                Material.BARRIER);
    }

    private static List<String> titleHeadlines(ArenaMatchHarness.FakePlayer player) {
        return player.titles().stream().map(title -> TournamentMessages.toPlainText(title.title())).toList();
    }
}

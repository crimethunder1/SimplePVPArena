package de.simplepvparena.gui;

import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KitChoiceAdminMenuSnapshotTest {

    private static final int MAIN_KIT_CHOICE_SLOT = 23;
    private static final int NEXT_PAGE_SLOT = 53;

    private final AdminMenuFixture fixture = AdminMenuFixture.withKitChoice();

    @Test
    void showsTheKitsOfTheChoiceWithTheDefaultKitMarked() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");

        beginTheChoice();

        assertEquals("""
                Choose the kit for round1-match1 | 54
                0 | CHEST | Archer | Worn items: 2 of 5 / Inventory items: 3 of 36 / Default kit / \
                Click to use it for this match
                1 | CHEST | Knight | Worn items: 2 of 5 / Inventory items: 3 of 36 / Click to use it for this match
                49 | BOOK | Kit choice | Match round1-match1: Alpha against Bravo / Default kit: Archer / \
                Time left: 30 seconds / The default kit is used when nobody chooses in time
                """, fixture.describeLastMenu());
    }

    @Test
    void marksTheDefaultKitWhereverItStandsInTheList() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.kitLibrary().setDefaultKit("Knight");

        beginTheChoice();

        assertEquals("""
                Choose the kit for round1-match1 | 54
                0 | CHEST | Archer | Worn items: 2 of 5 / Inventory items: 3 of 36 / Click to use it for this match
                1 | CHEST | Knight | Worn items: 2 of 5 / Inventory items: 3 of 36 / Default kit / \
                Click to use it for this match
                49 | BOOK | Kit choice | Match round1-match1: Alpha against Bravo / Default kit: Knight / \
                Time left: 30 seconds / The default kit is used when nobody chooses in time
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheTimeLeftWhenThePopupIsOpenedAgainFromTheMainMenu() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        beginTheChoice();
        fixture.kitChoiceScheduler().advanceSeconds(12);

        fixture.openMainMenu();
        fixture.click(MAIN_KIT_CHOICE_SLOT);

        assertEquals("""
                Choose the kit for round1-match1 | 54
                0 | CHEST | Archer | Worn items: 2 of 5 / Inventory items: 3 of 36 / Default kit / \
                Click to use it for this match
                1 | CHEST | Knight | Worn items: 2 of 5 / Inventory items: 3 of 36 / Click to use it for this match
                49 | BOOK | Kit choice | Match round1-match1: Alpha against Bravo / Default kit: Archer / \
                Time left: 18 seconds / The default kit is used when nobody chooses in time
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheFirstPageOfMoreKitsThanFitOnOnePageWithTheArrowToTheNextPage() {
        fixture.createNumberedKits(46);

        beginTheChoice();

        String menu = fixture.describeLastMenu();
        assertEquals("Choose the kit for round1-match1 | 54", menu.lines().findFirst().orElseThrow());
        assertEquals("0 | CHEST | Kit01 | Worn items: 0 of 5 / Inventory items: 1 of 36 / Default kit / "
                + "Click to use it for this match", menu.lines().skip(1).findFirst().orElseThrow());
        assertEquals("""
                44 | CHEST | Kit45 | Worn items: 0 of 5 / Inventory items: 1 of 36 / Click to use it for this match
                49 | BOOK | Kit choice | Match round1-match1: Alpha against Bravo / Default kit: Kit01 / \
                Time left: 30 seconds / The default kit is used when nobody chooses in time
                53 | ARROW | Next page | Page 2 of 2
                """, String.join("\n", menu.lines().skip(45).toList()) + "\n");
    }

    @Test
    void showsTheSecondPageWithTheArrowBackToTheFirstPage() {
        fixture.createNumberedKits(46);
        beginTheChoice();

        fixture.click(NEXT_PAGE_SLOT);

        assertEquals("""
                Choose the kit for round1-match1 | 54
                0 | CHEST | Kit46 | Worn items: 0 of 5 / Inventory items: 1 of 36 / Click to use it for this match
                45 | ARROW | Previous page | Page 1 of 2
                49 | BOOK | Kit choice | Match round1-match1: Alpha against Bravo / Default kit: Kit01 / \
                Time left: 30 seconds / The default kit is used when nobody chooses in time
                """, fixture.describeLastMenu());
    }

    @Test
    void tellsInTheInfoTileThatTheChoiceIsOverWhenThePopupIsFilledAfterTheDeadline() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        beginTheChoice();
        fixture.kitChoiceScheduler().advanceSeconds(30);

        fixture.refillLastMenu();

        assertEquals("""
                Choose the kit for round1-match1 | 54
                0 | CHEST | Archer | Worn items: 2 of 5 / Inventory items: 3 of 36 / Default kit / \
                Click to use it for this match
                1 | CHEST | Knight | Worn items: 2 of 5 / Inventory items: 3 of 36 / Click to use it for this match
                49 | BOOK | Kit choice | Match round1-match1 / The kit choice for match round1-match1 is over
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheKitChoiceTileInTheMainMenuWhileTheKitOfTheFinalIsChosen() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.startTournament();
        fixture.startFinal();
        BracketMatch finalMatch = fixture.tournamentManager().getBracket().orElseThrow().getFinalMatch();
        String teams = finalMatch.getFirstTeamName().orElseThrow() + " against "
                + finalMatch.getSecondTeamName().orElseThrow();

        fixture.openMainMenu();

        assertEquals("""
                Tournament administration | 27
                4 | BOOK | Tournament | Tournament state: RUNNING | rounds: 1 | running match: none | final: READY / \
                Registered teams: 2 / Arena: ready / Running match: none
                10 | GRASS_BLOCK | Arena | Fight area, spawns, cleaning and reset / Arena ready
                11 | PLAYER_HEAD | Teams | Registered teams: 2 / Shows every team, adds one and removes one
                12 | DIAMOND_SWORD | Match and tournament | Start, pause, cancel and force a result / \
                Running match: none
                13 | BRUSH | Mark border blocks by clicking them | Turns every right click of yours into a border mark \
                / A click marks a block, a click on a marked block / takes the mark away again / \
                No border item is needed: stairs, fences and every / \
                other block that already stands there can be marked / \
                Chests, doors and everything else stay shut while it is on / \
                The menu closes so you can click the blocks / The mode is off, this tile switches it on
                14 | NETHER_STAR | Start the final | Both finalists are known / Starts the final on the arena
                15 | PAPER | Tournament status | Writes the state, the rounds and the running match into the chat
                16 | WRITTEN_BOOK | Bracket | Writes every round and every match into the chat / \
                A bracket is a tree, the chat shows it better than a grid / The menu closes so you can read it
                18 | GRAY_DYE | Dry run of the pipeline | A tournament is in progress, the dry run is a check for \
                before an event / It registers two test teams and would put them into the registry of a running \
                bracket / Wait until the event is over or cancel it in the match menu of /tournament
                20 | COMPARATOR | Reload config.yml | Reads config.yml again and applies it live / \
                Round timeout, cleaning batch sizes, cell limit, / safety zone and border particles / \
                The arena, the teams and a running match stay untouched / The menu closes so you can read the answer
                22 | BARRIER | Close | Closes the menu
                23 | CHEST | Choose the kit for the next match | Match round1-match1: %s / \
                The popup closes when the time is over or a kit is chosen
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 2 / Default kit: Archer / \
                The items every fighter is given when a round starts / Create, replace and delete kits
                """.formatted(teams), fixture.describeLastMenu());
    }

    @Test
    void showsTheMainMenuWithoutTheKitChoiceTileOnceTheKitIsChosen() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        beginTheChoice();
        fixture.openMainMenu();
        String mainMenuWithTheTile = fixture.describeLastMenu();

        fixture.kitChoiceService().choose("Knight", "admin");
        fixture.openMainMenu();

        assertEquals(mainMenuWithTheTile.replace("""
                23 | CHEST | Choose the kit for the next match | Match round1-match1: Alpha against Bravo / \
                The popup closes when the time is over or a kit is chosen
                """, ""), fixture.describeLastMenu());
        assertFalse(fixture.describeLastMenu().contains("23 |"));
    }

    private void beginTheChoice() {
        fixture.kitChoiceService().begin("round1-match1", "Alpha", "Bravo", () -> { });
    }
}

package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainAdminMenuSnapshotTest {

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheMainMenuWithoutACalculatedArenaAndWithoutTeams() {
        fixture.openMainMenu();

        assertEquals("""
                Tournament administration | 27
                4 | BOOK | Tournament | Tournament state: IDLE / Registered teams: 0 / Arena: incomplete / \
                Running match: none
                10 | GRASS_BLOCK | Arena | Fight area, spawns, cleaning and reset / Arena incomplete
                11 | PLAYER_HEAD | Teams | Registered teams: 0 / Shows every team, adds one and removes one
                12 | DIAMOND_SWORD | Match and tournament | Start, pause, cancel and force a result / \
                Running match: none
                13 | BRUSH | Mark border blocks by clicking them | Turns every right click of yours into a border mark \
                / A click marks a block, a click on a marked block / takes the mark away again / \
                No border item is needed: stairs, fences and every / \
                other block that already stands there can be marked / \
                Chests, doors and everything else stay shut while it is on / \
                The menu closes so you can click the blocks / The mode is off, this tile switches it on
                14 | GRAY_DYE | Start the final | No tournament is in progress
                15 | PAPER | Tournament status | Writes the state, the rounds and the running match into the chat
                16 | WRITTEN_BOOK | Bracket | Writes every round and every match into the chat / \
                A bracket is a tree, the chat shows it better than a grid / The menu closes so you can read it
                18 | OBSERVER | Dry run of the pipeline | Walks the whole pipeline once, without an event / \
                Registers two test teams, builds their bracket, / decides a simulated match and removes them again / \
                Checks the arena and its spawns, the kit, the / countdown of the start gate and whether the / \
                cleaning after a match would start / Nothing is started, written or cleaned: / \
                no block, no file and no fighter is touched / A real test player stays necessary for the kit / \
                and the teleport really arriving / Refused while a tournament or a match runs / \
                The menu closes so you can read the answer
                20 | COMPARATOR | Reload config.yml | Reads config.yml again and applies it live / \
                Round timeout, cleaning batch sizes, cell limit, / safety zone and border particles / \
                The arena, the teams and a running match stay untouched / The menu closes so you can read the answer
                22 | BARRIER | Close | Closes the menu
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 0 / Default kit: none / \
                The items every fighter is given when a round starts / Create, replace and delete kits
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMainMenuWithAReadyArenaAndTeamsBeforeTheTournamentAndOffersTheDryRun() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");

        fixture.openMainMenu();

        assertEquals("""
                Tournament administration | 27
                4 | BOOK | Tournament | Tournament state: IDLE / Registered teams: 2 / Arena: ready / \
                Running match: none
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
                14 | GRAY_DYE | Start the final | No tournament is in progress
                15 | PAPER | Tournament status | Writes the state, the rounds and the running match into the chat
                16 | WRITTEN_BOOK | Bracket | Writes every round and every match into the chat / \
                A bracket is a tree, the chat shows it better than a grid / The menu closes so you can read it
                18 | OBSERVER | Dry run of the pipeline | Walks the whole pipeline once, without an event / \
                Registers two test teams, builds their bracket, / decides a simulated match and removes them again / \
                Checks the arena and its spawns, the kit, the / countdown of the start gate and whether the / \
                cleaning after a match would start / Nothing is started, written or cleaned: / \
                no block, no file and no fighter is touched / A real test player stays necessary for the kit / \
                and the teleport really arriving / Refused while a tournament or a match runs / \
                The menu closes so you can read the answer
                20 | COMPARATOR | Reload config.yml | Reads config.yml again and applies it live / \
                Round timeout, cleaning batch sizes, cell limit, / safety zone and border particles / \
                The arena, the teams and a running match stay untouched / The menu closes so you can read the answer
                22 | BARRIER | Close | Closes the menu
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 0 / Default kit: none / \
                The items every fighter is given when a round starts / Create, replace and delete kits
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMainMenuWhileTheTournamentRunsAMatchAndRefusesTheDryRun() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();

        fixture.openMainMenu();

        assertEquals("""
                Tournament administration | 27
                4 | BOOK | Tournament | Tournament state: RUNNING | rounds: 2 | running match: round1-match1 | final: \
                PENDING / Registered teams: 4 / Arena: ready / Running match: round1-match1 (%1$s 0 : 0 %2$s)
                10 | GRASS_BLOCK | Arena | Fight area, spawns, cleaning and reset / Arena ready
                11 | PLAYER_HEAD | Teams | Registered teams: 4 / Shows every team, adds one and removes one
                12 | DIAMOND_SWORD | Match and tournament | Start, pause, cancel and force a result / \
                Running match: round1-match1 (%1$s 0 : 0 %2$s)
                13 | BRUSH | Mark border blocks by clicking them | Turns every right click of yours into a border mark \
                / A click marks a block, a click on a marked block / takes the mark away again / \
                No border item is needed: stairs, fences and every / \
                other block that already stands there can be marked / \
                Chests, doors and everything else stay shut while it is on / \
                The menu closes so you can click the blocks / The mode is off, this tile switches it on
                14 | GRAY_DYE | Start the final | The final is not ready, its finalists are not decided yet
                15 | PAPER | Tournament status | Writes the state, the rounds and the running match into the chat
                16 | WRITTEN_BOOK | Bracket | Writes every round and every match into the chat / \
                A bracket is a tree, the chat shows it better than a grid / The menu closes so you can read it
                18 | GRAY_DYE | Dry run of the pipeline | A tournament is in progress, the dry run is a check for \
                before an event / It registers two test teams and would put them into the registry of a running \
                bracket / Wait until the event is over or cancel it in the match menu of /tournament
                20 | COMPARATOR | Reload config.yml | Reads config.yml again and applies it live / \
                Round timeout, cleaning batch sizes, cell limit, / safety zone and border particles / \
                The arena, the teams and a running match stay untouched / \
                A match is running: the new round timeout / arrives with the next round / \
                The menu closes so you can read the answer
                22 | BARRIER | Close | Closes the menu
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 0 / Default kit: none / \
                The items every fighter is given when a round starts / Create, replace and delete kits
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName()),
                fixture.describeLastMenu());
    }

    @Test
    void showsTheMainMenuWhileTheTournamentIsPaused() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.pauseTournament();

        fixture.openMainMenu();

        assertEquals("""
                Tournament administration | 27
                4 | BOOK | Tournament | Tournament state: PAUSED | rounds: 2 | running match: round1-match1 | final: \
                PENDING / Registered teams: 4 / Arena: ready / Running match: round1-match1 (%1$s 0 : 0 %2$s)
                10 | GRASS_BLOCK | Arena | Fight area, spawns, cleaning and reset / Arena ready
                11 | PLAYER_HEAD | Teams | Registered teams: 4 / Shows every team, adds one and removes one
                12 | DIAMOND_SWORD | Match and tournament | Start, pause, cancel and force a result / \
                Running match: round1-match1 (%1$s 0 : 0 %2$s)
                13 | BRUSH | Mark border blocks by clicking them | Turns every right click of yours into a border mark \
                / A click marks a block, a click on a marked block / takes the mark away again / \
                No border item is needed: stairs, fences and every / \
                other block that already stands there can be marked / \
                Chests, doors and everything else stay shut while it is on / \
                The menu closes so you can click the blocks / The mode is off, this tile switches it on
                14 | GRAY_DYE | Start the final | The final is not ready, its finalists are not decided yet
                15 | PAPER | Tournament status | Writes the state, the rounds and the running match into the chat
                16 | WRITTEN_BOOK | Bracket | Writes every round and every match into the chat / \
                A bracket is a tree, the chat shows it better than a grid / The menu closes so you can read it
                18 | GRAY_DYE | Dry run of the pipeline | A tournament is in progress, the dry run is a check for \
                before an event / It registers two test teams and would put them into the registry of a running \
                bracket / Wait until the event is over or cancel it in the match menu of /tournament
                20 | COMPARATOR | Reload config.yml | Reads config.yml again and applies it live / \
                Round timeout, cleaning batch sizes, cell limit, / safety zone and border particles / \
                The arena, the teams and a running match stay untouched / \
                A match is running: the new round timeout / arrives with the next round / \
                The menu closes so you can read the answer
                22 | BARRIER | Close | Closes the menu
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 0 / Default kit: none / \
                The items every fighter is given when a round starts / Create, replace and delete kits
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName()),
                fixture.describeLastMenu());
    }

    @Test
    void showsTheMainMenuWhenTheFinalIsReady() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();

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
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 0 / Default kit: none / \
                The items every fighter is given when a round starts / Create, replace and delete kits
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMainMenuWhileTheFinalIsRunning() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.startFinal();

        fixture.openMainMenu();

        assertEquals("""
                Tournament administration | 27
                4 | BOOK | Tournament | Tournament state: RUNNING | rounds: 1 | running match: round1-match1 | final: \
                RUNNING / Registered teams: 2 / Arena: ready / Running match: round1-match1 (%1$s 0 : 0 %2$s)
                10 | GRASS_BLOCK | Arena | Fight area, spawns, cleaning and reset / Arena ready
                11 | PLAYER_HEAD | Teams | Registered teams: 2 / Shows every team, adds one and removes one
                12 | DIAMOND_SWORD | Match and tournament | Start, pause, cancel and force a result / \
                Running match: round1-match1 (%1$s 0 : 0 %2$s)
                13 | BRUSH | Mark border blocks by clicking them | Turns every right click of yours into a border mark \
                / A click marks a block, a click on a marked block / takes the mark away again / \
                No border item is needed: stairs, fences and every / \
                other block that already stands there can be marked / \
                Chests, doors and everything else stay shut while it is on / \
                The menu closes so you can click the blocks / The mode is off, this tile switches it on
                14 | GRAY_DYE | Start the final | The final is running
                15 | PAPER | Tournament status | Writes the state, the rounds and the running match into the chat
                16 | WRITTEN_BOOK | Bracket | Writes every round and every match into the chat / \
                A bracket is a tree, the chat shows it better than a grid / The menu closes so you can read it
                18 | GRAY_DYE | Dry run of the pipeline | A tournament is in progress, the dry run is a check for \
                before an event / It registers two test teams and would put them into the registry of a running \
                bracket / Wait until the event is over or cancel it in the match menu of /tournament
                20 | COMPARATOR | Reload config.yml | Reads config.yml again and applies it live / \
                Round timeout, cleaning batch sizes, cell limit, / safety zone and border particles / \
                The arena, the teams and a running match stay untouched / \
                A match is running: the new round timeout / arrives with the next round / \
                The menu closes so you can read the answer
                22 | BARRIER | Close | Closes the menu
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 0 / Default kit: none / \
                The items every fighter is given when a round starts / Create, replace and delete kits
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName()),
                fixture.describeLastMenu());
    }

    @Test
    void showsTheMainMenuAfterTheFinalIsDecided() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.startFinal();
        fixture.decideRunningMatchFor("Alpha");

        fixture.openMainMenu();

        assertEquals("""
                Tournament administration | 27
                4 | BOOK | Tournament | Tournament state: FINISHED | rounds: 1 | running match: none | final: FINISHED \
                | champion: Alpha / Registered teams: 2 / Arena: ready / Running match: none
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
                14 | GRAY_DYE | Start the final | The final is over
                15 | PAPER | Tournament status | Writes the state, the rounds and the running match into the chat
                16 | WRITTEN_BOOK | Bracket | Writes every round and every match into the chat / \
                A bracket is a tree, the chat shows it better than a grid / The menu closes so you can read it
                18 | OBSERVER | Dry run of the pipeline | Walks the whole pipeline once, without an event / \
                Registers two test teams, builds their bracket, / decides a simulated match and removes them again / \
                Checks the arena and its spawns, the kit, the / countdown of the start gate and whether the / \
                cleaning after a match would start / Nothing is started, written or cleaned: / \
                no block, no file and no fighter is touched / A real test player stays necessary for the kit / \
                and the teleport really arriving / Refused while a tournament or a match runs / \
                The menu closes so you can read the answer
                20 | COMPARATOR | Reload config.yml | Reads config.yml again and applies it live / \
                Round timeout, cleaning batch sizes, cell limit, / safety zone and border particles / \
                The arena, the teams and a running match stay untouched / The menu closes so you can read the answer
                22 | BARRIER | Close | Closes the menu
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 0 / Default kit: none / \
                The items every fighter is given when a round starts / Create, replace and delete kits
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMainMenuWithTheBorderSelectorModeSwitchedOn() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.switchBorderSelectorModeOn();

        fixture.openMainMenu();

        assertEquals("""
                Tournament administration | 27
                4 | BOOK | Tournament | Tournament state: IDLE / Registered teams: 2 / Arena: ready / \
                Running match: none
                10 | GRASS_BLOCK | Arena | Fight area, spawns, cleaning and reset / Arena ready
                11 | PLAYER_HEAD | Teams | Registered teams: 2 / Shows every team, adds one and removes one
                12 | DIAMOND_SWORD | Match and tournament | Start, pause, cancel and force a result / \
                Running match: none
                13 | BRUSH | Mark border blocks by clicking them | Turns every right click of yours into a border mark \
                / A click marks a block, a click on a marked block / takes the mark away again / \
                No border item is needed: stairs, fences and every / \
                other block that already stands there can be marked / \
                Chests, doors and everything else stay shut while it is on / \
                The menu closes so you can click the blocks / The mode is on, this tile switches it off
                14 | GRAY_DYE | Start the final | No tournament is in progress
                15 | PAPER | Tournament status | Writes the state, the rounds and the running match into the chat
                16 | WRITTEN_BOOK | Bracket | Writes every round and every match into the chat / \
                A bracket is a tree, the chat shows it better than a grid / The menu closes so you can read it
                18 | OBSERVER | Dry run of the pipeline | Walks the whole pipeline once, without an event / \
                Registers two test teams, builds their bracket, / decides a simulated match and removes them again / \
                Checks the arena and its spawns, the kit, the / countdown of the start gate and whether the / \
                cleaning after a match would start / Nothing is started, written or cleaned: / \
                no block, no file and no fighter is touched / A real test player stays necessary for the kit / \
                and the teleport really arriving / Refused while a tournament or a match runs / \
                The menu closes so you can read the answer
                20 | COMPARATOR | Reload config.yml | Reads config.yml again and applies it live / \
                Round timeout, cleaning batch sizes, cell limit, / safety zone and border particles / \
                The arena, the teams and a running match stay untouched / The menu closes so you can read the answer
                22 | BARRIER | Close | Closes the menu
                24 | DIAMOND_CHESTPLATE | Kits | Kits: 0 / Default kit: none / \
                The items every fighter is given when a round starts / Create, replace and delete kits
                """, fixture.describeLastMenu());
    }

    @Test
    void namesTheNumberOfKitsAndTheDefaultKitOnTheKitTile() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");

        fixture.openMainMenu();

        MenuTile kitTile = fixture.lastMenu().findTile(24).orElseThrow();
        assertEquals("Kits", kitTile.displayName());
        assertEquals(List.of("Kits: 2", "Default kit: Archer", "The items every fighter is given when a round starts",
                "Create, replace and delete kits"), kitTile.loreLines());
    }
}

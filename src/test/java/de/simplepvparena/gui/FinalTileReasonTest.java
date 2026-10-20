package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalTileReasonTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MAIN_FINAL_SLOT = 14;
    private static final int MATCH_FINAL_SLOT = 15;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    @UseLanguage("en")
    void namesTheRealReasonWhyTheFinalCannotBeStartedInEnglish() {
        assertEquals(List.of(
                "before the tournament | GRAY_DYE | No tournament is in progress",
                "first round running | GRAY_DYE | The final is not ready, its finalists are not decided yet",
                "first round paused | GRAY_DYE | The final is not ready, its finalists are not decided yet",
                "finalists decided | NETHER_STAR | Both finalists are known / Starts the final on the arena",
                "finalists decided, paused | GRAY_DYE | The final is ready, continue the tournament to start it",
                "final running | GRAY_DYE | The final is running",
                "final decided | GRAY_DYE | The final is over",
                "next tournament running | GRAY_DYE | The final is not ready, its finalists are not decided yet",
                "next tournament cancelled | GRAY_DYE | No tournament is in progress"),
                walkThroughTwoTournaments());
    }

    @Test
    @UseLanguage("de")
    void namesTheRealReasonWhyTheFinalCannotBeStartedInGerman() {
        assertEquals(List.of(
                "before the tournament | GRAY_DYE | Es läuft kein Turnier",
                "first round running | GRAY_DYE | Das Finale ist nicht bereit, seine Finalisten stehen noch nicht "
                        + "fest",
                "first round paused | GRAY_DYE | Das Finale ist nicht bereit, seine Finalisten stehen noch nicht fest",
                "finalists decided | NETHER_STAR | Beide Finalisten stehen fest / Startet das Finale in der Arena",
                "finalists decided, paused | GRAY_DYE | Das Finale ist bereit, setze das Turnier fort, um es zu "
                        + "starten",
                "final running | GRAY_DYE | Das Finale läuft",
                "final decided | GRAY_DYE | Das Finale ist vorbei",
                "next tournament running | GRAY_DYE | Das Finale ist nicht bereit, seine Finalisten stehen noch "
                        + "nicht fest",
                "next tournament cancelled | GRAY_DYE | Es läuft kein Turnier"),
                walkThroughTwoTournaments());
    }

    private List<String> walkThroughTwoTournaments() {
        List<String> finalTiles = new ArrayList<>();
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        finalTiles.add(describeFinalTile("before the tournament"));
        fixture.startTournament();
        finalTiles.add(describeFinalTile("first round running"));
        fixture.pauseTournament();
        finalTiles.add(describeFinalTile("first round paused"));
        fixture.pauseTournament();
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        finalTiles.add(describeFinalTile("finalists decided"));
        fixture.pauseTournament();
        finalTiles.add(describeFinalTile("finalists decided, paused"));
        fixture.pauseTournament();
        fixture.startFinal();
        finalTiles.add(describeFinalTile("final running"));
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        finalTiles.add(describeFinalTile("final decided"));
        fixture.startTournament();
        finalTiles.add(describeFinalTile("next tournament running"));
        assertTrue(fixture.tournamentManager().cancelTournament().successful());
        finalTiles.add(describeFinalTile("next tournament cancelled"));
        return finalTiles;
    }

    private String describeFinalTile(String moment) {
        fixture.openMainMenu();
        String mainMenuTile = describe(fixture.lastMenu().findTile(MAIN_FINAL_SLOT).orElseThrow());
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        String matchMenuTile = describe(fixture.lastMenu().findTile(MATCH_FINAL_SLOT).orElseThrow());
        assertEquals(mainMenuTile, matchMenuTile, "both menus show the same tile " + moment);
        return moment + " | " + mainMenuTile;
    }

    private static String describe(MenuTile tile) {
        return tile.material().name() + " | " + String.join(" / ", tile.loreLines());
    }
}

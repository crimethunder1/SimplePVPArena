package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketViewAccessTest {

    private static final int BACK_SLOT = 48;
    private static final int NEXT_ROUND_SLOT = 53;
    private static final int LARGEST_MENU_SIZE = 54;
    private static final String NOT_ALLOWED = "You are not allowed to use the tournament menu";

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final AdminMenuFixture.OnlinePlayer viewer = fixture.letAnotherPlayerJoin("Viewer", false, true);
    private final AdminMenuFixture.OnlinePlayer visitor = fixture.letAnotherPlayerJoin("Visitor", false, false);

    @Test
    void makesTheBracketViewTheOnlyMenuAPlayerCanRead() {
        Set<AdminMenuType> readableMenuTypes = Arrays.stream(AdminMenuType.values())
                .filter(AdminMenuType::isReadableByPlayers)
                .collect(Collectors.toSet());

        assertEquals(Set.of(AdminMenuType.BRACKET_VIEW), readableMenuTypes);
    }

    @Test
    void letsAPlayerWithTheViewPermissionTurnTheRoundsOfTheBracketView() {
        startATournamentOfEightTeams();
        fixture.openBracketViewFor(viewer.player());

        fixture.clickAs(viewer.player(), NEXT_ROUND_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.findLastMenuOf(viewer.player()).orElseThrow();
        assertEquals(AdminMenuType.BRACKET_VIEW, menu.holder().getMenuType());
        assertEquals(1, menu.holder().getPageIndex());
        assertFalse(fixture.isMenuClosedFor(viewer.player()));
        assertEquals(List.of(), viewer.messages());
    }

    @Test
    void refusesAPlayerWithTheViewPermissionInEveryOtherMenuEvenWithAChangedHolder() {
        startATournamentOfEightTeams();
        fixture.openBracketViewFor(viewer.player());
        AdminMenuHolder bracketView = fixture.findLastMenuOf(viewer.player()).orElseThrow().holder();
        int clickCount = 0;

        for (AdminMenuType menuType : EnumSet.complementOf(EnumSet.of(AdminMenuType.BRACKET_VIEW))) {
            for (int slot = 0; slot < LARGEST_MENU_SIZE; slot++) {
                fixture.clickInAs(viewer.player(), holderOf(menuType), slot);
                clickCount++;
            }
        }

        assertSame(bracketView, fixture.findLastMenuOf(viewer.player()).orElseThrow().holder());
        assertTrue(fixture.isMenuClosedFor(viewer.player()));
        assertEquals(Collections.nCopies(clickCount, NOT_ALLOWED), viewer.messages());
        assertTrue(fixture.tournamentManager().isTournamentInProgress());
        assertEquals(8, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void neitherShowsNorFollowsTheBackTileForAPlayerWithoutTheAdminPermission() {
        startATournamentOfEightTeams();
        fixture.clickInAs(viewer.player(), AdminMenuHolder.bracketView(0, true), NEXT_ROUND_SLOT);
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.findLastMenuOf(viewer.player()).orElseThrow();

        fixture.clickAs(viewer.player(), BACK_SLOT);

        assertTrue(menu.holder().isOpenedFromMainMenu());
        assertFalse(menu.findTile(BACK_SLOT).isPresent());
        assertSame(menu, fixture.findLastMenuOf(viewer.player()).orElseThrow());
        assertEquals(List.of(), viewer.messages());
    }

    @Test
    void refusesAPlayerWithoutTheViewPermissionInTheBracketView() {
        startATournamentOfEightTeams();

        fixture.clickInAs(visitor.player(), AdminMenuHolder.bracketView(0, false), NEXT_ROUND_SLOT);

        assertTrue(fixture.findLastMenuOf(visitor.player()).isEmpty());
        assertTrue(fixture.isMenuClosedFor(visitor.player()));
        assertEquals(List.of(NOT_ALLOWED), visitor.messages());
    }

    @Test
    void refusesAPlayerWithoutTheViewPermissionInEveryOtherMenu() {
        for (AdminMenuType menuType : EnumSet.complementOf(EnumSet.of(AdminMenuType.BRACKET_VIEW))) {
            fixture.clickInAs(visitor.player(), holderOf(menuType), 0);
        }

        assertTrue(fixture.findLastMenuOf(visitor.player()).isEmpty());
        assertEquals(Collections.nCopies(AdminMenuType.values().length - 1, NOT_ALLOWED), visitor.messages());
    }

    @Test
    void letsAnAdminWithoutTheViewPermissionUseTheBracketView() {
        startATournamentOfEightTeams();
        fixture.openBracketView();

        fixture.click(NEXT_ROUND_SLOT);

        assertEquals(AdminMenuType.BRACKET_VIEW, fixture.lastMenu().holder().getMenuType());
        assertEquals(1, fixture.lastMenu().holder().getPageIndex());
        assertFalse(fixture.isMenuClosed());
        assertEquals(List.of(), fixture.messages());
    }

    @Test
    void refusesTheAdminInTheBracketViewOnceTheAdminPermissionIsGone() {
        startATournamentOfEightTeams();
        fixture.openBracketView();
        fixture.revokeAdminPermission();

        fixture.click(NEXT_ROUND_SLOT);

        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
        assertTrue(fixture.isMenuClosed());
        assertEquals(List.of(NOT_ALLOWED), fixture.messages());
    }

    @Test
    void keepsAnAdminWhoAlsoHoldsTheViewPermissionAnAdminInEveryMenu() {
        AdminMenuFixture.OnlinePlayer moderator = fixture.letAnotherPlayerJoin("Moderator", true, true);

        fixture.clickInAs(moderator.player(), AdminMenuHolder.main(), 12);

        assertEquals(AdminMenuType.MATCH, fixture.findLastMenuOf(moderator.player()).orElseThrow().holder()
                .getMenuType());
        assertEquals(List.of(), moderator.messages());
    }

    private void startATournamentOfEightTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        fixture.startTournament();
    }

    private static AdminMenuHolder holderOf(AdminMenuType menuType) {
        return switch (menuType) {
            case MAIN -> AdminMenuHolder.main();
            case ARENA -> AdminMenuHolder.arena();
            case KIT -> AdminMenuHolder.kit(0);
            case KIT_ACTIONS -> AdminMenuHolder.kitActions("Archer", 0);
            case KIT_CHOICE -> AdminMenuHolder.kitChoice("round1-match1", 0);
            case TEAMS -> AdminMenuHolder.teams(0);
            case TEAM_ACTIONS -> AdminMenuHolder.teamActions("Alpha", 0);
            case TEAM_MEMBERS -> AdminMenuHolder.teamMembers("Alpha", 0);
            case MATCH -> AdminMenuHolder.match();
            case FORCE_RESULT -> AdminMenuHolder.forceResult("round1-match1");
            case WAITING_MATCHES -> AdminMenuHolder.waitingMatches(0);
            case WAITING_MATCH_RESULT -> AdminMenuHolder.waitingMatchResult("round1-match1", 0);
            case CONFIRMATION -> AdminMenuHolder.confirmation(ConfirmationRequest.REMOVE_TEAM, "Alpha", 0);
            case BRACKET_VIEW -> AdminMenuHolder.bracketView(0, false);
        };
    }
}

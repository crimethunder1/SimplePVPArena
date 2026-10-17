package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminMenuHolderTest {

    @Test
    void namesTheMenuEveryHolderStandsFor() {
        assertEquals(AdminMenuType.MAIN, AdminMenuHolder.main().getMenuType());
        assertEquals(AdminMenuType.ARENA, AdminMenuHolder.arena().getMenuType());
        assertEquals(AdminMenuType.KIT, AdminMenuHolder.kit().getMenuType());
        assertEquals(AdminMenuType.TEAMS, AdminMenuHolder.teams(0).getMenuType());
        assertEquals(AdminMenuType.TEAM_ACTIONS, AdminMenuHolder.teamActions("Alpha", 0).getMenuType());
        assertEquals(AdminMenuType.MATCH, AdminMenuHolder.match().getMenuType());
        assertEquals(AdminMenuType.FORCE_RESULT, AdminMenuHolder.forceResult("round1-match1").getMenuType());
        assertEquals(AdminMenuType.CONFIRMATION,
                AdminMenuHolder.confirmation(ConfirmationRequest.CLEAN_ARENA, null, 0).getMenuType());
    }

    @Test
    void carriesTheTeamAMenuWasOpenedFor() {
        AdminMenuHolder holder = AdminMenuHolder.teamActions("Alpha", 2);

        assertEquals("Alpha", holder.getSubject().orElseThrow());
        assertEquals(2, holder.getPageIndex());
    }

    @Test
    void carriesTheMatchAForceResultMenuWasOpenedFor() {
        assertEquals("round1-match1", AdminMenuHolder.forceResult("round1-match1").getSubject().orElseThrow());
    }

    @Test
    void namesTheWaitingMatchMenus() {
        assertEquals(AdminMenuType.WAITING_MATCHES, AdminMenuHolder.waitingMatches(0).getMenuType());
        assertEquals(AdminMenuType.WAITING_MATCH_RESULT,
                AdminMenuHolder.waitingMatchResult("round1-match2", 0).getMenuType());
    }

    @Test
    void carriesThePageOfTheWaitingMatchesWithoutASubject() {
        AdminMenuHolder holder = AdminMenuHolder.waitingMatches(2);

        assertEquals(2, holder.getPageIndex());
        assertTrue(holder.getSubject().isEmpty());
        assertTrue(holder.getConfirmationRequest().isEmpty());
        assertEquals(0, AdminMenuHolder.waitingMatches(-1).getPageIndex());
    }

    @Test
    void carriesTheMatchAndThePageAWaitingMatchResultWasOpenedFrom() {
        AdminMenuHolder holder = AdminMenuHolder.waitingMatchResult("round1-match2", 1);

        assertEquals("round1-match2", holder.getSubject().orElseThrow());
        assertEquals(1, holder.getPageIndex());
        assertTrue(holder.getConfirmationRequest().isEmpty());
        assertThrows(NullPointerException.class, () -> AdminMenuHolder.waitingMatchResult(null, 0));
    }

    @Test
    void carriesNoSubjectForAMenuThatStandsForItself() {
        assertTrue(AdminMenuHolder.main().getSubject().isEmpty());
        assertTrue(AdminMenuHolder.arena().getSubject().isEmpty());
        assertTrue(AdminMenuHolder.teams(0).getSubject().isEmpty());
        assertTrue(AdminMenuHolder.match().getSubject().isEmpty());
    }

    @Test
    void carriesTheConfirmationRequestAndTheTeamListPageItWasStartedFrom() {
        AdminMenuHolder holder = AdminMenuHolder.confirmation(ConfirmationRequest.REMOVE_TEAM, "Alpha", 3);

        assertEquals(ConfirmationRequest.REMOVE_TEAM, holder.getConfirmationRequest().orElseThrow());
        assertEquals("Alpha", holder.getSubject().orElseThrow());
        assertEquals(3, holder.getPageIndex());
    }

    @Test
    void carriesNoConfirmationRequestForARegularMenu() {
        assertTrue(AdminMenuHolder.main().getConfirmationRequest().isEmpty());
        assertTrue(AdminMenuHolder.arena().getConfirmationRequest().isEmpty());
    }

    @Test
    void neverOpensOnANegativePage() {
        assertEquals(0, AdminMenuHolder.teams(-3).getPageIndex());
    }

    @Test
    void refusesToHandOutAnInventoryThatWasNotBuiltYet() {
        assertThrows(IllegalStateException.class, () -> AdminMenuHolder.main().getInventory());
    }
}

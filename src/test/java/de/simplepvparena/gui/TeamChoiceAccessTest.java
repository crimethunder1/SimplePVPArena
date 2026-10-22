package de.simplepvparena.gui;

import de.simplepvparena.registration.TeamMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamChoiceAccessTest {

    private static final int RED_BED_SLOT = 0;
    private static final String NOT_ALLOWED = "You are not allowed to use the tournament menu";
    private static final String REGISTER_FIRST = "Register first with the station or /tournament join";
    private static final String RANDOM_MODE = "The teams are formed at random, an admin forms them";
    private static final String CLOSED = "The team choice is closed while a tournament is in progress";

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final AdminMenuFixture.OnlinePlayer viewer = fixture.letAnotherPlayerJoin("Viewer", false, true);
    private final AdminMenuFixture.OnlinePlayer visitor = fixture.letAnotherPlayerJoin("Visitor", false, false);

    @Test
    void leavesExactlyThreeMenuTypesReachableForAPlayerWithoutTheAdminPermission() {
        Set<AdminMenuType> reachableMenuTypes = Arrays.stream(AdminMenuType.values())
                .filter(menuType -> menuType.isReadableByPlayers() || menuType.isReadableByVotingTeam()
                        || menuType.isReadableByRegisteredPlayers())
                .collect(Collectors.toSet());
        Set<AdminMenuType> menuTypesOfRegisteredPlayers = Arrays.stream(AdminMenuType.values())
                .filter(AdminMenuType::isReadableByRegisteredPlayers)
                .collect(Collectors.toSet());

        assertEquals(Set.of(AdminMenuType.BRACKET_VIEW, AdminMenuType.TEAM_CHOICE, AdminMenuType.UNEQUAL_TEAM_VOTE),
                reachableMenuTypes);
        assertEquals(Set.of(AdminMenuType.TEAM_CHOICE), menuTypesOfRegisteredPlayers);
    }

    @Test
    void letsARegisteredPlayerWithTheViewPermissionChooseATeam() {
        register(viewer);
        fixture.openTeamChoiceFor(viewer.player());

        fixture.clickAs(viewer.player(), RED_BED_SLOT);

        assertEquals("Red", fixture.teamRegistry().findTeamByPlayer(viewer.player().getUniqueId()).orElseThrow()
                .getTeamName());
        assertFalse(fixture.isMenuClosedFor(viewer.player()));
        assertEquals(List.of("You joined team Red"), viewer.messages());
    }

    @Test
    void keepsARegisteredPlayerOutOfEveryMenuButTheBracketViewAndTheTeamChoice() {
        register(viewer);
        fixture.openTeamChoiceFor(viewer.player());
        AdminMenuHolder teamChoice = fixture.findLastMenuOf(viewer.player()).orElseThrow().holder();
        EnumSet<AdminMenuType> refusedMenuTypes = EnumSet.complementOf(EnumSet.of(AdminMenuType.BRACKET_VIEW,
                AdminMenuType.TEAM_CHOICE));

        for (AdminMenuType menuType : refusedMenuTypes) {
            fixture.clickInAs(viewer.player(), holderOf(menuType), 0);
        }
        fixture.clickInAs(viewer.player(), AdminMenuHolder.bracketView(0, false), 0);

        assertEquals(Collections.nCopies(refusedMenuTypes.size(), NOT_ALLOWED), viewer.messages());
        assertEquals(teamChoice, fixture.findLastMenuOf(viewer.player()).orElseThrow().holder());
        assertEquals(0, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void closesTheTeamChoiceForAPlayerWhoIsNotRegistered() {
        fixture.clickInAs(viewer.player(), AdminMenuHolder.teamChoice(), RED_BED_SLOT);

        assertEquals(List.of(REGISTER_FIRST), viewer.messages());
        assertTrue(fixture.isMenuClosedFor(viewer.player()));
        assertEquals(0, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void closesTheTeamChoiceOnceThePlayerIsNoLongerRegistered() {
        register(viewer);
        fixture.openTeamChoiceFor(viewer.player());
        fixture.registrationService().removeRegistration(viewer.player().getUniqueId());

        fixture.clickAs(viewer.player(), RED_BED_SLOT);

        assertEquals(List.of(REGISTER_FIRST), viewer.messages());
        assertTrue(fixture.isMenuClosedFor(viewer.player()));
        assertEquals(0, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void closesTheTeamChoiceWhenTheTeamsAreFormedAtRandom() {
        register(viewer);
        fixture.openTeamChoiceFor(viewer.player());
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);

        fixture.clickAs(viewer.player(), RED_BED_SLOT);

        assertEquals(List.of(RANDOM_MODE), viewer.messages());
        assertTrue(fixture.isMenuClosedFor(viewer.player()));
        assertEquals(0, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void closesTheTeamChoiceWhileATournamentIsInProgress() {
        register(viewer);
        fixture.openTeamChoiceFor(viewer.player());
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();

        fixture.clickAs(viewer.player(), RED_BED_SLOT);

        assertEquals(List.of(CLOSED), viewer.messages());
        assertTrue(fixture.isMenuClosedFor(viewer.player()));
        assertEquals(List.of("Alpha", "Bravo"), fixture.teamRegistry().getTeamNames());
    }

    @Test
    void refusesARegisteredPlayerWithoutTheViewPermission() {
        register(visitor);

        fixture.clickInAs(visitor.player(), AdminMenuHolder.teamChoice(), RED_BED_SLOT);

        assertEquals(List.of(NOT_ALLOWED), visitor.messages());
        assertTrue(fixture.isMenuClosedFor(visitor.player()));
        assertEquals(0, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void refusesAnAdminWhoIsNotRegistered() {
        fixture.clickIn(AdminMenuHolder.teamChoice(), RED_BED_SLOT);

        assertEquals(List.of(REGISTER_FIRST), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertEquals(0, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void letsAnAdminWhoIsRegisteredChooseATeam() {
        fixture.registrationList().register(fixture.admin().getUniqueId(), "admin");
        fixture.openTeamChoiceFor(fixture.admin());

        fixture.click(RED_BED_SLOT);

        assertTrue(fixture.teamRegistry().findTeam("Red").orElseThrow()
                .containsPlayer(fixture.admin().getUniqueId()));
        assertEquals(List.of("You joined team Red"), fixture.messages());
    }

    @Test
    void opensTheTeamChoiceOnlyForThoseWhoMayUseIt() {
        fixture.openTeamChoiceFor(viewer.player());
        register(viewer);
        fixture.teamModeSetting().setTeamMode(TeamMode.RANDOM);
        fixture.openTeamChoiceFor(viewer.player());

        assertTrue(fixture.findLastMenuOf(viewer.player()).isEmpty());
        assertEquals(List.of(REGISTER_FIRST, RANDOM_MODE), viewer.messages());
    }

    private void register(AdminMenuFixture.OnlinePlayer player) {
        fixture.registrationList().register(player.player().getUniqueId(), player.player().getName());
    }

    private static AdminMenuHolder holderOf(AdminMenuType menuType) {
        return switch (menuType) {
            case MAIN -> AdminMenuHolder.main();
            case ARENA -> AdminMenuHolder.arena();
            case KIT -> AdminMenuHolder.kit(0);
            case KIT_ACTIONS -> AdminMenuHolder.kitActions("Archer", 0);
            case KIT_CHOICE -> AdminMenuHolder.kitChoice("M1", 0);
            case TEAMS -> AdminMenuHolder.teams(0);
            case TEAM_ACTIONS -> AdminMenuHolder.teamActions("Alpha", 0);
            case TEAM_MEMBERS -> AdminMenuHolder.teamMembers("Alpha", 0);
            case REGISTRATIONS -> AdminMenuHolder.registrations(0);
            case MATCH -> AdminMenuHolder.match();
            case FORCE_RESULT -> AdminMenuHolder.forceResult("M1");
            case WAITING_MATCHES -> AdminMenuHolder.waitingMatches(0);
            case WAITING_MATCH_RESULT -> AdminMenuHolder.waitingMatchResult("M1", 0);
            case DECIDED_MATCHES -> AdminMenuHolder.decidedMatches(0);
            case CONFIRMATION -> AdminMenuHolder.confirmation(ConfirmationRequest.REMOVE_TEAM, "Alpha", 0);
            case BRACKET_VIEW -> AdminMenuHolder.bracketView(0, false);
            case UNEQUAL_TEAM_VOTE -> AdminMenuHolder.unequalTeamVote("M1");
            case TEAM_CHOICE -> AdminMenuHolder.teamChoice();
        };
    }
}

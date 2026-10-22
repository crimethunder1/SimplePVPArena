package de.simplepvparena.gui;

import de.simplepvparena.match.PendingUnequalTeamVote;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnequalTeamVoteAccessTest {

    private static final int FIRST_HEAD_SLOT = 9;
    private static final String NOT_ALLOWED = "You are not allowed to use the tournament menu";
    private static final AdminMenuHolder VOTE_OF_M1 = AdminMenuHolder.unequalTeamVote("M1");

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final AdminMenuFixture.OnlinePlayer viewerOfTheVotingTeam =
            fixture.letAnotherPlayerJoin("Anna", false, true);
    private final AdminMenuFixture.OnlinePlayer visitorOfTheVotingTeam =
            fixture.letAnotherPlayerJoin("Ben", false, false);
    private final AdminMenuFixture.OnlinePlayer adminOfTheVotingTeam =
            fixture.letAnotherPlayerJoin("Moderator", true, false);
    private final AdminMenuFixture.OnlinePlayer playerOfTheSmallerTeam =
            fixture.letAnotherPlayerJoin("Cleo", false, true);
    private final AdminMenuFixture.OnlinePlayer viewerOutsideOfBothTeams =
            fixture.letAnotherPlayerJoin("Viewer", false, true);

    @Test
    void makesTheVoteTheOnlyMenuTheVotingTeamCanUseAndKeepsTheBracketViewTheOnlyMenuEveryViewerCanRead() {
        Set<AdminMenuType> menuTypesOfTheVotingTeam = Arrays.stream(AdminMenuType.values())
                .filter(AdminMenuType::isReadableByVotingTeam)
                .collect(Collectors.toSet());
        Set<AdminMenuType> menuTypesOfEveryViewer = Arrays.stream(AdminMenuType.values())
                .filter(AdminMenuType::isReadableByPlayers)
                .collect(Collectors.toSet());

        assertEquals(Set.of(AdminMenuType.UNEQUAL_TEAM_VOTE), menuTypesOfTheVotingTeam);
        assertEquals(Set.of(AdminMenuType.BRACKET_VIEW), menuTypesOfEveryViewer);
    }

    @Test
    void letsEveryOnlinePlayerOfTheVotingTeamVoteWhateverPermissionThePlayerHas() {
        startTheVote();
        PendingUnequalTeamVote vote = fixture.unequalTeamVoteService().getPending().orElseThrow();

        for (AdminMenuFixture.OnlinePlayer voter : List.of(viewerOfTheVotingTeam, visitorOfTheVotingTeam,
                adminOfTheVotingTeam)) {
            fixture.clickInAs(voter.player(), VOTE_OF_M1, FIRST_HEAD_SLOT);
            assertTrue(vote.findVoteOf(voter.player().getUniqueId()).isPresent(), voter.player().getName());
            assertFalse(voter.messages().contains(NOT_ALLOWED));
        }
    }

    @Test
    void refusesThePlayersOfTheSmallerTeam() {
        startTheVote();

        fixture.clickInAs(playerOfTheSmallerTeam.player(), VOTE_OF_M1, FIRST_HEAD_SLOT);

        assertRefused(playerOfTheSmallerTeam);
    }

    @Test
    void refusesAPlayerWithTheViewPermissionOutsideOfBothTeams() {
        startTheVote();

        fixture.clickInAs(viewerOutsideOfBothTeams.player(), VOTE_OF_M1, FIRST_HEAD_SLOT);

        assertRefused(viewerOutsideOfBothTeams);
    }

    @Test
    void refusesAnAdminOutsideOfTheVotingTeam() {
        startTheVote();

        fixture.clickIn(VOTE_OF_M1, FIRST_HEAD_SLOT);

        assertEquals(List.of(NOT_ALLOWED), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertTrue(fixture.unequalTeamVoteService().getPending().orElseThrow()
                .findVoteOf(fixture.admin().getUniqueId()).isEmpty());
    }

    @Test
    void refusesAPlayerOfTheVotingTeamWhoLeftTheServerDuringTheVote() {
        startTheVote();

        fixture.letPlayerLeave(visitorOfTheVotingTeam);
        fixture.clickInAs(visitorOfTheVotingTeam.player(), VOTE_OF_M1, FIRST_HEAD_SLOT);

        assertTrue(visitorOfTheVotingTeam.messages().contains(NOT_ALLOWED));
        assertTrue(fixture.unequalTeamVoteService().getPending().orElseThrow()
                .findVoteOf(visitorOfTheVotingTeam.player().getUniqueId()).isEmpty());
    }

    @Test
    void refusesThePlayersOfTheVotingTeamOnceTheVoteIsOverOrBeforeItBegins() {
        fixture.clickInAs(visitorOfTheVotingTeam.player(), VOTE_OF_M1, FIRST_HEAD_SLOT);
        startTheVote();
        fixture.unequalTeamVoteService().cancel();

        fixture.clickInAs(viewerOfTheVotingTeam.player(), VOTE_OF_M1, FIRST_HEAD_SLOT);

        assertEquals(NOT_ALLOWED, visitorOfTheVotingTeam.messages().getFirst());
        assertTrue(viewerOfTheVotingTeam.messages().contains(NOT_ALLOWED));
    }

    @Test
    void keepsThePlayersOfTheVotingTeamOutOfEveryOtherMenuThatIsNotReadableByThem() {
        startTheVote();

        for (AdminMenuType menuType : EnumSet.complementOf(EnumSet.of(AdminMenuType.UNEQUAL_TEAM_VOTE,
                AdminMenuType.BRACKET_VIEW))) {
            fixture.clickInAs(visitorOfTheVotingTeam.player(), holderOf(menuType), 0);
        }
        fixture.clickInAs(visitorOfTheVotingTeam.player(), AdminMenuHolder.bracketView(0, false), 0);

        long refusals = visitorOfTheVotingTeam.messages().stream().filter(NOT_ALLOWED::equals).count();
        assertEquals(AdminMenuType.values().length - 1, refusals);
    }

    private void startTheVote() {
        fixture.registerTeamOfOnlinePlayers("Alpha", viewerOfTheVotingTeam, visitorOfTheVotingTeam,
                adminOfTheVotingTeam);
        fixture.registerTeamOfOnlinePlayers("Bravo", playerOfTheSmallerTeam);
        fixture.beginUnequalTeamVote("Alpha", "Bravo");
    }

    private void assertRefused(AdminMenuFixture.OnlinePlayer player) {
        assertTrue(player.messages().contains(NOT_ALLOWED), player.messages().toString());
        assertTrue(fixture.isMenuClosedFor(player.player()));
        PendingUnequalTeamVote vote = fixture.unequalTeamVoteService().getPending().orElseThrow();
        assertTrue(vote.findVoteOf(player.player().getUniqueId()).isEmpty());
        assertEquals(0, vote.countVotesFor(viewerOfTheVotingTeam.player().getUniqueId()));
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
            case UNEQUAL_TEAM_VOTE -> VOTE_OF_M1;
        };
    }
}

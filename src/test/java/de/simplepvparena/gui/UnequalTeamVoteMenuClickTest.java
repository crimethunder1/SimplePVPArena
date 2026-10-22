package de.simplepvparena.gui;

import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.match.PendingUnequalTeamVote;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnequalTeamVoteMenuClickTest {

    private static final int INFO_SLOT = 4;
    private static final int ANNA_SLOT = 9;
    private static final int BEN_SLOT = 10;
    private static final String[] VOTE = {"vote"};
    private static final String BEGIN_MESSAGE = "Your team has more players online than Bravo: vote within 30 "
            + "seconds who fights. /tournament vote opens the vote again";

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final TournamentCommand command = fixture.createTournamentCommand();
    private final AdminMenuFixture.OnlinePlayer anna = fixture.letAnotherPlayerJoin("Anna", false, true);
    private final AdminMenuFixture.OnlinePlayer ben = fixture.letAnotherPlayerJoin("Ben", false, true);
    private final AdminMenuFixture.OnlinePlayer cleo = fixture.letAnotherPlayerJoin("Cleo", false, true);

    @Test
    void opensTheMenuForEveryOnlinePlayerOfTheLargerTeamAndForNobodyElse() {
        startTwoAgainstOne();

        for (AdminMenuFixture.OnlinePlayer voter : List.of(anna, ben)) {
            assertEquals(AdminMenuType.UNEQUAL_TEAM_VOTE,
                    fixture.findLastMenuOf(voter.player()).orElseThrow().holder().getMenuType());
            assertEquals("M1", fixture.findLastMenuOf(voter.player()).orElseThrow().holder().getSubject()
                    .orElseThrow());
        }
        assertTrue(fixture.findLastMenuOf(cleo.player()).isEmpty());
        assertTrue(fixture.findLastMenuOf(fixture.admin()).isEmpty());
    }

    @Test
    void votesForThePlayerOfTheClickedHeadAndLetsTheVoterChangeTheVote() {
        startTwoAgainstOne();
        PendingUnequalTeamVote vote = fixture.unequalTeamVoteService().getPending().orElseThrow();

        fixture.clickAs(anna.player(), ANNA_SLOT);

        assertEquals(anna.player().getUniqueId(), vote.findVoteOf(anna.player().getUniqueId()).orElseThrow());
        assertEquals(1, vote.countVotesFor(anna.player().getUniqueId()));

        fixture.clickAs(anna.player(), BEN_SLOT);

        assertEquals(ben.player().getUniqueId(), vote.findVoteOf(anna.player().getUniqueId()).orElseThrow());
        assertEquals(0, vote.countVotesFor(anna.player().getUniqueId()));
        assertFalse(fixture.isMenuClosedFor(anna.player()));
        assertEquals(List.of(BEGIN_MESSAGE), anna.messages());
    }

    @Test
    void closesTheMenuOfEveryVoterOnceTheLastVoterHasVoted() {
        startTwoAgainstOne();

        fixture.clickAs(anna.player(), BEN_SLOT);
        fixture.clickAs(ben.player(), BEN_SLOT);

        assertFalse(fixture.unequalTeamVoteService().isPending());
        assertTrue(fixture.isMenuClosedFor(anna.player()));
        assertTrue(fixture.isMenuClosedFor(ben.player()));
    }

    @Test
    void closesTheMenusWhenTheVoteIsCancelled() {
        startTwoAgainstOne();

        fixture.unequalTeamVoteService().cancel();

        assertTrue(fixture.isMenuClosedFor(anna.player()));
        assertTrue(fixture.isMenuClosedFor(ben.player()));
    }

    @Test
    void ignoresAClickOnTheInfoTileAndOnEverySlotWithoutAHead() {
        startTwoAgainstOne();
        PendingUnequalTeamVote vote = fixture.unequalTeamVoteService().getPending().orElseThrow();

        for (int slot : new int[] {0, INFO_SLOT, 8, BEN_SLOT + 1, 17, 26}) {
            fixture.clickAs(anna.player(), slot);
        }

        assertTrue(vote.findVoteOf(anna.player().getUniqueId()).isEmpty());
        assertFalse(fixture.isMenuClosedFor(anna.player()));
        assertEquals(List.of(BEGIN_MESSAGE), anna.messages());
    }

    @Test
    void closesAMenuOfAnEarlierMatchAndTellsThatItsVoteIsOver() {
        startTwoAgainstOne();

        fixture.clickInAs(anna.player(), AdminMenuHolder.unequalTeamVote("M0"), BEN_SLOT);

        assertTrue(fixture.isMenuClosedFor(anna.player()));
        assertEquals("The vote for match M0 is over", anna.messages().getLast());
        assertTrue(fixture.unequalTeamVoteService().getPending().orElseThrow()
                .findVoteOf(anna.player().getUniqueId()).isEmpty());
    }

    @Test
    void reopensTheMenuForAVoterWithTheCommand() {
        startTwoAgainstOne();
        RecordingAdminMenuWindows.RecordedMenu firstMenu = fixture.findLastMenuOf(anna.player()).orElseThrow();

        command.onCommand(anna.player(), null, "tournament", VOTE);

        RecordingAdminMenuWindows.RecordedMenu reopenedMenu = fixture.findLastMenuOf(anna.player()).orElseThrow();
        assertNotSame(firstMenu, reopenedMenu);
        assertEquals(AdminMenuType.UNEQUAL_TEAM_VOTE, reopenedMenu.holder().getMenuType());

        fixture.clickAs(ben.player(), ANNA_SLOT);

        assertEquals("9 | PLAYER_HEAD | Anna | 1 vote / Click to vote for this player",
                fixture.describeLastMenuOf(anna.player()).lines().skip(2).findFirst().orElseThrow(),
                "the reopened menu is refreshed as well");
    }

    @Test
    void acceptsTheArgumentInCapitalLetters() {
        startTwoAgainstOne();
        command.onCommand(anna.player(), null, "tournament", new String[] {"VOTE"});

        assertEquals(List.of(BEGIN_MESSAGE), anna.messages());
        assertEquals(AdminMenuType.UNEQUAL_TEAM_VOTE,
                fixture.findLastMenuOf(anna.player()).orElseThrow().holder().getMenuType());
    }

    @Test
    void refusesTheCommandForAPlayerOutsideOfTheVotingTeamAndWithoutAVote() {
        command.onCommand(anna.player(), null, "tournament", VOTE);
        startTwoAgainstOne();
        command.onCommand(cleo.player(), null, "tournament", VOTE);

        assertEquals("There is no vote for you right now", anna.messages().getFirst());
        assertEquals(List.of("There is no vote for you right now"), cleo.messages());
        assertTrue(fixture.findLastMenuOf(cleo.player()).isEmpty());
    }

    @Test
    void refusesTheCommandFromTheConsole() {
        List<String> consoleTexts = new ArrayList<>();
        CommandSender console = SharedBukkitServer.proxy(CommandSender.class, (method, arguments) ->
                switch (method.getName()) {
                    case "sendMessage" -> {
                        consoleTexts.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                        yield null;
                    }
                    case "hasPermission" -> false;
                    default -> throw new UnsupportedOperationException("CommandSender." + method.getName());
                });

        command.onCommand(console, null, "tournament", VOTE);

        assertEquals(List.of("This command can only be used as a player"), consoleTexts);
    }

    @Test
    void offersTheArgumentOnlyToTheVotersWhileTheVoteRuns() {
        assertEquals(List.of(), command.onTabComplete(anna.player(), null, "tournament", new String[] {"v"}));

        startTwoAgainstOne();

        assertEquals(List.of("vote"), command.onTabComplete(anna.player(), null, "tournament", new String[] {"v"}));
        assertEquals(List.of("bracket", "join", "leave", "vote"),
                command.onTabComplete(ben.player(), null, "tournament", new String[] {""}));
        assertEquals(List.of("bracket", "join", "leave"),
                command.onTabComplete(cleo.player(), null, "tournament", new String[] {""}));

        fixture.unequalTeamVoteService().cancel();

        assertEquals(List.of(), command.onTabComplete(anna.player(), null, "tournament", new String[] {"v"}));
    }

    private void startTwoAgainstOne() {
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo);
        fixture.beginUnequalTeamVote("Alpha", "Bravo");
    }
}

package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnequalTeamVoteMenuSnapshotTest {

    private static final int BEN_SLOT = 10;

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final AdminMenuFixture.OnlinePlayer anna = fixture.letAnotherPlayerJoin("Anna", false, true);
    private final AdminMenuFixture.OnlinePlayer ben = fixture.letAnotherPlayerJoin("Ben", false, true);
    private final AdminMenuFixture.OnlinePlayer cleo = fixture.letAnotherPlayerJoin("Cleo", false, true);

    @Test
    void showsAHeadForEveryOnlinePlayerOfTheVotingTeamAndTheInfoTile() {
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo);

        fixture.beginUnequalTeamVote("Alpha", "Bravo");

        assertEquals("""
                Who fights in match M1? | 27
                4 | BOOK | Vote | Your team has 2 players online, 1 may fight / Time left: 30 seconds / \
                When the time is over, chance decides the rest
                9 | PLAYER_HEAD | Anna | 0 votes / Click to vote for this player
                10 | PLAYER_HEAD | Ben | 0 votes / Click to vote for this player
                """, fixture.describeLastMenuOf(anna.player()));
        assertEquals(fixture.describeLastMenuOf(anna.player()), fixture.describeLastMenuOf(ben.player()));
    }

    @Test
    void countsAVoteInTheMenusOfEveryVoterAndWritesOneVoteInTheSingular() {
        AdminMenuFixture.OnlinePlayer dora = fixture.letAnotherPlayerJoin("Dora", false, false);
        AdminMenuFixture.OnlinePlayer emil = fixture.letAnotherPlayerJoin("Emil", false, false);
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben, dora, emil);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo, fixture.letAnotherPlayerJoin("Finn", false, false));
        fixture.beginUnequalTeamVote("Alpha", "Bravo");

        fixture.clickAs(anna.player(), BEN_SLOT);
        fixture.clickAs(dora.player(), BEN_SLOT);
        fixture.clickAs(ben.player(), BEN_SLOT - 1);

        String expected = """
                Who fights in match M1? | 27
                4 | BOOK | Vote | Your team has 4 players online, 2 may fight / Time left: 30 seconds / \
                When the time is over, chance decides the rest
                9 | PLAYER_HEAD | Anna | 1 vote / Click to vote for this player
                10 | PLAYER_HEAD | Ben | 2 votes / Click to vote for this player
                11 | PLAYER_HEAD | Dora | 0 votes / Click to vote for this player
                12 | PLAYER_HEAD | Emil | 0 votes / Click to vote for this player
                """;
        assertEquals(expected, fixture.describeLastMenuOf(anna.player()));
        assertEquals(expected, fixture.describeLastMenuOf(ben.player()));
        assertEquals(expected, fixture.describeLastMenuOf(dora.player()));
        assertEquals(expected, fixture.describeLastMenuOf(emil.player()));
    }

    @Test
    void showsTheTimeLeftSecondBySecondAndOneSecondInTheSingular() {
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo);
        fixture.beginUnequalTeamVote("Alpha", "Bravo");

        fixture.voteScheduler().advanceSeconds(12);

        assertEquals("4 | BOOK | Vote | Your team has 2 players online, 1 may fight / Time left: 18 seconds / "
                + "When the time is over, chance decides the rest", infoLineOf(anna));

        fixture.voteScheduler().advanceSeconds(17);

        assertEquals("4 | BOOK | Vote | Your team has 2 players online, 1 may fight / Time left: 1 second / "
                + "When the time is over, chance decides the rest", infoLineOf(anna));
    }

    @Test
    void namesTwoPlacesOfFourAgainstTwo() {
        AdminMenuFixture.OnlinePlayer dora = fixture.letAnotherPlayerJoin("Dora", false, false);
        AdminMenuFixture.OnlinePlayer emil = fixture.letAnotherPlayerJoin("Emil", false, false);
        AdminMenuFixture.OnlinePlayer finn = fixture.letAnotherPlayerJoin("Finn", false, false);
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben, dora, emil);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo, finn);

        fixture.beginUnequalTeamVote("Alpha", "Bravo");

        assertEquals("""
                Who fights in match M1? | 27
                4 | BOOK | Vote | Your team has 4 players online, 2 may fight / Time left: 30 seconds / \
                When the time is over, chance decides the rest
                9 | PLAYER_HEAD | Anna | 0 votes / Click to vote for this player
                10 | PLAYER_HEAD | Ben | 0 votes / Click to vote for this player
                11 | PLAYER_HEAD | Dora | 0 votes / Click to vote for this player
                12 | PLAYER_HEAD | Emil | 0 votes / Click to vote for this player
                """, fixture.describeLastMenuOf(emil.player()));
    }

    @Test
    void dropsTheHeadOfAVoterWhoLeftTheServer() {
        AdminMenuFixture.OnlinePlayer dora = fixture.letAnotherPlayerJoin("Dora", false, false);
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben, dora);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo);
        fixture.beginUnequalTeamVote("Alpha", "Bravo");

        fixture.letPlayerLeave(ben);

        assertEquals("""
                Who fights in match M1? | 27
                4 | BOOK | Vote | Your team has 2 players online, 1 may fight / Time left: 30 seconds / \
                When the time is over, chance decides the rest
                9 | PLAYER_HEAD | Anna | 0 votes / Click to vote for this player
                10 | PLAYER_HEAD | Dora | 0 votes / Click to vote for this player
                """, fixture.describeLastMenuOf(anna.player()));
    }

    @Test
    void tellsInTheInfoTileThatTheVoteIsOverWhenTheMenuIsFilledAfterTheEnd() {
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo);
        fixture.beginUnequalTeamVote("Alpha", "Bravo");
        fixture.unequalTeamVoteService().cancel();

        fixture.refillLastMenuOf(anna.player());

        assertEquals("""
                Who fights in match M1? | 27
                4 | BOOK | Vote | The vote for match M1 is over
                """, fixture.describeLastMenuOf(anna.player()));
    }

    private String infoLineOf(AdminMenuFixture.OnlinePlayer player) {
        return fixture.describeLastMenuOf(player.player()).lines()
                .filter(line -> line.startsWith("4 |"))
                .findFirst()
                .orElseThrow();
    }
}

package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanUnequalTeamVoteMenuSnapshotTest {

    private static final int BEN_SLOT = 10;

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final AdminMenuFixture.OnlinePlayer anna = fixture.letAnotherPlayerJoin("Anna", false, true);
    private final AdminMenuFixture.OnlinePlayer ben = fixture.letAnotherPlayerJoin("Ben", false, true);
    private final AdminMenuFixture.OnlinePlayer cleo = fixture.letAnotherPlayerJoin("Cleo", false, true);

    @Test
    void showsTheVoteOfTwoAgainstOneInGerman() {
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo);

        fixture.beginUnequalTeamVote("Alpha", "Bravo");

        assertEquals("""
                Wer kämpft in Match M1? | 27
                4 | BOOK | Abstimmung | Dein Team hat 2 Spieler online, 1 darf kämpfen / Restzeit: 30 Sekunden / \
                Wenn die Zeit um ist, entscheidet der Zufall den Rest
                9 | PLAYER_HEAD | Anna | 0 Stimmen / Klicke, um für diesen Spieler zu stimmen
                10 | PLAYER_HEAD | Ben | 0 Stimmen / Klicke, um für diesen Spieler zu stimmen
                """, fixture.describeLastMenuOf(anna.player()));
        assertEquals(List.of("Dein Team hat mehr Spieler online als Bravo: Stimme innerhalb von 30 Sekunden ab, wer "
                + "kämpft. /tournament vote öffnet die Abstimmung wieder"), anna.messages());
    }

    @Test
    void writesOneVoteTwoFightersAndOneSecondInGerman() {
        AdminMenuFixture.OnlinePlayer dora = fixture.letAnotherPlayerJoin("Dora", false, false);
        AdminMenuFixture.OnlinePlayer emil = fixture.letAnotherPlayerJoin("Emil", false, false);
        AdminMenuFixture.OnlinePlayer finn = fixture.letAnotherPlayerJoin("Finn", false, false);
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben, dora, emil);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo, finn);
        fixture.beginUnequalTeamVote("Alpha", "Bravo");

        fixture.clickAs(anna.player(), BEN_SLOT);
        fixture.voteScheduler().advanceSeconds(29);

        assertEquals("""
                Wer kämpft in Match M1? | 27
                4 | BOOK | Abstimmung | Dein Team hat 4 Spieler online, 2 dürfen kämpfen / Restzeit: 1 Sekunde / \
                Wenn die Zeit um ist, entscheidet der Zufall den Rest
                9 | PLAYER_HEAD | Anna | 0 Stimmen / Klicke, um für diesen Spieler zu stimmen
                10 | PLAYER_HEAD | Ben | 1 Stimme / Klicke, um für diesen Spieler zu stimmen
                11 | PLAYER_HEAD | Dora | 0 Stimmen / Klicke, um für diesen Spieler zu stimmen
                12 | PLAYER_HEAD | Emil | 0 Stimmen / Klicke, um für diesen Spieler zu stimmen
                """, fixture.describeLastMenuOf(anna.player()));
    }

    @Test
    void refusesTheCommandOutsideOfTheVoteInGerman() {
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo);
        fixture.beginUnequalTeamVote("Alpha", "Bravo");

        fixture.createTournamentCommand().onCommand(cleo.player(), null, "tournament", new String[] {"vote"});

        assertEquals(List.of("Gerade läuft keine Abstimmung für dich"), cleo.messages());
    }
}

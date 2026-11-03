package de.simplepvparena.placement;

import de.simplepvparena.team.TeamMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchLeaderboardTest {

    private static final TeamMember ANNA = new TeamMember(UUID.randomUUID(), "Anna");
    private static final TeamMember BERT = new TeamMember(UUID.randomUUID(), "Bert");
    private static final TeamMember CARL = new TeamMember(UUID.randomUUID(), "Carl");

    private final AtomicInteger changes = new AtomicInteger();
    private final AutoMatchLeaderboard leaderboard = new AutoMatchLeaderboard(changes::incrementAndGet);

    @Test
    void countsAWinForEveryMemberOfTheWinningTeamAndRanksByWinsThenName() {
        leaderboard.recordWin(List.of(BERT, ANNA));
        leaderboard.recordWin(List.of(CARL));
        leaderboard.recordWin(List.of(CARL));

        assertEquals(List.of("Carl 2", "Anna 1", "Bert 1"), leaderboard.top(10).stream()
                .map(wins -> wins.playerName() + " " + wins.wins()).toList());
        assertEquals(List.of("Carl 2"), leaderboard.top(1).stream()
                .map(wins -> wins.playerName() + " " + wins.wins()).toList());
        assertEquals(3, changes.get());
    }

    @Test
    void anEmptyWinnerListAndClearingAnEmptyBoardChangeNothing() {
        leaderboard.recordWin(List.of());
        leaderboard.clear();

        assertTrue(leaderboard.isEmpty());
        assertEquals(0, changes.get());
    }

    @Test
    void theFileKeepsTheWinsAcrossARestart(@TempDir Path folder) throws IOException {
        FileAutoMatchLeaderboardStorage storage = new FileAutoMatchLeaderboardStorage(
                folder.resolve(FileAutoMatchLeaderboardStorage.FILE_NAME));
        assertEquals(List.of(), storage.load());
        leaderboard.recordWin(List.of(ANNA));
        leaderboard.recordWin(List.of(ANNA, BERT));

        storage.save(leaderboard.all());
        AutoMatchLeaderboard restored = new AutoMatchLeaderboard(() -> { });
        restored.load(storage.load());

        assertEquals(leaderboard.top(10), restored.top(10));
    }
}

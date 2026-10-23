package de.simplepvparena.listener;

import de.simplepvparena.arena.AreaInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AreaInventoryChatMessagesTest {

    @TempDir
    Path dataFolder;

    private AreaInventoryFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
    }

    @Test
    void sendsEachMessageIntoTheChatAtMostOnceAMinuteAndOtherwiseIntoTheActionBar() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Wanderer");

        crossTheBorderTwice(player);

        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE, AreaInventoryFixture.BACK), player.messages());
        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE, AreaInventoryFixture.BACK), player.actionBars());
    }

    @Test
    void aMinuteLaterTheMessagesComeIntoTheChatAgain() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Wanderer");
        fixture.service().reconcile(player.player(), fixture.inside());
        fixture.service().reconcile(player.player(), fixture.outside());
        player.forgetReceivedTexts();

        fixture.advanceClockMillis(AreaInventoryService.CHAT_MESSAGE_INTERVAL_MILLIS);
        fixture.service().reconcile(player.player(), fixture.inside());
        fixture.service().reconcile(player.player(), fixture.outside());

        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE, AreaInventoryFixture.BACK), player.messages());
        assertEquals(List.of(), player.actionBars());
    }

    @Test
    void countsTheMinuteForEveryPlayerOnItsOwn() {
        ArenaMatchHarness.FakePlayer firstPlayer = fixture.addPlayerOutsideWithItems("First");
        ArenaMatchHarness.FakePlayer secondPlayer = fixture.addPlayerOutsideWithItems("Second");
        fixture.service().reconcile(firstPlayer.player(), fixture.inside());

        fixture.service().reconcile(secondPlayer.player(), fixture.inside());

        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE), firstPlayer.messages());
        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE), secondPlayer.messages());
    }

    @Test
    void theChatIntervalIsOneMinute() {
        assertEquals(60_000L, AreaInventoryService.CHAT_MESSAGE_INTERVAL_MILLIS);
    }

    private void crossTheBorderTwice(ArenaMatchHarness.FakePlayer player) {
        for (int crossing = 0; crossing < 2; crossing++) {
            fixture.service().reconcile(player.player(), fixture.inside());
            fixture.advanceClockMillis(10_000L);
            fixture.service().reconcile(player.player(), fixture.outside());
            fixture.advanceClockMillis(10_000L);
        }
    }
}

package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitChoiceAdminMenuPresenterTest {

    private static final int SECOND_KIT_SLOT = 1;

    private final AdminMenuFixture fixture = AdminMenuFixture.withKitChoice();
    private final AdminMenuFixture.OnlinePlayer moderator = fixture.letAnotherPlayerJoin("Moderator", true);
    private final AdminMenuFixture.OnlinePlayer visitor = fixture.letAnotherPlayerJoin("Visitor", false);

    private int matchStartCount;

    KitChoiceAdminMenuPresenterTest() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
    }

    @Test
    void opensThePopupForEveryOnlineAdminAndForNobodyElse() {
        beginTheChoice();

        assertEquals(AdminMenuType.KIT_CHOICE, fixture.lastMenu().holder().getMenuType());
        assertEquals(Optional.of("round1-match1"), fixture.lastMenu().holder().getSubject());
        RecordingAdminMenuWindows.RecordedMenu moderatorMenu = fixture.findLastMenuOf(moderator.player())
                .orElseThrow();
        assertEquals(AdminMenuType.KIT_CHOICE, moderatorMenu.holder().getMenuType());
        assertEquals(Optional.empty(), fixture.findLastMenuOf(visitor.player()));
        assertEquals(List.of("Match round1-match1 (Alpha against Bravo): choose the kit within 30 seconds, "
                + "otherwise the default kit Archer is used"), fixture.adminNotifications());
    }

    @Test
    void closesThePopupOfTheOtherAdminsWithTheNameOfTheChooser() {
        beginTheChoice();

        fixture.click(SECOND_KIT_SLOT);

        assertTrue(fixture.isMenuClosedFor(moderator.player()));
        assertEquals(List.of("admin chose the kit Knight for match round1-match1"), moderator.messages());
        assertEquals(List.of(), visitor.messages());
        assertEquals(1, matchStartCount);
    }

    @Test
    void leavesAnotherMenuOfAnAdminOpenWhenTheChoiceEnds() {
        beginTheChoice();
        fixture.openMainMenuFor(moderator.player());

        fixture.click(SECOND_KIT_SLOT);

        assertFalse(fixture.isMenuClosedFor(moderator.player()));
        assertEquals(AdminMenuType.MAIN, fixture.findLastMenuOf(moderator.player()).orElseThrow().holder()
                .getMenuType());
        assertEquals(List.of("admin chose the kit Knight for match round1-match1"), moderator.messages());
    }

    @Test
    void closesEveryPopupWhenTheTimeIsOver() {
        beginTheChoice();

        fixture.kitChoiceScheduler().advanceSeconds(30);

        assertTrue(fixture.isMenuClosed());
        assertTrue(fixture.isMenuClosedFor(moderator.player()));
        String expiryMessage = "No kit was chosen for match round1-match1, the default kit Archer is used";
        assertEquals(List.of(expiryMessage), fixture.messages());
        assertEquals(List.of(expiryMessage), moderator.messages());
        assertEquals(List.of(), visitor.messages());
        assertEquals(1, matchStartCount);
    }

    @Test
    void closesEveryPopupWhenTheChoiceIsCancelled() {
        beginTheChoice();

        fixture.kitChoiceService().cancel();

        assertTrue(fixture.isMenuClosed());
        assertTrue(fixture.isMenuClosedFor(moderator.player()));
        assertEquals(List.of("The kit choice was cancelled"), fixture.messages());
        assertEquals(List.of("The kit choice was cancelled"), moderator.messages());
        assertEquals(0, matchStartCount);
    }

    @Test
    void startsTheMatchAtOnceWhenOnlyPlayersWithoutTheAdminPermissionAreOnline() {
        AdminMenuFixture otherFixture = AdminMenuFixture.withKitChoice();
        otherFixture.createKit("Archer");
        otherFixture.createKit("Knight");
        otherFixture.letTheAdminLeave();
        otherFixture.letAnotherPlayerJoin("Visitor", false);
        int[] otherMatchStartCount = new int[1];

        otherFixture.kitChoiceService().begin("round1-match1", "Alpha against Bravo",
                () -> otherMatchStartCount[0]++);

        assertEquals(1, otherMatchStartCount[0]);
        assertFalse(otherFixture.kitChoiceService().isPending());
        assertEquals(List.of(), otherFixture.adminNotifications());
        assertEquals(List.of("No admin is online to choose the kit for match round1-match1, the default kit "
                + "Archer is used"), otherFixture.kitChoiceScheduler().logLines());
    }

    @Test
    void keepsTheChoiceOpenAndTheDeadlineRunningWhenAnAdminLeaves() {
        beginTheChoice();

        fixture.letTheAdminLeave();
        fixture.kitChoiceScheduler().advanceSeconds(29);

        assertTrue(fixture.kitChoiceService().isPending());
        assertEquals(0, matchStartCount);

        fixture.kitChoiceScheduler().advanceSeconds(1);

        assertEquals(1, matchStartCount);
        assertTrue(fixture.isMenuClosedFor(moderator.player()));
    }

    private void beginTheChoice() {
        fixture.kitChoiceService().begin("round1-match1", "Alpha against Bravo", () -> matchStartCount++);
    }
}

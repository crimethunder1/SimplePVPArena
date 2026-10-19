package de.simplepvparena.tournament;

import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.InMemoryKitLibraryStorage;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.TickingScheduler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitChoiceServiceTest {

    private static final String MATCH_ID = "round1-match1";
    private static final String FIRST_TEAM = "Alpha";
    private static final String SECOND_TEAM = "Bravo";
    private static final String OPENING_MESSAGE = "Match round1-match1 (Alpha against Bravo): choose the kit within "
            + "30 seconds, otherwise the default kit Archer is used";
    private static final String REMINDER_MESSAGE = "10 seconds left to choose the kit for match round1-match1";

    private final TickingScheduler scheduler = new TickingScheduler();
    private final InMemoryKitLibraryStorage storage = new InMemoryKitLibraryStorage();
    private final KitService kitService = new KitService();
    private final List<String> writeFailures = new ArrayList<>();
    private final KitLibrary kitLibrary = new KitLibrary(storage, kitService, new FakeKitItemSerializer(),
            problem -> { }, writeFailures::add);
    private final List<String> adminMessages = new ArrayList<>();
    private final RecordingKitChoicePresenter presenter = new RecordingKitChoicePresenter();
    private final KitChoiceService kitChoiceService = new KitChoiceService(scheduler.plugin(), kitLibrary,
            adminMessages::add, scheduler.clock());

    private int matchStartCount;

    KitChoiceServiceTest() {
        SharedBukkitServer.install();
        kitChoiceService.setPresenter(presenter);
    }

    @Test
    void opensAChoiceWithTwoKitsAndAnAdminWithoutStartingTheMatch() {
        createArcherAndKnight();

        begin();

        assertTrue(kitChoiceService.isPending());
        assertEquals(Optional.of(new PendingKitChoice(MATCH_ID, FIRST_TEAM, SECOND_TEAM, "Archer",
                TickingScheduler.START.plusSeconds(30))), kitChoiceService.getPending());
        assertEquals(30, kitChoiceService.getSecondsLeft());
        assertEquals(List.of(kitChoiceService.getPending().orElseThrow()), presenter.shownChoices());
        assertEquals(List.of(OPENING_MESSAGE), adminMessages);
        assertEquals(0, matchStartCount);
        assertEquals(2, scheduler.pendingTaskCount());
    }

    @Test
    void startsTheMatchAtOnceWithTheOnlyKit() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        begin();

        assertFalse(kitChoiceService.isPending());
        assertEquals(1, matchStartCount);
        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
        assertEquals(List.of(), presenter.shownChoices());
        assertEquals(List.of(), adminMessages);
        assertEquals(0, scheduler.pendingTaskCount());
    }

    @Test
    void startsTheMatchAtOnceWithTheEmptyKitWhenThereIsNoKit() {
        begin();

        assertFalse(kitChoiceService.isPending());
        assertEquals(1, matchStartCount);
        assertTrue(kitService.getKitDefinition().isEmpty());
        assertEquals(List.of(), presenter.shownChoices());
        assertEquals(0, scheduler.pendingTaskCount());
    }

    @Test
    void startsTheMatchAtOnceWithTheDefaultKitAndALogLineWhenNoAdminIsOnline() {
        createArcherAndKnight();
        kitLibrary.activate("Knight");
        presenter.letEveryAdminLeave();

        begin();

        assertFalse(kitChoiceService.isPending());
        assertEquals(1, matchStartCount);
        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
        assertEquals(List.of("No admin is online to choose the kit for match round1-match1, the default kit "
                + "Archer is used"), scheduler.logLines());
        assertEquals(List.of(), presenter.shownChoices());
        assertEquals(List.of(), adminMessages);
        assertEquals(0, scheduler.pendingTaskCount());
    }

    @Test
    void startsTheMatchAtOnceWhenTheServiceNeverChooses() {
        KitChoiceService neverChoosing = new KitChoiceService();
        neverChoosing.setPresenter(presenter);

        neverChoosing.begin(MATCH_ID, FIRST_TEAM, SECOND_TEAM, () -> matchStartCount++);

        assertFalse(neverChoosing.isPending());
        assertEquals(1, matchStartCount);
        assertEquals(List.of(), presenter.shownChoices());
    }

    @Test
    void activatesTheChosenKitOnlyForThisMatchAndStartsItOnce() {
        createArcherAndKnight();
        int savedLibraryCount = storage.savedLibraries().size();
        begin();

        assertTrue(kitChoiceService.choose("Knight", "KT"));

        assertEquals(1, matchStartCount);
        assertFalse(kitChoiceService.isPending());
        assertSame(kitDefinitionOf("Knight"), kitService.getKitDefinition());
        assertEquals(Optional.of("Archer"), kitLibrary.getDefaultKitName());
        assertEquals(savedLibraryCount, storage.savedLibraries().size());
        assertEquals(Optional.of("Archer"), storage.lastSavedLibrary().defaultKitName());
        assertEquals(List.of(), writeFailures);
        assertEquals(List.of("KT chose the kit Knight for match round1-match1"), presenter.closeMessages());
        assertEquals(0, scheduler.pendingTaskCount());
        scheduler.advanceSeconds(60);
        assertEquals(1, matchStartCount);
    }

    @Test
    void namesTheKitAsItIsStoredWhenItIsChosenInAnotherCase() {
        createArcherAndKnight();
        begin();

        assertTrue(kitChoiceService.choose("knight", "KT"));

        assertSame(kitDefinitionOf("Knight"), kitService.getKitDefinition());
        assertEquals(List.of("KT chose the kit Knight for match round1-match1"), presenter.closeMessages());
    }

    @Test
    void startsTheMatchOnceWithTheDefaultKitWhenNobodyChoosesWithinThirtySeconds() {
        createArcherAndKnight();
        kitLibrary.activate("Knight");
        begin();

        scheduler.advanceSeconds(29);
        assertEquals(0, matchStartCount);
        assertTrue(kitChoiceService.isPending());
        assertEquals(1, kitChoiceService.getSecondsLeft());

        scheduler.advanceSeconds(1);

        assertEquals(1, matchStartCount);
        assertFalse(kitChoiceService.isPending());
        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
        assertEquals(List.of("No kit was chosen for match round1-match1, the default kit Archer is used"),
                presenter.closeMessages());
        scheduler.advanceSeconds(60);
        assertEquals(1, matchStartCount);
        assertEquals(0, scheduler.pendingTaskCount());
    }

    @Test
    void remindsTheAdminsOnceWhenTenSecondsAreLeft() {
        createArcherAndKnight();
        begin();

        scheduler.advanceTicks(20 * TickingScheduler.TICKS_PER_SECOND - 1);
        assertEquals(List.of(OPENING_MESSAGE), adminMessages);

        scheduler.advanceTicks(1);
        assertEquals(List.of(OPENING_MESSAGE, REMINDER_MESSAGE), adminMessages);
        assertEquals(10, kitChoiceService.getSecondsLeft());

        scheduler.advanceSeconds(9);
        assertEquals(List.of(OPENING_MESSAGE, REMINDER_MESSAGE), adminMessages);
    }

    @Test
    void sendsNoReminderAfterAKitWasChosen() {
        createArcherAndKnight();
        begin();
        scheduler.advanceSeconds(5);

        assertTrue(kitChoiceService.choose("Knight", "KT"));
        scheduler.advanceSeconds(60);

        assertEquals(List.of(OPENING_MESSAGE), adminMessages);
        assertEquals(1, matchStartCount);
    }

    @Test
    void refusesTheChoiceOfASecondAdminAfterTheFirstOne() {
        createArcherAndKnight();
        begin();

        assertTrue(kitChoiceService.choose("Knight", "KT"));
        assertFalse(kitChoiceService.choose("Archer", "Other"));

        assertEquals(1, matchStartCount);
        assertSame(kitDefinitionOf("Knight"), kitService.getKitDefinition());
        assertEquals(List.of("KT chose the kit Knight for match round1-match1"), presenter.closeMessages());
    }

    @Test
    void refusesAnUnknownKitAndKeepsTheChoiceOpen() {
        createArcherAndKnight();
        begin();

        assertFalse(kitChoiceService.choose("Wizard", "KT"));

        assertTrue(kitChoiceService.isPending());
        assertEquals(0, matchStartCount);
        assertEquals(List.of(), presenter.closeMessages());
        assertEquals(2, scheduler.pendingTaskCount());
        scheduler.advanceSeconds(30);
        assertEquals(1, matchStartCount);
    }

    @Test
    void refusesAChoiceWhenNoChoiceIsOpen() {
        createArcherAndKnight();

        assertFalse(kitChoiceService.choose("Knight", "KT"));

        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
    }

    @Test
    void cancelsAChoiceWithoutStartingTheMatchAndClearsItsTasks() {
        createArcherAndKnight();
        begin();

        kitChoiceService.cancel();

        assertFalse(kitChoiceService.isPending());
        assertEquals(Optional.empty(), kitChoiceService.getPending());
        assertEquals(0, kitChoiceService.getSecondsLeft());
        assertEquals(List.of("The kit choice was cancelled"), presenter.closeMessages());
        assertEquals(0, scheduler.pendingTaskCount());
        scheduler.advanceSeconds(60);
        assertEquals(0, matchStartCount);
        assertEquals(List.of(OPENING_MESSAGE), adminMessages);
        assertFalse(kitChoiceService.choose("Knight", "KT"));
    }

    @Test
    void ignoresACancelWhenNoChoiceIsOpen() {
        kitChoiceService.cancel();

        assertEquals(List.of(), presenter.closeMessages());
    }

    @Test
    void refusesASecondBeginWhileAChoiceIsOpenAndKeepsTheFirstChoice() {
        createArcherAndKnight();
        begin();
        int[] secondMatchStartCount = new int[1];

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> kitChoiceService.begin("round1-match2", "Charlie", "Delta",
                        () -> secondMatchStartCount[0]++));

        assertEquals("the kit for match round1-match1 is still being chosen, match round1-match2 cannot begin "
                + "a second kit choice", failure.getMessage());
        assertEquals(0, secondMatchStartCount[0]);
        assertEquals(MATCH_ID, kitChoiceService.getPending().orElseThrow().matchId());
        assertEquals(1, presenter.shownChoices().size());
        assertTrue(kitChoiceService.choose("Knight", "KT"));
        assertEquals(1, matchStartCount);
        assertEquals(0, secondMatchStartCount[0]);
    }

    @Test
    void beginsANewChoiceAfterTheFormerOneEnded() {
        createArcherAndKnight();
        begin();
        assertTrue(kitChoiceService.choose("Knight", "KT"));

        kitChoiceService.begin("round1-match2", "Charlie", "Delta", () -> matchStartCount++);

        assertEquals("round1-match2", kitChoiceService.getPending().orElseThrow().matchId());
        assertEquals(2, presenter.shownChoices().size());
        assertEquals(1, matchStartCount);
    }

    @Test
    void makesTheDefaultKitActiveAgainWhenTheMatchIsOver() {
        createArcherAndKnight();
        begin();
        assertTrue(kitChoiceService.choose("Knight", "KT"));

        kitChoiceService.activateDefaultKit();

        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
        assertEquals(Optional.of("Archer"), kitLibrary.getDefaultKitName());
    }

    @Test
    void keepsTheDefaultKitAndTheFileWhenAKitIsActivated() {
        createArcherAndKnight();
        int savedLibraryCount = storage.savedLibraries().size();

        kitLibrary.activate("Knight");
        assertSame(kitDefinitionOf("Knight"), kitService.getKitDefinition());
        kitLibrary.activateDefaultKit();

        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
        assertEquals(Optional.of("Archer"), kitLibrary.getDefaultKitName());
        assertEquals(savedLibraryCount, storage.savedLibraries().size());
        assertThrows(IllegalArgumentException.class, () -> kitLibrary.activate("Wizard"));
        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
    }

    private void createArcherAndKnight() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));
        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));
    }

    private void begin() {
        kitChoiceService.begin(MATCH_ID, FIRST_TEAM, SECOND_TEAM, () -> matchStartCount++);
    }

    private KitDefinition kitDefinitionOf(String kitName) {
        return kitLibrary.findKit(kitName).orElseThrow().kitDefinition();
    }
}

package de.simplepvparena.listener;

import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorMode;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeSpectatorItems;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("en")
class SpectatorItemInTheEffectAreaTest {

    private static final double ON_THE_TRIBUNE = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;
    private static final double INSIDE_THE_FIGHT_AREA = 5.5D;
    private static final double OUTSIDE_THE_EFFECT_AREA = 500.5D;
    private static final ItemStack SPECTATOR_TOGGLE_ITEM = new FakeItemStack(FakeSpectatorItems.SPECTATOR_TOGGLE_LABEL);

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer visitor;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        visitor = harness.addPlayer("Visitor");
        visitor.moveTo(harness.at(ON_THE_TRIBUNE, ON_THE_TRIBUNE));
    }

    @Test
    void aPlayerInTheEffectAreaWithoutAnAssignmentFliesIntoTheArena() {
        useSpectatorItem(visitor);

        assertEquals(List.of(), visitor.messages());
        assertEquals(Optional.of(SpectatorMode.ARENA_FLIGHT),
                harness.getSpectatorService().findSpectatorMode(visitor.uniqueId()));
        assertEquals(GameMode.SPECTATOR, visitor.gameMode());
        Location flightSpawn = harness.getArenaRegistry().getArena().getSpectatorFlightSpawn()
                .flatMap(PositionSnapshot::toLocation).orElseThrow();
        assertEquals(flightSpawn, visitor.location());
    }

    @Test
    void aPlayerInsideTheFightAreaWithoutAnAssignmentFliesAsWell() {
        visitor.moveTo(harness.at(INSIDE_THE_FIGHT_AREA, INSIDE_THE_FIGHT_AREA));

        useSpectatorItem(visitor);

        assertEquals(List.of(), visitor.messages());
        assertEquals(Optional.of(SpectatorMode.ARENA_FLIGHT),
                harness.getSpectatorService().findSpectatorMode(visitor.uniqueId()));
    }

    @Test
    void theFlightOfAPlayerWithoutAnAssignmentEndsOnTheTribuneWithASingleSpectatorItem() {
        visitor.keepItemsSetBySlot();
        ItemStack[] contents = new ItemStack[ArenaMatchHarness.INVENTORY_SLOT_COUNT];
        contents[3] = SPECTATOR_TOGGLE_ITEM;
        visitor.setContents(contents);
        useSpectatorItem(visitor);
        PlayerMoveListener moveListener = new PlayerMoveListener(harness.getMatchService(),
                harness.getSpectatorService(), harness.getArenaRegistry(), harness.getPlugin().getLogger());

        moveListener.onPlayerMove(new PlayerMoveEvent(visitor.player(),
                harness.at(INSIDE_THE_FIGHT_AREA, INSIDE_THE_FIGHT_AREA),
                harness.at(OUTSIDE_THE_EFFECT_AREA, OUTSIDE_THE_EFFECT_AREA)));

        assertEquals(Optional.of(SpectatorMode.TRIBUNE),
                harness.getSpectatorService().findSpectatorMode(visitor.uniqueId()));
        assertEquals(GameMode.ADVENTURE, visitor.gameMode());
        assertEquals(ON_THE_TRIBUNE, visitor.location().getX(), 1.0E-9D);
        assertEquals(1L, Arrays.stream(visitor.contents()).filter(SPECTATOR_TOGGLE_ITEM::equals).count());
    }

    @Test
    void aPlayerOutsideTheEffectAreaWithoutAnAssignmentIsStillToldToSpectate() {
        visitor.moveTo(harness.at(OUTSIDE_THE_EFFECT_AREA, OUTSIDE_THE_EFFECT_AREA));

        useSpectatorItem(visitor);

        assertEquals(List.of("You are not watching the arena, use /tournament spectate"), visitor.messages());
        assertEquals(Optional.empty(), harness.getSpectatorService().findSpectatorMode(visitor.uniqueId()));
    }

    @Test
    void withoutACalculatedFightAreaNobodyStandsInTheEffectArea() {
        harness.getArenaRegistry().getArena().setShape(null);

        useSpectatorItem(visitor);

        assertEquals(List.of("You are not watching the arena, use /tournament spectate"), visitor.messages());
        assertEquals(Optional.empty(), harness.getSpectatorService().findSpectatorMode(visitor.uniqueId()));
    }

    @Test
    void aPlayerInTheEffectAreaStaysOnTheGroundWhileTheTribuneSpawnIsMissing() {
        harness.forgetTribuneSpawn();
        GameMode gameModeBefore = visitor.gameMode();

        useSpectatorItem(visitor);

        assertEquals(List.of("The tribune spawn is not set, use the spectator item to stop flying"),
                visitor.messages());
        assertEquals(Optional.empty(), harness.getSpectatorService().findSpectatorMode(visitor.uniqueId()));
        assertEquals(gameModeBefore, visitor.gameMode());
        assertEquals(ON_THE_TRIBUNE, visitor.location().getX(), 1.0E-9D);
    }

    @Test
    void anAssignedSpectatorOutsideTheEffectAreaStillSwitchesAsBefore() {
        harness.getSpectatorService().sendToTribune(visitor.player());
        visitor.moveTo(harness.at(OUTSIDE_THE_EFFECT_AREA, OUTSIDE_THE_EFFECT_AREA));

        useSpectatorItem(visitor);

        assertEquals(List.of(), visitor.messages());
        assertEquals(Optional.of(SpectatorMode.ARENA_FLIGHT),
                harness.getSpectatorService().findSpectatorMode(visitor.uniqueId()));
    }

    @Test
    void anActiveFighterInTheEffectAreaStillCannotSwitch() {
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        harness.startMatch(visitor, mate, opponent, opponentMate);

        useSpectatorItem(visitor);

        assertEquals(List.of("You cannot switch to spectator mode while you are fighting"), visitor.messages());
        assertEquals(Optional.empty(), harness.getSpectatorService().findSpectatorMode(visitor.uniqueId()));
    }

    private void useSpectatorItem(ArenaMatchHarness.FakePlayer player) {
        PlayerInteractListener listener = new PlayerInteractListener(harness.getMatchService(),
                harness.getSpectatorService(), new BorderSelectorModeRegistry(), harness.getPlugin().getLogger());
        player.forgetReceivedTexts();
        listener.onPlayerInteract(new PlayerInteractEvent(player.player(), Action.RIGHT_CLICK_AIR,
                SPECTATOR_TOGGLE_ITEM, null, BlockFace.UP, EquipmentSlot.HAND));
    }
}

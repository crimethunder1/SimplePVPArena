package de.simplepvparena.listener;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaItemRuleTest {

    private static final double INSIDE_X = 5.5D;
    private static final double INSIDE_Z = 5.5D;
    private static final double TRIBUNE_X = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;
    private static final double TRIBUNE_Z = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;
    private ArenaMatchHarness.FakePlayer spectator;
    private ArenaMatchHarness.FakePlayer bystander;
    private ArenaMatchHarness.FakePlayer admin;
    private ArenaItemRuleListener itemRuleListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
        spectator = harness.addPlayer("Spectator");
        bystander = harness.addPlayer("Bystander");
        admin = harness.addPlayer("Admin");
        admin.grantAdminPermission();
        itemRuleListener = new ArenaItemRuleListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry());
    }

    @Test
    void keepsAParticipantAnAssignedSpectatorAndAnyoneInTheFightAreaFromDroppingItems() {
        harness.startMatch(fighter, mate, opponent, opponentMate);
        fighter.moveTo(onTheTribune());
        assignSpectatorOnTheTribune();
        bystander.moveTo(insideTheFightArea());

        assertTrue(isDropCancelled(fighter), "a participant on the tribune");
        assertTrue(isDropCancelled(spectator), "an assigned spectator on the tribune");
        assertTrue(isDropCancelled(bystander), "a player inside the fight area");
    }

    @Test
    void letsAPlayerOutsideWithoutAMatchAndAnAdminDropItems() {
        assertFalse(isDropCancelled(bystander), "a player on the tribune without a match");

        admin.moveTo(insideTheFightArea());
        assertFalse(isDropCancelled(admin), "an admin inside the fight area");

        harness.startMatch(admin, mate, opponent, opponentMate);
        assertFalse(isDropCancelled(admin), "an admin as a participant");
    }

    @Test
    void keepsAParticipantAnAssignedSpectatorAndAnyoneInTheFightAreaFromPickingUpItems() {
        harness.startMatch(fighter, mate, opponent, opponentMate);
        fighter.moveTo(onTheTribune());
        assignSpectatorOnTheTribune();
        bystander.moveTo(insideTheFightArea());

        assertTrue(isPickupCancelled(fighter, onTheTribune()), "a participant on the tribune");
        assertTrue(isPickupCancelled(spectator, onTheTribune()), "an assigned spectator on the tribune");
        assertTrue(isPickupCancelled(bystander, insideTheFightArea()), "a player inside the fight area");
    }

    @Test
    void keepsAPlayerOutsideFromPickingUpAnItemInsideTheFightArea() {
        assertFalse(isPickupCancelled(bystander, onTheTribune()), "an item on the tribune");
        assertTrue(isPickupCancelled(bystander, insideTheFightArea()), "an item inside the fight area");
    }

    @Test
    void letsAnAdminPickUpItems() {
        assertFalse(isPickupCancelled(admin, insideTheFightArea()), "an item inside the fight area");

        admin.moveTo(insideTheFightArea());
        assertFalse(isPickupCancelled(admin, insideTheFightArea()), "while standing in the fight area");

        harness.startMatch(admin, mate, opponent, opponentMate);
        assertFalse(isPickupCancelled(admin, insideTheFightArea()), "and as a participant");
    }

    private void assignSpectatorOnTheTribune() {
        assertTrue(harness.getSpectatorService().enterArenaFlight(spectator.player()));
        spectator.moveTo(onTheTribune());
        assertTrue(harness.getSpectatorService().findSpectatorMode(spectator.uniqueId()).isPresent());
    }

    private boolean isDropCancelled(ArenaMatchHarness.FakePlayer player) {
        PlayerDropItemEvent event = new PlayerDropItemEvent(player.player(), createItem(player.location()));
        itemRuleListener.onPlayerDropItem(event);
        return event.isCancelled();
    }

    private boolean isPickupCancelled(ArenaMatchHarness.FakePlayer player, Location itemLocation) {
        EntityPickupItemEvent event = new EntityPickupItemEvent(player.player(), createItem(itemLocation), 0);
        itemRuleListener.onEntityPickupItem(event);
        return event.isCancelled();
    }

    private Location insideTheFightArea() {
        return harness.at(INSIDE_X, INSIDE_Z);
    }

    private Location onTheTribune() {
        return harness.at(TRIBUNE_X, TRIBUNE_Z);
    }

    private static Item createItem(Location location) {
        return ArenaMatchHarness.proxy(Item.class, (method, arguments) -> switch (method.getName()) {
            case "getLocation" -> location.clone();
            default -> throw new UnsupportedOperationException("Item." + method.getName());
        });
    }
}

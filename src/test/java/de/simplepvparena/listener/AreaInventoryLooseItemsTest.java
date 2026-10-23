package de.simplepvparena.listener;

import de.simplepvparena.support.FakeItemStack;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AreaInventoryLooseItemsTest {

    private static final ItemStack TORCH = new FakeItemStack("torch on the cursor");
    private static final ItemStack PLANKS = new FakeItemStack("planks in the grid");
    private static final ItemStack STICK = new FakeItemStack("stick in the grid");

    @TempDir
    Path dataFolder;

    private AreaInventoryFixture fixture;
    private ArenaMatchHarness harness;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
        harness = fixture.harness();
    }

    @Test
    void savesTheItemOnTheCursorAndTheCraftingGridOnEnteringAndEmptiesBoth() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Crafter");
        player.holdOnCursor(TORCH);
        player.putIntoCraftingGrid(PLANKS, null, STICK, null);

        fixture.service().reconcile(player.player(), fixture.inside());

        assertNull(player.itemOnCursor());
        assertArrayEquals(new ItemStack[ArenaMatchHarness.CRAFTING_GRID_SLOT_COUNT], player.craftingGrid());
        assertArrayEquals(AreaInventoryFixture.emptyContents(), player.contents());
        assertEquals(List.of(TORCH, PLANKS, STICK), fixture.findSavedInventory(player).looseItems());
    }

    @Test
    void givesTheLooseItemsBackIntoFreeSlotsOnLeaving() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Crafter");
        player.holdOnCursor(TORCH);
        player.putIntoCraftingGrid(PLANKS, STICK);
        fixture.service().reconcile(player.player(), fixture.inside());

        fixture.service().reconcile(player.player(), fixture.outside());

        ItemStack[] expectedContents = AreaInventoryFixture.ownContents();
        expectedContents[1] = TORCH;
        expectedContents[2] = PLANKS;
        expectedContents[3] = STICK;
        assertArrayEquals(expectedContents, player.contents());
        assertEquals(List.of(), harness.droppedItems());
        assertEquals(List.of(), fixture.warnings());
    }

    @Test
    void dropsWhatDoesNotFitAtTheFeetOfThePlayerOutsideOfTheArea() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Hoarder");
        ItemStack[] fullContents = new ItemStack[ArenaMatchHarness.INVENTORY_SLOT_COUNT];
        for (int slotIndex = 0; slotIndex < ArenaMatchHarness.STORAGE_SLOT_COUNT - 1; slotIndex++) {
            fullContents[slotIndex] = new FakeItemStack("cobblestone " + slotIndex);
        }
        player.setContents(fullContents);
        player.holdOnCursor(TORCH);
        player.putIntoCraftingGrid(PLANKS);
        fixture.service().reconcile(player.player(), fixture.inside());
        player.moveTo(fixture.outside());

        fixture.service().reconcile(player.player(), fixture.outside());

        assertEquals(TORCH, player.contents()[ArenaMatchHarness.STORAGE_SLOT_COUNT - 1]);
        assertEquals(List.of(new ArenaMatchHarness.DroppedItem(fixture.outside(), PLANKS)), harness.droppedItems());
    }

    @Test
    void leavesTheGridOfAnotherOpenInventoryAloneAndSavesOnlyTheCursor() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Worker");
        player.openInventoryOfType(InventoryType.WORKBENCH);
        player.holdOnCursor(TORCH);
        player.putIntoCraftingGrid(PLANKS);

        fixture.service().reconcile(player.player(), fixture.inside());

        assertNull(player.itemOnCursor());
        assertEquals(PLANKS, player.craftingGrid()[0]);
        assertEquals(List.of(TORCH), fixture.findSavedInventory(player).looseItems());
    }

    @Test
    void writesNoLooseItemsForAnEmptyCursorAndAnEmptyGrid() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Tidy");

        fixture.service().reconcile(player.player(), fixture.inside());
        fixture.service().reconcile(player.player(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertEquals(List.of(), harness.droppedItems());
    }
}

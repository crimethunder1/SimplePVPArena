package de.simplepvparena.arena;

import de.simplepvparena.support.FakeSpectatorItems;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorServiceTest {

    private static final String WORLD_NAME = "spectator-service";
    private static final int SPECTATOR_TOGGLE_ITEM_SLOT = 8;

    private final UUID playerUniqueId = UUID.randomUUID();
    private final Map<Integer, ItemStack> inventoryItemsBySlot = new LinkedHashMap<>();
    private final ArenaRegistry arenaRegistry = new ArenaRegistry();
    private final FakeSpectatorItems spectatorItems = new FakeSpectatorItems();

    private SpectatorService spectatorService;
    private Player player;
    private Location playerLocation;
    private GameMode playerGameMode = GameMode.SURVIVAL;

    @BeforeEach
    void setUp() {
        World world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> WORLD_NAME;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
        SharedBukkitServer.register(world, uniqueId -> null);
        arenaRegistry.getArena().setTribuneSpawn(new PositionSnapshot(WORLD_NAME, 20.5D, 64.0D, 20.5D, 0F, 0F));
        spectatorService = new SpectatorService(arenaRegistry, spectatorItems);
        player = createPlayer();
    }

    @Test
    void sendsAPlayerToTheTribuneAndHandsOutTheToggleItem() {
        assertTrue(spectatorService.sendToTribune(player));

        assertEquals(Optional.of(SpectatorMode.TRIBUNE), spectatorService.findSpectatorMode(playerUniqueId));
        assertEquals(1, spectatorItems.getCreatedItems().size(), "exactly one toggle item is built");
        assertSame(spectatorItems.getCreatedItems().get(0), inventoryItemsBySlot.get(SPECTATOR_TOGGLE_ITEM_SLOT),
                "and handed out in the spectator slot");
        assertEquals(GameMode.ADVENTURE, playerGameMode);
        assertEquals(20.5D, playerLocation.getX(), 1.0E-9D);
    }

    @Test
    void leavesNoAssignmentBehindWhenTheToggleItemCannotBeBuilt() {
        RuntimeException failure = new IllegalArgumentException("COMPASS isn't an item");
        spectatorItems.failCreationWith(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> spectatorService.sendToTribune(player));

        assertSame(failure, thrown, "the failure reaches the caller unchanged");
        assertEquals(Optional.empty(), spectatorService.findSpectatorMode(playerUniqueId));
        assertEquals(Set.of(), spectatorService.getSpectators());
        assertEquals(Map.of(), inventoryItemsBySlot, "no item is handed out");
    }

    @Test
    void refusesToBeBuiltWithoutItsDependencies() {
        NullPointerException missingArenaRegistry = assertThrows(NullPointerException.class,
                () -> new SpectatorService(null, spectatorItems));
        NullPointerException missingSpectatorItems = assertThrows(NullPointerException.class,
                () -> new SpectatorService(arenaRegistry, null));

        assertEquals("arenaRegistry", missingArenaRegistry.getMessage());
        assertEquals("spectatorItems", missingSpectatorItems.getMessage());
    }

    private Player createPlayer() {
        PlayerInventory inventory = SharedBukkitServer.proxy(PlayerInventory.class,
                (method, arguments) -> switch (method.getName()) {
                    case "setItem" -> {
                        inventoryItemsBySlot.put((Integer) arguments[0], (ItemStack) arguments[1]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
                });
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> playerUniqueId;
            case "getInventory" -> inventory;
            case "teleport" -> {
                playerLocation = (Location) arguments[0];
                yield true;
            }
            case "setGameMode" -> {
                playerGameMode = (GameMode) arguments[0];
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }
}

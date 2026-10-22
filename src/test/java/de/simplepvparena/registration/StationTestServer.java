package de.simplepvparena.registration;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.permission.ViewPermission;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class StationTestServer {

    static final String WORLD_NAME = "event";

    private static final int INVENTORY_SIZE = 41;

    private final Map<List<Integer>, Material> materialsByPosition = new HashMap<>();
    private final RegistrationList registrationList = new RegistrationList();
    private final RegistrationStations stations = new RegistrationStations();
    private final World world;
    private final Plugin plugin;
    private final RegistrationService registrationService;
    private final RegistrationStationService stationService;

    private boolean tournamentInProgress;
    private int saveCount;

    StationTestServer() {
        SharedBukkitServer.install();
        world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> WORLD_NAME;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
        plugin = SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getName", "namespace" -> "simplepvparena";
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
        registrationService = new RegistrationService(registrationList, () -> tournamentInProgress,
                () -> saveCount++);
        stationService = new RegistrationStationService(plugin, stations, registrationService,
                material -> material.name().endsWith("_BUTTON"), () -> saveCount++,
                material -> new RecordingItemStack(material, 1));
    }

    RegistrationStationService stationService() {
        return stationService;
    }

    RegistrationService registrationService() {
        return registrationService;
    }

    RegistrationList registrationList() {
        return registrationList;
    }

    RegistrationStations stations() {
        return stations;
    }

    int saveCount() {
        return saveCount;
    }

    void startTournament() {
        tournamentInProgress = true;
    }

    World world() {
        return world;
    }

    Block placeBlock(int blockX, int blockY, int blockZ, Material material) {
        materialsByPosition.put(List.of(blockX, blockY, blockZ), material);
        return blockAt(blockX, blockY, blockZ);
    }

    Block blockAt(int blockX, int blockY, int blockZ) {
        return SharedBukkitServer.proxy(Block.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getX" -> blockX;
            case "getY" -> blockY;
            case "getZ" -> blockZ;
            case "getType" -> materialsByPosition.getOrDefault(List.of(blockX, blockY, blockZ), Material.AIR);
            case "getLocation" -> new Location(world, blockX, blockY, blockZ);
            default -> throw new UnsupportedOperationException("Block." + method.getName());
        });
    }

    ItemStack createTool() {
        return stationService.createTool();
    }

    TestPlayer joinPlayer(String playerName, boolean admin) {
        return new TestPlayer(playerName, admin);
    }

    static final class TestPlayer {

        private final UUID uniqueId = UUID.randomUUID();
        private final List<String> messages = new ArrayList<>();
        private final List<String> actionBarTexts = new ArrayList<>();
        private final ItemStack[] contents = new ItemStack[INVENTORY_SIZE];
        private final Player player;

        private boolean sneaking;

        private TestPlayer(String playerName, boolean admin) {
            PlayerInventory inventory = SharedBukkitServer.proxy(PlayerInventory.class,
                    (method, arguments) -> switch (method.getName()) {
                        case "getContents" -> contents.clone();
                        case "getItem" -> contents[(Integer) arguments[0]];
                        case "setItem" -> {
                            contents[(Integer) arguments[0]] = (ItemStack) arguments[1];
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
                    });
            player = SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
                case "getUniqueId" -> uniqueId;
                case "getName" -> playerName;
                case "hasPermission" -> AdminPermission.ADMIN_PERMISSION_NAME.equals(arguments[0]) && admin
                        || ViewPermission.VIEW_PERMISSION_NAME.equals(arguments[0]);
                case "isSneaking" -> sneaking;
                case "getInventory" -> inventory;
                case "sendMessage" -> {
                    messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                    yield null;
                }
                case "sendActionBar" -> {
                    actionBarTexts.add(TournamentMessages.toPlainText((Component) arguments[0]));
                    yield null;
                }
                default -> throw new UnsupportedOperationException("Player." + method.getName());
            });
        }

        Player player() {
            return player;
        }

        UUID uniqueId() {
            return uniqueId;
        }

        List<String> messages() {
            return messages;
        }

        List<String> actionBarTexts() {
            return actionBarTexts;
        }

        void setSneaking(boolean sneaking) {
            this.sneaking = sneaking;
        }

        void putIntoSlot(int slot, ItemStack itemStack) {
            contents[slot] = itemStack;
        }

        ItemStack itemInSlot(int slot) {
            return contents[slot];
        }
    }
}

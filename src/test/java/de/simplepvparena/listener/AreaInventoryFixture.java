package de.simplepvparena.listener;

import de.simplepvparena.arena.AreaInventoryLookup;
import de.simplepvparena.arena.AreaInventoryService;
import de.simplepvparena.arena.AreaInventoryStorage;
import de.simplepvparena.arena.AreaPlayerCondition;
import de.simplepvparena.arena.FileAreaInventoryStorage;
import de.simplepvparena.kit.FilePlayerStateSnapshotStorage;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeItemStackCodec;
import de.simplepvparena.support.FakePlayerPotionEffects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

final class AreaInventoryFixture {

    static final String KEPT_SAFE = "Your inventory is kept safe while you are in the event area, you get it back "
            + "when you leave";
    static final String BACK = "Your inventory is back";
    static final String NOT_GIVEN_BACK = "Your inventory could not be restored, an admin has been informed";
    static final String DROP_LOCKED = "You cannot drop items in the event area";
    static final String ENDER_CHEST_LOCKED = "The ender chest is locked in the event area";

    private final Path dataFolder;
    private final List<String> warnings = new ArrayList<>();
    private final AtomicLong clock = new AtomicLong(1_000_000L);
    private final List<Runnable> saveObservers = new ArrayList<>();
    private final FakePlayerPotionEffects potionEffects = new FakePlayerPotionEffects();
    private final ArenaMatchHarness harness;
    private final FileAreaInventoryStorage fileStorage;
    private final AreaInventoryStorage storage;
    private final World lobbyWorld = ArenaMatchHarness.proxy(World.class, (method, arguments) -> {
        if (method.getName().equals("getName")) {
            return ArenaMatchHarness.LOBBY_WORLD_NAME;
        }
        throw new UnsupportedOperationException("World." + method.getName());
    });

    private AreaInventoryService service;
    private IOException writeFailure;

    AreaInventoryFixture(Path dataFolder) {
        this.dataFolder = dataFolder;
        this.harness = new ArenaMatchHarness(plugin -> new KitService(plugin, new FilePlayerStateSnapshotStorage(
                dataFolder.resolve(FilePlayerStateSnapshotStorage.DIRECTORY_NAME), new FakeItemStackCodec()),
                warnings::add));
        this.fileStorage = new FileAreaInventoryStorage(dataFolder.resolve(FileAreaInventoryStorage.DIRECTORY_NAME),
                new FakeItemStackCodec());
        this.storage = new ObservedStorage();
        this.service = createService();
    }

    AreaInventoryService createService() {
        return new AreaInventoryService(harness.getPlugin(), harness.getArenaRegistry()::findEffectArea, storage,
                harness.getKitService()::hasAppliedKit,
                player -> harness.getMatchService().isActiveFighter(player.getUniqueId()),
                harness.getSpectatorService(), potionEffects, warnings::add, clock::get);
    }

    void restartService() {
        service = createService();
    }

    ArenaMatchHarness harness() {
        return harness;
    }

    AreaInventoryService service() {
        return service;
    }

    FakePlayerPotionEffects potionEffects() {
        return potionEffects;
    }

    KitService kitService() {
        return harness.getKitService();
    }

    List<String> warnings() {
        return warnings;
    }

    void advanceClockMillis(long millis) {
        clock.addAndGet(millis);
    }

    void failWritesWith(IOException failure) {
        this.writeFailure = failure;
    }

    void observeSave(Runnable observer) {
        saveObservers.add(observer);
    }

    Path inventoryFileOf(ArenaMatchHarness.FakePlayer player) {
        return inventoryDirectory().resolve(player.uniqueId() + ".yml");
    }

    Path brokenInventoryFileOf(ArenaMatchHarness.FakePlayer player) {
        return inventoryDirectory().resolve(player.uniqueId() + ".yml.broken");
    }

    Path inventoryDirectory() {
        return dataFolder.resolve(FileAreaInventoryStorage.DIRECTORY_NAME);
    }

    Path matchSnapshotFileOf(ArenaMatchHarness.FakePlayer player) {
        return dataFolder.resolve(FilePlayerStateSnapshotStorage.DIRECTORY_NAME).resolve(player.uniqueId() + ".yml");
    }

    AreaInventoryLookup findSavedInventory(ArenaMatchHarness.FakePlayer player) {
        return fileStorage.find(player.uniqueId());
    }

    Location inside() {
        return harness.at(5.5D, 5.5D);
    }

    Location onTheTribune() {
        return harness.at(ArenaMatchHarness.WALL_MAXIMUM + 5.5D, ArenaMatchHarness.WALL_MAXIMUM + 5.5D);
    }

    Location outside() {
        return harness.at(500.5D, 500.5D);
    }

    Location inTheLobby() {
        return new Location(lobbyWorld, 5.5D, ArenaMatchHarness.GROUND_Y, 5.5D);
    }

    ArenaMatchHarness.FakePlayer addPlayerOutsideWithItems(String playerName) {
        ArenaMatchHarness.FakePlayer player = harness.addPlayer(playerName);
        player.moveTo(outside());
        player.setContents(ownContents());
        player.setLevel(17);
        return player;
    }

    static ItemStack[] ownContents() {
        ItemStack[] contents = new ItemStack[ArenaMatchHarness.INVENTORY_SLOT_COUNT];
        contents[0] = new FakeItemStack("diamond sword");
        contents[8] = new FakeItemStack("golden apple");
        contents[39] = new FakeItemStack("netherite helmet");
        contents[40] = new FakeItemStack("shield");
        return contents;
    }

    static ItemStack[] contentsTakenInside() {
        ItemStack[] contents = new ItemStack[ArenaMatchHarness.INVENTORY_SLOT_COUNT];
        contents[0] = new FakeItemStack("creative command block");
        contents[1] = new FakeItemStack("compass");
        contents[2] = new FakeItemStack("height marker");
        contents[3] = new FakeItemStack("start gate block");
        contents[38] = new FakeItemStack("picked up elytra");
        return contents;
    }

    static ItemStack[] emptyContents() {
        return new ItemStack[ArenaMatchHarness.INVENTORY_SLOT_COUNT];
    }

    private final class ObservedStorage implements AreaInventoryStorage {

        @Override
        public void save(UUID playerUniqueId, ItemStack[] contents, List<ItemStack> looseItems,
                         AreaPlayerCondition condition) throws IOException {
            saveObservers.forEach(Runnable::run);
            if (writeFailure != null) {
                throw writeFailure;
            }
            fileStorage.save(playerUniqueId, contents, looseItems, condition);
        }

        @Override
        public AreaInventoryLookup find(UUID playerUniqueId) {
            return fileStorage.find(playerUniqueId);
        }

        @Override
        public void delete(UUID playerUniqueId) throws IOException {
            fileStorage.delete(playerUniqueId);
        }
    }
}

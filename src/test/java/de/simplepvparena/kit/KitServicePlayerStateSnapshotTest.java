package de.simplepvparena.kit;

import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeItemStackCodec;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitServicePlayerStateSnapshotTest {

    private static final String PLUGIN_NAMESPACE = "simplepvparena";
    private static final Plugin PLUGIN = createPlugin();
    private static final NamespacedKey KIT_APPLIED_KEY = new NamespacedKey(PLUGIN, KitService.KIT_APPLIED_KEY_NAME);

    private final List<String> warnings = new ArrayList<>();

    @Test
    void savesTheStateBeforeTheInventoryIsClearedWhenTheKitIsAppliedForTheFirstTime(@TempDir Path pluginFolder)
            throws IOException {
        Path snapshotDirectory = pluginFolder.resolve(FilePlayerStateSnapshotStorage.DIRECTORY_NAME);
        StatefulPlayer player = playerWithPrivateState("Owner");

        kitServiceIn(snapshotDirectory).applyKit(player.player);

        assertEquals(List.of("getContents", "clear"), player.inventoryEvents,
                "the contents are read before the inventory is cleared");
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        assertTrue(Files.isRegularFile(snapshotFile));
        try (Stream<Path> files = Files.list(snapshotDirectory)) {
            assertEquals(List.of(snapshotFile), files.toList(), "no temporary file is left behind");
        }
        YamlConfiguration storedSnapshot = YamlConfiguration.loadConfiguration(snapshotFile.toFile());
        assertEquals(1, storedSnapshot.getInt("format-version"));
        assertTrue(storedSnapshot.isString("saved-at"));
        assertTrue(storedSnapshot.isString("contents"));
        assertEquals(7, storedSnapshot.getInt("level"));
        assertEquals(0.25, storedSnapshot.getDouble("exp"));
        assertEquals(12, storedSnapshot.getInt("food-level"));
        assertEquals(3.5, storedSnapshot.getDouble("saturation"));
        assertEquals(14.0, storedSnapshot.getDouble("health"));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void keepsTheFirstSnapshotWhenTheKitIsAppliedAgain(@TempDir Path snapshotDirectory) {
        KitService kitService = kitServiceIn(snapshotDirectory);
        StatefulPlayer player = playerWithPrivateState("Owner");
        ItemStack[] privateContents = player.contents.clone();

        kitService.applyKit(player.player);
        player.wearKitState();
        kitService.applyKit(player.player);

        assertEquals(1, player.inventoryEvents.stream().filter("getContents"::equals).count(),
                "the second round reads no contents");
        assertEquals(7, YamlConfiguration.loadConfiguration(snapshotFileOf(snapshotDirectory, player).toFile())
                .getInt("level"));

        kitService.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertEquals(7, player.level);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void restoresTheStateAndForgetsTheSnapshotWhenTheKitIsCleared(@TempDir Path snapshotDirectory) {
        KitService kitService = kitServiceIn(snapshotDirectory);
        StatefulPlayer player = playerWithPrivateState("Owner");
        ItemStack[] privateContents = player.contents.clone();

        kitService.applyKit(player.player);
        player.wearKitState();
        kitService.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertEquals(7, player.level);
        assertEquals(0.25f, player.exp);
        assertEquals(12, player.foodLevel);
        assertEquals(3.5f, player.saturation);
        assertEquals(14.0, player.health);
        assertFalse(kitService.hasAppliedKit(player.player));
        assertFalse(Files.exists(snapshotFileOf(snapshotDirectory, player)));
        assertTrue(warnings.isEmpty());

        player.persistentData.put(KIT_APPLIED_KEY, (byte) 1);
        kitService.clearKit(player.player);

        assertArrayEquals(new ItemStack[privateContents.length], player.contents,
                "the snapshot is gone from memory as well");
        assertEquals(1, warnings.size());
    }

    @Test
    void leavesAPlayerWithoutTheMarkerAndWithoutASnapshotUntouched(@TempDir Path snapshotDirectory) {
        StatefulPlayer player = playerWithPrivateState("Visitor");
        ItemStack[] privateContents = player.contents.clone();

        kitServiceIn(snapshotDirectory).clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertEquals(List.of(), player.inventoryEvents, "the inventory is not touched");
        assertEquals(7, player.level);
        assertEquals(0.25f, player.exp);
        assertEquals(12, player.foodLevel);
        assertEquals(3.5f, player.saturation);
        assertEquals(14.0, player.health);
        assertFalse(Files.exists(snapshotFileOf(snapshotDirectory, player)));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void restoresAPlayerWithoutTheMarkerFromAReadableFile(@TempDir Path snapshotDirectory) {
        StatefulPlayer player = playerWithPrivateState("Forgotten");
        ItemStack[] privateContents = player.contents.clone();
        kitServiceIn(snapshotDirectory).applyKit(player.player);
        player.wearKitState();
        player.persistentData.clear();

        KitService kitServiceAfterARestart = kitServiceIn(snapshotDirectory);
        kitServiceAfterARestart.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertEquals(7, player.level);
        assertEquals(0.25f, player.exp);
        assertEquals(12, player.foodLevel);
        assertEquals(3.5f, player.saturation);
        assertEquals(14.0, player.health);
        assertFalse(Files.exists(snapshotFileOf(snapshotDirectory, player)));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void keepsTheSnapshotAndTheMarkerWhenTheRestoreFails(@TempDir Path snapshotDirectory) throws IOException {
        KitService kitService = kitServiceIn(snapshotDirectory);
        StatefulPlayer player = playerWithPrivateState("Stubborn");
        ItemStack[] privateContents = player.contents.clone();
        kitService.applyKit(player.player);
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        String storedContent = Files.readString(snapshotFile, StandardCharsets.UTF_8);
        player.wearKitState();
        player.setContentsFailure = new IllegalArgumentException("the slot count does not match");

        kitService.clearKit(player.player);

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("Stubborn"), warnings.get(0));
        assertTrue(warnings.get(0).contains("the slot count does not match"), warnings.get(0));
        assertTrue(kitService.hasAppliedKit(player.player), "the marker is kept for the next join");
        assertEquals(storedContent, Files.readString(snapshotFile, StandardCharsets.UTF_8),
                "the file is kept unchanged");

        Files.delete(snapshotFile);
        player.setContentsFailure = null;
        kitService.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents, "the snapshot in memory is kept as well");
        assertEquals(7, player.level);
        assertEquals(14.0, player.health);
        assertFalse(kitService.hasAppliedKit(player.player));
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void restoresFromTheKeptFileAfterAFailedRestoreAndARestart(@TempDir Path snapshotDirectory) {
        StatefulPlayer player = playerWithPrivateState("Stubborn");
        ItemStack[] privateContents = player.contents.clone();
        KitService kitService = kitServiceIn(snapshotDirectory);
        kitService.applyKit(player.player);
        player.wearKitState();
        player.setContentsFailure = new IllegalArgumentException("the slot count does not match");
        kitService.clearKit(player.player);
        player.setContentsFailure = null;

        KitService kitServiceAfterARestart = kitServiceIn(snapshotDirectory);
        kitServiceAfterARestart.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertEquals(7, player.level);
        assertFalse(kitServiceAfterARestart.hasAppliedKit(player.player));
        assertFalse(Files.exists(snapshotFileOf(snapshotDirectory, player)));
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void restoresFromTheFileAfterARestart(@TempDir Path snapshotDirectory) {
        StatefulPlayer player = playerWithPrivateState("Owner");
        ItemStack[] privateContents = player.contents.clone();
        kitServiceIn(snapshotDirectory).applyKit(player.player);
        player.wearKitState();

        KitService kitServiceAfterARestart = kitServiceIn(snapshotDirectory);
        kitServiceAfterARestart.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertEquals(7, player.level);
        assertEquals(0.25f, player.exp);
        assertEquals(12, player.foodLevel);
        assertEquals(3.5f, player.saturation);
        assertEquals(14.0, player.health);
        assertFalse(kitServiceAfterARestart.hasAppliedKit(player.player));
        assertFalse(Files.exists(snapshotFileOf(snapshotDirectory, player)));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void clearsTheKitAndWarnsWhenNoSnapshotExists(@TempDir Path snapshotDirectory) {
        StatefulPlayer player = playerWithPrivateState("Crashed");
        player.persistentData.put(KIT_APPLIED_KEY, (byte) 1);
        int slotCount = player.contents.length;

        KitService kitService = kitServiceIn(snapshotDirectory);
        kitService.clearKit(player.player);

        assertArrayEquals(new ItemStack[slotCount], player.contents);
        assertEquals(0, player.level);
        assertFalse(kitService.hasAppliedKit(player.player));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("Crashed"), warnings.get(0));
    }

    @Test
    void movesAnUnreadableFileAsideClearsTheKitAndNamesTheFile(@TempDir Path snapshotDirectory) throws IOException {
        StatefulPlayer player = playerWithPrivateState("Broken");
        player.persistentData.put(KIT_APPLIED_KEY, (byte) 1);
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        Files.createDirectories(snapshotDirectory);
        Files.writeString(snapshotFile, "format-version: [1\nlevel: 3\n", StandardCharsets.UTF_8);

        assertBrokenFileIsMovedAside(player, snapshotDirectory, "format-version: [1\nlevel: 3\n");
    }

    @Test
    void keepsAnOlderBrokenFileAndGivesTheNewOneATimeMark(@TempDir Path snapshotDirectory) throws IOException {
        StatefulPlayer player = playerWithPrivateState("Broken");
        player.persistentData.put(KIT_APPLIED_KEY, (byte) 1);
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        Path olderBrokenFile = snapshotDirectory.resolve(snapshotFile.getFileName() + ".broken");
        Files.createDirectories(snapshotDirectory);
        Files.writeString(snapshotFile, "format-version: [1\nlevel: 3\n", StandardCharsets.UTF_8);
        Files.writeString(olderBrokenFile, "older broken file", StandardCharsets.UTF_8);

        kitServiceIn(snapshotDirectory).clearKit(player.player);

        assertFalse(Files.exists(snapshotFile));
        assertEquals("older broken file", Files.readString(olderBrokenFile, StandardCharsets.UTF_8));
        String timeMarkedName = snapshotFile.getFileName() + "\\.\\d+\\.broken";
        List<Path> newerBrokenFiles;
        try (Stream<Path> files = Files.list(snapshotDirectory)) {
            newerBrokenFiles = files.filter(file -> file.getFileName().toString().matches(timeMarkedName)).toList();
        }
        assertEquals(1, newerBrokenFiles.size(), newerBrokenFiles.toString());
        assertEquals("format-version: [1\nlevel: 3\n",
                Files.readString(newerBrokenFiles.get(0), StandardCharsets.UTF_8));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains(newerBrokenFiles.get(0).getFileName().toString()), warnings.get(0));
    }

    @Test
    void movesAFileWithAMissingFieldAsideClearsTheKitAndNamesTheFile(@TempDir Path snapshotDirectory)
            throws IOException {
        StatefulPlayer player = playerWithPrivateState("Broken");
        kitServiceIn(snapshotDirectory).applyKit(player.player);
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        YamlConfiguration storedSnapshot = YamlConfiguration.loadConfiguration(snapshotFile.toFile());
        storedSnapshot.set("health", null);
        String contentWithoutHealth = storedSnapshot.saveToString();
        Files.writeString(snapshotFile, contentWithoutHealth, StandardCharsets.UTF_8);

        assertBrokenFileIsMovedAside(player, snapshotDirectory, contentWithoutHealth);
    }

    @Test
    void leavesAPlayerWithoutTheMarkerUntouchedAndWarnsWhenTheFileIsUnreadable(@TempDir Path snapshotDirectory)
            throws IOException {
        StatefulPlayer player = playerWithPrivateState("Visitor");
        ItemStack[] privateContents = player.contents.clone();
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        Path brokenFile = snapshotDirectory.resolve(snapshotFile.getFileName() + ".broken");
        String brokenContent = "format-version: [1\nlevel: 3\n";
        Files.createDirectories(snapshotDirectory);
        Files.writeString(snapshotFile, brokenContent, StandardCharsets.UTF_8);
        KitService kitService = kitServiceIn(snapshotDirectory);

        kitService.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertEquals(List.of(), player.inventoryEvents, "the inventory is not touched");
        assertEquals(7, player.level);
        assertEquals(0.25f, player.exp);
        assertEquals(12, player.foodLevel);
        assertEquals(3.5f, player.saturation);
        assertEquals(14.0, player.health);
        assertFalse(kitService.hasAppliedKit(player.player));
        assertFalse(Files.exists(snapshotFile));
        assertEquals(brokenContent, Files.readString(brokenFile, StandardCharsets.UTF_8));
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("Visitor"), warnings.get(0));
        assertTrue(warnings.get(0).contains(brokenFile.getFileName().toString()), warnings.get(0));
        assertTrue(warnings.get(0).contains("nothing was cleared"), warnings.get(0));
    }

    @Test
    void setsTheFileAsideAsRestoredWhenItCannotBeDeletedAfterTheRestore(@TempDir Path snapshotDirectory)
            throws IOException {
        KitService kitService = new KitService(PLUGIN, new DeleteFailingSnapshotStorage(snapshotDirectory, null),
                warnings::add);
        StatefulPlayer player = playerWithPrivateState("Locked");
        ItemStack[] privateContents = player.contents.clone();
        kitService.applyKit(player.player);
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        Path restoredFile = snapshotDirectory.resolve(snapshotFile.getFileName() + ".restored");
        String storedContent = Files.readString(snapshotFile, StandardCharsets.UTF_8);
        Files.writeString(restoredFile, "older restored file", StandardCharsets.UTF_8);
        player.wearKitState();

        kitService.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertEquals(7, player.level);
        assertFalse(kitService.hasAppliedKit(player.player));
        assertFalse(Files.exists(snapshotFile));
        assertEquals(storedContent, Files.readString(restoredFile, StandardCharsets.UTF_8),
                "an older restored file is replaced");
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("Locked"), warnings.get(0));
        assertTrue(warnings.get(0).contains(DeleteFailingSnapshotStorage.DELETE_FAILURE_MESSAGE), warnings.get(0));
        assertTrue(warnings.get(0).contains(".restored"), warnings.get(0));

        player.contents[1] = new FakeItemStack("golden apple");
        player.level = 9;
        ItemStack[] currentContents = player.contents.clone();
        player.inventoryEvents.clear();

        kitService.clearKit(player.player);
        kitServiceIn(snapshotDirectory).clearKit(player.player);

        assertArrayEquals(currentContents, player.contents);
        assertEquals(List.of(), player.inventoryEvents, "the inventory is not touched");
        assertEquals(9, player.level);
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void warnsWithTheFailureAndForgetsTheSnapshotInMemoryWhenTheFileCanNeitherBeDeletedNorSetAside(
            @TempDir Path snapshotDirectory) throws IOException {
        KitService kitService = new KitService(PLUGIN, new DeleteFailingSnapshotStorage(snapshotDirectory,
                new IOException("the folder is read only")), warnings::add);
        StatefulPlayer player = playerWithPrivateState("Locked");
        ItemStack[] privateContents = player.contents.clone();
        kitService.applyKit(player.player);
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        player.wearKitState();

        kitService.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertTrue(Files.exists(snapshotFile), "the file stays where it is");
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("Locked"), warnings.get(0));
        assertTrue(warnings.get(0).contains(DeleteFailingSnapshotStorage.DELETE_FAILURE_MESSAGE), warnings.get(0));
        assertFalse(warnings.get(0).contains(".restored"), warnings.get(0));

        Files.delete(snapshotFile);
        player.inventoryEvents.clear();
        kitService.clearKit(player.player);

        assertEquals(List.of(), player.inventoryEvents, "the snapshot is gone from memory as well");
        assertArrayEquals(privateContents, player.contents);
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void forgetsTheSnapshotInMemoryWhenDeletingTheFileThrowsAnUnexpectedException(@TempDir Path snapshotDirectory)
            throws IOException {
        KitService kitService = new KitService(PLUGIN, new DeleteCrashingSnapshotStorage(snapshotDirectory),
                warnings::add);
        StatefulPlayer player = playerWithPrivateState("Crashing");
        ItemStack[] privateContents = player.contents.clone();
        kitService.applyKit(player.player);
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        player.wearKitState();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> kitService.clearKit(player.player), "the unexpected failure still reaches the caller");

        assertEquals(DeleteCrashingSnapshotStorage.DELETE_FAILURE_MESSAGE, failure.getMessage());
        assertArrayEquals(privateContents, player.contents);
        assertTrue(warnings.isEmpty(), warnings.toString());

        Files.delete(snapshotFile);
        player.inventoryEvents.clear();
        kitService.clearKit(player.player);

        assertEquals(List.of(), player.inventoryEvents, "the snapshot is gone from memory as well");
        assertArrayEquals(privateContents, player.contents);
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void keepsTheSnapshotInMemoryWhenTheFileCannotBeWritten(@TempDir Path pluginFolder) throws IOException {
        Path snapshotDirectory = pluginFolder.resolve(FilePlayerStateSnapshotStorage.DIRECTORY_NAME);
        Files.writeString(snapshotDirectory, "a file where the directory belongs", StandardCharsets.UTF_8);
        KitService kitService = kitServiceIn(snapshotDirectory);
        StatefulPlayer player = playerWithPrivateState("Unlucky");
        ItemStack[] privateContents = player.contents.clone();

        kitService.applyKit(player.player);

        assertTrue(kitService.hasAppliedKit(player.player), "the kit is applied anyway");
        assertArrayEquals(new ItemStack[privateContents.length], player.contents);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("Unlucky"), warnings.get(0));

        player.wearKitState();
        kitService.clearKit(player.player);

        assertArrayEquals(privateContents, player.contents);
        assertEquals(7, player.level);
        assertEquals(14.0, player.health);
        assertFalse(kitService.hasAppliedKit(player.player));
        assertEquals(1, warnings.size(), "restoring from memory needs no further warning");
    }

    @Test
    void limitsTheRestoredHealthToTheCurrentMaximum(@TempDir Path snapshotDirectory) {
        KitService kitService = kitServiceIn(snapshotDirectory);
        StatefulPlayer player = playerWithPrivateState("Owner");
        player.health = 18.0;

        kitService.applyKit(player.player);
        player.maximumHealth = 10.0;
        kitService.clearKit(player.player);

        assertEquals(10.0, player.health);
    }

    @Test
    void neverRestoresTheHealthBelowOne(@TempDir Path snapshotDirectory) {
        KitService kitService = kitServiceIn(snapshotDirectory);
        StatefulPlayer player = playerWithPrivateState("Owner");
        player.health = 0.0;

        kitService.applyKit(player.player);
        kitService.clearKit(player.player);

        assertEquals(1.0, player.health);
    }

    private void assertBrokenFileIsMovedAside(StatefulPlayer player, Path snapshotDirectory, String brokenContent)
            throws IOException {
        Path snapshotFile = snapshotFileOf(snapshotDirectory, player);
        Path brokenFile = snapshotDirectory.resolve(snapshotFile.getFileName() + ".broken");
        int slotCount = player.contents.length;
        KitService kitService = kitServiceIn(snapshotDirectory);

        kitService.clearKit(player.player);

        assertFalse(Files.exists(snapshotFile));
        assertEquals(brokenContent, Files.readString(brokenFile, StandardCharsets.UTF_8));
        assertArrayEquals(new ItemStack[slotCount], player.contents);
        assertFalse(kitService.hasAppliedKit(player.player));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("Broken"), warnings.get(0));
        assertTrue(warnings.get(0).contains(brokenFile.getFileName().toString()), warnings.get(0));
    }

    private KitService kitServiceIn(Path snapshotDirectory) {
        return new KitService(PLUGIN, new FilePlayerStateSnapshotStorage(snapshotDirectory, new FakeItemStackCodec()),
                warnings::add);
    }

    private static Path snapshotFileOf(Path snapshotDirectory, StatefulPlayer player) {
        return snapshotDirectory.resolve(player.uniqueId + ".yml");
    }

    private static StatefulPlayer playerWithPrivateState(String playerName) {
        StatefulPlayer player = new StatefulPlayer(playerName);
        player.contents[0] = new FakeItemStack("diamond sword");
        player.contents[5] = new FakeItemStack("bread");
        player.contents[39] = new FakeItemStack("netherite helmet");
        player.contents[40] = new FakeItemStack("shield");
        player.level = 7;
        player.exp = 0.25f;
        player.foodLevel = 12;
        player.saturation = 3.5f;
        player.health = 14.0;
        return player;
    }

    private static Plugin createPlugin() {
        return SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getName", "namespace" -> PLUGIN_NAMESPACE;
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    private static final class DeleteFailingSnapshotStorage implements PlayerStateSnapshotStorage {

        private static final String DELETE_FAILURE_MESSAGE = "the file is locked";

        private final FilePlayerStateSnapshotStorage fileStorage;
        private final IOException setAsideFailure;

        private DeleteFailingSnapshotStorage(Path snapshotDirectory, IOException setAsideFailure) {
            this.fileStorage = new FilePlayerStateSnapshotStorage(snapshotDirectory, new FakeItemStackCodec());
            this.setAsideFailure = setAsideFailure;
        }

        @Override
        public void save(UUID playerUniqueId, PlayerStateSnapshot snapshot) throws IOException {
            fileStorage.save(playerUniqueId, snapshot);
        }

        @Override
        public PlayerStateSnapshotLookup find(UUID playerUniqueId) {
            return fileStorage.find(playerUniqueId);
        }

        @Override
        public void delete(UUID playerUniqueId) throws IOException {
            throw new IOException(DELETE_FAILURE_MESSAGE);
        }

        @Override
        public void setAsideAsRestored(UUID playerUniqueId) throws IOException {
            if (setAsideFailure != null) {
                throw setAsideFailure;
            }
            fileStorage.setAsideAsRestored(playerUniqueId);
        }
    }

    private static final class DeleteCrashingSnapshotStorage implements PlayerStateSnapshotStorage {

        private static final String DELETE_FAILURE_MESSAGE = "the storage broke down";

        private final FilePlayerStateSnapshotStorage fileStorage;

        private DeleteCrashingSnapshotStorage(Path snapshotDirectory) {
            this.fileStorage = new FilePlayerStateSnapshotStorage(snapshotDirectory, new FakeItemStackCodec());
        }

        @Override
        public void save(UUID playerUniqueId, PlayerStateSnapshot snapshot) throws IOException {
            fileStorage.save(playerUniqueId, snapshot);
        }

        @Override
        public PlayerStateSnapshotLookup find(UUID playerUniqueId) {
            return fileStorage.find(playerUniqueId);
        }

        @Override
        public void delete(UUID playerUniqueId) {
            throw new IllegalStateException(DELETE_FAILURE_MESSAGE);
        }

        @Override
        public void setAsideAsRestored(UUID playerUniqueId) throws IOException {
            fileStorage.setAsideAsRestored(playerUniqueId);
        }
    }

    private static final class StatefulPlayer {

        private static final int SLOT_COUNT = 43;

        private final UUID uniqueId = UUID.randomUUID();
        private final Map<Object, Object> persistentData = new LinkedHashMap<>();
        private final List<String> inventoryEvents = new ArrayList<>();
        private final Player player;

        private ItemStack[] contents = new ItemStack[SLOT_COUNT];
        private int level;
        private float exp;
        private int foodLevel;
        private float saturation;
        private double health;
        private double maximumHealth = 20.0;
        private RuntimeException setContentsFailure;

        private StatefulPlayer(String playerName) {
            PlayerInventory inventory = SharedBukkitServer.proxy(PlayerInventory.class,
                    (method, arguments) -> switch (method.getName()) {
                        case "getContents" -> {
                            inventoryEvents.add("getContents");
                            yield contents.clone();
                        }
                        case "setContents" -> {
                            inventoryEvents.add("setContents");
                            if (setContentsFailure != null) {
                                throw setContentsFailure;
                            }
                            contents = ((ItemStack[]) arguments[0]).clone();
                            yield null;
                        }
                        case "clear" -> {
                            inventoryEvents.add("clear");
                            contents = new ItemStack[SLOT_COUNT];
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
                    });
            AttributeInstance maximumHealthAttribute = SharedBukkitServer.proxy(AttributeInstance.class,
                    (method, arguments) -> switch (method.getName()) {
                        case "getValue" -> maximumHealth;
                        default -> throw new UnsupportedOperationException("AttributeInstance." + method.getName());
                    });
            this.player = SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
                case "getUniqueId" -> uniqueId;
                case "getName" -> playerName;
                case "getInventory" -> inventory;
                case "getLevel" -> level;
                case "setLevel" -> {
                    level = (Integer) arguments[0];
                    yield null;
                }
                case "getExp" -> exp;
                case "setExp" -> {
                    exp = (Float) arguments[0];
                    yield null;
                }
                case "getFoodLevel" -> foodLevel;
                case "setFoodLevel" -> {
                    foodLevel = (Integer) arguments[0];
                    yield null;
                }
                case "getSaturation" -> saturation;
                case "setSaturation" -> {
                    saturation = (Float) arguments[0];
                    yield null;
                }
                case "getHealth" -> health;
                case "setHealth" -> {
                    health = (Double) arguments[0];
                    yield null;
                }
                case "getAttribute" -> maximumHealthAttribute;
                case "getActivePotionEffects" -> List.of();
                case "getMaximumAir" -> 300;
                case "getCooldown" -> 0;
                case "getPersistentDataContainer" -> SharedBukkitServer.createPersistentDataContainer(persistentData);
                case "setGameMode", "setFireTicks", "removePotionEffect", "setAbsorptionAmount", "setArrowsInBody",
                     "setFreezeTicks", "setRemainingAir", "setFallDistance", "setVelocity", "setCooldown" -> null;
                default -> throw new UnsupportedOperationException("Player." + method.getName());
            });
        }

        private void wearKitState() {
            ItemStack[] kitContents = new ItemStack[SLOT_COUNT];
            Arrays.fill(kitContents, 0, 3, new FakeItemStack("kit arrow"));
            contents = kitContents;
            level = 0;
            exp = 0.0f;
            foodLevel = 4;
            saturation = 0.0f;
            health = 3.0;
        }
    }
}

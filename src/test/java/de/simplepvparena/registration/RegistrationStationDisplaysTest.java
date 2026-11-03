package de.simplepvparena.registration;

import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationStationDisplaysTest {

    private static final String WORLD_NAME = "event";
    private static final RegistrationStation STATION = new RegistrationStation(WORLD_NAME, 10, 64, 20);
    private static final RegistrationStation NEIGHBOUR_STATION = new RegistrationStation(WORLD_NAME, 12, 70, 25);
    private static final RegistrationStation FAR_STATION = new RegistrationStation(WORLD_NAME, 100, 64, -40);
    private static final RegistrationStation OTHER_WORLD_STATION = new RegistrationStation("nether", 10, 64, 20);
    private static final FakeItemStack STATION_ITEM = new FakeItemStack("station item");

    private final RegistrationStations stations = new RegistrationStations();
    private final Set<List<Integer>> loadedChunks = new HashSet<>();
    private final World world = SharedBukkitServer.proxy(World.class, (method, arguments) ->
            switch (method.getName()) {
                case "getName" -> WORLD_NAME;
                case "isChunkLoaded" -> loadedChunks.contains(List.of((Integer) arguments[0], (Integer) arguments[1]));
                case "getBlockAt" -> SharedBukkitServer.proxy(Block.class, (blockMethod, blockArguments) -> {
                    throw new UnsupportedOperationException("Block." + blockMethod.getName());
                });
                default -> throw new UnsupportedOperationException("World." + method.getName());
            });
    private final RecordingDisplayEntities displayEntities = new RecordingDisplayEntities();
    private final AtomicReference<Optional<ItemStack>> stationItem = new AtomicReference<>(Optional.empty());
    private final AtomicReference<Optional<String>> titleImage = new AtomicReference<>(Optional.empty());
    private final AtomicInteger registeredCount = new AtomicInteger(3);
    private final AtomicBoolean visualsReady = new AtomicBoolean(true);

    @Test
    void theStartShowsAButtonAClickAreaAndATextAboveEveryStationInALoadedChunkOnly() {
        stations.add(STATION);
        stations.add(FAR_STATION);
        stations.add(OTHER_WORLD_STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();

        displays.showAll();

        assertEquals(List.of("base", "cap", "click", "text"), displayEntities.kinds());
        assertEquals(new Location(world, 10.5D, 65.15D, 20.5D), displayEntities.ofKind("cap").location());
        assertEquals(new Location(world, 10.5D, 65.15D, 20.5D), displayEntities.ofKind("click").location());
        assertEquals(new Location(world, 10.5D, 65.9D, 20.5D), displayEntities.ofKind("text").location());
        assertEquals("Register\nPress the button!\n3 players registered",
                displayEntities.ofKind("text").plainText());
    }

    @Test
    void theTextHasABoldTitleInThePrefixColourAHintAndTheCount() {
        stations.add(STATION);
        loadChunk(0, 1);

        createDisplays().showAll();

        List<Component> lines = displayEntities.ofKind("text").text().children();
        assertEquals(5, lines.size(), lines.toString());
        assertEquals(TournamentMessages.PREFIX_COLOUR, lines.get(0).color());
        assertTrue(lines.get(0).hasDecoration(TextDecoration.BOLD));
        assertEquals(TournamentMessages.DETAIL_COLOUR, lines.get(2).color());
        assertFalse(lines.get(2).hasDecoration(TextDecoration.BOLD));
    }

    @Test
    void aKnownTitleImageReplacesTheTitleText() {
        titleImage.set(Optional.of(""));
        stations.add(STATION);
        loadChunk(0, 1);

        createDisplays().showAll();

        assertEquals("\nPress the button!\n3 players registered",
                displayEntities.ofKind("text").plainText());
    }

    @Test
    @UseLanguage("de")
    void theTextIsGermanWithGermanSettings() {
        registeredCount.set(1);
        stations.add(STATION);
        loadChunk(0, 1);

        createDisplays().showAll();

        assertEquals("Anmelden\nDrück den Knopf!\n1 Spieler angemeldet",
                displayEntities.ofKind("text").plainText());
    }

    @Test
    void aKnownStationItemCoversTheButtonInsteadOfTheBigButtonAndIsPressedWithIt() {
        stationItem.set(Optional.of(STATION_ITEM));
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();

        displays.showAll();
        displays.stationUsed(STATION);

        assertEquals(List.of("cover", "text"), displayEntities.kinds());
        assertEquals(STATION_ITEM, displayEntities.ofKind("cover").itemStack());
        assertEquals(List.of(displayEntities.ofKind("cover").entity()), displayEntities.pressed);
        assertEquals(Optional.empty(), displays.findStationOfClickArea(displayEntities.ofKind("cover").entity()));
    }

    @Test
    void theCounterChangesTheTextOnlyWhenTheCountChanged() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();

        displays.updateCounters();
        assertEquals(0, displayEntities.textChanges);

        registeredCount.set(4);
        displays.updateCounters();

        assertEquals(1, displayEntities.textChanges);
        assertTrue(displayEntities.ofKind("text").plainText().endsWith("4 players registered"));
    }

    @Test
    void usingAStationPressesItsButtonAndRefreshesTheCount() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();
        registeredCount.set(4);

        displays.stationUsed(STATION);
        displays.stationUsed(FAR_STATION);

        assertEquals(List.of(displayEntities.ofKind("cap").entity()), displayEntities.pressed);
        assertTrue(displayEntities.ofKind("text").plainText().endsWith("4 players registered"));
    }

    @Test
    void theClickAreaLeadsBackToItsStation() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();

        assertEquals(Optional.of(STATION), displays.findStationOfClickArea(displayEntities.ofKind("click").entity()));
        assertEquals(Optional.empty(), displays.findStationOfClickArea(displayEntities.ofKind("text").entity()));
    }

    @Test
    void loadingAChunkShowsTheStationsOfThatChunkOnly() {
        stations.add(STATION);
        stations.add(NEIGHBOUR_STATION);
        stations.add(FAR_STATION);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();
        assertEquals(List.of(), displayEntities.spawned);

        loadChunk(0, 1);
        displays.showChunk(chunk(0, 1));

        assertEquals(List.of(new Location(world, 10.5D, 65.9D, 20.5D), new Location(world, 12.5D, 71.9D, 25.5D)),
                displayEntities.spawned.stream().filter(display -> display.kind().equals("text"))
                        .map(FakeDisplay::location).toList());
    }

    @Test
    void addingAStationShowsItsDisplaysAndRemovingItTakesThemAway() {
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();

        stations.add(STATION);
        displays.stationAdded(STATION);
        assertEquals(4, displayEntities.spawned.size());
        assertTrue(displayEntities.spawned.stream().noneMatch(FakeDisplay::removed));

        stations.remove(STATION);
        displays.stationRemoved(STATION);
        assertTrue(displayEntities.spawned.stream().allMatch(FakeDisplay::removed));
    }

    @Test
    void aStationAddedInAnUnloadedChunkGetsItsDisplaysWhenTheChunkLoads() {
        RegistrationStationDisplays displays = createDisplays();
        stations.add(STATION);
        displays.stationAdded(STATION);
        assertEquals(List.of(), displayEntities.spawned);

        loadChunk(0, 1);
        displays.showChunk(chunk(0, 1));

        assertEquals(4, displayEntities.spawned.size());
    }

    @Test
    void unloadingAChunkRemovesTheDisplaysOfThatChunkOnly() {
        stations.add(STATION);
        stations.add(FAR_STATION);
        loadChunk(0, 1);
        loadChunk(6, -3);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();

        displays.hideChunk(chunk(0, 1));

        assertTrue(displayEntities.spawned.subList(0, 4).stream().allMatch(FakeDisplay::removed));
        assertTrue(displayEntities.spawned.subList(4, 8).stream().noneMatch(FakeDisplay::removed));
    }

    @Test
    void theShutdownRemovesEveryDisplay() {
        stations.add(STATION);
        stations.add(FAR_STATION);
        loadChunk(0, 1);
        loadChunk(6, -3);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();

        displays.hideAll();

        assertEquals(8, displayEntities.spawned.size());
        assertTrue(displayEntities.spawned.stream().allMatch(FakeDisplay::removed));
    }

    @Test
    void aStationHasAtMostOneSetOfDisplays() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();

        displays.showAll();
        displays.showAll();
        displays.stationAdded(STATION);
        displays.showChunk(chunk(0, 1));

        assertEquals(16, displayEntities.spawned.size());
        assertEquals(4, displayEntities.spawned.stream().filter(display -> !display.removed()).count());
    }

    @Test
    void theRefreshAfterAReloadRebuildsEveryDisplayWithTheNewItem() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();

        stationItem.set(Optional.of(STATION_ITEM));
        displays.refreshAll();

        assertTrue(displayEntities.spawned.subList(0, 4).stream().allMatch(FakeDisplay::removed));
        assertFalse(displayEntities.ofKind("cover").removed());
    }

    @Test
    @UseLanguage("en")
    void theRefreshAfterAReloadPicksUpAChangedLanguage() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();

        Messages.use(MessageCatalogLoader.shippedCatalog("de"));
        displays.refreshAll();

        List<FakeDisplay> texts = displayEntities.spawned.stream()
                .filter(display -> display.kind().equals("text")).toList();
        assertTrue(texts.get(0).plainText().startsWith("Register\n"));
        assertTrue(texts.get(1).plainText().startsWith("Anmelden\n"));
        assertTrue(texts.get(0).removed());
    }

    @Test
    void itemsAdderGettingReadyAfterThePluginRebuildsEveryDisplayOnce() {
        visualsReady.set(false);
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();
        displays.refreshOnceReady();
        assertEquals(4, displayEntities.spawned.size(), "nothing changes while ItemsAdder still loads");

        visualsReady.set(true);
        stationItem.set(Optional.of(STATION_ITEM));
        displays.refreshOnceReady();
        displays.refreshOnceReady();

        assertEquals(6, displayEntities.spawned.size());
        assertTrue(displayEntities.spawned.get(0).removed());
        assertFalse(displayEntities.ofKind("cover").removed());
    }

    @Test
    void visualsThatAreReadyFromTheStartNeverTriggerARebuild() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();

        displays.refreshOnceReady();

        assertEquals(4, displayEntities.spawned.size());
    }

    @Test
    void loadingAChunkRemovesMarkedDisplaysThatBelongToNoStationAndKeepsEverythingElse() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();
        FakeDisplay ownDisplay = displayEntities.ofKind("text");
        FakeDisplay leftOver = displayEntities.foreign(true);
        FakeDisplay otherEntity = displayEntities.foreign(false);

        displays.showChunk(chunk(0, 1, ownDisplay.entity(), leftOver.entity(), otherEntity.entity()));

        assertTrue(leftOver.removed());
        assertFalse(otherEntity.removed());
        assertTrue(ownDisplay.removed(), "the own display is replaced by a new one");
        assertEquals(4, displayEntities.spawned.stream().filter(display -> !display.removed()).count());
    }

    @Test
    void loadingAChunkWithoutStationsStillRemovesLeftOverDisplays() {
        RegistrationStationDisplays displays = createDisplays();
        FakeDisplay leftOver = displayEntities.foreign(true);

        displays.showChunk(chunk(3, 3, leftOver.entity()));

        assertTrue(leftOver.removed());
        assertEquals(List.of(), displayEntities.spawned);
    }

    private RegistrationStationDisplays createDisplays() {
        return new RegistrationStationDisplays(stations, worldName -> WORLD_NAME.equals(worldName) ? world : null,
                displayEntities, stationItem::get, titleImage::get, registeredCount::get, visualsReady::get);
    }

    private void loadChunk(int chunkX, int chunkZ) {
        loadedChunks.add(List.of(chunkX, chunkZ));
    }

    private Chunk chunk(int chunkX, int chunkZ, Entity... entities) {
        return SharedBukkitServer.proxy(Chunk.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getX" -> chunkX;
            case "getZ" -> chunkZ;
            case "getEntities" -> entities.clone();
            default -> throw new UnsupportedOperationException("Chunk." + method.getName());
        });
    }

    private static final class RecordingDisplayEntities implements StationDisplayEntities {

        private final List<FakeDisplay> spawned = new ArrayList<>();
        private final Set<UUID> markedEntities = new HashSet<>();
        private final List<Entity> pressed = new ArrayList<>();

        private int textChanges;

        @Override
        public Entity spawnItemDisplay(Location location, ItemStack itemStack) {
            return record(new FakeDisplay("item", location, null, itemStack));
        }

        @Override
        public Entity spawnTextDisplay(Location location, Component text) {
            return record(new FakeDisplay("text", location, text, null));
        }

        @Override
        public List<Entity> spawnButton(Location location) {
            return List.of(record(new FakeDisplay("base", location, null, null)),
                    record(new FakeDisplay("cap", location, null, null)));
        }

        @Override
        public Entity spawnClickArea(Location location) {
            return record(new FakeDisplay("click", location, null, null));
        }

        @Override
        public Entity spawnButtonCover(Block buttonBlock, ItemStack image) {
            return record(new FakeDisplay("cover", null, null, image));
        }

        @Override
        public void changeText(Entity display, Component text) {
            textChanges++;
            spawned.stream().filter(fake -> fake.entity() == display).findFirst().orElseThrow().text = text;
        }

        @Override
        public void pressButton(Entity cap) {
            pressed.add(cap);
        }

        @Override
        public boolean isStationDisplay(Entity entity) {
            return markedEntities.contains(entity.getUniqueId());
        }

        private List<String> kinds() {
            return spawned.stream().map(FakeDisplay::kind).toList();
        }

        private FakeDisplay ofKind(String kind) {
            return spawned.stream().filter(display -> display.kind().equals(kind)).reduce((first, second) -> second)
                    .orElseThrow();
        }

        private FakeDisplay foreign(boolean marked) {
            FakeDisplay display = new FakeDisplay("foreign", null, null, null);
            if (marked) {
                markedEntities.add(display.uniqueId);
            }
            return display;
        }

        private Entity record(FakeDisplay display) {
            spawned.add(display);
            markedEntities.add(display.uniqueId);
            return display.entity();
        }
    }

    private static final class FakeDisplay {

        private final UUID uniqueId = UUID.randomUUID();
        private final String kind;
        private final Location location;
        private final ItemStack itemStack;
        private final Entity entity;

        private Component text;
        private boolean removed;

        private FakeDisplay(String kind, Location location, Component text, ItemStack itemStack) {
            this.kind = kind;
            this.location = location;
            this.text = text;
            this.itemStack = itemStack;
            this.entity = SharedBukkitServer.proxy(Entity.class, (method, arguments) -> switch (method.getName()) {
                case "getUniqueId" -> uniqueId;
                case "remove" -> {
                    removed = true;
                    yield null;
                }
                default -> throw new UnsupportedOperationException("Entity." + method.getName());
            });
        }

        private String kind() {
            return kind;
        }

        private Location location() {
            return location;
        }

        private Component text() {
            return text;
        }

        private String plainText() {
            return PlainTextComponentSerializer.plainText().serialize(text);
        }

        private ItemStack itemStack() {
            return itemStack;
        }

        private Entity entity() {
            return entity;
        }

        private boolean removed() {
            return removed;
        }
    }
}

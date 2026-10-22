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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                default -> throw new UnsupportedOperationException("World." + method.getName());
            });
    private final RecordingDisplayEntities displayEntities = new RecordingDisplayEntities();
    private final AtomicReference<Optional<ItemStack>> stationItem = new AtomicReference<>(Optional.empty());
    private final AtomicBoolean visualsReady = new AtomicBoolean(true);

    @Test
    void theStartShowsATextAboveEveryStationInALoadedChunkOnly() {
        stations.add(STATION);
        stations.add(FAR_STATION);
        stations.add(OTHER_WORLD_STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();

        displays.showAll();

        assertEquals(1, displayEntities.spawned.size());
        FakeDisplay display = displayEntities.spawned.getFirst();
        assertEquals("text", display.kind());
        assertEquals(new Location(world, 10.5D, 65.7D, 20.5D), display.location());
        assertEquals("Register\nClick to take part", display.plainText());
    }

    @Test
    void theTextHasABoldTitleInThePrefixColourAndAPlainHintBelow() {
        stations.add(STATION);
        loadChunk(0, 1);

        createDisplays().showAll();

        List<Component> lines = displayEntities.spawned.getFirst().text().children();
        assertEquals(3, lines.size(), lines.toString());
        assertEquals(TournamentMessages.PREFIX_COLOUR, lines.get(0).color());
        assertTrue(lines.get(0).hasDecoration(TextDecoration.BOLD));
        assertEquals(Component.newline(), lines.get(1));
        assertEquals(TournamentMessages.DETAIL_COLOUR, lines.get(2).color());
        assertFalse(lines.get(2).hasDecoration(TextDecoration.BOLD));
    }

    @Test
    @UseLanguage("de")
    void theTextIsGermanWithGermanSettings() {
        stations.add(STATION);
        loadChunk(0, 1);

        createDisplays().showAll();

        assertEquals("Anmelden\nKlicke, um teilzunehmen", displayEntities.spawned.getFirst().plainText());
    }

    @Test
    void aKnownStationItemIsShownInsteadOfTheText() {
        stationItem.set(Optional.of(STATION_ITEM));
        stations.add(STATION);
        loadChunk(0, 1);

        createDisplays().showAll();

        FakeDisplay display = displayEntities.spawned.getFirst();
        assertEquals("item", display.kind());
        assertEquals(STATION_ITEM, display.itemStack());
        assertEquals(new Location(world, 10.5D, 65.7D, 20.5D), display.location());
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

        assertEquals(List.of(new Location(world, 10.5D, 65.7D, 20.5D), new Location(world, 12.5D, 71.7D, 25.5D)),
                displayEntities.spawned.stream().map(FakeDisplay::location).toList());
    }

    @Test
    void addingAStationShowsItsDisplayAndRemovingItTakesTheDisplayAway() {
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();

        stations.add(STATION);
        displays.stationAdded(STATION);
        FakeDisplay display = displayEntities.spawned.getFirst();
        assertFalse(display.removed());

        stations.remove(STATION);
        displays.stationRemoved(STATION);
        assertTrue(display.removed());
        assertEquals(1, displayEntities.spawned.size());
    }

    @Test
    void aStationAddedInAnUnloadedChunkGetsItsDisplayWhenTheChunkLoads() {
        RegistrationStationDisplays displays = createDisplays();
        stations.add(STATION);
        displays.stationAdded(STATION);
        assertEquals(List.of(), displayEntities.spawned);

        loadChunk(0, 1);
        displays.showChunk(chunk(0, 1));

        assertEquals(1, displayEntities.spawned.size());
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

        assertTrue(displayEntities.spawned.get(0).removed());
        assertFalse(displayEntities.spawned.get(1).removed());
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

        assertEquals(2, displayEntities.spawned.size());
        assertTrue(displayEntities.spawned.stream().allMatch(FakeDisplay::removed));
    }

    @Test
    void aStationHasAtMostOneDisplay() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();

        displays.showAll();
        displays.showAll();
        displays.stationAdded(STATION);
        displays.showChunk(chunk(0, 1));

        assertEquals(4, displayEntities.spawned.size());
        assertEquals(1, displayEntities.spawned.stream().filter(display -> !display.removed()).count());
    }

    @Test
    void theRefreshAfterAReloadRebuildsEveryDisplayWithTheNewItem() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();

        stationItem.set(Optional.of(STATION_ITEM));
        displays.refreshAll();

        assertEquals(2, displayEntities.spawned.size());
        assertTrue(displayEntities.spawned.get(0).removed());
        assertEquals("item", displayEntities.spawned.get(1).kind());
        assertFalse(displayEntities.spawned.get(1).removed());
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

        assertEquals("Register\nClick to take part", displayEntities.spawned.get(0).plainText());
        assertEquals("Anmelden\nKlicke, um teilzunehmen", displayEntities.spawned.get(1).plainText());
        assertTrue(displayEntities.spawned.get(0).removed());
    }

    @Test
    void itemsAdderGettingReadyAfterThePluginRebuildsEveryDisplayOnce() {
        visualsReady.set(false);
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();
        displays.refreshOnceReady();
        assertEquals(1, displayEntities.spawned.size(), "nothing changes while ItemsAdder still loads");

        visualsReady.set(true);
        stationItem.set(Optional.of(STATION_ITEM));
        displays.refreshOnceReady();
        displays.refreshOnceReady();

        assertEquals(2, displayEntities.spawned.size());
        assertTrue(displayEntities.spawned.get(0).removed());
        assertEquals("item", displayEntities.spawned.get(1).kind());
    }

    @Test
    void visualsThatAreReadyFromTheStartNeverTriggerARebuild() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();

        displays.refreshOnceReady();

        assertEquals(1, displayEntities.spawned.size());
    }

    @Test
    void loadingAChunkRemovesMarkedDisplaysThatBelongToNoStationAndKeepsEverythingElse() {
        stations.add(STATION);
        loadChunk(0, 1);
        RegistrationStationDisplays displays = createDisplays();
        displays.showAll();
        FakeDisplay ownDisplay = displayEntities.spawned.getFirst();
        FakeDisplay leftOver = displayEntities.foreign(true);
        FakeDisplay otherEntity = displayEntities.foreign(false);

        displays.showChunk(chunk(0, 1, ownDisplay.entity(), leftOver.entity(), otherEntity.entity()));

        assertTrue(leftOver.removed());
        assertFalse(otherEntity.removed());
        assertTrue(ownDisplay.removed(), "the own display is replaced by a new one");
        assertEquals(1, displayEntities.spawned.stream().filter(display -> !display.removed()).count());
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
                displayEntities, stationItem::get, visualsReady::get);
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

        @Override
        public Entity spawnItemDisplay(Location location, ItemStack itemStack) {
            return record(new FakeDisplay("item", location, null, itemStack));
        }

        @Override
        public Entity spawnTextDisplay(Location location, Component text) {
            return record(new FakeDisplay("text", location, text, null));
        }

        @Override
        public boolean isStationDisplay(Entity entity) {
            return markedEntities.contains(entity.getUniqueId());
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
        private final Component text;
        private final ItemStack itemStack;
        private final Entity entity;

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
            assertNull(itemStack);
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

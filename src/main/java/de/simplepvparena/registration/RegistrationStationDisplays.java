package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.RegistrationMessage;
import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class RegistrationStationDisplays implements RegistrationStationObserver {

    public static final double BUTTON_HEIGHT_ABOVE_THE_BLOCK = 0.15D;
    public static final double TEXT_HEIGHT_ABOVE_THE_BLOCK = 0.9D;

    private static final double BLOCK_CENTRE = 0.5D;
    private static final double BLOCK_HEIGHT = 1.0D;
    private static final int CHUNK_SHIFT = 4;

    private final RegistrationStations stations;
    private final Function<String, World> worldLookup;
    private final StationDisplayEntities displayEntities;
    private final Supplier<Optional<ItemStack>> stationItemSource;
    private final Supplier<Optional<String>> titleImageSource;
    private final IntSupplier registeredCount;
    private final BooleanSupplier visualsReady;
    private final Map<RegistrationStation, StationDisplay> displays = new LinkedHashMap<>();

    private boolean visualsWereReady;

    public RegistrationStationDisplays(RegistrationStations stations, Function<String, World> worldLookup,
                                       StationDisplayEntities displayEntities,
                                       Supplier<Optional<ItemStack>> stationItemSource,
                                       Supplier<Optional<String>> titleImageSource, IntSupplier registeredCount,
                                       BooleanSupplier visualsReady) {
        this.stations = Objects.requireNonNull(stations, "stations");
        this.worldLookup = Objects.requireNonNull(worldLookup, "worldLookup");
        this.displayEntities = Objects.requireNonNull(displayEntities, "displayEntities");
        this.stationItemSource = Objects.requireNonNull(stationItemSource, "stationItemSource");
        this.titleImageSource = Objects.requireNonNull(titleImageSource, "titleImageSource");
        this.registeredCount = Objects.requireNonNull(registeredCount, "registeredCount");
        this.visualsReady = Objects.requireNonNull(visualsReady, "visualsReady");
        this.visualsWereReady = visualsReady.getAsBoolean();
    }

    public void showAll() {
        for (RegistrationStation station : stations.stations()) {
            show(station);
        }
    }

    public void hideAll() {
        for (RegistrationStation station : List.copyOf(displays.keySet())) {
            hide(station);
        }
    }

    public void refreshAll() {
        hideAll();
        showAll();
    }

    public void refreshOnceReady() {
        if (visualsWereReady || !visualsReady.getAsBoolean()) {
            return;
        }
        visualsWereReady = true;
        refreshAll();
    }

    public void updateCounters() {
        int count = registeredCount.getAsInt();
        for (StationDisplay display : displays.values()) {
            if (display.shownCount != count) {
                display.shownCount = count;
                displayEntities.changeText(display.text, text(count));
            }
        }
    }

    public void press(RegistrationStation station) {
        StationDisplay display = displays.get(station);
        if (display != null) {
            displayEntities.pressButton(display.cap());
        }
    }

    public Optional<RegistrationStation> findStationOfClickArea(Entity entity) {
        for (Map.Entry<RegistrationStation, StationDisplay> entry : displays.entrySet()) {
            Entity clickArea = entry.getValue().clickArea;
            if (clickArea != null && clickArea.getUniqueId().equals(entity.getUniqueId())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void showChunk(Chunk chunk) {
        removeForeignDisplays(chunk.getEntities());
        for (RegistrationStation station : stationsIn(chunk)) {
            hide(station);
            spawn(station, chunk.getWorld());
        }
    }

    public void hideChunk(Chunk chunk) {
        for (RegistrationStation station : stationsIn(chunk)) {
            hide(station);
        }
    }

    @Override
    public void stationAdded(RegistrationStation station) {
        show(station);
    }

    @Override
    public void stationRemoved(RegistrationStation station) {
        hide(station);
    }

    @Override
    public void stationUsed(RegistrationStation station) {
        press(station);
        updateCounters();
    }

    private void show(RegistrationStation station) {
        hide(station);
        World world = worldLookup.apply(station.worldName());
        if (world == null || !world.isChunkLoaded(chunkCoordinate(station.blockX()),
                chunkCoordinate(station.blockZ()))) {
            return;
        }
        spawn(station, world);
    }

    private void spawn(RegistrationStation station, World world) {
        int count = registeredCount.getAsInt();
        Optional<ItemStack> buttonImage = stationItemSource.get();
        List<Entity> entities = new ArrayList<>();
        Entity cap;
        Entity clickArea = null;
        if (buttonImage.isPresent()) {
            cap = displayEntities.spawnButtonCover(
                    world.getBlockAt(station.blockX(), station.blockY(), station.blockZ()), buttonImage.get());
            entities.add(cap);
        } else {
            List<Entity> button = displayEntities.spawnButton(above(world, station, BUTTON_HEIGHT_ABOVE_THE_BLOCK));
            clickArea = displayEntities.spawnClickArea(above(world, station, BUTTON_HEIGHT_ABOVE_THE_BLOCK));
            entities.addAll(button);
            entities.add(clickArea);
            cap = button.getLast();
        }
        Entity text = displayEntities.spawnTextDisplay(above(world, station, TEXT_HEIGHT_ABOVE_THE_BLOCK),
                text(count));
        entities.add(text);
        displays.put(station, new StationDisplay(List.copyOf(entities), cap, clickArea, text, count));
    }

    private static Location above(World world, RegistrationStation station, double height) {
        return new Location(world, station.blockX() + BLOCK_CENTRE, station.blockY() + BLOCK_HEIGHT + height,
                station.blockZ() + BLOCK_CENTRE);
    }

    private void hide(RegistrationStation station) {
        StationDisplay display = displays.remove(station);
        if (display != null) {
            display.entities.forEach(Entity::remove);
        }
    }

    private void removeForeignDisplays(Entity[] entities) {
        Set<UUID> ownDisplays = new HashSet<>();
        for (StationDisplay display : displays.values()) {
            display.entities.forEach(entity -> ownDisplays.add(entity.getUniqueId()));
        }
        for (Entity entity : entities) {
            if (displayEntities.isStationDisplay(entity) && !ownDisplays.contains(entity.getUniqueId())) {
                entity.remove();
            }
        }
    }

    private List<RegistrationStation> stationsIn(Chunk chunk) {
        String worldName = chunk.getWorld().getName();
        return stations.stations().stream()
                .filter(station -> station.worldName().equals(worldName)
                        && chunkCoordinate(station.blockX()) == chunk.getX()
                        && chunkCoordinate(station.blockZ()) == chunk.getZ())
                .toList();
    }

    private static int chunkCoordinate(int blockCoordinate) {
        return blockCoordinate >> CHUNK_SHIFT;
    }

    private Component text(int count) {
        TextComponent.Builder text = Component.text();
        Optional<String> titleImage = titleImageSource.get();
        if (titleImage.isPresent()) {
            text.append(Component.text(titleImage.get(), NamedTextColor.WHITE));
        } else {
            text.append(Component.text(Messages.text(RegistrationMessage.DISPLAY_TITLE),
                    TournamentMessages.PREFIX_COLOUR, TextDecoration.BOLD));
        }
        return text.append(Component.newline())
                .append(Component.text(Messages.text(RegistrationMessage.DISPLAY_HINT),
                        TournamentMessages.DETAIL_COLOUR))
                .append(Component.newline())
                .append(Component.text(Messages.plural(RegistrationMessage.DISPLAY_COUNT, count),
                        NamedTextColor.GREEN))
                .build();
    }

    private static final class StationDisplay {

        private final List<Entity> entities;
        private final Entity cap;
        private final Entity clickArea;
        private final Entity text;

        private int shownCount;

        private StationDisplay(List<Entity> entities, Entity cap, Entity clickArea, Entity text, int shownCount) {
            this.entities = entities;
            this.cap = cap;
            this.clickArea = clickArea;
            this.text = text;
            this.shownCount = shownCount;
        }

        private Entity cap() {
            return cap;
        }
    }
}

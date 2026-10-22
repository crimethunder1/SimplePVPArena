package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.RegistrationMessage;
import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

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
import java.util.function.Supplier;

public final class RegistrationStationDisplays implements RegistrationStationObserver {

    public static final double HEIGHT_ABOVE_THE_BLOCK = 0.7D;

    private static final double BLOCK_CENTRE = 0.5D;
    private static final double BLOCK_HEIGHT = 1.0D;
    private static final int CHUNK_SHIFT = 4;

    private final RegistrationStations stations;
    private final Function<String, World> worldLookup;
    private final StationDisplayEntities displayEntities;
    private final Supplier<Optional<ItemStack>> stationItemSource;
    private final BooleanSupplier visualsReady;
    private final Map<RegistrationStation, Entity> displays = new LinkedHashMap<>();

    private boolean visualsWereReady;

    public RegistrationStationDisplays(RegistrationStations stations, Function<String, World> worldLookup,
                                       StationDisplayEntities displayEntities,
                                       Supplier<Optional<ItemStack>> stationItemSource,
                                       BooleanSupplier visualsReady) {
        this.stations = Objects.requireNonNull(stations, "stations");
        this.worldLookup = Objects.requireNonNull(worldLookup, "worldLookup");
        this.displayEntities = Objects.requireNonNull(displayEntities, "displayEntities");
        this.stationItemSource = Objects.requireNonNull(stationItemSource, "stationItemSource");
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
        Location location = new Location(world, station.blockX() + BLOCK_CENTRE,
                station.blockY() + BLOCK_HEIGHT + HEIGHT_ABOVE_THE_BLOCK, station.blockZ() + BLOCK_CENTRE);
        Optional<ItemStack> stationItem = stationItemSource.get();
        Entity display = stationItem.isPresent()
                ? displayEntities.spawnItemDisplay(location, stationItem.get())
                : displayEntities.spawnTextDisplay(location, text());
        displays.put(station, display);
    }

    private void hide(RegistrationStation station) {
        Entity display = displays.remove(station);
        if (display != null) {
            display.remove();
        }
    }

    private void removeForeignDisplays(Entity[] entities) {
        Set<UUID> ownDisplays = new HashSet<>();
        for (Entity display : displays.values()) {
            ownDisplays.add(display.getUniqueId());
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

    private static Component text() {
        return Component.text()
                .append(Component.text(Messages.text(RegistrationMessage.DISPLAY_TITLE),
                        TournamentMessages.PREFIX_COLOUR, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(Messages.text(RegistrationMessage.DISPLAY_HINT),
                        TournamentMessages.DETAIL_COLOUR))
                .build();
    }
}

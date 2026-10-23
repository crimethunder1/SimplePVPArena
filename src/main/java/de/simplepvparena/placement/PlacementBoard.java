package de.simplepvparena.placement;

import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.config.PluginSettings;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PlacementBoard {

    public static final double HEIGHT_ABOVE_THE_PLACE = 2.0D;
    public static final long REFRESH_PERIOD_TICKS = 20L;

    private static final int CHUNK_SHIFT = 4;

    private final Plugin plugin;
    private final Supplier<Optional<PositionSnapshot>> placeSource;
    private final Function<String, World> worldLookup;
    private final PlacementBoardEntities boardEntities;
    private final Supplier<PlacementStandings> standingsSource;

    private boolean enabled = PluginSettings.DEFAULT_PLACEMENT_BOARD_ENABLED;
    private BukkitTask refreshTask;
    private Entity board;
    private PositionSnapshot shownPlace;
    private Component shownText;

    public PlacementBoard(Plugin plugin, Supplier<Optional<PositionSnapshot>> placeSource,
                          Function<String, World> worldLookup, PlacementBoardEntities boardEntities,
                          Supplier<PlacementStandings> standingsSource) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.placeSource = Objects.requireNonNull(placeSource, "placeSource");
        this.worldLookup = Objects.requireNonNull(worldLookup, "worldLookup");
        this.boardEntities = Objects.requireNonNull(boardEntities, "boardEntities");
        this.standingsSource = Objects.requireNonNull(standingsSource, "standingsSource");
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        refresh();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void start() {
        if (refreshTask != null) {
            return;
        }
        refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refresh, REFRESH_PERIOD_TICKS,
                REFRESH_PERIOD_TICKS);
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public boolean isRunning() {
        return refreshTask != null;
    }

    public void refresh() {
        Optional<PositionSnapshot> place = enabled ? placeSource.get() : Optional.empty();
        PlacementStandings standings = standingsSource.get();
        if (place.isEmpty() || !standings.hasContent()) {
            hide();
            return;
        }
        Component text = PlacementLines.boardText(standings);
        if (board != null && (!place.get().equals(shownPlace) || !board.isValid())) {
            hide();
        }
        if (board == null) {
            spawn(place.get(), text);
            return;
        }
        if (!text.equals(shownText)) {
            boardEntities.changeText(board, text);
            shownText = text;
        }
    }

    public void rebuild() {
        hide();
        refresh();
    }

    public void hide() {
        if (board != null) {
            board.remove();
        }
        board = null;
        shownPlace = null;
        shownText = null;
    }

    public void showChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (boardEntities.isBoard(entity) && entity != board) {
                entity.remove();
            }
        }
        if (placeSource.get().filter(place -> liesIn(place, chunk)).isPresent()) {
            rebuild();
        }
    }

    public void hideChunk(Chunk chunk) {
        if (shownPlace != null && liesIn(shownPlace, chunk)) {
            hide();
        }
    }

    public Optional<Entity> findBoard() {
        return Optional.ofNullable(board);
    }

    private void spawn(PositionSnapshot place, Component text) {
        World world = worldLookup.apply(place.worldName());
        if (world == null || !world.isChunkLoaded(chunkCoordinate(place.x()), chunkCoordinate(place.z()))) {
            return;
        }
        board = boardEntities.spawnBoard(new Location(world, place.x(), place.y() + HEIGHT_ABOVE_THE_PLACE,
                place.z(), place.yaw(), 0.0F), text);
        shownPlace = place;
        shownText = text;
    }

    private static boolean liesIn(PositionSnapshot place, Chunk chunk) {
        return place.worldName().equals(chunk.getWorld().getName())
                && chunkCoordinate(place.x()) == chunk.getX()
                && chunkCoordinate(place.z()) == chunk.getZ();
    }

    private static int chunkCoordinate(double coordinate) {
        return (int) Math.floor(coordinate) >> CHUNK_SHIFT;
    }
}

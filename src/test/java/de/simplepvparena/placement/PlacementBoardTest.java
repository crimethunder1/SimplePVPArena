package de.simplepvparena.placement;

import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementBoardTest {

    private static final String WORLD_NAME = "arena";
    private static final PlacementTeam ALPHA = new PlacementTeam("Alpha", NamedTextColor.RED);
    private static final PlacementTeam BRAVO = new PlacementTeam("Bravo", NamedTextColor.WHITE);
    private static final PositionSnapshot PLACE = new PositionSnapshot(WORLD_NAME, 20.5D, 64.0D, 35.5D, 90.0F, 0.0F);

    private final SidebarFixture fixture = new SidebarFixture();
    private final Set<String> loadedChunks = new HashSet<>(Set.of("1/2"));
    private final World world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
        case "getName" -> WORLD_NAME;
        case "isChunkLoaded" -> loadedChunks.contains(arguments[0] + "/" + arguments[1]);
        default -> throw new UnsupportedOperationException("World." + method.getName());
    });
    private final RecordingBoardEntities boardEntities = new RecordingBoardEntities();

    private Optional<PositionSnapshot> place = Optional.of(PLACE);
    private PlacementStandings standings = runningTournament(1);

    private final PlacementBoard board = new PlacementBoard(fixture.plugin(), () -> place,
            worldName -> WORLD_NAME.equals(worldName) ? world : null, boardEntities, () -> standings);

    @Test
    void standsTwoBlocksAboveThePlaceWithTheTitleAndTheLines() {
        board.refresh();

        assertEquals(1, boardEntities.spawned.size());
        FakeBoard shown = boardEntities.spawned.get(0);
        assertEquals(new Location(world, 20.5D, 66.0D, 35.5D, 90.0F, 0.0F), shown.location);
        assertEquals("Tournament\nRound 1 of 2\nAlpha 1 : 0 Bravo\nStill in: 2 teams\nAlpha\nBravo",
                TournamentMessages.toPlainText(shown.text));
        assertEquals(Optional.of(shown.proxy), board.findBoard());
    }

    @Test
    void changesTheTextOnlyWhenItChanged() {
        board.refresh();
        board.refresh();
        assertEquals(0, boardEntities.textChanges);

        standings = runningTournament(2);
        board.refresh();

        assertEquals(1, boardEntities.spawned.size());
        assertEquals(1, boardEntities.textChanges);
        assertTrue(TournamentMessages.toPlainText(boardEntities.spawned.get(0).text).contains("Alpha 2 : 0 Bravo"));
    }

    @Test
    void disappearsWithoutContentWithoutAPlaceAndWhileItIsSwitchedOff() {
        board.refresh();
        standings = PlacementStandings.none();
        board.refresh();
        assertTrue(boardEntities.spawned.get(0).removed);
        assertEquals(Optional.empty(), board.findBoard());

        standings = runningTournament(1);
        board.refresh();
        place = Optional.empty();
        board.refresh();
        assertTrue(boardEntities.spawned.get(1).removed);

        place = Optional.of(PLACE);
        board.setEnabled(false);
        assertFalse(board.isEnabled());
        assertEquals(Optional.empty(), board.findBoard());
        board.setEnabled(true);
        assertEquals(3, boardEntities.spawned.size());
        assertFalse(boardEntities.spawned.get(2).removed);
    }

    @Test
    void movesWithItsPlace() {
        board.refresh();
        place = Optional.of(new PositionSnapshot(WORLD_NAME, 21.5D, 70.0D, 36.5D, 0.0F, 0.0F));

        board.refresh();

        assertTrue(boardEntities.spawned.get(0).removed);
        assertEquals(new Location(world, 21.5D, 72.0D, 36.5D, 0.0F, 0.0F), boardEntities.spawned.get(1).location);
    }

    @Test
    void waitsForTheChunkAndTheWorldOfThePlace() {
        loadedChunks.clear();
        board.refresh();
        assertEquals(List.of(), boardEntities.spawned);

        place = Optional.of(new PositionSnapshot("nether", 20.5D, 64.0D, 35.5D, 0.0F, 0.0F));
        board.refresh();
        assertEquals(List.of(), boardEntities.spawned);

        place = Optional.of(PLACE);
        loadedChunks.add("1/2");
        board.showChunk(chunk(1, 2, List.of()));
        assertEquals(1, boardEntities.spawned.size());
    }

    @Test
    void goesWithTheUnloadedChunkAndComesBackWithItWhileForeignBoardsAreRemoved() {
        board.refresh();
        board.hideChunk(chunk(0, 0, List.of()));
        assertFalse(boardEntities.spawned.get(0).removed, "another chunk leaves the board alone");

        board.hideChunk(chunk(1, 2, List.of()));
        assertTrue(boardEntities.spawned.get(0).removed);

        FakeBoard leftOver = boardEntities.foreignBoard();
        board.showChunk(chunk(1, 2, List.of(leftOver.proxy)));

        assertTrue(leftOver.removed);
        assertEquals(2, boardEntities.spawned.size());
        assertFalse(boardEntities.spawned.get(1).removed);
    }

    @Test
    void comesBackWhenTheEntityWasRemovedByAnythingElse() {
        board.refresh();
        boardEntities.spawned.get(0).valid = false;

        board.refresh();

        assertEquals(2, boardEntities.spawned.size());
    }

    @Test
    void rebuildsAfterTheReloadAndHidesWhenThePluginStops() {
        board.refresh();
        board.rebuild();
        assertTrue(boardEntities.spawned.get(0).removed);
        assertEquals(2, boardEntities.spawned.size());

        board.hide();
        assertTrue(boardEntities.spawned.get(1).removed);
    }

    @Test
    void startsOneRefreshEverySecondAndStopsIt() {
        board.start();
        board.start();
        assertTrue(board.isRunning());
        assertEquals(List.of(PlacementBoard.REFRESH_PERIOD_TICKS), fixture.repeatingPeriods());
        fixture.repeatingTasks().get(0).run();
        assertEquals(1, boardEntities.spawned.size());

        board.stop();
        assertFalse(board.isRunning());
        assertEquals(20L, PlacementBoard.REFRESH_PERIOD_TICKS);
        assertEquals(2.0D, PlacementBoard.HEIGHT_ABOVE_THE_PLACE);
    }

    private Chunk chunk(int chunkX, int chunkZ, List<Entity> entities) {
        return SharedBukkitServer.proxy(Chunk.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getX" -> chunkX;
            case "getZ" -> chunkZ;
            case "getEntities" -> entities.toArray(Entity[]::new);
            default -> throw new UnsupportedOperationException("Chunk." + method.getName());
        });
    }

    private static PlacementStandings runningTournament(int alphaPoints) {
        return PlacementStandings.tournament(PlacementPhase.TOURNAMENT_RUNNING, 1, 2,
                Optional.of(new PlacementMatch(ALPHA, alphaPoints, BRAVO, 0)), List.of(ALPHA, BRAVO), List.of(),
                List.of());
    }

    private static final class FakeBoard {

        private final Location location;
        private final Entity proxy;

        private Component text;
        private boolean removed;
        private boolean valid = true;

        private FakeBoard(Location location, Component text) {
            this.location = location;
            this.text = text;
            this.proxy = SharedBukkitServer.proxy(Entity.class, (method, arguments) -> switch (method.getName()) {
                case "remove" -> {
                    removed = true;
                    yield null;
                }
                case "isValid" -> valid && !removed;
                default -> throw new UnsupportedOperationException("Entity." + method.getName());
            });
        }
    }

    private static final class RecordingBoardEntities implements PlacementBoardEntities {

        private final List<FakeBoard> spawned = new ArrayList<>();
        private final List<FakeBoard> boards = new ArrayList<>();

        private int textChanges;

        @Override
        public Entity spawnBoard(Location location, Component text) {
            FakeBoard board = new FakeBoard(location, text);
            spawned.add(board);
            boards.add(board);
            return board.proxy;
        }

        @Override
        public void changeText(Entity board, Component text) {
            textChanges++;
            boards.stream().filter(fakeBoard -> fakeBoard.proxy == board).findFirst().orElseThrow().text = text;
        }

        @Override
        public boolean isBoard(Entity entity) {
            return boards.stream().anyMatch(fakeBoard -> fakeBoard.proxy == entity);
        }

        private FakeBoard foreignBoard() {
            FakeBoard board = new FakeBoard(null, Component.empty());
            boards.add(board);
            return board;
        }
    }
}

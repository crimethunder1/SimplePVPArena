package de.simplepvparena.registration;

import de.simplepvparena.support.RecordingItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationStationServiceTest {

    @TempDir
    Path pluginFolder;

    private final StationTestServer server = new StationTestServer();
    private final RegistrationStationService stationService = server.stationService();
    private final StationTestServer.TestPlayer admin = server.joinPlayer("Admin", true);

    @Test
    void labelsTheToolWithItsNameAndLore() {
        RecordingItemStack tool = (RecordingItemStack) server.createTool();

        assertEquals("Registration station tool", tool.getName());
        assertEquals(List.of("Click a button to make it a registration station, click a station to make it a normal "
                + "button again"), tool.getLore());
        assertTrue(stationService.isTool(tool));
    }

    @Test
    void recognisesTheToolByItsMarkerAndNeverByItsName() {
        RecordingItemStack namedRod = RecordingItemStack.named(Material.BLAZE_ROD, "Registration station tool");

        assertFalse(stationService.isTool(namedRod));
        assertFalse(stationService.isTool(new RecordingItemStack(Material.BLAZE_ROD, 1)));
        assertFalse(stationService.isTool(null));
    }

    @Test
    void makesAButtonARegistrationStation() {
        server.placeBlock(10, 64, 20, Material.STONE_BUTTON);

        stationService.useTool(admin.player(), server.blockAt(10, 64, 20));

        assertEquals(List.of(new RegistrationStation(StationTestServer.WORLD_NAME, 10, 64, 20)),
                server.stations().stations());
        assertTrue(stationService.isStation(server.blockAt(10, 64, 20)));
        assertEquals(List.of("This button is now a registration station"), admin.messages());
        assertEquals(1, server.saveCount());
    }

    @Test
    void acceptsEveryKindOfButton() {
        server.placeBlock(1, 64, 0, Material.OAK_BUTTON);
        server.placeBlock(2, 64, 0, Material.POLISHED_BLACKSTONE_BUTTON);

        stationService.useTool(admin.player(), server.blockAt(1, 64, 0));
        stationService.useTool(admin.player(), server.blockAt(2, 64, 0));

        assertEquals(2, server.stations().stations().size());
    }

    @Test
    void makesAStationANormalButtonAgain() {
        server.placeBlock(10, 64, 20, Material.STONE_BUTTON);
        stationService.useTool(admin.player(), server.blockAt(10, 64, 20));

        stationService.useTool(admin.player(), server.blockAt(10, 64, 20));

        assertEquals(List.of(), server.stations().stations());
        assertEquals("This button is no longer a registration station", admin.messages().get(1));
        assertEquals(2, server.saveCount());
    }

    @Test
    void asksForAButtonWhenAnotherBlockIsClicked() {
        server.placeBlock(10, 64, 20, Material.STONE);

        stationService.useTool(admin.player(), server.blockAt(10, 64, 20));
        stationService.useTool(admin.player(), null);

        assertEquals(List.of(), server.stations().stations());
        assertEquals(List.of("Click a button", "Click a button"), admin.messages());
        assertEquals(0, server.saveCount());
    }

    @Test
    void allowsSeveralStations() {
        server.placeBlock(10, 64, 20, Material.STONE_BUTTON);
        server.placeBlock(-5, 70, 3, Material.OAK_BUTTON);

        stationService.useTool(admin.player(), server.blockAt(10, 64, 20));
        stationService.useTool(admin.player(), server.blockAt(-5, 70, 3));

        assertTrue(server.stations().isStation(new Location(server.world(), 10.5, 64.2, 20.9)));
        assertTrue(server.stations().isStation(new Location(server.world(), -4.5, 70, 3.5)));
        assertFalse(server.stations().isStation(new Location(server.world(), 11, 64, 20)));
        assertFalse(server.stations().isStation(new Location(null, 10, 64, 20)));
        assertFalse(server.stations().isStation((Location) null));
    }

    @Test
    void keepsTheStationsOverARestart() {
        server.placeBlock(10, 64, 20, Material.STONE_BUTTON);
        stationService.useTool(admin.player(), server.blockAt(10, 64, 20));
        server.registrationList().register(admin.uniqueId(), "Admin");
        FileRegistrationStorage storage = new FileRegistrationStorage(pluginFolder.resolve("registrations.yml"),
                Logger.getAnonymousLogger(), notice -> { });

        storage.save(server.registrationList(), server.stations());
        RegistrationList reloadedList = new RegistrationList();
        RegistrationStations reloadedStations = new RegistrationStations();
        storage.load(reloadedList, reloadedStations);

        assertEquals(server.stations().stations(), reloadedStations.stations());
        assertEquals(server.registrationList().getEntries(), reloadedList.getEntries());
    }

    @Test
    void removesAStationWhoseBlockWasBroken() {
        server.placeBlock(10, 64, 20, Material.STONE_BUTTON);
        stationService.useTool(admin.player(), server.blockAt(10, 64, 20));

        stationService.removeBrokenStation(admin.player(), server.blockAt(10, 64, 20));

        assertEquals(List.of(), server.stations().stations());
        assertEquals("This button is no longer a registration station", admin.messages().get(1));
        assertEquals(2, server.saveCount());
    }

    @Test
    void ignoresTheBreakOfABlockThatIsNoStation() {
        server.placeBlock(10, 64, 20, Material.STONE_BUTTON);

        stationService.removeBrokenStation(admin.player(), server.blockAt(10, 64, 20));

        assertEquals(List.of(), admin.messages());
        assertEquals(0, server.saveCount());
    }
}

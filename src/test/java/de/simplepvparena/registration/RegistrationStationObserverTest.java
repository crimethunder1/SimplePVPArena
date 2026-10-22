package de.simplepvparena.registration;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationStationObserverTest {

    private final StationTestServer server = new StationTestServer();
    private final List<String> changes = new ArrayList<>();
    private final StationTestServer.TestPlayer admin = server.joinPlayer("Admin", true);

    RegistrationStationObserverTest() {
        server.stationService().addObserver(new RegistrationStationObserver() {

            @Override
            public void stationAdded(RegistrationStation station) {
                changes.add("added " + station);
            }

            @Override
            public void stationRemoved(RegistrationStation station) {
                changes.add("removed " + station);
            }
        });
    }

    @Test
    void addingAndRemovingAStationWithTheToolIsReported() {
        server.placeBlock(1, 64, 1, Material.STONE_BUTTON);

        server.stationService().useTool(admin.player(), server.blockAt(1, 64, 1));
        server.stationService().useTool(admin.player(), server.blockAt(1, 64, 1));

        RegistrationStation station = new RegistrationStation(StationTestServer.WORLD_NAME, 1, 64, 1);
        assertEquals(List.of("added " + station, "removed " + station), changes);
    }

    @Test
    void breakingAStationIsReported() {
        server.placeBlock(1, 64, 1, Material.STONE_BUTTON);
        server.stationService().useTool(admin.player(), server.blockAt(1, 64, 1));
        changes.clear();

        server.stationService().removeBrokenStation(admin.player(), server.blockAt(1, 64, 1));

        assertEquals(List.of("removed " + new RegistrationStation(StationTestServer.WORLD_NAME, 1, 64, 1)), changes);
    }

    @Test
    void clickingSomethingElseThanAButtonOrBreakingANormalBlockReportsNothing() {
        server.placeBlock(1, 64, 1, Material.STONE);

        server.stationService().useTool(admin.player(), server.blockAt(1, 64, 1));
        server.stationService().removeBrokenStation(admin.player(), server.blockAt(1, 64, 1));

        assertEquals(List.of(), changes);
    }
}

package de.simplepvparena.gui;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaSpawnType;
import de.simplepvparena.arena.PositionSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TribuneInTheEffectAreaTest {

    private static final String WORLD_NAME = "admin-menu";
    private static final double CENTRE_Z = 3.5D;
    private static final PositionSnapshot TRIBUNE_OF_THE_FIXTURE = new PositionSnapshot(WORLD_NAME, 11.5D, 64.0D,
            11.5D, 0F, 0F);

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void acceptsATribuneInsideTheEffectArea() {
        fixture.configureArena();
        fixture.standAt(50.5D, CENTRE_Z);

        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.TRIBUNE);

        assertEquals(new PositionSnapshot(WORLD_NAME, 50.5D, 64.0D, CENTRE_Z, 0F, 0F),
                arena().getConfiguredTribuneSpawn().orElseThrow());
        assertEquals("Set the tribune spawn to admin-menu 50.5/64.0/3.5", fixture.messages().getFirst());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void refusesATribuneOutsideOfTheEffectAreaAndNamesTheDistance() {
        fixture.configureArena();
        fixture.standAt(200.5D, CENTRE_Z);

        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.TRIBUNE);

        assertEquals(List.of("The tribune spawn must lie inside the area of effect, admin-menu 200.5/64.0/3.5 lies "
                + "197 blocks from its centre 3.5/3.5, radius 99.5 blocks"
                + "\nStand closer to the fight area, then set it again. Nothing was stored"
                + "\nThe margin around the fight area is set in config.yml under arena.effect-margin"),
                fixture.messages());
        assertEquals(TRIBUNE_OF_THE_FIXTURE, arena().getConfiguredTribuneSpawn().orElseThrow());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void refusesATribuneJustOutsideTheEdgeAndAcceptsOneJustInside() {
        fixture.configureArena();
        fixture.arenaRegistry().setEffectMargin(10);
        double edgeX = 3.5D + fixture.arenaRegistry().findEffectArea().orElseThrow().getRadius();

        fixture.standAt(edgeX + 0.1D, CENTRE_Z);
        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.TRIBUNE);

        assertEquals(TRIBUNE_OF_THE_FIXTURE, arena().getConfiguredTribuneSpawn().orElseThrow());

        fixture.standAt(edgeX - 0.1D, CENTRE_Z);
        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.TRIBUNE);

        assertEquals(edgeX - 0.1D, arena().getConfiguredTribuneSpawn().orElseThrow().x());
    }

    @Test
    void acceptsEveryTribuneWhileNoFightAreaIsCalculated() {
        fixture.standAt(5000.5D, CENTRE_Z);

        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.TRIBUNE);

        assertEquals(5000.5D, arena().getConfiguredTribuneSpawn().orElseThrow().x());
        assertEquals("Set the tribune spawn to admin-menu 5000.5/64.0/3.5", fixture.messages().getFirst());
    }

    @Test
    void keepsTheCheckOfTheTeamSpawnsAsItWas() {
        fixture.configureArena();
        fixture.standAt(200.5D, CENTRE_Z);

        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.FIRST_TEAM);

        assertTrue(fixture.messages().getFirst().startsWith("The first team spawn must lie inside the calculated "
                + "fight area"), fixture.messages().toString());
    }

    @Test
    void warnsAboutATribuneOutsideOfTheNewAreaAndKeepsTheCalculation() {
        fixture.configureArena();
        PositionSnapshot farTribune = new PositionSnapshot(WORLD_NAME, 300.5D, 64.0D, CENTRE_Z, 0F, 0F);
        arena().setTribuneSpawn(farTribune);
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertTrue(fixture.messages().getFirst().startsWith("The fight area was calculated from the border blocks"),
                fixture.messages().toString());
        assertTrue(fixture.messages().contains("The tribune spawn admin-menu 300.5/64.0/3.5 lies outside of the "
                + "area of effect, 297 blocks from its centre 3.5/3.5, radius 99.5 blocks"
                + "\nThe calculation stays valid. Set the tribune spawn again closer to the fight area, the margin "
                + "around it is set in config.yml under arena.effect-margin"), fixture.messages().toString());
        assertEquals(farTribune, arena().getConfiguredTribuneSpawn().orElseThrow());
        assertTrue(arena().isFullyConfigured());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void doesNotWarnAboutATribuneInsideTheArea() {
        fixture.configureArena();
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertTrue(fixture.messages().stream().noneMatch(message -> message.contains("area of effect")),
                fixture.messages().toString());
    }

    @Test
    void showsTheAreaOfEffectWithTheCurrentMarginInTheInfoTile() {
        fixture.configureArena();
        fixture.arenaRegistry().setEffectMargin(10);

        assertTrue(fixture.arenaSetupService().describeArena()
                .contains("  Area of effect: centre 3.5/3.5, radius 13.5 blocks"));

        arena().setShape(null);

        assertTrue(fixture.arenaSetupService().describeArena()
                .contains("  Area of effect: none until the fight area is calculated"));
    }

    private Arena arena() {
        return fixture.arenaRegistry().getArena();
    }
}

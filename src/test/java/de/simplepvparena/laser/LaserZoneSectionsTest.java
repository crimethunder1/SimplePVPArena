package de.simplepvparena.laser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LaserZoneSectionsTest {

    @Test
    void keepsOneSectionWhileTheFightersStandOrJumpOnTheGround() {
        assertEquals(1, LaserZoneService.sectionsFor(0.0D, 12));
        assertEquals(1, LaserZoneService.sectionsFor(1.25D, 12), "a jump does not extend the poles");
    }

    @Test
    void extendsOneSectionForEveryFurtherSectionHeightAFighterClimbs() {
        assertEquals(2, LaserZoneService.sectionsFor(2.0D, 12));
        assertEquals(2, LaserZoneService.sectionsFor(4.4D, 12));
        assertEquals(3, LaserZoneService.sectionsFor(4.6D, 12));
        assertEquals(10, LaserZoneService.sectionsFor(30.0D, 12));
    }

    @Test
    void staysWithinTheLimitAndNeverDropsBelowOneSection() {
        assertEquals(12, LaserZoneService.sectionsFor(200.0D, 12));
        assertEquals(3, LaserZoneService.sectionsFor(200.0D, 3));
        assertEquals(1, LaserZoneService.sectionsFor(-10.0D, 12), "a fighter below the ground");
        assertEquals(1, LaserZoneService.sectionsFor(50.0D, 0));
    }
}

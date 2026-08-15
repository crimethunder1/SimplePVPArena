package de.simplepvparena.border;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderSelectorModeRegistryTest {

    private final BorderSelectorModeRegistry registry = new BorderSelectorModeRegistry();
    private final UUID admin = UUID.randomUUID();
    private final UUID otherAdmin = UUID.randomUUID();

    @Test
    void isOffForEverybodyWhoNeverSwitchedItOn() {
        assertFalse(registry.isActive(admin));
        assertEquals(0, registry.getActiveCount());
    }

    @Test
    void switchesTheModeOnForOneAdmin() {
        assertTrue(registry.activate(admin));

        assertTrue(registry.isActive(admin));
        assertEquals(1, registry.getActiveCount());
    }

    @Test
    void reportsThatAModeThatWasAlreadyOnDidNotChange() {
        registry.activate(admin);

        assertFalse(registry.activate(admin));
        assertTrue(registry.isActive(admin));
        assertEquals(1, registry.getActiveCount());
    }

    @Test
    void switchesTheModeOffAgain() {
        registry.activate(admin);

        assertTrue(registry.deactivate(admin));

        assertFalse(registry.isActive(admin));
        assertEquals(0, registry.getActiveCount());
    }

    @Test
    void reportsThatAModeThatWasAlreadyOffDidNotChange() {
        assertFalse(registry.deactivate(admin));
        assertEquals(0, registry.getActiveCount());
    }

    @Test
    void togglesTheModeOnAndOffWithTheSameCall() {
        assertTrue(registry.toggle(admin));
        assertTrue(registry.isActive(admin));

        assertFalse(registry.toggle(admin));
        assertFalse(registry.isActive(admin));

        assertTrue(registry.toggle(admin));
        assertTrue(registry.isActive(admin));
    }

    @Test
    void keepsTheModesOfTwoAdminsApart() {
        registry.activate(admin);

        assertFalse(registry.isActive(otherAdmin));

        assertTrue(registry.toggle(otherAdmin));

        assertTrue(registry.isActive(admin));
        assertTrue(registry.isActive(otherAdmin));

        registry.deactivate(admin);

        assertFalse(registry.isActive(admin));
        assertTrue(registry.isActive(otherAdmin));
        assertEquals(1, registry.getActiveCount());
    }

    @Test
    void dropsTheModeOfAnAdminWhoWentOffline() {
        registry.activate(admin);
        registry.activate(otherAdmin);

        assertTrue(registry.deactivate(admin));

        assertFalse(registry.isActive(admin));
        assertTrue(registry.isActive(otherAdmin));
    }

    @Test
    void doesNotSwitchAModeOnForAPlayerWhoIsNotThere() {
        assertFalse(registry.activate(null));
        assertFalse(registry.deactivate(null));
        assertFalse(registry.toggle(null));
        assertFalse(registry.isActive(null));
        assertEquals(0, registry.getActiveCount());
    }

    @Test
    void dropsEveryModeAtOnce() {
        registry.activate(admin);
        registry.activate(otherAdmin);

        registry.clear();

        assertEquals(0, registry.getActiveCount());
        assertFalse(registry.isActive(admin));
        assertFalse(registry.isActive(otherAdmin));
    }
}

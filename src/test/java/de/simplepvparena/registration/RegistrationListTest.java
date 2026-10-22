package de.simplepvparena.registration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationListTest {

    private final RegistrationList registrationList = new RegistrationList();
    private final UUID steveUniqueId = UUID.randomUUID();
    private final UUID alexUniqueId = UUID.randomUUID();
    private final UUID notchUniqueId = UUID.randomUUID();

    @Test
    void registersAPlayer() {
        assertTrue(registrationList.register(steveUniqueId, "Steve"));

        assertTrue(registrationList.isRegistered(steveUniqueId));
        assertEquals(List.of(new RegistrationEntry(steveUniqueId, "Steve")), registrationList.getEntries());
        assertEquals(1, registrationList.getCount());
    }

    @Test
    void registersAPlayerOnlyOnce() {
        registrationList.register(steveUniqueId, "Steve");

        assertFalse(registrationList.register(steveUniqueId, "Steve"));

        assertEquals(1, registrationList.getCount());
    }

    @Test
    void unregistersAPlayer() {
        registrationList.register(steveUniqueId, "Steve");

        assertTrue(registrationList.unregister(steveUniqueId));

        assertFalse(registrationList.isRegistered(steveUniqueId));
        assertEquals(List.of(), registrationList.getEntries());
    }

    @Test
    void reportsThatAPlayerWhoIsNotRegisteredCannotBeUnregistered() {
        registrationList.register(steveUniqueId, "Steve");

        assertFalse(registrationList.unregister(alexUniqueId));

        assertEquals(1, registrationList.getCount());
    }

    @Test
    void keepsTheOrderOfTheRegistrations() {
        registrationList.register(notchUniqueId, "Notch");
        registrationList.register(steveUniqueId, "Steve");
        registrationList.register(alexUniqueId, "Alex");
        registrationList.unregister(steveUniqueId);
        registrationList.register(steveUniqueId, "Steve");

        assertEquals(List.of("Notch", "Alex", "Steve"), playerNames());
    }

    @Test
    void remembersANewNameWithoutChangingThePosition() {
        registrationList.register(steveUniqueId, "Steve");
        registrationList.register(alexUniqueId, "Alex");

        assertTrue(registrationList.rememberName(steveUniqueId, "Steve2"));
        assertFalse(registrationList.rememberName(steveUniqueId, "Steve2"));
        assertFalse(registrationList.rememberName(notchUniqueId, "Notch"));

        assertEquals(List.of("Steve2", "Alex"), playerNames());
    }

    @Test
    void clearsEveryRegistration() {
        registrationList.register(steveUniqueId, "Steve");
        registrationList.register(alexUniqueId, "Alex");

        registrationList.clear();

        assertEquals(0, registrationList.getCount());
        assertFalse(registrationList.isRegistered(alexUniqueId));
    }

    private List<String> playerNames() {
        return registrationList.getEntries().stream().map(RegistrationEntry::playerName).toList();
    }
}

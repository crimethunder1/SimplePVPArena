package de.simplepvparena.gui;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationAdminMenuRegistrationTest {

    private final ConfirmationAdminMenu confirmationAdminMenu = new ConfirmationAdminMenu(
            new AdminMenuNavigator(new RecordingAdminMenuWindows()));

    @Test
    void namesEveryConfirmationRequestWithoutARegisteredConfirmation() {
        for (ConfirmationRequest request : EnumSet.complementOf(EnumSet.of(ConfirmationRequest.REMOVE_TEAM,
                ConfirmationRequest.CANCEL_TOURNAMENT))) {
            confirmationAdminMenu.register(request, new SilentConfirmation());
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                confirmationAdminMenu::requireEveryConfirmationRegistered);

        assertTrue(failure.getMessage().contains("REMOVE_TEAM"), failure.getMessage());
        assertTrue(failure.getMessage().contains("CANCEL_TOURNAMENT"), failure.getMessage());
        assertFalse(failure.getMessage().contains("SAVE_KIT"), failure.getMessage());
    }

    @Test
    void acceptsAConfirmationForEveryConfirmationRequest() {
        for (ConfirmationRequest request : ConfirmationRequest.values()) {
            confirmationAdminMenu.register(request, new SilentConfirmation());
        }

        assertDoesNotThrow(confirmationAdminMenu::requireEveryConfirmationRegistered);
    }

    @Test
    void refusesASecondConfirmationForTheSameRequest() {
        confirmationAdminMenu.register(ConfirmationRequest.SAVE_KIT, new SilentConfirmation());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> confirmationAdminMenu.register(ConfirmationRequest.SAVE_KIT, new SilentConfirmation()));

        assertTrue(failure.getMessage().contains("SAVE_KIT"), failure.getMessage());
    }

    private static final class SilentConfirmation implements AdminMenuConfirmation {

        @Override
        public List<String> describe(AdminMenuHolder holder) {
            return List.of();
        }

        @Override
        public void carryOut(Player player, AdminMenuHolder holder) {
        }

        @Override
        public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
            return AdminMenuHolder.main();
        }
    }
}

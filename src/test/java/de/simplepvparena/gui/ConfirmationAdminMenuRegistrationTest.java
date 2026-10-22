package de.simplepvparena.gui;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertFalse(failure.getMessage().contains("REPLACE_KIT"), failure.getMessage());
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
        confirmationAdminMenu.register(ConfirmationRequest.REPLACE_KIT, new SilentConfirmation());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> confirmationAdminMenu.register(ConfirmationRequest.REPLACE_KIT, new SilentConfirmation()));

        assertTrue(failure.getMessage().contains("REPLACE_KIT"), failure.getMessage());
    }

    @Test
    void namesTheKitConfirmationsWhenTheyAreNotRegistered() {
        for (ConfirmationRequest request : EnumSet.complementOf(EnumSet.of(ConfirmationRequest.REPLACE_KIT,
                ConfirmationRequest.DELETE_KIT))) {
            confirmationAdminMenu.register(request, new SilentConfirmation());
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                confirmationAdminMenu::requireEveryConfirmationRegistered);

        assertTrue(failure.getMessage().contains("REPLACE_KIT"), failure.getMessage());
        assertTrue(failure.getMessage().contains("DELETE_KIT"), failure.getMessage());
        assertFalse(failure.getMessage().contains("REMOVE_TEAM"), failure.getMessage());
    }

    @Test
    void namesTheConfirmationOfTakingBackAResultWhenItIsNotRegistered() {
        for (ConfirmationRequest request : EnumSet.complementOf(EnumSet.of(ConfirmationRequest.REVOKE_RESULT))) {
            confirmationAdminMenu.register(request, new SilentConfirmation());
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                confirmationAdminMenu::requireEveryConfirmationRegistered);

        assertTrue(failure.getMessage().endsWith("[REVOKE_RESULT]"), failure.getMessage());
    }

    @Test
    void namesTheMatchInTheQuestionOfTakingBackAResultAndLeavesTheOtherQuestionsAsTheyAre() {
        assertEquals("Take back the result of round1-match2?",
                ConfirmationRequest.REVOKE_RESULT.getTitle(Optional.of("round1-match2")));
        assertEquals("Remove the team?", ConfirmationRequest.REMOVE_TEAM.getTitle(Optional.of("Alpha")));
        assertEquals("Restart the match?", ConfirmationRequest.RESTART_MATCH.getTitle(Optional.empty()));
    }

    @Test
    void knowsTheKitConfirmationsAndNoLongerTheSavingOfASingleKit() {
        assertEquals("Replace the kit with your inventory?", ConfirmationRequest.REPLACE_KIT.getTitle());
        assertEquals("Delete the kit?", ConfirmationRequest.DELETE_KIT.getTitle());
        assertFalse(Arrays.stream(ConfirmationRequest.values())
                .anyMatch(request -> request.name().equals("SAVE_KIT")));
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

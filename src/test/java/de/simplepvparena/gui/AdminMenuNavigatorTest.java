package de.simplepvparena.gui;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminMenuNavigatorTest {

    private final AdminMenuNavigator navigator = new AdminMenuNavigator(new RecordingAdminMenuWindows());

    @Test
    void namesEveryMenuTypeWithoutARegisteredMenu() {
        for (AdminMenuType menuType : EnumSet.complementOf(EnumSet.of(AdminMenuType.TEAMS,
                AdminMenuType.CONFIRMATION))) {
            navigator.register(menuType, new SilentMenu());
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                navigator::requireEveryMenuRegistered);

        assertTrue(failure.getMessage().contains("TEAMS"), failure.getMessage());
        assertTrue(failure.getMessage().contains("CONFIRMATION"), failure.getMessage());
        assertFalse(failure.getMessage().contains("MAIN"), failure.getMessage());
    }

    @Test
    void namesTheWaitingMatchMenusWhenTheyAreNotRegistered() {
        for (AdminMenuType menuType : EnumSet.complementOf(EnumSet.of(AdminMenuType.WAITING_MATCHES,
                AdminMenuType.WAITING_MATCH_RESULT))) {
            navigator.register(menuType, new SilentMenu());
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                navigator::requireEveryMenuRegistered);

        assertTrue(failure.getMessage().contains("WAITING_MATCHES"), failure.getMessage());
        assertTrue(failure.getMessage().contains("WAITING_MATCH_RESULT"), failure.getMessage());
        assertFalse(failure.getMessage().contains("FORCE_RESULT"), failure.getMessage());
    }

    @Test
    void namesTheKitMenusWhenTheyAreNotRegistered() {
        for (AdminMenuType menuType : EnumSet.complementOf(EnumSet.of(AdminMenuType.KIT,
                AdminMenuType.KIT_ACTIONS))) {
            navigator.register(menuType, new SilentMenu());
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                navigator::requireEveryMenuRegistered);

        assertTrue(failure.getMessage().endsWith("[KIT, KIT_ACTIONS]"), failure.getMessage());
    }

    @Test
    void namesTheKitChoiceMenuWhenItIsNotRegistered() {
        for (AdminMenuType menuType : EnumSet.complementOf(EnumSet.of(AdminMenuType.KIT_CHOICE))) {
            navigator.register(menuType, new SilentMenu());
        }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                navigator::requireEveryMenuRegistered);

        assertTrue(failure.getMessage().endsWith("[KIT_CHOICE]"), failure.getMessage());
    }

    @Test
    void acceptsAMenuForEveryMenuType() {
        for (AdminMenuType menuType : AdminMenuType.values()) {
            navigator.register(menuType, new SilentMenu());
        }

        assertDoesNotThrow(navigator::requireEveryMenuRegistered);
    }

    @Test
    void refusesASecondMenuForTheSameTypeAndKeepsTheFirst() {
        AdminMenu firstMenu = new SilentMenu();
        navigator.register(AdminMenuType.KIT, firstMenu);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> navigator.register(AdminMenuType.KIT, new SilentMenu()));

        assertTrue(failure.getMessage().contains("KIT"), failure.getMessage());
        assertSame(firstMenu, navigator.menuOf(AdminMenuType.KIT));
    }

    @Test
    void returnsTheRegisteredMenuOfAType() {
        AdminMenu kitMenu = new SilentMenu();
        AdminMenu forceResultMenu = new SilentMenu();
        navigator.register(AdminMenuType.KIT, kitMenu);
        navigator.register(AdminMenuType.FORCE_RESULT, forceResultMenu);

        assertSame(kitMenu, navigator.menuOf(AdminMenuType.KIT));
        assertSame(forceResultMenu, navigator.menuOf(AdminMenuType.FORCE_RESULT));
    }

    @Test
    void refusesToReturnAMenuOfATypeWithoutOne() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> navigator.menuOf(AdminMenuType.TEAM_ACTIONS));

        assertTrue(failure.getMessage().contains("TEAM_ACTIONS"), failure.getMessage());
    }

    private static final class SilentMenu implements AdminMenu {

        @Override
        public String title(AdminMenuHolder holder) {
            return "Silent";
        }

        @Override
        public int size() {
            return AdminMenuLayout.SMALL_MENU_SIZE;
        }

        @Override
        public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        }

        @Override
        public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        }
    }
}

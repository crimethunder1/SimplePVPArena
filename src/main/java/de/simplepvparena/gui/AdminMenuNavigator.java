package de.simplepvparena.gui;

import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Function;

final class AdminMenuNavigator {

    private static final int SMALL_MENU_SIZE = 27;
    private static final int TEAM_MENU_SIZE = 54;

    private final AdminMenuWindows windows;
    private final Function<AdminMenuHolder, String> titleProvider;
    private final MenuFiller menuFiller;

    AdminMenuNavigator(AdminMenuWindows windows, Function<AdminMenuHolder, String> titleProvider,
                       MenuFiller menuFiller) {
        this.windows = Objects.requireNonNull(windows, "windows");
        this.titleProvider = Objects.requireNonNull(titleProvider, "titleProvider");
        this.menuFiller = Objects.requireNonNull(menuFiller, "menuFiller");
    }

    void open(Player player, AdminMenuHolder holder) {
        int size = sizeOf(holder.getMenuType());
        String title = titleProvider.apply(holder);
        windows.open(player, holder, size, title, canvas -> fill(player, holder, canvas));
    }

    void close(Player player) {
        windows.close(player);
    }

    void refresh(Player player, AdminMenuHolder holder) {
        windows.refresh(player, holder, canvas -> fill(player, holder, canvas));
    }

    private void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        canvas.clear();
        menuFiller.fill(player, holder, canvas);
    }

    private static int sizeOf(AdminMenuType menuType) {
        return menuType == AdminMenuType.TEAMS ? TEAM_MENU_SIZE : SMALL_MENU_SIZE;
    }

    @FunctionalInterface
    interface MenuFiller {

        void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas);
    }
}

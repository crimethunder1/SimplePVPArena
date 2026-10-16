package de.simplepvparena.gui;

import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class AdminMenuNavigator implements AdminMenuNavigation {

    private final AdminMenuWindows windows;
    private final Map<AdminMenuType, AdminMenu> menusByType = new EnumMap<>(AdminMenuType.class);

    AdminMenuNavigator(AdminMenuWindows windows) {
        this.windows = Objects.requireNonNull(windows, "windows");
    }

    void register(AdminMenuType menuType, AdminMenu menu) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(menu, "menu");
        if (menusByType.containsKey(menuType)) {
            throw new IllegalStateException("the " + menuType + " menu is registered already");
        }
        menusByType.put(menuType, menu);
    }

    AdminMenu menuOf(AdminMenuType menuType) {
        AdminMenu menu = menusByType.get(menuType);
        if (menu == null) {
            throw new IllegalStateException("the " + menuType + " menu is not registered");
        }
        return menu;
    }

    void requireEveryMenuRegistered() {
        Set<AdminMenuType> missingMenuTypes = EnumSet.allOf(AdminMenuType.class);
        missingMenuTypes.removeAll(menusByType.keySet());
        if (!missingMenuTypes.isEmpty()) {
            throw new IllegalStateException("no menu is registered for " + missingMenuTypes);
        }
    }

    @Override
    public void open(Player player, AdminMenuHolder holder) {
        AdminMenu menu = menuOf(holder.getMenuType());
        int size = menu.size();
        String title = menu.title(holder);
        windows.open(player, holder, size, title, canvas -> fill(player, holder, canvas));
    }

    @Override
    public void close(Player player) {
        windows.close(player);
    }

    @Override
    public void refresh(Player player, AdminMenuHolder holder) {
        windows.refresh(player, holder, canvas -> fill(player, holder, canvas));
    }

    private void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        canvas.clear();
        menuOf(holder.getMenuType()).fill(player, holder, canvas);
    }
}

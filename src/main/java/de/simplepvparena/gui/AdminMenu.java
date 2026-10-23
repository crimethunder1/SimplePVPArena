package de.simplepvparena.gui;

import org.bukkit.entity.Player;

interface AdminMenu {

    String title(AdminMenuHolder holder);

    int size();

    void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas);

    void handleClick(Player player, AdminMenuHolder holder, int slot);

    default void handleClick(Player player, AdminMenuHolder holder, int slot, boolean shiftClick) {
        handleClick(player, holder, slot);
    }
}

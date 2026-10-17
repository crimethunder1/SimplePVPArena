package de.simplepvparena.gui;

import org.bukkit.entity.Player;

interface AdminMenuNavigation {

    void open(Player player, AdminMenuHolder holder);

    void close(Player player);

    void refresh(Player player, AdminMenuHolder holder);

    boolean isShowing(Player player, AdminMenuType menuType);
}

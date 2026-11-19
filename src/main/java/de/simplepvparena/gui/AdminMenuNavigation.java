package de.simplepvparena.gui;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
interface AdminMenuNavigation {

    void open(Player player, AdminMenuHolder holder);

    void close(Player player);

    void refresh(Player player, AdminMenuHolder holder);

    boolean isShowing(Player player, AdminMenuType menuType);
}

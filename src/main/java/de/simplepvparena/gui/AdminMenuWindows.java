package de.simplepvparena.gui;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
interface AdminMenuWindows {

    void open(Player player, AdminMenuHolder holder, int size, String title, Consumer<AdminMenuCanvas> filler);

    void refresh(Player player, AdminMenuHolder holder, Consumer<AdminMenuCanvas> filler);

    void close(Player player);

    boolean isShowing(Player player, AdminMenuType menuType);
}

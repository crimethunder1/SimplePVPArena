package de.simplepvparena.gui;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

interface AdminMenuWindows {

    void open(Player player, AdminMenuHolder holder, int size, String title, Consumer<AdminMenuCanvas> filler);

    void refresh(Player player, AdminMenuHolder holder, Consumer<AdminMenuCanvas> filler);

    void close(Player player);
}

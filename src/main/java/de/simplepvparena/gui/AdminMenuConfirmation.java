package de.simplepvparena.gui;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
interface AdminMenuConfirmation {

    List<String> describe(AdminMenuHolder holder);

    void carryOut(Player player, AdminMenuHolder holder);

    AdminMenuHolder returnMenu(AdminMenuHolder holder);
}

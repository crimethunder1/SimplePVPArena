package de.simplepvparena.gui;

import org.bukkit.entity.Player;

import java.util.List;

interface AdminMenuConfirmation {

    List<String> describe(AdminMenuHolder holder);

    void carryOut(Player player, AdminMenuHolder holder);

    AdminMenuHolder returnMenu(AdminMenuHolder holder);
}

package de.simplepvparena.command;

import org.bukkit.permissions.Permissible;

public final class AdminPermission {

    private AdminPermission() {
    }

    public static boolean isAdmin(Permissible permissible) {
        return permissible.hasPermission(TournamentCommand.ADMIN_PERMISSION);
    }
}

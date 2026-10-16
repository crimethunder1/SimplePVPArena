package de.simplepvparena.permission;

import org.bukkit.permissions.Permissible;

public final class AdminPermission {

    public static final String ADMIN_PERMISSION_NAME = "tournament.admin";

    private AdminPermission() {
    }

    public static boolean isAdmin(Permissible permissible) {
        return permissible.hasPermission(ADMIN_PERMISSION_NAME);
    }
}

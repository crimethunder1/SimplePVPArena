package de.simplepvparena.permission;

import org.bukkit.permissions.Permissible;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ViewPermission {

    public static final String VIEW_PERMISSION_NAME = "tournament.view";

    private ViewPermission() {
    }

    public static boolean mayView(Permissible permissible) {
        return AdminPermission.isAdmin(permissible) || permissible.hasPermission(VIEW_PERMISSION_NAME);
    }
}

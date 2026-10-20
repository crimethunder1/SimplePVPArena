package de.simplepvparena.permission;

import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.permissions.Permissible;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewPermissionTest {

    @Test
    void letsAPlayerWithTheViewPermissionView() {
        assertTrue(ViewPermission.mayView(holding(ViewPermission.VIEW_PERMISSION_NAME)));
    }

    @Test
    void letsAnAdminViewWithoutTheViewPermission() {
        assertTrue(ViewPermission.mayView(holding(AdminPermission.ADMIN_PERMISSION_NAME)));
    }

    @Test
    void refusesAPlayerWithNeitherPermission() {
        assertFalse(ViewPermission.mayView(holding()));
        assertFalse(ViewPermission.mayView(holding("tournament.spectate")));
    }

    @Test
    void doesNotMakeAViewerAnAdmin() {
        assertFalse(AdminPermission.isAdmin(holding(ViewPermission.VIEW_PERMISSION_NAME)));
    }

    private static Permissible holding(String... permissionNames) {
        Set<String> heldPermissions = Set.of(permissionNames);
        return SharedBukkitServer.proxy(Permissible.class, (method, arguments) -> switch (method.getName()) {
            case "hasPermission" -> heldPermissions.contains((String) arguments[0]);
            default -> throw new UnsupportedOperationException("Permissible." + method.getName());
        });
    }
}

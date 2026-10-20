package de.simplepvparena.command;

import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.permission.ViewPermission;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShippedPluginDescriptionTest {

    @Test
    void grantsTheAdminPermissionToOperatorsByDefault() {
        assertEquals(PermissionDefault.OP, findPermission(AdminPermission.ADMIN_PERMISSION_NAME).getDefault());
    }

    @Test
    void grantsTheSpectatePermissionToEveryoneByDefault() {
        assertEquals(PermissionDefault.TRUE, findPermission(TournamentCommand.SPECTATE_PERMISSION).getDefault());
    }

    @Test
    void grantsTheViewPermissionToEveryoneByDefault() {
        assertEquals(PermissionDefault.TRUE, findPermission(ViewPermission.VIEW_PERMISSION_NAME).getDefault());
    }

    private static Permission findPermission(String permissionName) {
        return shippedDescription().getPermissions().stream()
                .filter(permission -> permission.getName().equals(permissionName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(permissionName + " is missing from plugin.yml"));
    }

    private static PluginDescriptionFile shippedDescription() {
        try (InputStream shippedFile = ShippedPluginDescriptionTest.class.getResourceAsStream("/plugin.yml")) {
            assertNotNull(shippedFile, "plugin.yml is missing from the plugin resources");
            try (Reader reader = new InputStreamReader(shippedFile, StandardCharsets.UTF_8)) {
                return new PluginDescriptionFile(reader);
            }
        } catch (IOException | InvalidDescriptionException failure) {
            throw new IllegalStateException("plugin.yml could not be read", failure);
        }
    }
}

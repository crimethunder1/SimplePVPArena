package de.simplepvparena.arena;

import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface AreaInventoryStorage {

    void save(UUID playerUniqueId, ItemStack[] contents, List<ItemStack> looseItems) throws IOException;

    AreaInventoryLookup find(UUID playerUniqueId);

    void delete(UUID playerUniqueId) throws IOException;
}

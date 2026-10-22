package de.simplepvparena.registration;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public interface StationDisplayEntities {

    Entity spawnItemDisplay(Location location, ItemStack itemStack);

    Entity spawnTextDisplay(Location location, Component text);

    boolean isStationDisplay(Entity entity);
}

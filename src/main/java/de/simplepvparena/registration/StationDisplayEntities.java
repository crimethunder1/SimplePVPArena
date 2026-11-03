package de.simplepvparena.registration;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface StationDisplayEntities {

    Entity spawnItemDisplay(Location location, ItemStack itemStack);

    Entity spawnTextDisplay(Location location, Component text);

    List<Entity> spawnButton(Location location);

    Entity spawnClickArea(Location location);

    Entity spawnButtonCover(Block buttonBlock, ItemStack image);

    void changeText(Entity display, Component text);

    void pressButton(Entity cap);

    boolean isStationDisplay(Entity entity);
}

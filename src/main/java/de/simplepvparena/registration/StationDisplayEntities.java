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

    /**
     * Spawns the parts of the big button, the pressable cap is the last entity of the list.
     */
    List<Entity> spawnButton(Location location);

    Entity spawnClickArea(Location location);

    /**
     * Lays the image of the item flat onto the face of the button block, it moves in when the button is pressed.
     */
    Entity spawnButtonCover(Block buttonBlock, ItemStack image);

    void changeText(Entity display, Component text);

    void pressButton(Entity cap);

    boolean isStationDisplay(Entity entity);
}

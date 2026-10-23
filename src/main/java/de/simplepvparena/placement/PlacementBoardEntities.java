package de.simplepvparena.placement;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface PlacementBoardEntities {

    Entity spawnBoard(Location location, Component text);

    void changeText(Entity board, Component text);

    boolean isBoard(Entity entity);
}

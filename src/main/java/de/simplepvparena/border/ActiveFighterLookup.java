package de.simplepvparena.border;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
@FunctionalInterface
public interface ActiveFighterLookup {

    boolean isActiveFighter(UUID playerUniqueId);
}

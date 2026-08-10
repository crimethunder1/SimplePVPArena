package de.simplepvparena.border;

import java.util.UUID;

@FunctionalInterface
public interface ActiveFighterLookup {

    boolean isActiveFighter(UUID playerUniqueId);
}

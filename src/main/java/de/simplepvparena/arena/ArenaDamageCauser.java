package de.simplepvparena.arena;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum ArenaDamageCauser {
    NONE,
    OPPONENT,
    ADMIN,
    SELF,
    TEAM_MATE,
    OTHER_PLAYER,
    MOB,
    NON_LIVING_ENTITY
}

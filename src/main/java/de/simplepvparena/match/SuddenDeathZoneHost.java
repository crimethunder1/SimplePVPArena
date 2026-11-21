package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What a {@link SuddenDeathZone} needs from the running match.
 */
@NullMarked
public interface SuddenDeathZoneHost {

    Set<UUID> activeFighters();

    /** True while an admin paused the round timer, the zone neither shrinks nor hurts meanwhile. */
    boolean paused();

    /** Damages the given fighters at once and ends the round when a team fell. */
    void damage(Map<UUID, Double> damageByFighter);

    /** The zone broke down and removed itself, sudden death falls back to damaging every fighter. */
    void zoneFailed();
}

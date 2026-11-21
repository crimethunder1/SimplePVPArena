package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

/**
 * A zone that takes over the damage of sudden death, for example the shrinking laser ring.
 */
@NullMarked
public interface SuddenDeathZone {

    SuddenDeathZone NONE = new SuddenDeathZone() {
        @Override
        public boolean start(Match match, LaserZoneSettings settings, SuddenDeathZoneHost host) {
            return false;
        }

        @Override
        public void stop() {
        }
    };

    /**
     * Starts the zone for the running round of the match.
     *
     * @return false when the zone cannot be shown, sudden death then damages every fighter as before
     */
    boolean start(Match match, LaserZoneSettings settings, SuddenDeathZoneHost host);

    void stop();
}

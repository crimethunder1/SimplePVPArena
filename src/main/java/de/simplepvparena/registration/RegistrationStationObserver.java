package de.simplepvparena.registration;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface RegistrationStationObserver {

    void stationAdded(RegistrationStation station);

    void stationRemoved(RegistrationStation station);

    default void stationUsed(RegistrationStation station) {
    }
}

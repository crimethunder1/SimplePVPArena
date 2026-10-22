package de.simplepvparena.registration;

public interface RegistrationStationObserver {

    void stationAdded(RegistrationStation station);

    void stationRemoved(RegistrationStation station);
}

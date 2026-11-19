package de.simplepvparena.listener;

import de.simplepvparena.registration.RegistrationStation;
import de.simplepvparena.registration.RegistrationStationDisplays;
import de.simplepvparena.registration.RegistrationStationService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class RegistrationStationButtonListener implements Listener {

    private final RegistrationStationDisplays stationDisplays;
    private final RegistrationStationService stationService;

    public RegistrationStationButtonListener(RegistrationStationDisplays stationDisplays,
                                             RegistrationStationService stationService) {
        this.stationDisplays = Objects.requireNonNull(stationDisplays, "stationDisplays");
        this.stationService = Objects.requireNonNull(stationService, "stationService");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Optional<RegistrationStation> station = stationDisplays.findStationOfClickArea(event.getRightClicked());
        if (station.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            stationService.useStation(event.getPlayer(), station.get());
        }
    }
}

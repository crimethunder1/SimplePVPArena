package de.simplepvparena.kit;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitServicePlayerStateTest {

    private static final int MAXIMUM_AIR = 300;

    @Test
    void resetsAbsorptionArrowsFreezingAirFallAndVelocityWhenTheKitIsApplied() {
        RecordingPlayer recordingPlayer = new RecordingPlayer();

        new KitService().applyKit(recordingPlayer.player);

        assertEquals(List.of(0.0), recordingPlayer.argumentsOf("setAbsorptionAmount"));
        assertEquals(List.of(0, false), recordingPlayer.argumentsOf("setArrowsInBody"));
        assertEquals(List.of(0), recordingPlayer.argumentsOf("setFireTicks"));
        assertEquals(List.of(0), recordingPlayer.argumentsOf("setFreezeTicks"));
        assertEquals(List.of(MAXIMUM_AIR), recordingPlayer.argumentsOf("setRemainingAir"));
        assertEquals(List.of(0.0f), recordingPlayer.argumentsOf("setFallDistance"));
        assertEquals(List.of(new Vector()), recordingPlayer.argumentsOf("setVelocity"));
        assertEquals(List.of(GameMode.SURVIVAL), recordingPlayer.argumentsOf("setGameMode"));
    }

    private static final class RecordingPlayer {

        private final UUID uniqueId = UUID.randomUUID();
        private final Map<String, List<Object>> argumentsByMethodName = new LinkedHashMap<>();
        private final Player player;

        private RecordingPlayer() {
            PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(
                    PlayerInventory.class.getClassLoader(), new Class<?>[] {PlayerInventory.class},
                    (proxyInstance, method, arguments) -> switch (method.getName()) {
                        case "clear", "setItem" -> null;
                        default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
                    });
            this.player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                    new Class<?>[] {Player.class}, (proxyInstance, method, arguments) -> {
                        if (method.getName().startsWith("set") || method.getName().equals("removePotionEffect")) {
                            argumentsByMethodName.computeIfAbsent(method.getName(), name -> new ArrayList<>())
                                    .addAll(List.of(arguments));
                            return null;
                        }
                        return switch (method.getName()) {
                            case "getUniqueId" -> uniqueId;
                            case "getInventory" -> inventory;
                            case "getActivePotionEffects" -> List.of();
                            case "getAttribute" -> null;
                            case "getMaximumAir" -> MAXIMUM_AIR;
                            case "getCooldown" -> 0;
                            case "hashCode" -> System.identityHashCode(proxyInstance);
                            default -> throw new UnsupportedOperationException("Player." + method.getName());
                        };
                    });
        }

        private List<Object> argumentsOf(String methodName) {
            return argumentsByMethodName.getOrDefault(methodName, List.of());
        }
    }
}

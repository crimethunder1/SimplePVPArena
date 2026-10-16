package de.simplepvparena.kit;

import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitServicePlayerStateTest {

    private static final int MAXIMUM_AIR = 300;

    private static final String PLUGIN_NAMESPACE = "simplepvparena";

    private static final NamespacedKey KIT_APPLIED_KEY =
            new NamespacedKey(createPlugin(), KitService.KIT_APPLIED_KEY_NAME);

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

    @Test
    void resetsTheWholePlayerStateAndClearsTheInventoryWhenTheKitIsCleared() {
        RecordingPlayer recordingPlayer = new RecordingPlayer();

        new KitService().clearKit(recordingPlayer.player);

        assertEquals(1, recordingPlayer.inventoryClearCount, "the inventory is cleared once");
        assertEquals(List.of(0.0), recordingPlayer.argumentsOf("setAbsorptionAmount"));
        assertEquals(List.of(0, false), recordingPlayer.argumentsOf("setArrowsInBody"));
        assertEquals(List.of(0), recordingPlayer.argumentsOf("setFireTicks"));
        assertEquals(List.of(0), recordingPlayer.argumentsOf("setFreezeTicks"));
        assertEquals(List.of(MAXIMUM_AIR), recordingPlayer.argumentsOf("setRemainingAir"));
        assertEquals(List.of(0.0f), recordingPlayer.argumentsOf("setFallDistance"));
        assertEquals(List.of(new Vector()), recordingPlayer.argumentsOf("setVelocity"));
        assertEquals(List.of(GameMode.SURVIVAL), recordingPlayer.argumentsOf("setGameMode"));
        assertEquals(List.of(20.0), recordingPlayer.argumentsOf("setHealth"));
        assertEquals(List.of(20), recordingPlayer.argumentsOf("setFoodLevel"));
        assertEquals(List.of(20.0f), recordingPlayer.argumentsOf("setSaturation"));
        assertEquals(List.of(0.0f), recordingPlayer.argumentsOf("setExp"));
        assertEquals(List.of(0), recordingPlayer.argumentsOf("setLevel"));
    }

    @Test
    void storesTheMarkerWhenTheKitIsAppliedAndRemovesItWhenTheKitIsCleared() {
        RecordingPlayer recordingPlayer = new RecordingPlayer();
        KitService kitService = new KitService(createPlugin());

        assertFalse(kitService.hasAppliedKit(recordingPlayer.player), "a fresh player carries no marker");

        kitService.applyKit(recordingPlayer.player);

        assertEquals((byte) 1, recordingPlayer.persistentData.get(KIT_APPLIED_KEY));
        assertTrue(kitService.hasAppliedKit(recordingPlayer.player));

        kitService.clearKit(recordingPlayer.player);

        assertFalse(recordingPlayer.persistentData.containsKey(KIT_APPLIED_KEY));
        assertFalse(kitService.hasAppliedKit(recordingPlayer.player));
    }

    @Test
    void followsTheMarkerAloneWhenAnotherKitServiceReadsIt() {
        RecordingPlayer recordingPlayer = new RecordingPlayer();
        recordingPlayer.persistentData.put(KIT_APPLIED_KEY, (byte) 1);

        KitService kitServiceAfterARestart = new KitService(createPlugin());

        assertTrue(kitServiceAfterARestart.hasAppliedKit(recordingPlayer.player),
                "the marker survives the loss of everything held in memory");
    }

    @Test
    void neverTouchesThePersistentDataWithoutAPlugin() {
        RecordingPlayer recordingPlayer = new RecordingPlayer();
        KitService kitService = new KitService();

        kitService.applyKit(recordingPlayer.player);
        assertFalse(kitService.hasAppliedKit(recordingPlayer.player));
        kitService.clearKit(recordingPlayer.player);

        assertEquals(0, recordingPlayer.persistentDataContainerRequestCount);
        assertTrue(recordingPlayer.persistentData.isEmpty());
    }

    private static Plugin createPlugin() {
        return SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getName", "namespace" -> PLUGIN_NAMESPACE;
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    private static final class RecordingPlayer {

        private final UUID uniqueId = UUID.randomUUID();
        private final Map<String, List<Object>> argumentsByMethodName = new LinkedHashMap<>();
        private final Map<Object, Object> persistentData = new LinkedHashMap<>();
        private final Player player;

        private int inventoryClearCount;
        private int persistentDataContainerRequestCount;

        private RecordingPlayer() {
            PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(
                    PlayerInventory.class.getClassLoader(), new Class<?>[] {PlayerInventory.class},
                    (proxyInstance, method, arguments) -> switch (method.getName()) {
                        case "clear" -> {
                            inventoryClearCount++;
                            yield null;
                        }
                        case "setItem" -> null;
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
                            case "getPersistentDataContainer" -> {
                                persistentDataContainerRequestCount++;
                                yield SharedBukkitServer.createPersistentDataContainer(persistentData);
                            }
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

package de.simplepvparena.support;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SharedBukkitServer {

    private static final Map<String, World> WORLDS = new LinkedHashMap<>();
    private static final List<Function<UUID, Player>> PLAYER_LOOKUPS = new ArrayList<>();

    private SharedBukkitServer() {
    }

    public static synchronized void register(World world, Function<UUID, Player> playerLookup) {
        WORLDS.put(world.getName(), world);
        PLAYER_LOOKUPS.add(playerLookup);
        install();
    }

    public static synchronized void install() {
        if (Bukkit.getServer() != null) {
            return;
        }
        try {
            Field serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, create());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("the Bukkit singleton could not be set for the test", failure);
        }
    }

    private static synchronized World findWorld(Object worldName) {
        return WORLDS.get(worldName);
    }

    private static synchronized Player findPlayer(Object playerUniqueId) {
        for (Function<UUID, Player> playerLookup : PLAYER_LOOKUPS) {
            Player player = playerLookup.apply((UUID) playerUniqueId);
            if (player != null) {
                return player;
            }
        }
        return null;
    }

    private static Server create() {
        Logger quietLogger = Logger.getLogger("BukkitSingleton");
        quietLogger.setLevel(Level.OFF);
        Object itemFactory = createItemFactory();
        return proxy(Server.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> findWorld(arguments[0]);
            case "getPlayer" -> findPlayer(arguments[0]);
            case "getLogger" -> quietLogger;
            case "getName" -> "SharedBukkitServer";
            case "getVersion", "getBukkitVersion" -> "test";
            case "getItemFactory" -> itemFactory;
            default -> throw new UnsupportedOperationException("Bukkit." + method.getName());
        });
    }

    private static Object createItemFactory() {
        return proxy(ItemFactory.class, (method, arguments) -> switch (method.getName()) {
            case "getItemMeta" -> createItemMeta();
            case "isApplicable" -> true;
            case "asMetaFor" -> arguments[0];
            case "updateMaterial" -> arguments[1];
            default -> throw new UnsupportedOperationException("ItemFactory." + method.getName());
        });
    }

    private static Object createItemMeta() {
        Map<Object, Object> persistentData = new LinkedHashMap<>();
        Object[] self = new Object[1];
        self[0] = proxy(ItemMeta.class, (method, arguments) -> switch (method.getName()) {
            case "displayName", "lore" -> null;
            case "clone" -> self[0];
            case "getPersistentDataContainer" -> createPersistentDataContainer(persistentData);
            default -> throw new UnsupportedOperationException("ItemMeta." + method.getName());
        });
        return self[0];
    }

    private static Object createPersistentDataContainer(Map<Object, Object> persistentData) {
        return proxy(PersistentDataContainer.class, (method, arguments) -> switch (method.getName()) {
            case "set" -> {
                persistentData.put(arguments[0], arguments[2]);
                yield null;
            }
            case "get" -> persistentData.get(arguments[0]);
            case "has" -> persistentData.containsKey(arguments[0]);
            default -> throw new UnsupportedOperationException(
                    "PersistentDataContainer." + method.getName());
        });
    }

    public static <T> T proxy(Class<T> type, FakeCall call) {
        InvocationHandler handler = (proxyInstance, method, arguments) -> switch (method.getName()) {
            case "equals" -> proxyInstance == arguments[0];
            case "hashCode" -> System.identityHashCode(proxyInstance);
            case "toString" -> "fake " + type.getSimpleName();
            default -> call.invoke(method, arguments);
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    @FunctionalInterface
    public interface FakeCall {

        Object invoke(Method method, Object[] arguments);
    }
}

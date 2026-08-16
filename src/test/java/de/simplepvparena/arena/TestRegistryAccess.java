package de.simplepvparena.arena;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Registry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Stream;

public final class TestRegistryAccess implements RegistryAccess {

    @Override
    @SuppressWarnings({"unchecked", "removal"})
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return (Registry<T>) createEmptyRegistry();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> registryKey) {
        return (Registry<T>) createEmptyRegistry();
    }

    private static Registry<?> createEmptyRegistry() {
        return (Registry<?>) proxy(Registry.class, (proxyInstance, method, arguments) ->
                switch (method.getName()) {
                    case "get", "getOrThrow", "getKey" -> null;
                    case "iterator" -> List.of().iterator();
                    case "stream" -> Stream.empty();
                    case "equals" -> proxyInstance == arguments[0];
                    case "hashCode" -> System.identityHashCode(proxyInstance);
                    case "toString" -> "empty test registry";
                    default -> throw new UnsupportedOperationException("Registry." + method.getName());
                });
    }

    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }
}

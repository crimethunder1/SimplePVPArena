package de.simplepvparena.support;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Locale;

public final class FakeBlockData {

    private FakeBlockData() {
    }

    public static BlockData of(Material material) {
        return create(material, "", false);
    }

    public static BlockData of(Material material, String properties) {
        return create(material, properties, false);
    }

    public static BlockData withBlockEntity(Material material, String properties) {
        return create(material, properties, true);
    }

    public static boolean holdsBlockEntity(BlockData blockData) {
        return blockData.createBlockState() instanceof TileState;
    }

    private static BlockData create(Material material, String properties, boolean blockEntity) {
        String asString = "minecraft:" + material.name().toLowerCase(Locale.ROOT)
                + (properties.isEmpty() ? "" : "[" + properties + "]");
        InvocationHandler handler = (proxyInstance, method, arguments) -> switch (method.getName()) {
            case "equals" -> arguments[0] instanceof BlockData other && asString.equals(other.getAsString());
            case "hashCode" -> asString.hashCode();
            case "toString", "getAsString" -> asString;
            case "getMaterial" -> material;
            case "clone" -> proxyInstance;
            case "createBlockState" -> blockEntity
                    ? SharedBukkitServer.proxy(TileState.class, (stateMethod, stateArguments) -> {
                        throw new UnsupportedOperationException("TileState." + stateMethod.getName());
                    })
                    : SharedBukkitServer.proxy(BlockState.class, (stateMethod, stateArguments) -> {
                        throw new UnsupportedOperationException("BlockState." + stateMethod.getName());
                    });
            default -> throw new UnsupportedOperationException("BlockData." + method.getName());
        };
        return (BlockData) Proxy.newProxyInstance(BlockData.class.getClassLoader(),
                new Class<?>[] {BlockData.class}, handler);
    }
}

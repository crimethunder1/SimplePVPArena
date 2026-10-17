package de.simplepvparena.support;

import de.simplepvparena.kit.KitItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class FakeKitItemSerializer implements KitItemSerializer {

    private final Set<String> undecodableLabels;

    public FakeKitItemSerializer(String... undecodableLabels) {
        this.undecodableLabels = Set.of(undecodableLabels);
    }

    @Override
    public byte[] encode(ItemStack itemStack) {
        return ((EncodableItemStack) itemStack).getLabel().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ItemStack decode(byte[] itemBytes) {
        String label = new String(itemBytes, StandardCharsets.UTF_8);
        if (undecodableLabels.contains(label)) {
            throw new IllegalArgumentException("undecodable item " + label);
        }
        return new EncodableItemStack(label);
    }
}

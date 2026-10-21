package de.simplepvparena.visual;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RecordingCustomVisuals implements CustomVisuals {

    private final Map<String, String> fontImages = new LinkedHashMap<>();
    private final Map<String, ItemStack> items = new LinkedHashMap<>();
    private final Set<String> failingIdentifiers = new LinkedHashSet<>();
    private final List<String> lookups = new ArrayList<>();

    private boolean ready = true;

    public RecordingCustomVisuals withFontImage(String identifier, String fontImage) {
        fontImages.put(identifier, fontImage);
        return this;
    }

    public RecordingCustomVisuals withItem(String identifier, ItemStack itemStack) {
        items.put(identifier, itemStack);
        return this;
    }

    public RecordingCustomVisuals failingFor(String identifier) {
        failingIdentifiers.add(identifier);
        return this;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public List<String> lookups() {
        return lookups;
    }

    @Override
    public Optional<String> findFontImage(String identifier) {
        lookups.add("font image " + identifier);
        failIfAsked(identifier);
        return ready ? Optional.ofNullable(fontImages.get(identifier)) : Optional.empty();
    }

    @Override
    public Optional<ItemStack> findItem(String identifier) {
        lookups.add("item " + identifier);
        failIfAsked(identifier);
        ItemStack itemStack = ready ? items.get(identifier) : null;
        return itemStack == null ? Optional.empty() : Optional.of(itemStack.clone());
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    private void failIfAsked(String identifier) {
        if (failingIdentifiers.contains(identifier)) {
            throw new IllegalStateException("ItemsAdder is broken for " + identifier);
        }
    }
}

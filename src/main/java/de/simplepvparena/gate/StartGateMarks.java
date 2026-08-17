package de.simplepvparena.gate;

import de.simplepvparena.arena.BlockPositionCodec;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class StartGateMarks {

    private static final long[][] NO_LAYERS = new long[0][];

    private final Map<String, TreeMap<Long, Material>> materialsByPositionByWorld = new LinkedHashMap<>();

    public boolean mark(String worldName, int blockX, int blockY, int blockZ, Material material) {
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(material, "material");
        return materialsByPositionByWorld
                .computeIfAbsent(worldName, name -> new TreeMap<>())
                .putIfAbsent(BlockPositionCodec.encode(blockX, blockY, blockZ), material) == null;
    }

    public boolean clearMark(String worldName, int blockX, int blockY, int blockZ) {
        TreeMap<Long, Material> materialsByPosition = materialsByPositionByWorld.get(worldName);
        if (materialsByPosition == null) {
            return false;
        }
        boolean removed = materialsByPosition.remove(BlockPositionCodec.encode(blockX, blockY, blockZ)) != null;
        if (materialsByPosition.isEmpty()) {
            materialsByPositionByWorld.remove(worldName);
        }
        return removed;
    }

    public boolean isMarked(String worldName, int blockX, int blockY, int blockZ) {
        TreeMap<Long, Material> materialsByPosition = materialsByPositionByWorld.get(worldName);
        return materialsByPosition != null
                && materialsByPosition.containsKey(BlockPositionCodec.encode(blockX, blockY, blockZ));
    }

    public Optional<Material> findMaterial(String worldName, int blockX, int blockY, int blockZ) {
        TreeMap<Long, Material> materialsByPosition = materialsByPositionByWorld.get(worldName);
        if (materialsByPosition == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                materialsByPosition.get(BlockPositionCodec.encode(blockX, blockY, blockZ)));
    }

    public boolean isEmpty() {
        return materialsByPositionByWorld.isEmpty();
    }

    public Set<String> getWorldNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(materialsByPositionByWorld.keySet()));
    }

    public Map<Long, Material> getMarks(String worldName) {
        TreeMap<Long, Material> materialsByPosition = materialsByPositionByWorld.get(worldName);
        return materialsByPosition == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(materialsByPosition));
    }

    public int getMarkedBlockCount() {
        int markedBlockCount = 0;
        for (TreeMap<Long, Material> materialsByPosition : materialsByPositionByWorld.values()) {
            markedBlockCount += materialsByPosition.size();
        }
        return markedBlockCount;
    }

    public int getMarkedBlockCount(String worldName) {
        TreeMap<Long, Material> materialsByPosition = materialsByPositionByWorld.get(worldName);
        return materialsByPosition == null ? 0 : materialsByPosition.size();
    }

    public long[][] collectLayersTopDown(String worldName) {
        TreeMap<Long, Material> materialsByPosition = materialsByPositionByWorld.get(worldName);
        if (materialsByPosition == null) {
            return NO_LAYERS;
        }
        TreeMap<Integer, List<Long>> positionsByHeight = new TreeMap<>(Collections.reverseOrder());
        for (long encodedPosition : materialsByPosition.keySet()) {
            positionsByHeight
                    .computeIfAbsent(BlockPositionCodec.decodeY(encodedPosition), height -> new ArrayList<>())
                    .add(encodedPosition);
        }
        long[][] layers = new long[positionsByHeight.size()][];
        int layerIndex = 0;
        for (List<Long> positionsOfLayer : positionsByHeight.values()) {
            layers[layerIndex] = toArray(positionsOfLayer);
            layerIndex++;
        }
        return layers;
    }

    public int getLayerCount(String worldName) {
        return collectLayersTopDown(worldName).length;
    }

    public void replaceWorld(String worldName, Map<Long, Material> materialsByPosition) {
        Objects.requireNonNull(worldName, "worldName");
        if (materialsByPosition.isEmpty()) {
            materialsByPositionByWorld.remove(worldName);
            return;
        }
        materialsByPositionByWorld.put(worldName, new TreeMap<>(materialsByPosition));
    }

    public void clear() {
        materialsByPositionByWorld.clear();
    }

    private static long[] toArray(List<Long> positions) {
        long[] encodedPositions = new long[positions.size()];
        for (int index = 0; index < encodedPositions.length; index++) {
            encodedPositions[index] = positions.get(index);
        }
        return encodedPositions;
    }
}

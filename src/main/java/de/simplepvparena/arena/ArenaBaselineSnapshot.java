package de.simplepvparena.arena;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ArenaBaselineSnapshot implements ArenaCleaningTarget {

    public static final long MAXIMUM_BLOCK_COUNT = 8_000_000L;

    private final ArenaShape shape;
    private final ArenaShape capturedVolume;
    private final int depthBelowFloor;
    private final short[] paletteIndexes;
    private final BlockData[] palette;
    private final BitSet blockEntityPaletteIndexes;
    private final Map<Integer, BlockState> blockEntitiesByBlockIndex;
    private final int distinctMaterialCount;
    private final int distinctBlockStateCount;
    private final long nonAirBlockCount;

    ArenaBaselineSnapshot(ArenaShape shape, int depthBelowFloor, short[] paletteIndexes, BlockData[] palette,
                          BitSet blockEntityPaletteIndexes, Map<Integer, BlockState> blockEntitiesByBlockIndex) {
        this.shape = Objects.requireNonNull(shape, "shape");
        this.depthBelowFloor = depthBelowFloor;
        this.capturedVolume = shape.withFloorLoweredBy(depthBelowFloor);
        this.paletteIndexes = Objects.requireNonNull(paletteIndexes, "paletteIndexes");
        this.palette = Objects.requireNonNull(palette, "palette");
        this.blockEntityPaletteIndexes = Objects.requireNonNull(blockEntityPaletteIndexes,
                "blockEntityPaletteIndexes");
        this.blockEntitiesByBlockIndex = Objects.requireNonNull(blockEntitiesByBlockIndex,
                "blockEntitiesByBlockIndex");
        long[] usageByPaletteIndex = new long[palette.length];
        for (short paletteIndex : paletteIndexes) {
            usageByPaletteIndex[Short.toUnsignedInt(paletteIndex)]++;
        }
        Set<Material> usedMaterials = EnumSet.noneOf(Material.class);
        int usedBlockStateCount = 0;
        long nonAirCount = 0L;
        for (int paletteIndex = 0; paletteIndex < palette.length; paletteIndex++) {
            if (usageByPaletteIndex[paletteIndex] == 0L) {
                continue;
            }
            Material material = palette[paletteIndex].getMaterial();
            usedMaterials.add(material);
            usedBlockStateCount++;
            if (material != Material.AIR) {
                nonAirCount += usageByPaletteIndex[paletteIndex];
            }
        }
        this.distinctMaterialCount = usedMaterials.size();
        this.distinctBlockStateCount = usedBlockStateCount;
        this.nonAirBlockCount = nonAirCount;
    }

    public ArenaShape getShape() {
        return shape;
    }

    public ArenaShape getCapturedVolume() {
        return capturedVolume;
    }

    public int getDepthBelowFloor() {
        return depthBelowFloor;
    }

    public long getBlockCount() {
        return paletteIndexes.length;
    }

    public int getDistinctMaterialCount() {
        return distinctMaterialCount;
    }

    public int getDistinctBlockStateCount() {
        return distinctBlockStateCount;
    }

    public int getPaletteSize() {
        return palette.length;
    }

    public int getBlockEntityCount() {
        return blockEntitiesByBlockIndex.size();
    }

    public long getNonAirBlockCount() {
        return nonAirBlockCount;
    }

    public boolean covers(ArenaShape otherShape) {
        return shape.hasSameVolume(otherShape);
    }

    public Optional<Material> findMaterialAt(int blockX, int blockY, int blockZ) {
        return findBlockDataAt(blockX, blockY, blockZ).map(BlockData::getMaterial);
    }

    public Optional<BlockData> findBlockDataAt(int blockX, int blockY, int blockZ) {
        long blockIndex = capturedVolume.findBlockIndex(blockX, blockY, blockZ);
        if (blockIndex < 0L) {
            return Optional.empty();
        }
        return Optional.of(palette[Short.toUnsignedInt(paletteIndexes[(int) blockIndex])]);
    }

    public boolean holdsCapturedMaterial(int blockX, int blockY, int blockZ, Material currentMaterial) {
        return findMaterialAt(blockX, blockY, blockZ)
                .filter(capturedMaterial -> capturedMaterial == currentMaterial)
                .isPresent();
    }

    public boolean isBelowTheCapturedVolume(int blockY) {
        return blockY < capturedVolume.getFloorY();
    }

    @Override
    public Material findMaterial(int blockX, int blockY, int blockZ) {
        return findMaterialAt(blockX, blockY, blockZ).orElse(Material.AIR);
    }

    @Override
    public Optional<BlockData> findBlockData(int blockX, int blockY, int blockZ) {
        return findBlockDataAt(blockX, blockY, blockZ);
    }

    @Override
    public Optional<BlockState> findBlockEntity(int blockX, int blockY, int blockZ) {
        long blockIndex = capturedVolume.findBlockIndex(blockX, blockY, blockZ);
        if (blockIndex < 0L
                || !blockEntityPaletteIndexes.get(Short.toUnsignedInt(paletteIndexes[(int) blockIndex]))) {
            return Optional.empty();
        }
        return Optional.ofNullable(blockEntitiesByBlockIndex.get((int) blockIndex));
    }

    @Override
    public String describeChange() {
        return "were restored to the baseline the last calculation took";
    }

    public String describe() {
        return getBlockCount() + " blocks reaching " + depthBelowFloor + " blocks below the floor, "
                + distinctMaterialCount + " different materials in " + distinctBlockStateCount
                + " different block states, " + nonAirBlockCount + " of them are not air, "
                + blockEntitiesByBlockIndex.size() + " hold contents such as text, patterns or items";
    }
}

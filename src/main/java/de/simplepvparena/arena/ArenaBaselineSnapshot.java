package de.simplepvparena.arena;

import org.bukkit.Material;

import java.util.Objects;
import java.util.Optional;

public final class ArenaBaselineSnapshot implements ArenaCleaningTarget {

    public static final long MAXIMUM_BLOCK_COUNT = 8_000_000L;

    private static final Material[] MATERIALS_BY_ORDINAL = Material.values();

    static {
        if (MATERIALS_BY_ORDINAL.length > Short.MAX_VALUE) {
            throw new IllegalStateException("there are " + MATERIALS_BY_ORDINAL.length
                    + " materials, a baseline stores them as shorts and holds at most " + Short.MAX_VALUE);
        }
    }

    private final ArenaShape shape;
    private final short[] materialOrdinals;
    private final int distinctMaterialCount;
    private final long nonAirBlockCount;

    ArenaBaselineSnapshot(ArenaShape shape, short[] materialOrdinals) {
        this.shape = Objects.requireNonNull(shape, "shape");
        this.materialOrdinals = Objects.requireNonNull(materialOrdinals, "materialOrdinals");
        boolean[] seenMaterials = new boolean[MATERIALS_BY_ORDINAL.length];
        int distinctCount = 0;
        long nonAirCount = 0L;
        for (short materialOrdinal : materialOrdinals) {
            if (!seenMaterials[materialOrdinal]) {
                seenMaterials[materialOrdinal] = true;
                distinctCount++;
            }
            if (MATERIALS_BY_ORDINAL[materialOrdinal] != Material.AIR) {
                nonAirCount++;
            }
        }
        this.distinctMaterialCount = distinctCount;
        this.nonAirBlockCount = nonAirCount;
    }

    static short toOrdinal(Material material) {
        return (short) material.ordinal();
    }

    public ArenaShape getShape() {
        return shape;
    }

    public long getBlockCount() {
        return materialOrdinals.length;
    }

    public int getDistinctMaterialCount() {
        return distinctMaterialCount;
    }

    public long getNonAirBlockCount() {
        return nonAirBlockCount;
    }

    public boolean covers(ArenaShape otherShape) {
        return shape.hasSameVolume(otherShape);
    }

    public Optional<Material> findMaterialAt(int blockX, int blockY, int blockZ) {
        long blockIndex = shape.findBlockIndex(blockX, blockY, blockZ);
        if (blockIndex < 0L) {
            return Optional.empty();
        }
        return Optional.of(MATERIALS_BY_ORDINAL[materialOrdinals[(int) blockIndex]]);
    }

    public boolean holdsCapturedMaterial(int blockX, int blockY, int blockZ, Material currentMaterial) {
        return findMaterialAt(blockX, blockY, blockZ)
                .filter(capturedMaterial -> capturedMaterial == currentMaterial)
                .isPresent();
    }

    public boolean isBelowTheCapturedVolume(int blockY) {
        return blockY < shape.getFloorY();
    }

    @Override
    public Material findMaterial(int blockX, int blockY, int blockZ) {
        return findMaterialAt(blockX, blockY, blockZ).orElse(Material.AIR);
    }

    @Override
    public String describeChange() {
        return "were restored to the baseline the last calculation took";
    }

    public String describe() {
        return getBlockCount() + " blocks, " + distinctMaterialCount + " different materials, "
                + nonAirBlockCount + " of them are not air";
    }
}

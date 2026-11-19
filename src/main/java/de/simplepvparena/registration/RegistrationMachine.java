package de.simplepvparena.registration;

import org.bukkit.block.BlockFace;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NullMarked
public record RegistrationMachine(String worldName, int blockX, int blockY, int blockZ, BlockFace facing) {

    public static final int WIDTH = 2;
    public static final int HEIGHT = 2;

    public RegistrationMachine {
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(facing, "facing");
        if (!isHorizontal(facing)) {
            throw new IllegalArgumentException("facing " + facing + " is no horizontal direction");
        }
    }

    public static boolean isHorizontal(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST;
    }

    public BlockFace right() {
        return switch (facing) {
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            case NORTH -> BlockFace.EAST;
            default -> BlockFace.SOUTH;
        };
    }

    public List<int[]> blocks() {
        BlockFace right = right();
        List<int[]> blocks = new ArrayList<>();
        for (int column = 0; column < WIDTH; column++) {
            for (int row = 0; row < HEIGHT; row++) {
                blocks.add(new int[] {blockX + right.getModX() * column, blockY + row,
                        blockZ + right.getModZ() * column});
            }
        }
        return blocks;
    }

    public boolean contains(String otherWorldName, int x, int y, int z) {
        if (!worldName.equals(otherWorldName)) {
            return false;
        }
        for (int[] block : blocks()) {
            if (block[0] == x && block[1] == y && block[2] == z) {
                return true;
            }
        }
        return false;
    }

    public int minimumX() {
        return Math.min(blockX, blockX + right().getModX());
    }

    public int minimumZ() {
        return Math.min(blockZ, blockZ + right().getModZ());
    }

    public double frontCentreX() {
        return minimumX() + (right().getModX() != 0 ? 1.0D : 0.5D) + facing.getModX() * 0.5D;
    }

    public double frontCentreZ() {
        return minimumZ() + (right().getModZ() != 0 ? 1.0D : 0.5D) + facing.getModZ() * 0.5D;
    }
}

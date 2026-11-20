package de.simplepvparena.placement;

import org.bukkit.block.BlockFace;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NullMarked
public record BracketBoard(String worldName, int blockX, int blockY, int blockZ, BlockFace facing) {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 4;

    private static final int FIRST_COLUMN = -(WIDTH / 2);

    public BracketBoard {
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(facing, "facing");
        if (!isHorizontal(facing)) {
            throw new IllegalArgumentException("facing " + facing + " is no horizontal direction");
        }
    }

    public static boolean isHorizontal(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST;
    }

    public BlockFace side() {
        return facing == BlockFace.NORTH || facing == BlockFace.SOUTH ? BlockFace.EAST : BlockFace.SOUTH;
    }

    public List<int[]> blocks() {
        BlockFace side = side();
        List<int[]> blocks = new ArrayList<>();
        for (int column = FIRST_COLUMN; column < FIRST_COLUMN + WIDTH; column++) {
            for (int row = 0; row < HEIGHT; row++) {
                blocks.add(new int[] {blockX + side.getModX() * column, blockY + row,
                        blockZ + side.getModZ() * column});
            }
        }
        return blocks;
    }

    public boolean contains(String otherWorldName, int x, int y, int z) {
        if (!worldName.equals(otherWorldName) || y < blockY || y >= blockY + HEIGHT) {
            return false;
        }
        BlockFace side = side();
        int column = side.getModX() != 0 ? x - blockX : z - blockZ;
        int depth = side.getModX() != 0 ? z - blockZ : x - blockX;
        return depth == 0 && column >= FIRST_COLUMN && column < FIRST_COLUMN + WIDTH;
    }

    public double frontCentreX() {
        return side().getModX() != 0 ? blockX + FIRST_COLUMN + WIDTH / 2.0D
                : blockX + 0.5D + facing.getModX() * 0.5D;
    }

    public double frontCentreZ() {
        return side().getModZ() != 0 ? blockZ + FIRST_COLUMN + WIDTH / 2.0D
                : blockZ + 0.5D + facing.getModZ() * 0.5D;
    }
}

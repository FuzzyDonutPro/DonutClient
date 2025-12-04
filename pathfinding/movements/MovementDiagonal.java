package com.donut.client.pathfinding.movements;

import com.donut.client.utils.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class MovementDiagonal extends Movement {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public MovementDiagonal(BlockPos src, BlockPos dest) {
        super(src, dest);
        this.cost = 1.414; // sqrt(2) for diagonal
    }

    @Override
    public boolean calculate() {
        // Check destination is walkable
        if (!isWalkable(dest)) {
            valid = false;
            return false;
        }

        // Check headroom
        if (!BlockUtils.isAir(dest.up()) && !BlockUtils.isPassable(dest.up())) {
            valid = false;
            return false;
        }

        // Check ground
        if (!BlockUtils.isSolid(dest.down())) {
            valid = false;
            return false;
        }

        // Check both cardinal directions are clear (no corner cutting)
        int dx = dest.getX() - src.getX();
        int dz = dest.getZ() - src.getZ();

        BlockPos side1 = src.add(dx, 0, 0);
        BlockPos side2 = src.add(0, 0, dz);

        if (!isWalkable(side1) || !isWalkable(side2)) {
            valid = false;
            return false;
        }

        valid = true;
        return true;
    }

    @Override
    public BlockPos[] getPositionsToCheck() {
        int dx = dest.getX() - src.getX();
        int dz = dest.getZ() - src.getZ();

        return new BlockPos[]{
                dest,
                dest.up(),
                dest.down(),
                src.add(dx, 0, 0), // Check corners
                src.add(0, 0, dz)
        };
    }

    @Override
    public String getName() {
        return "MovementDiagonal";
    }

    private boolean isWalkable(BlockPos pos) {
        return BlockUtils.isAir(pos) || BlockUtils.isPassable(pos);
    }
}
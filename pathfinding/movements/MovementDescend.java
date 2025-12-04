package com.donut.client.pathfinding.movements;

import com.donut.client.utils.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class MovementDescend extends Movement {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int MAX_FALL = 10;

    public MovementDescend(BlockPos src, BlockPos dest) {
        super(src, dest);

        int fallDistance = src.getY() - dest.getY();
        // Small falls are cheap, big falls are risky
        this.cost = 1.0 + (fallDistance * 0.1);
    }

    @Override
    public boolean calculate() {
        int fallDistance = src.getY() - dest.getY();

        // Check fall distance
        if (fallDistance < 1 || fallDistance > MAX_FALL) {
            valid = false;
            return false;
        }

        // Check destination is walkable
        if (!isWalkable(dest)) {
            valid = false;
            return false;
        }

        // Check headroom at destination
        if (!BlockUtils.isAir(dest.up()) && !BlockUtils.isPassable(dest.up())) {
            valid = false;
            return false;
        }

        // Check ground at destination (must land on something solid or in water)
        if (!BlockUtils.isSolid(dest.down()) && !BlockUtils.isLiquid(dest)) {
            valid = false;
            return false;
        }

        // Check path down is clear
        for (int y = src.getY() - 1; y > dest.getY(); y--) {
            BlockPos check = new BlockPos(dest.getX(), y, dest.getZ());
            if (!isWalkable(check)) {
                valid = false;
                return false;
            }
        }

        valid = true;
        return true;
    }

    @Override
    public BlockPos[] getPositionsToCheck() {
        int fallDistance = src.getY() - dest.getY();
        BlockPos[] positions = new BlockPos[fallDistance + 3];

        positions[0] = dest;
        positions[1] = dest.up();
        positions[2] = dest.down();

        // Check all blocks in fall path
        for (int i = 1; i < fallDistance; i++) {
            positions[i + 2] = new BlockPos(dest.getX(), src.getY() - i, dest.getZ());
        }

        return positions;
    }

    @Override
    public String getName() {
        return "MovementDescend";
    }

    private boolean isWalkable(BlockPos pos) {
        return BlockUtils.isAir(pos) || BlockUtils.isPassable(pos);
    }
}
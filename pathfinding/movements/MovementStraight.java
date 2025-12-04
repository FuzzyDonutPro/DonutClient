package com.donut.client.pathfinding.movements;

import com.donut.client.utils.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class MovementStraight extends Movement {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public MovementStraight(BlockPos src, BlockPos dest) {
        super(src, dest);
        this.cost = 1.0;
    }

    @Override
    public boolean calculate() {
        if (mc.world == null) {
            valid = false;
            return false;
        }

        // Must be same Y level and adjacent
        if (dest.getY() != src.getY()) {
            valid = false;
            return false;
        }

        int distance = Math.abs(dest.getX() - src.getX()) + Math.abs(dest.getZ() - src.getZ());
        if (distance != 1) {
            valid = false;
            return false;
        }

        // STRICT: Check destination has NO collision
        if (BlockUtils.hasCollision(dest)) {
            valid = false;
            return false;
        }

        // STRICT: Check headroom has NO collision
        if (BlockUtils.hasCollision(dest.up())) {
            valid = false;
            return false;
        }

        // CRITICAL: Must have solid ground below
        BlockPos below = dest.down();
        if (!BlockUtils.isSolid(below)) {
            // Exception: if in water or on ladder, it's okay
            if (!BlockUtils.isLiquid(dest) && !BlockUtils.isClimbable(dest)) {
                valid = false;
                return false;
            }
        }

        valid = true;
        return true;
    }

    @Override
    public BlockPos[] getPositionsToCheck() {
        return new BlockPos[]{
                dest,
                dest.up(),
                dest.down()
        };
    }

    @Override
    public String getName() {
        return "MovementStraight";
    }

    private boolean isPassable(BlockPos pos) {
        if (mc.world == null) return false;

        // Air is always passable
        if (BlockUtils.isAir(pos)) {
            return true;
        }

        // Check if passable (plants, torches, etc)
        if (BlockUtils.isPassable(pos)) {
            return true;
        }

        return false;
    }
}
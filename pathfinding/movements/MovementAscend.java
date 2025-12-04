package com.donut.client.pathfinding.movements;

import com.donut.client.utils.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class MovementAscend extends Movement {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final int height;

    public MovementAscend(BlockPos src, BlockPos dest) {
        super(src, dest);

        this.height = dest.getY() - src.getY();

        // Higher jumps cost more
        this.cost = 1.3 + (height - 1) * 1.0;
    }

    @Override
    public boolean calculate() {
        // REALISTIC: Only allow 1-2 block jumps (player can jump 1.25 blocks max)
        // For 3+ blocks, player needs to pillar which is handled separately
        if (height < 1 || height > 2) {
            valid = false;
            return false;
        }

        // Check if moving horizontally
        int dx = Math.abs(dest.getX() - src.getX());
        int dz = Math.abs(dest.getZ() - src.getZ());
        boolean isMovingHorizontally = (dx + dz) > 0;

        // Check destination is walkable
        if (!isWalkable(dest)) {
            valid = false;
            return false;
        }

        // CRITICAL: Check headroom - need 2 full blocks of space
        if (!BlockUtils.isAir(dest.up()) && !BlockUtils.isPassable(dest.up())) {
            valid = false;
            return false;
        }

        // Extra headroom check for jumping
        if (!BlockUtils.isAir(dest.up(2)) && !BlockUtils.isPassable(dest.up(2))) {
            valid = false;
            return false;
        }

        // Check ground at destination
        if (!BlockUtils.isSolid(dest.down())) {
            valid = false;
            return false;
        }

        // Check we have solid ground at source to jump from
        if (!BlockUtils.isSolid(src.down())) {
            valid = false;
            return false;
        }

        // If jumping 2 blocks, need extra validation
        if (height == 2) {
            // Can't jump 2 blocks while moving horizontally (too hard)
            if (isMovingHorizontally) {
                valid = false;
                return false;
            }

            // Check intermediate position (1 block up from source)
            BlockPos intermediate = src.up(1);
            if (!isWalkable(intermediate)) {
                valid = false;
                return false;
            }

            // Need headroom at intermediate
            if (!BlockUtils.isAir(intermediate.up()) && !BlockUtils.isPassable(intermediate.up())) {
                valid = false;
                return false;
            }
        }

        // If moving horizontally, check path is clear
        if (isMovingHorizontally) {
            // Check the block we're jumping over/onto
            int stepX = Integer.compare(dest.getX() - src.getX(), 0);
            int stepZ = Integer.compare(dest.getZ() - src.getZ(), 0);
            BlockPos between = src.add(stepX, 0, stepZ);

            // Need clearance above the block we're jumping from
            if (!BlockUtils.isAir(src.up()) && !BlockUtils.isPassable(src.up())) {
                valid = false;
                return false;
            }

            if (!BlockUtils.isAir(src.up(2)) && !BlockUtils.isPassable(src.up(2))) {
                valid = false;
                return false;
            }

            // The "between" block should either be solid (we jump over it) or air
            // If it's solid, we need clearance above it
            if (BlockUtils.isSolid(between)) {
                if (!BlockUtils.isAir(between.up()) && !BlockUtils.isPassable(between.up())) {
                    valid = false;
                    return false;
                }
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
                dest.up(2),
                dest.down(),
                src.down(),
                src.up(),
                src.up(2)
        };
    }

    @Override
    public String getName() {
        return "MovementAscend" + height;
    }

    private boolean isWalkable(BlockPos pos) {
        if (mc.world == null) return false;
        return BlockUtils.isAir(pos) || BlockUtils.isPassable(pos);
    }
}
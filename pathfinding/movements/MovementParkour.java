package com.donut.client.pathfinding.movements;

import com.donut.client.utils.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class MovementParkour extends Movement {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final int distance;

    public MovementParkour(BlockPos src, BlockPos dest) {
        super(src, dest);

        int dx = Math.abs(dest.getX() - src.getX());
        int dz = Math.abs(dest.getZ() - src.getZ());
        this.distance = Math.max(dx, dz);

        // Parkour is risky and costs more
        this.cost = 2.0 + (distance * 0.5);
    }

    @Override
    public boolean calculate() {
        // Only for 2-4 block gaps
        if (distance < 2 || distance > 4) {
            valid = false;
            return false;
        }

        // Must be same Y or 1 block difference
        int dy = dest.getY() - src.getY();
        if (Math.abs(dy) > 1) {
            valid = false;
            return false;
        }

        // Check source has solid ground (need sprint speed)
        if (!BlockUtils.isSolid(src.down())) {
            valid = false;
            return false;
        }

        // Check we have running space (1 block behind)
        BlockPos behind = getBlockBehind(src, dest);
        if (!BlockUtils.isSolid(behind.down()) ||
                !BlockUtils.isAir(behind) ||
                !BlockUtils.isAir(behind.up())) {
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

        // Check landing has solid ground
        if (!BlockUtils.isSolid(dest.down())) {
            valid = false;
            return false;
        }

        // Check gap is actually a gap (no blocks in between)
        if (!isGapClear(src, dest)) {
            valid = false;
            return false;
        }

        valid = true;
        return true;
    }

    @Override
    public BlockPos[] getPositionsToCheck() {
        return new BlockPos[]{
                dest,
                dest.up(),
                dest.down(),
                src.down(),
                getBlockBehind(src, dest)
        };
    }

    @Override
    public String getName() {
        return "MovementParkour" + distance;
    }

    private boolean isWalkable(BlockPos pos) {
        return BlockUtils.isAir(pos) || BlockUtils.isPassable(pos);
    }

    private BlockPos getBlockBehind(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();

        // Normalize direction
        if (dx != 0) dx = dx > 0 ? -1 : 1;
        if (dz != 0) dz = dz > 0 ? -1 : 1;

        return from.add(dx, 0, dz);
    }

    private boolean isGapClear(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();

        int steps = Math.max(Math.abs(dx), Math.abs(dz));

        // Check all positions in the gap (not source or dest)
        for (int i = 1; i < steps; i++) {
            BlockPos check = from.add(
                    dx * i / steps,
                    0,
                    dz * i / steps
            );

            // Should be air above the gap
            if (!BlockUtils.isAir(check) && !BlockUtils.isPassable(check)) {
                return false;
            }

            // Gap below should have no ground
            if (BlockUtils.isSolid(check.down())) {
                return false;
            }
        }

        return true;
    }
}
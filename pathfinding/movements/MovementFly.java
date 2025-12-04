package com.donut.client.pathfinding.movements;

import com.donut.client.utils.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class MovementFly extends Movement {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public MovementFly(BlockPos src, BlockPos dest) {
        super(src, dest);

        // Flying is fast and easy
        int dx = Math.abs(dest.getX() - src.getX());
        int dy = Math.abs(dest.getY() - src.getY());
        int dz = Math.abs(dest.getZ() - src.getZ());

        // Diagonal movement
        if (dx > 0 && dz > 0) {
            this.cost = 1.4;
        } else {
            this.cost = 1.0;
        }

        // Vertical movement is slightly more costly
        if (dy > 0) {
            this.cost += dy * 0.2;
        }
    }

    @Override
    public boolean calculate() {
        if (mc.world == null) {
            valid = false;
            return false;
        }

        // Check if player can actually fly
        if (mc.player == null || !mc.player.getAbilities().flying) {
            valid = false;
            return false;
        }

        // STRICT: Check destination has NO collision
        if (BlockUtils.hasCollision(dest)) {
            valid = false;
            return false;
        }

        // STRICT: Check we have clearance (flying hitbox is still 2 blocks tall)
        if (BlockUtils.hasCollision(dest.up())) {
            valid = false;
            return false;
        }

        // Check path is clear (simple line check)
        if (!isPathClear(src, dest)) {
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
                dest.up()
        };
    }

    @Override
    public String getName() {
        return "MovementFly";
    }

    private boolean isPassable(BlockPos pos) {
        return BlockUtils.isAir(pos) || BlockUtils.isPassable(pos) || BlockUtils.isLiquid(pos);
    }

    private boolean isPathClear(BlockPos from, BlockPos to) {
        if (mc.world == null) return false;

        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) return true;

        // Check every position along the line
        for (int i = 1; i < steps; i++) {
            BlockPos check = from.add(
                    dx * i / steps,
                    dy * i / steps,
                    dz * i / steps
            );

            // STRICT: No collision allowed
            if (BlockUtils.hasCollision(check)) {
                return false;
            }

            // Check player height (2 blocks tall)
            if (BlockUtils.hasCollision(check.up())) {
                return false;
            }
        }

        return true;
    }
}
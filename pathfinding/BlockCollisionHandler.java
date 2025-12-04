package com.donut.client.pathfinding;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Handles advanced collision detection for complex block geometries
 */
public class BlockCollisionHandler {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Player collision box dimensions
    private static final double PLAYER_WIDTH = 0.6;
    private static final double PLAYER_HEIGHT = 1.8;
    private static final double PLAYER_EYE_HEIGHT = 1.62;

    /**
     * Checks if a position has a collision with the player's hitbox
     */
    public boolean hasCollision(Vec3d pos) {
        if (mc.world == null) return false;

        Box playerBox = getPlayerBox(pos);

        // Check all blocks the player box intersects with
        int minX = (int) Math.floor(playerBox.minX);
        int minY = (int) Math.floor(playerBox.minY);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxX = (int) Math.ceil(playerBox.maxX);
        int maxY = (int) Math.ceil(playerBox.maxY);
        int maxZ = (int) Math.ceil(playerBox.maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    if (hasBlockCollision(blockPos, playerBox)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Gets the player's collision box at a position
     */
    private Box getPlayerBox(Vec3d pos) {
        double halfWidth = PLAYER_WIDTH / 2.0;
        return new Box(
                pos.x - halfWidth, pos.y, pos.z - halfWidth,
                pos.x + halfWidth, pos.y + PLAYER_HEIGHT, pos.z + halfWidth
        );
    }

    /**
     * Checks if a block has collision with the player box
     */
    private boolean hasBlockCollision(BlockPos pos, Box playerBox) {
        if (mc.world == null) return false;

        BlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();

        // Air has no collision
        if (state.isAir()) return false;

        // Non-solid blocks
        if (!state.isSolidBlock(mc.world, pos)) {
            // Some non-solid blocks still have collision
            if (block instanceof FenceBlock ||
                    block instanceof FenceGateBlock ||
                    block instanceof WallBlock) {
                return true;
            }
            return false;
        }

        // Get the block's collision shape
        Box blockBox = getBlockCollisionBox(pos, state);

        // Check if boxes intersect
        return playerBox.intersects(blockBox);
    }

    /**
     * Gets the collision box for a block
     */
    private Box getBlockCollisionBox(BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        // Full blocks
        if (state.isFullCube(mc.world, pos)) {
            return new Box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        }

        // Slabs
        if (block instanceof SlabBlock) {
            return new Box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 1);
        }

        // Stairs
        if (block instanceof StairsBlock) {
            return new Box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 1);
        }

        // Fences (taller than 1 block)
        if (block instanceof FenceBlock) {
            return new Box(pos.getX() + 0.375, pos.getY(), pos.getZ() + 0.375,
                    pos.getX() + 0.625, pos.getY() + 1.5, pos.getZ() + 0.625);
        }

        // Walls
        if (block instanceof WallBlock) {
            return new Box(pos.getX() + 0.25, pos.getY(), pos.getZ() + 0.25,
                    pos.getX() + 0.75, pos.getY() + 1.5, pos.getZ() + 0.75);
        }

        // Carpet
        if (block instanceof CarpetBlock) {
            return new Box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 0.0625, pos.getZ() + 1);
        }

        // Farmland
        if (block instanceof FarmlandBlock) {
            return new Box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 0.9375, pos.getZ() + 1);
        }

        // Default full block
        return new Box(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    /**
     * Checks if player can fit in a space
     */
    public boolean canFitIn(BlockPos pos) {
        if (mc.world == null) return false;

        // Check if there's 2 blocks of vertical space
        BlockState current = mc.world.getBlockState(pos);
        BlockState above = mc.world.getBlockState(pos.up());

        // Both blocks must be passable
        if (!isPassable(current) || !isPassable(above)) {
            return false;
        }

        // Check horizontal space
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return !hasCollision(center);
    }

    /**
     * Checks if a block is passable (player can walk through)
     */
    private boolean isPassable(BlockState state) {
        if (state.isAir()) return true;

        Block block = state.getBlock();

        // Non-solid blocks that are passable
        return block instanceof PlantBlock ||
                block instanceof TorchBlock ||
                block instanceof SignBlock ||
                block instanceof BannerBlock ||
                block instanceof CropBlock ||
                block instanceof CocoaBlock ||
                (block instanceof TrapdoorBlock && state.get(TrapdoorBlock.OPEN)) ||
                (block instanceof DoorBlock && state.get(DoorBlock.OPEN));
    }

    /**
     * Gets the clearance height at a position
     */
    public double getClearanceHeight(BlockPos pos) {
        if (mc.world == null) return 0;

        double height = 0;

        for (int i = 0; i < 10; i++) {
            BlockPos checkPos = pos.up(i);
            BlockState state = mc.world.getBlockState(checkPos);

            if (!isPassable(state)) {
                break;
            }

            height += 1.0;
        }

        return height;
    }

    /**
     * Checks if player can stand at a position
     */
    public boolean canStandAt(BlockPos pos) {
        if (mc.world == null) return false;

        // Must have solid ground below
        BlockState below = mc.world.getBlockState(pos.down());
        if (!below.isSolidBlock(mc.world, pos.down())) {
            return false;
        }

        // Must have space to stand
        return canFitIn(pos);
    }

    /**
     * Checks if there's a ceiling above that would block movement
     */
    public boolean hasCeiling(BlockPos pos, int range) {
        if (mc.world == null) return false;

        for (int i = 1; i <= range; i++) {
            BlockPos checkPos = pos.up(i);
            BlockState state = mc.world.getBlockState(checkPos);

            if (state.isSolidBlock(mc.world, checkPos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the nearest safe position (no collision)
     */
    public Vec3d getNearestSafePosition(Vec3d pos, double maxDistance) {
        if (!hasCollision(pos)) return pos;

        // Try positions in expanding radius
        for (double dist = 0.1; dist <= maxDistance; dist += 0.1) {
            for (double angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                Vec3d testPos = new Vec3d(
                        pos.x + Math.cos(rad) * dist,
                        pos.y,
                        pos.z + Math.sin(rad) * dist
                );

                if (!hasCollision(testPos)) {
                    return testPos;
                }
            }
        }

        return pos;
    }

    /**
     * Checks if a path between two positions is clear
     */
    public boolean isPathClear(Vec3d start, Vec3d end) {
        double distance = start.distanceTo(end);
        int steps = (int) Math.ceil(distance * 2);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3d checkPos = new Vec3d(
                    start.x + (end.x - start.x) * t,
                    start.y + (end.y - start.y) * t,
                    start.z + (end.z - start.z) * t
            );

            if (hasCollision(checkPos)) {
                return false;
            }
        }

        return true;
    }
}
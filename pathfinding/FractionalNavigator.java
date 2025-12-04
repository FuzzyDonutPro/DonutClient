package com.donut.client.pathfinding;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Handles navigation through partial blocks (slabs, stairs, trapdoors, etc.)
 */
public class FractionalNavigator {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Checks if a block is a partial block that can be navigated through
     */
    public boolean isPartialBlock(BlockPos pos) {
        if (mc.world == null) return false;

        Block block = mc.world.getBlockState(pos).getBlock();

        return block instanceof SlabBlock ||
                block instanceof StairsBlock ||
                block instanceof TrapdoorBlock ||
                block instanceof CarpetBlock ||
                block instanceof SnowBlock ||
                block instanceof FarmlandBlock ||
                block instanceof PressurePlateBlock ||
                block instanceof CocoaBlock;
    }

    /**
     * Gets the precise Y-coordinate for standing on a partial block
     */
    public double getBlockTopY(BlockPos pos) {
        if (mc.world == null) return pos.getY() + 1.0;

        Block block = mc.world.getBlockState(pos).getBlock();
        BlockState state = mc.world.getBlockState(pos);

        // Slabs
        if (block instanceof SlabBlock) {
            return pos.getY() + 0.5;
        }

        // Stairs
        if (block instanceof StairsBlock) {
            return pos.getY() + 0.5;
        }

        // Farmland
        if (block instanceof FarmlandBlock) {
            return pos.getY() + 0.9375; // 15/16 blocks
        }

        // Carpet
        if (block instanceof CarpetBlock) {
            return pos.getY() + 0.0625; // 1/16 block
        }

        // Snow layers
        if (block instanceof SnowBlock) {
            return pos.getY() + 0.125; // 2/16 blocks (approximate)
        }

        // Pressure plates
        if (block instanceof PressurePlateBlock) {
            return pos.getY() + 0.0625; // 1/16 block
        }

        // Cocoa beans (attached to logs)
        if (block instanceof CocoaBlock) {
            return pos.getY() + 0.25; // Approximate
        }

        // Trapdoors (when open, you can walk through)
        if (block instanceof TrapdoorBlock) {
            return pos.getY() + 0.1875; // 3/16 blocks
        }

        // Full block default
        return pos.getY() + 1.0;
    }

    /**
     * Gets the target position with fractional Y coordinate
     */
    public Vec3d getFractionalTarget(BlockPos pos) {
        double y = getBlockTopY(pos);
        return new Vec3d(pos.getX() + 0.5, y, pos.getZ() + 0.5);
    }

    /**
     * Checks if the player can walk through a block at head level
     */
    public boolean canWalkThrough(BlockPos pos) {
        if (mc.world == null) return false;

        Block block = mc.world.getBlockState(pos).getBlock();
        BlockState state = mc.world.getBlockState(pos);

        // Air is always walkable
        if (state.isAir()) return true;

        // Trapdoors (if open)
        if (block instanceof TrapdoorBlock) {
            return state.get(TrapdoorBlock.OPEN);
        }

        // Doors (if open)
        if (block instanceof DoorBlock) {
            return state.get(DoorBlock.OPEN);
        }

        // Fence gates (if open)
        if (block instanceof FenceGateBlock) {
            return state.get(FenceGateBlock.OPEN);
        }

        // Crops (can walk through)
        if (block instanceof CropBlock) return true;
        if (block instanceof CocoaBlock) return true;

        // Flowers, grass, etc
        if (block instanceof PlantBlock) return true;

        // Signs
        if (block instanceof SignBlock) return true;
        if (block instanceof WallSignBlock) return true;

        // Banners
        if (block instanceof BannerBlock) return true;
        if (block instanceof WallBannerBlock) return true;

        return false;
    }

    /**
     * Checks if a position requires crouching to navigate
     */
    public boolean requiresCrouch(BlockPos pos) {
        if (mc.world == null) return false;

        Block block = mc.world.getBlockState(pos).getBlock();

        // Need to crouch near edges to prevent falling
        if (isNearEdge(pos)) return true;

        // Need to crouch to fit through some openings
        Block above = mc.world.getBlockState(pos.up()).getBlock();
        if (above instanceof SlabBlock || above instanceof StairsBlock) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a position is near a dangerous edge
     */
    private boolean isNearEdge(BlockPos pos) {
        if (mc.world == null) return false;

        // Check all 4 horizontal directions
        BlockPos[] directions = {
                pos.north(), pos.south(), pos.east(), pos.west()
        };

        for (BlockPos checkPos : directions) {
            // Check if there's a 2+ block drop
            if (mc.world.getBlockState(checkPos.down()).isAir() &&
                    mc.world.getBlockState(checkPos.down(2)).isAir()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the collision box height for a block
     */
    public double getCollisionHeight(BlockPos pos) {
        if (mc.world == null) return 1.0;

        Block block = mc.world.getBlockState(pos).getBlock();

        if (block instanceof SlabBlock) return 0.5;
        if (block instanceof StairsBlock) return 0.5;
        if (block instanceof FarmlandBlock) return 0.9375;
        if (block instanceof CarpetBlock) return 0.0625;
        if (block instanceof SnowBlock) return 0.125;
        if (block instanceof PressurePlateBlock) return 0.0625;

        return 1.0;
    }

    /**
     * Checks if player needs to jump to get onto this block
     */
    public boolean needsJumpToReach(BlockPos current, BlockPos target) {
        double currentHeight = getBlockTopY(current);
        double targetHeight = getBlockTopY(target);

        // If target is more than 0.5 blocks higher, need to jump
        return targetHeight - currentHeight > 0.5;
    }

    /**
     * Calculates the optimal movement speed for partial blocks
     */
    public float getOptimalSpeed(BlockPos pos) {
        if (mc.world == null) return 1.0f;

        Block block = mc.world.getBlockState(pos).getBlock();

        // Slow down on ice
        if (block instanceof IceBlock) return 0.7f;

        // Slow down on slime blocks
        if (block instanceof SlimeBlock) return 0.8f;

        // Slow down on soul sand
        if (block instanceof SoulSandBlock) return 0.4f;

        // Normal speed for most blocks
        return 1.0f;
    }

    /**
     * Checks if a block requires special movement handling
     */
    public boolean requiresSpecialHandling(BlockPos pos) {
        return isPartialBlock(pos) ||
                requiresCrouch(pos) ||
                !canWalkThrough(pos.up());
    }
}
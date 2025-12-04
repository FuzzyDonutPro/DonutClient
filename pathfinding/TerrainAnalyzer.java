package com.donut.client.pathfinding;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Analyzes terrain for pathfinding decisions
 */
public class TerrainAnalyzer {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Checks if a block is climbable (ladder, vine, etc.)
     */
    public boolean isClimbable(BlockPos pos) {
        if (mc.world == null) return false;

        Block block = mc.world.getBlockState(pos).getBlock();

        return block instanceof LadderBlock ||
                block instanceof VineBlock ||
                block instanceof ScaffoldingBlock;
    }

    /**
     * Checks if a block is dangerous to walk on/through
     */
    public boolean isDangerous(BlockPos pos) {
        if (mc.world == null) return false;

        Block block = mc.world.getBlockState(pos).getBlock();

        return block instanceof AbstractFireBlock ||
                block == Blocks.LAVA ||
                block == Blocks.CACTUS ||
                block == Blocks.MAGMA_BLOCK ||
                block == Blocks.SWEET_BERRY_BUSH ||
                block == Blocks.WITHER_ROSE ||
                block == Blocks.POWDER_SNOW;
    }

    /**
     * Checks if a position is in water
     */
    public boolean isInWater(BlockPos pos) {
        if (mc.world == null) return false;

        FluidState fluidState = mc.world.getFluidState(pos);
        return fluidState.getFluid() == Fluids.WATER ||
                fluidState.getFluid() == Fluids.FLOWING_WATER;
    }

    /**
     * Checks if a position is in lava
     */
    public boolean isInLava(BlockPos pos) {
        if (mc.world == null) return false;

        FluidState fluidState = mc.world.getFluidState(pos);
        return fluidState.getFluid() == Fluids.LAVA ||
                fluidState.getFluid() == Fluids.FLOWING_LAVA;
    }

    /**
     * Checks if a position is safe to walk to
     */
    public boolean isSafe(BlockPos pos) {
        if (mc.world == null) return false;

        // Check current position
        if (isDangerous(pos)) return false;
        if (isInLava(pos)) return false;

        // Check below (must have ground)
        BlockState below = mc.world.getBlockState(pos.down());
        if (below.isAir() && !isClimbable(pos)) {
            // Check for void
            if (pos.getY() < mc.world.getBottomY() + 5) {
                return false;
            }
        }

        // Check above (must have clearance)
        BlockState above = mc.world.getBlockState(pos.up());
        if (above.isSolidBlock(mc.world, pos.up()) && !isClimbable(pos)) {
            return false;
        }

        return true;
    }

    /**
     * Gets the terrain type at a position
     */
    public TerrainType getTerrainType(BlockPos pos) {
        if (mc.world == null) return TerrainType.UNKNOWN;

        if (isInLava(pos)) return TerrainType.LAVA;
        if (isInWater(pos)) return TerrainType.WATER;
        if (isClimbable(pos)) return TerrainType.CLIMBABLE;
        if (isDangerous(pos)) return TerrainType.DANGEROUS;

        Block block = mc.world.getBlockState(pos).getBlock();

        if (block instanceof IceBlock) return TerrainType.ICE;
        if (block instanceof SlimeBlock) return TerrainType.SLIME;
        if (block instanceof HoneyBlock) return TerrainType.HONEY;
        if (block instanceof SoulSandBlock) return TerrainType.SOUL_SAND;

        BlockState below = mc.world.getBlockState(pos.down());
        if (below.isSolidBlock(mc.world, pos.down())) {
            return TerrainType.SOLID_GROUND;
        }

        if (below.isAir()) return TerrainType.AIR;

        return TerrainType.NORMAL;
    }

    /**
     * Calculates movement cost multiplier based on terrain
     */
    public double getTerrainCostMultiplier(BlockPos pos) {
        TerrainType type = getTerrainType(pos);

        switch (type) {
            case LAVA:
                return 10.0; // Very dangerous, high cost
            case DANGEROUS:
                return 5.0;  // Dangerous, avoid if possible
            case WATER:
                return 2.0;  // Slower movement
            case CLIMBABLE:
                return 1.5;  // Slightly slower
            case SOUL_SAND:
                return 2.5;  // Very slow
            case HONEY:
                return 3.0;  // Sticky
            case ICE:
                return 0.8;  // Faster but slippery
            case AIR:
                return 10.0; // Can't walk on air
            case SOLID_GROUND:
            case NORMAL:
            default:
                return 1.0;  // Normal cost
        }
    }

    /**
     * Checks if a position is near a cliff/drop
     */
    public boolean isNearCliff(BlockPos pos, int checkDistance) {
        if (mc.world == null) return false;

        // Check all horizontal directions
        Direction[] directions = {
                Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST
        };

        for (Direction dir : directions) {
            BlockPos checkPos = pos.offset(dir);

            // Check if there's a significant drop
            int dropHeight = 0;
            for (int i = 1; i <= checkDistance; i++) {
                BlockPos below = checkPos.down(i);
                if (!mc.world.getBlockState(below).isAir()) {
                    dropHeight = i - 1;
                    break;
                }
                if (i == checkDistance) {
                    dropHeight = checkDistance;
                }
            }

            if (dropHeight >= 3) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the ground level below a position
     */
    public int findGroundLevel(BlockPos pos, int maxDepth) {
        if (mc.world == null) return pos.getY();

        for (int i = 0; i <= maxDepth; i++) {
            BlockPos checkPos = pos.down(i);
            BlockState state = mc.world.getBlockState(checkPos);

            if (state.isSolidBlock(mc.world, checkPos)) {
                return checkPos.getY() + 1;
            }
        }

        return pos.getY() - maxDepth;
    }

    /**
     * Checks if there's water nearby (for swimming)
     */
    public boolean hasWaterNearby(BlockPos pos, int radius) {
        if (mc.world == null) return false;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (isInWater(checkPos)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Gets the slope at a position (for steep terrain detection)
     */
    public double getSlope(BlockPos pos) {
        if (mc.world == null) return 0;

        int northHeight = findGroundLevel(pos.north(), 10);
        int southHeight = findGroundLevel(pos.south(), 10);
        int eastHeight = findGroundLevel(pos.east(), 10);
        int westHeight = findGroundLevel(pos.west(), 10);

        int maxDiff = Math.max(
                Math.abs(northHeight - southHeight),
                Math.abs(eastHeight - westHeight)
        );

        return maxDiff / 2.0; // Average slope
    }

    /**
     * Checks if terrain is too steep to walk
     */
    public boolean isTooSteep(BlockPos pos) {
        return getSlope(pos) > 2.0;
    }

    /**
     * Checks if a position has good visibility (for avoiding enclosed spaces)
     */
    public boolean hasGoodVisibility(BlockPos pos, int range) {
        if (mc.world == null) return false;

        int airBlocks = 0;
        int totalChecked = 0;

        for (int x = -range; x <= range; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (mc.world.getBlockState(checkPos).isAir()) {
                        airBlocks++;
                    }
                    totalChecked++;
                }
            }
        }

        double openness = (double) airBlocks / totalChecked;
        return openness > 0.7; // 70% open space
    }

    /**
     * Terrain types
     */
    public enum TerrainType {
        SOLID_GROUND,
        AIR,
        WATER,
        LAVA,
        CLIMBABLE,
        DANGEROUS,
        ICE,
        SLIME,
        HONEY,
        SOUL_SAND,
        NORMAL,
        UNKNOWN
    }
}
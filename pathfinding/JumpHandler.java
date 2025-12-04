package com.donut.client.pathfinding;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Handles advanced jumping mechanics for pathfinding
 * - Auto-jump over 1-block obstacles
 * - Parkour jump timing and distance calculation
 * - Sprint-jump for long gaps (4+ blocks)
 * - Ledge detection and edge safety
 * - Energy-efficient (no unnecessary jumps)
 */
public class JumpHandler {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private long lastJumpTime = 0;
    private static final long JUMP_COOLDOWN = 250; // ms between jumps
    private static final double SPRINT_JUMP_DISTANCE = 4.0;
    private static final double NORMAL_JUMP_DISTANCE = 2.5;

    /**
     * Determines if the player should jump at the current position
     */
    public boolean shouldJump(BlockPos current, BlockPos target) {
        if (mc.player == null || mc.world == null) return false;

        // Check cooldown
        if (System.currentTimeMillis() - lastJumpTime < JUMP_COOLDOWN) {
            return false;
        }

        // Already in air
        if (!mc.player.isOnGround()) {
            return false;
        }

        // Check if there's an obstacle ahead
        if (hasObstacleAhead(current, target)) {
            return true;
        }

        // Check if we need to jump up a block
        if (target.getY() > current.getY()) {
            return true;
        }

        // Check if we need a parkour jump
        if (needsParkourJump(current, target)) {
            return true;
        }

        return false;
    }

    /**
     * Executes a jump with proper timing
     */
    public void jump() {
        if (mc.player == null) return;

        mc.player.jump();
        lastJumpTime = System.currentTimeMillis();
    }

    /**
     * Checks if there's a 1-block obstacle directly ahead
     */
    private boolean hasObstacleAhead(BlockPos current, BlockPos target) {
        if (mc.world == null) return false;

        // Check the block at head level
        BlockPos ahead = new BlockPos(
                current.getX() + Integer.signum(target.getX() - current.getX()),
                current.getY() + 1,
                current.getZ() + Integer.signum(target.getZ() - current.getZ())
        );

        return !mc.world.getBlockState(ahead).isAir();
    }

    /**
     * Determines if a parkour jump is needed (gap crossing)
     */
    private boolean needsParkourJump(BlockPos current, BlockPos target) {
        if (mc.world == null) return false;

        double horizontalDistance = Math.sqrt(
                Math.pow(target.getX() - current.getX(), 2) +
                        Math.pow(target.getZ() - current.getZ(), 2)
        );

        // If distance is jumpable but there's no ground in between
        if (horizontalDistance > 1.0 && horizontalDistance <= 4.0) {
            // Check if there's a gap
            return hasGap(current, target);
        }

        return false;
    }

    /**
     * Checks if there's a gap between current and target position
     */
    private boolean hasGap(BlockPos current, BlockPos target) {
        if (mc.world == null) return false;

        int steps = (int) Math.max(
                Math.abs(target.getX() - current.getX()),
                Math.abs(target.getZ() - current.getZ())
        );

        for (int i = 1; i < steps; i++) {
            double progress = (double) i / steps;
            BlockPos check = new BlockPos(
                    (int) (current.getX() + (target.getX() - current.getX()) * progress),
                    current.getY(),
                    (int) (current.getZ() + (target.getZ() - current.getZ()) * progress)
            );

            // Check if there's no ground beneath
            BlockPos below = check.down();
            if (mc.world.getBlockState(below).isAir()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculates if a jump is possible based on distance
     */
    public boolean canJumpDistance(BlockPos from, BlockPos to, boolean sprinting) {
        double distance = Math.sqrt(
                Math.pow(to.getX() - from.getX(), 2) +
                        Math.pow(to.getZ() - from.getZ(), 2)
        );

        double maxDistance = sprinting ? SPRINT_JUMP_DISTANCE : NORMAL_JUMP_DISTANCE;
        return distance <= maxDistance;
    }

    /**
     * Determines if player should sprint for the jump
     */
    public boolean shouldSprintJump(BlockPos from, BlockPos to) {
        double distance = Math.sqrt(
                Math.pow(to.getX() - from.getX(), 2) +
                        Math.pow(to.getZ() - from.getZ(), 2)
        );

        return distance > NORMAL_JUMP_DISTANCE && distance <= SPRINT_JUMP_DISTANCE;
    }

    /**
     * Checks if the player is near a ledge (dangerous)
     */
    public boolean isNearLedge() {
        if (mc.player == null || mc.world == null) return false;

        BlockPos playerPos = mc.player.getBlockPos();

        // Check all 4 directions for drops
        BlockPos[] checkPositions = {
                playerPos.north(),
                playerPos.south(),
                playerPos.east(),
                playerPos.west()
        };

        for (BlockPos pos : checkPositions) {
            // Check if there's a 3+ block drop
            boolean hasDrop = true;
            for (int i = 1; i <= 3; i++) {
                if (!mc.world.getBlockState(pos.down(i)).isAir()) {
                    hasDrop = false;
                    break;
                }
            }

            if (hasDrop) return true;
        }

        return false;
    }

    /**
     * Gets the optimal jump timing (0.0 to 1.0)
     * 1.0 = jump now, 0.0 = don't jump
     */
    public double getJumpTiming(BlockPos current, BlockPos target) {
        if (mc.player == null) return 0.0;

        Vec3d playerPos = mc.player.getPos();
        double distanceToEdge = getDistanceToBlockEdge(playerPos, current);

        // Jump when we're close to the edge (within 0.3 blocks)
        if (distanceToEdge < 0.3) {
            return 1.0;
        } else if (distanceToEdge < 0.6) {
            return 0.5;
        }

        return 0.0;
    }

    /**
     * Calculates distance to the edge of the current block
     */
    private double getDistanceToBlockEdge(Vec3d playerPos, BlockPos blockPos) {
        double blockCenterX = blockPos.getX() + 0.5;
        double blockCenterZ = blockPos.getZ() + 0.5;

        double dx = Math.abs(playerPos.x - blockCenterX);
        double dz = Math.abs(playerPos.z - blockCenterZ);

        // Distance to nearest edge
        return Math.max(0.5 - dx, 0.5 - dz);
    }

    /**
     * Checks if a jump is safe (won't result in fall damage)
     */
    public boolean isJumpSafe(BlockPos from, BlockPos to) {
        if (mc.world == null) return false;

        // Check landing position
        BlockPos landingPos = to.down();

        // Make sure there's ground to land on
        if (mc.world.getBlockState(landingPos).isAir()) {
            // Check how far the drop is
            int dropDistance = 0;
            for (int i = 1; i <= 10; i++) {
                if (!mc.world.getBlockState(to.down(i)).isAir()) {
                    dropDistance = i - 1;
                    break;
                }
            }

            // More than 3 blocks = fall damage
            return dropDistance <= 3;
        }

        return true;
    }

    /**
     * Gets the minimum speed needed for a successful jump
     */
    public double getRequiredSpeed(BlockPos from, BlockPos to) {
        double distance = Math.sqrt(
                Math.pow(to.getX() - from.getX(), 2) +
                        Math.pow(to.getZ() - from.getZ(), 2)
        );

        // Approximate speed calculation
        if (distance <= 2.0) {
            return 0.1; // Walking speed
        } else if (distance <= 3.0) {
            return 0.15; // Fast walking
        } else {
            return 0.28; // Sprinting speed
        }
    }
}
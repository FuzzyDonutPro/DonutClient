package com.donut.client.macros.farming;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * SugarCaneFarm - Automated sugar cane farming
 * Features: Layer detection, efficient harvesting, auto-replant support
 */
public class SugarCaneFarm extends Macro {

    private final MinecraftClient mc;

    // State
    private FarmState state = FarmState.IDLE;
    private BlockPos currentCaneBase = null;
    private int currentLayer = 0;

    // Farm settings
    private BlockPos farmCorner1 = null;
    private BlockPos farmCorner2 = null;
    private int maxHeight = 3; // Only harvest up to 3 blocks high

    // Settings
    private boolean leaveBottom = true; // Leave bottom block to regrow
    private boolean useBuilder = false; // Use builder's wand for replanting

    // Statistics
    private int canesHarvested = 0;
    private int basesFound = 0;

    public enum FarmState {
        IDLE,
        SCANNING,
        MOVING,
        HARVESTING,
        COMPLETE
    }

    public SugarCaneFarm() {
        super("Sugar Cane Farm", "Automated sugar cane farming");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = FarmState.SCANNING;
        currentCaneBase = null;
        currentLayer = 0;
        canesHarvested = 0;
        basesFound = 0;
        System.out.println("[Sugar Cane Farm] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Sugar Cane Farm] Starting...");
        System.out.println("[Sugar Cane Farm] Max height: " + maxHeight);
        System.out.println("[Sugar Cane Farm] Leave bottom: " + leaveBottom);

        if (farmCorner1 == null || farmCorner2 == null) {
            System.out.println("[Sugar Cane Farm] ERROR: Farm corners not set!");
            onDisable();
            return;
        }

        state = FarmState.SCANNING;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Sugar Cane Farm] Stopped");
        printStatistics();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                // Do nothing
                break;
            case SCANNING:
                scan();
                break;
            case MOVING:
                move();
                break;
            case HARVESTING:
                harvest();
                break;
            case COMPLETE:
                complete();
                break;
        }
    }

    /**
     * Scan for sugar cane
     */
    private void scan() {
        if (farmCorner1 == null || farmCorner2 == null) return;

        // Calculate farm bounds
        int minX = Math.min(farmCorner1.getX(), farmCorner2.getX());
        int maxX = Math.max(farmCorner1.getX(), farmCorner2.getX());
        int minY = Math.min(farmCorner1.getY(), farmCorner2.getY());
        int maxY = Math.max(farmCorner1.getY(), farmCorner2.getY());
        int minZ = Math.min(farmCorner1.getZ(), farmCorner2.getZ());
        int maxZ = Math.max(farmCorner1.getZ(), farmCorner2.getZ());

        // Find nearest mature cane
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (isCaneBase(pos) && isMature(pos)) {
                        double dist = mc.player.getBlockPos().getSquaredDistance(pos);

                        if (dist < nearestDist) {
                            nearest = pos;
                            nearestDist = dist;
                        }
                    }
                }
            }
        }

        if (nearest != null) {
            currentCaneBase = nearest;
            basesFound++;
            state = FarmState.MOVING;
        }
    }

    /**
     * Check if position is a cane base
     */
    private boolean isCaneBase(BlockPos pos) {
        // TODO: Check if block is sugar cane
        // And check if block below is dirt/grass/sand
        return false;
    }

    /**
     * Check if cane is mature (grown to harvest)
     */
    private boolean isMature(BlockPos basePos) {
        // Check if there are at least 2 blocks above base
        int height = 0;

        for (int y = 1; y <= maxHeight; y++) {
            BlockPos checkPos = basePos.up(y);

            if (isSugarCane(checkPos)) {
                height++;
            } else {
                break;
            }
        }

        return height >= 2; // At least 2 blocks tall
    }

    /**
     * Check if block is sugar cane
     */
    private boolean isSugarCane(BlockPos pos) {
        // TODO: Check if block is sugar cane
        return false;
    }

    /**
     * Move to cane base
     */
    private void move() {
        if (currentCaneBase == null) {
            state = FarmState.SCANNING;
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();

        // Check if close enough
        if (playerPos.getSquaredDistance(currentCaneBase) < 16) { // Within 4 blocks
            state = FarmState.HARVESTING;
            return;
        }

        // Calculate direction
        int dx = currentCaneBase.getX() - playerPos.getX();
        int dz = currentCaneBase.getZ() - playerPos.getZ();

        // Set yaw
        if (Math.abs(dx) > 0 || Math.abs(dz) > 0) {
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
            mc.player.setYaw(yaw);
        }

        // Move forward
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(true);

            if (mc.player.horizontalCollision) {
                mc.options.jumpKey.setPressed(true);
            } else {
                mc.options.jumpKey.setPressed(false);
            }
        }
    }

    /**
     * Harvest cane
     */
    private void harvest() {
        if (currentCaneBase == null) {
            state = FarmState.SCANNING;
            return;
        }

        // Harvest from top to bottom
        int startLayer = leaveBottom ? 1 : 0;

        for (int y = maxHeight; y >= startLayer; y--) {
            BlockPos harvestPos = currentCaneBase.up(y);

            if (isSugarCane(harvestPos)) {
                breakBlock(harvestPos);
                canesHarvested++;
                System.out.println("[Sugar Cane Farm] Harvested layer " + y);
            }
        }

        // Reset and scan for next
        currentCaneBase = null;
        state = FarmState.SCANNING;
    }

    /**
     * Break block
     */
    private void breakBlock(BlockPos pos) {
        // TODO: Break block
        // Look at block and left-click
    }

    /**
     * Complete macro
     */
    private void complete() {
        System.out.println("========================================");
        System.out.println("🎋 SUGAR CANE FARM COMPLETE 🎋");
        System.out.println("Canes Harvested: " + canesHarvested);
        System.out.println("Bases Found: " + basesFound);
        System.out.println("========================================");

        onDisable();
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("SUGAR CANE FARM STATISTICS");
        System.out.println("========================================");
        System.out.println("Canes Harvested: " + canesHarvested);
        System.out.println("Bases Found: " + basesFound);
        System.out.println("Runtime: " + getRuntimeFormatted());

        long seconds = getRuntime() / 1000;
        if (seconds > 0) {
            int canesPerHour = (int)(canesHarvested * 3600L / seconds);
            System.out.println("Rate: " + canesPerHour + " canes/hour");
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | Harvested: %d | Bases: %d",
                state, canesHarvested, basesFound);
    }

    // ==================== GETTERS/SETTERS ====================

    public void setFarmCorners(BlockPos corner1, BlockPos corner2) {
        this.farmCorner1 = corner1;
        this.farmCorner2 = corner2;
    }

    public void setMaxHeight(int height) {
        this.maxHeight = height;
    }

    public void setLeaveBottom(boolean leave) {
        this.leaveBottom = leave;
    }

    public void setUseBuilder(boolean use) {
        this.useBuilder = use;
    }
}
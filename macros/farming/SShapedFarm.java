package com.donut.client.macros.farming;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * SShapedFarm - S-shaped farming pattern
 * Features: Efficient S-pattern movement, no backtracking
 */
public class SShapedFarm extends Macro {

    private final MinecraftClient mc;

    // State
    private FarmState state = FarmState.IDLE;
    private List<BlockPos> farmPath = new ArrayList<>();
    private int currentPathIndex = 0;

    // Farm settings
    private BlockPos farmCorner1 = null;
    private BlockPos farmCorner2 = null;
    private CropType cropType = CropType.WHEAT;

    // Settings
    private boolean autoReplant = true;
    private int rowSpacing = 1;

    // Statistics
    private int cropsHarvested = 0;
    private int pathsCompleted = 0;

    public enum FarmState {
        IDLE,
        GENERATING_PATH,
        MOVING,
        HARVESTING,
        COMPLETE
    }

    public enum CropType {
        WHEAT,
        CARROT,
        POTATO,
        NETHER_WART
    }

    public SShapedFarm() {
        super("S-Shaped Farm", "S-shaped farming pattern");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = FarmState.GENERATING_PATH;
        farmPath.clear();
        currentPathIndex = 0;
        cropsHarvested = 0;
        pathsCompleted = 0;
        System.out.println("[S-Shaped Farm] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[S-Shaped Farm] Starting...");
        System.out.println("[S-Shaped Farm] Crop: " + cropType);

        if (farmCorner1 == null || farmCorner2 == null) {
            System.out.println("[S-Shaped Farm] ERROR: Farm corners not set!");
            onDisable();
            return;
        }

        state = FarmState.GENERATING_PATH;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[S-Shaped Farm] Stopped");
        printStatistics();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                // Do nothing
                break;
            case GENERATING_PATH:
                generatePath();
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
     * Generate S-shaped path through farm
     */
    private void generatePath() {
        if (farmCorner1 == null || farmCorner2 == null) return;

        farmPath.clear();

        // Calculate farm bounds
        int minX = Math.min(farmCorner1.getX(), farmCorner2.getX());
        int maxX = Math.max(farmCorner1.getX(), farmCorner2.getX());
        int minZ = Math.min(farmCorner1.getZ(), farmCorner2.getZ());
        int maxZ = Math.max(farmCorner1.getZ(), farmCorner2.getZ());
        int y = farmCorner1.getY();

        // Generate S-pattern
        boolean leftToRight = true;

        for (int z = minZ; z <= maxZ; z += rowSpacing) {
            if (leftToRight) {
                // Left to right
                for (int x = minX; x <= maxX; x++) {
                    farmPath.add(new BlockPos(x, y, z));
                }
            } else {
                // Right to left
                for (int x = maxX; x >= minX; x--) {
                    farmPath.add(new BlockPos(x, y, z));
                }
            }

            leftToRight = !leftToRight; // Alternate direction
        }

        System.out.println("[S-Shaped Farm] Generated path with " + farmPath.size() + " blocks");

        currentPathIndex = 0;
        state = FarmState.MOVING;
    }

    /**
     * Move along path
     */
    private void move() {
        if (currentPathIndex >= farmPath.size()) {
            // Path complete
            pathsCompleted++;
            state = FarmState.COMPLETE;
            return;
        }

        BlockPos targetPos = farmPath.get(currentPathIndex);
        BlockPos playerPos = mc.player.getBlockPos();

        // Check if at target
        if (playerPos.equals(targetPos)) {
            state = FarmState.HARVESTING;
            return;
        }

        // Calculate direction
        int dx = targetPos.getX() - playerPos.getX();
        int dz = targetPos.getZ() - playerPos.getZ();

        // Set yaw
        if (Math.abs(dx) > 0 || Math.abs(dz) > 0) {
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
            mc.player.setYaw(yaw);
        }

        // Move forward
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(true);

            // Jump if blocked
            if (mc.player.horizontalCollision) {
                mc.options.jumpKey.setPressed(true);
            } else {
                mc.options.jumpKey.setPressed(false);
            }
        }
    }

    /**
     * Harvest crop at current position
     */
    private void harvest() {
        BlockPos currentPos = farmPath.get(currentPathIndex);

        // Check if mature crop
        if (isMatureCrop(currentPos)) {
            breakBlock(currentPos);
            cropsHarvested++;

            if (autoReplant) {
                replant(currentPos);
            }
        }

        // Move to next position
        currentPathIndex++;
        state = FarmState.MOVING;
    }

    /**
     * Check if crop is mature
     */
    private boolean isMatureCrop(BlockPos pos) {
        // TODO: Check block state
        return false;
    }

    /**
     * Break block
     */
    private void breakBlock(BlockPos pos) {
        // TODO: Break block
    }

    /**
     * Replant crop
     */
    private void replant(BlockPos pos) {
        // TODO: Right-click to plant
    }

    /**
     * Complete macro
     */
    private void complete() {
        System.out.println("========================================");
        System.out.println("🌾 S-SHAPED FARM COMPLETE 🌾");
        System.out.println("Crops Harvested: " + cropsHarvested);
        System.out.println("Paths Completed: " + pathsCompleted);
        System.out.println("========================================");

        onDisable();
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("S-SHAPED FARM STATISTICS");
        System.out.println("========================================");
        System.out.println("Crop Type: " + cropType);
        System.out.println("Path Length: " + farmPath.size());
        System.out.println("Crops Harvested: " + cropsHarvested);
        System.out.println("Paths Completed: " + pathsCompleted);
        System.out.println("Runtime: " + getRuntimeFormatted());

        long seconds = getRuntime() / 1000;
        if (seconds > 0) {
            int cropsPerHour = (int)(cropsHarvested * 3600L / seconds);
            System.out.println("Rate: " + cropsPerHour + " crops/hour");
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | %s | Progress: %d/%d | Harvested: %d",
                state, cropType, currentPathIndex, farmPath.size(), cropsHarvested);
    }

    // ==================== GETTERS/SETTERS ====================

    public void setFarmCorners(BlockPos corner1, BlockPos corner2) {
        this.farmCorner1 = corner1;
        this.farmCorner2 = corner2;
    }

    public void setCropType(CropType type) {
        this.cropType = type;
    }

    public void setAutoReplant(boolean auto) {
        this.autoReplant = auto;
    }

    public void setRowSpacing(int spacing) {
        this.rowSpacing = spacing;
    }
}
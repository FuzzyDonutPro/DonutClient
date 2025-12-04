package com.donut.client.macros.farming;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * MelonPumpkinFarm - Automated melon/pumpkin farming
 * Features: Stem detection, block breaking, auto-replant
 */
public class MelonPumpkinFarm extends Macro {

    private final MinecraftClient mc;

    // State
    private FarmState state = FarmState.IDLE;
    private BlockPos currentPos = null;

    // Farm settings
    private CropType cropType = CropType.MELON;
    private BlockPos farmCorner1 = null;
    private BlockPos farmCorner2 = null;

    // Settings
    private boolean breakOnlyGrown = true; // Don't break stems
    private boolean autoReplant = false; // Melons/pumpkins don't need replanting
    private int scanInterval = 20; // Scan every 20 ticks (1 second)

    // Statistics
    private int blocksHarvested = 0;
    private long coinsEarned = 0;

    public enum FarmState {
        IDLE,
        SCANNING,
        MOVING,
        HARVESTING,
        COMPLETE
    }

    public enum CropType {
        MELON,
        PUMPKIN
    }

    public MelonPumpkinFarm() {
        super("Melon/Pumpkin Farm", "Automated melon and pumpkin farming");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = FarmState.SCANNING;
        currentPos = null;
        blocksHarvested = 0;
        coinsEarned = 0;
        System.out.println("[Melon/Pumpkin Farm] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Melon/Pumpkin Farm] Starting...");
        System.out.println("[Melon/Pumpkin Farm] Crop: " + cropType);

        if (farmCorner1 == null || farmCorner2 == null) {
            System.out.println("[Melon/Pumpkin Farm] ERROR: Farm corners not set!");
            onDisable();
            return;
        }

        state = FarmState.SCANNING;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Melon/Pumpkin Farm] Stopped");
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
     * Scan for grown melons/pumpkins
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

        // Find nearest melon/pumpkin
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (isHarvestable(pos)) {
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
            currentPos = nearest;
            state = FarmState.MOVING;
        }
    }

    /**
     * Check if block is harvestable
     */
    private boolean isHarvestable(BlockPos pos) {
        // TODO: Check if block is melon or pumpkin
        // MELON: minecraft:melon_block
        // PUMPKIN: minecraft:pumpkin
        return false;
    }

    /**
     * Move to target position
     */
    private void move() {
        if (currentPos == null) {
            state = FarmState.SCANNING;
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();

        // Check if close enough to harvest
        if (playerPos.getSquaredDistance(currentPos) < 16) { // Within 4 blocks
            state = FarmState.HARVESTING;
            return;
        }

        // Calculate direction
        int dx = currentPos.getX() - playerPos.getX();
        int dz = currentPos.getZ() - playerPos.getZ();

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
     * Harvest block
     */
    private void harvest() {
        if (currentPos == null) {
            state = FarmState.SCANNING;
            return;
        }

        // Break block
        breakBlock(currentPos);
        blocksHarvested++;

        // Calculate coins (rough estimate)
        int coinValue = cropType == CropType.MELON ? 50 : 100;
        coinsEarned += coinValue;

        System.out.println("[Melon/Pumpkin Farm] Harvested " + cropType + " at " + currentPos);

        // Reset and scan for next
        currentPos = null;
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
        System.out.println("🍉 MELON/PUMPKIN FARM COMPLETE 🎃");
        System.out.println("Harvested: " + blocksHarvested);
        System.out.println("Coins: ~" + coinsEarned);
        System.out.println("========================================");

        onDisable();
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("MELON/PUMPKIN FARM STATISTICS");
        System.out.println("========================================");
        System.out.println("Crop Type: " + cropType);
        System.out.println("Harvested: " + blocksHarvested);
        System.out.println("Coins Earned: ~" + coinsEarned);
        System.out.println("Runtime: " + getRuntimeFormatted());

        long seconds = getRuntime() / 1000;
        if (seconds > 0) {
            int blocksPerHour = (int)(blocksHarvested * 3600L / seconds);
            long coinsPerHour = coinsEarned * 3600L / seconds;
            System.out.println("Rate: " + blocksPerHour + " blocks/hour");
            System.out.println("Coins/Hour: ~" + coinsPerHour);
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | %s | Harvested: %d | Coins: ~%d",
                state, cropType, blocksHarvested, coinsEarned);
    }

    // ==================== GETTERS/SETTERS ====================

    public void setCropType(CropType type) {
        this.cropType = type;
    }

    public void setFarmCorners(BlockPos corner1, BlockPos corner2) {
        this.farmCorner1 = corner1;
        this.farmCorner2 = corner2;
    }

    public void setBreakOnlyGrown(boolean onlyGrown) {
        this.breakOnlyGrown = onlyGrown;
    }

    public void setScanInterval(int interval) {
        this.scanInterval = interval;
    }
}
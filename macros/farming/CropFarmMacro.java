package com.donut.client.macros.farming;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * CropFarmMacro - Automated crop farming
 */
public class CropFarmMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private FarmState state = FarmState.IDLE;
    private Queue<BlockPos> harvestQueue = new LinkedList<>();
    private BlockPos currentTarget = null;

    // Settings
    private CropType cropType = CropType.WHEAT;
    private FarmPattern pattern = FarmPattern.ROWS;
    private boolean autoReplant = true;
    private int farmRadius = 20;

    // Statistics
    private int cropsHarvested = 0;
    private int cropsReplanted = 0;

    public enum FarmState {
        IDLE, SCANNING, HARVESTING, REPLANTING, COMPLETE
    }

    public enum CropType {
        WHEAT(Blocks.WHEAT),
        CARROT(Blocks.CARROTS),
        POTATO(Blocks.POTATOES),
        NETHER_WART(Blocks.NETHER_WART),
        SUGAR_CANE(Blocks.SUGAR_CANE),
        CACTUS(Blocks.CACTUS),
        MELON(Blocks.MELON),
        PUMPKIN(Blocks.PUMPKIN),
        COCOA(Blocks.COCOA);

        public final Block block;

        CropType(Block block) {
            this.block = block;
        }
    }

    public enum FarmPattern {
        ROWS,      // Row by row
        SPIRAL,    // Spiral outward
        LAYERS     // Layer by layer
    }

    public CropFarmMacro() {
        super("Crop Farm", "Automated crop farming");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = FarmState.IDLE;
        harvestQueue.clear();
        cropsHarvested = 0;
        cropsReplanted = 0;
        System.out.println("[Crop Farm] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = FarmState.SCANNING;
        System.out.println("[Crop Farm] Starting...");
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                // Wait
                break;
            case SCANNING:
                scanForCrops();
                break;
            case HARVESTING:
                harvestCrop();
                break;
            case REPLANTING:
                replantCrop();
                break;
            case COMPLETE:
                // Done
                break;
        }
    }

    /**
     * Scan for crops to harvest
     */
    private void scanForCrops() {
        if (mc.player == null || mc.world == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        harvestQueue.clear();

        // Scan area based on pattern
        switch (pattern) {
            case ROWS:
                scanRows(playerPos);
                break;
            case SPIRAL:
                scanSpiral(playerPos);
                break;
            case LAYERS:
                scanLayers(playerPos);
                break;
        }

        System.out.println("[Crop Farm] Found " + harvestQueue.size() + " crops to harvest");

        if (harvestQueue.isEmpty()) {
            state = FarmState.COMPLETE;
        } else {
            state = FarmState.HARVESTING;
        }
    }

    /**
     * Scan in rows
     */
    private void scanRows(BlockPos center) {
        for (int x = -farmRadius; x <= farmRadius; x++) {
            for (int z = -farmRadius; z <= farmRadius; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (isMatureCrop(pos)) {
                        harvestQueue.add(pos);
                    }
                }
            }
        }
    }

    /**
     * Scan in spiral
     */
    private void scanSpiral(BlockPos center) {
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int maxSteps = (farmRadius * 2) * (farmRadius * 2);

        for (int i = 0; i < maxSteps; i++) {
            if (-farmRadius <= x && x <= farmRadius && -farmRadius <= z && z <= farmRadius) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (isMatureCrop(pos)) {
                        harvestQueue.add(pos);
                    }
                }
            }

            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }

            x += dx;
            z += dz;
        }
    }

    /**
     * Scan in layers
     */
    private void scanLayers(BlockPos center) {
        for (int y = -2; y <= 2; y++) {
            for (int x = -farmRadius; x <= farmRadius; x++) {
                for (int z = -farmRadius; z <= farmRadius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (isMatureCrop(pos)) {
                        harvestQueue.add(pos);
                    }
                }
            }
        }
    }

    /**
     * Check if crop is mature
     */
    private boolean isMatureCrop(BlockPos pos) {
        if (mc.world == null) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == cropType.block;
    }

    /**
     * Harvest crop
     */
    private void harvestCrop() {
        if (currentTarget == null) {
            currentTarget = harvestQueue.poll();
        }

        if (currentTarget == null) {
            // No more crops
            if (autoReplant) {
                state = FarmState.REPLANTING;
            } else {
                state = FarmState.COMPLETE;
            }
            return;
        }

        // Move to crop
        if (mc.player.getPos().distanceTo(currentTarget.toCenterPos()) > 5.0) {
            // TODO: Pathfind to crop
            return;
        }

        // Look at crop
        lookAt(currentTarget);

        // Break crop
        breakBlock(currentTarget);

        cropsHarvested++;
        currentTarget = null;
    }

    /**
     * Replant crop
     */
    private void replantCrop() {
        // TODO: Replant logic
        cropsReplanted++;
        state = FarmState.COMPLETE;
    }

    /**
     * Look at position
     */
    private void lookAt(BlockPos pos) {
        if (mc.player == null) return;

        Vec3d eyes = mc.player.getEyePos();
        Vec3d target = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3d dir = target.subtract(eyes).normalize();

        double yaw = Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        double pitch = -Math.toDegrees(Math.asin(dir.y));

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    /**
     * Break block
     */
    private void breakBlock(BlockPos pos) {
        // TODO: Actually break the block
        System.out.println("[Crop Farm] Harvesting: " + pos);
    }

    /**
     * Get status
     */
    public String getStatus() {
        return String.format("%s | Harvested: %d | Queue: %d",
                state, cropsHarvested, harvestQueue.size());
    }

    // Getters/Setters
    public void setCropType(CropType type) {
        this.cropType = type;
    }

    public void setPattern(FarmPattern pattern) {
        this.pattern = pattern;
    }

    public void setAutoReplant(boolean auto) {
        this.autoReplant = auto;
    }

    public void setFarmRadius(int radius) {
        this.farmRadius = radius;
    }
}
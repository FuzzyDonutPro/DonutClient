package com.donut.client.macros.farming;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * VerticalFarmMacro - Vertical farming (mushrooms, cocoa, nether wart, etc.)
 */
public class VerticalFarmMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private FarmState state = FarmState.IDLE;
    private int currentY = 0;
    private Queue<BlockPos> harvestQueue = new LinkedList<>();

    // Settings
    private FarmType farmType = FarmType.MUSHROOMS;
    private FarmPattern pattern = FarmPattern.SPIRAL;
    private int minY = 70;
    private int maxY = 90;
    private int radius = 10;
    private boolean bidirectional = true;
    private boolean autoReplant = true;

    // Statistics
    private int blocksHarvested = 0;
    private int blocksReplanted = 0;
    private int layersCompleted = 0;

    public enum FarmState {
        IDLE, MOVING_UP, MOVING_DOWN, HARVESTING, REPLANTING, COMPLETE
    }

    public enum FarmType {
        MUSHROOMS(Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM),
        COCOA(Blocks.COCOA),
        NETHER_WART(Blocks.NETHER_WART),
        CACTUS(Blocks.CACTUS),
        SUGAR_CANE(Blocks.SUGAR_CANE);

        public final Block[] blocks;

        FarmType(Block... blocks) {
            this.blocks = blocks;
        }
    }

    public enum FarmPattern {
        SPIRAL,    // Spiral around center
        GRID       // Grid pattern
    }

    public VerticalFarmMacro() {
        super("Vertical Farm", "Vertical farming for mushrooms, cocoa, etc.");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = FarmState.IDLE;
        harvestQueue.clear();
        blocksHarvested = 0;
        blocksReplanted = 0;
        layersCompleted = 0;
        System.out.println("[Vertical Farm] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            currentY = mc.player.getBlockPos().getY();
        }
        state = FarmState.MOVING_UP;
        System.out.println("[Vertical Farm] Starting...");
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                // Wait
                break;
            case MOVING_UP:
                moveUp();
                break;
            case MOVING_DOWN:
                moveDown();
                break;
            case HARVESTING:
                harvestLayer();
                break;
            case REPLANTING:
                replantLayer();
                break;
            case COMPLETE:
                // Done
                break;
        }
    }

    /**
     * Move up to next layer
     */
    private void moveUp() {
        if (mc.player == null) return;

        int playerY = mc.player.getBlockPos().getY();

        if (playerY >= maxY) {
            if (bidirectional) {
                state = FarmState.MOVING_DOWN;
            } else {
                state = FarmState.COMPLETE;
            }
            return;
        }

        // TODO: Move player up
        currentY = playerY;
        state = FarmState.HARVESTING;
    }

    /**
     * Move down to next layer
     */
    private void moveDown() {
        if (mc.player == null) return;

        int playerY = mc.player.getBlockPos().getY();

        if (playerY <= minY) {
            state = FarmState.COMPLETE;
            return;
        }

        // TODO: Move player down
        currentY = playerY;
        state = FarmState.HARVESTING;
    }

    /**
     * Harvest current layer
     */
    private void harvestLayer() {
        if (mc.player == null || mc.world == null) return;

        BlockPos centerPos = mc.player.getBlockPos();

        // Scan layer based on pattern
        switch (pattern) {
            case SPIRAL:
                harvestSpiral(centerPos);
                break;
            case GRID:
                harvestGrid(centerPos);
                break;
        }

        layersCompleted++;

        // Move to next layer
        if (bidirectional && state == FarmState.HARVESTING) {
            if (currentY < maxY) {
                state = FarmState.MOVING_UP;
            } else {
                state = FarmState.MOVING_DOWN;
            }
        } else {
            state = FarmState.MOVING_UP;
        }
    }

    /**
     * Harvest in spiral pattern
     */
    private void harvestSpiral(BlockPos center) {
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int maxSteps = (radius * 2) * (radius * 2);

        for (int i = 0; i < maxSteps; i++) {
            if (-radius <= x && x <= radius && -radius <= z && z <= radius) {
                BlockPos pos = center.add(x, 0, z);
                harvestBlock(pos);
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
     * Harvest in grid pattern
     */
    private void harvestGrid(BlockPos center) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = center.add(x, 0, z);
                harvestBlock(pos);
            }
        }
    }

    /**
     * Harvest a block
     */
    private void harvestBlock(BlockPos pos) {
        if (mc.world == null) return;

        Block block = mc.world.getBlockState(pos).getBlock();

        // Check if it's a harvestable block
        for (Block targetBlock : farmType.blocks) {
            if (block == targetBlock) {
                lookAt(pos);
                breakBlock(pos);
                blocksHarvested++;
                return;
            }
        }
    }

    /**
     * Replant layer
     */
    private void replantLayer() {
        // TODO: Replanting logic
        blocksReplanted++;
        state = FarmState.MOVING_UP;
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
        System.out.println("[Vertical Farm] Harvesting: " + pos);
    }

    /**
     * Get status
     */
    public String getStatus() {
        return String.format("%s | Y: %d | Harvested: %d | Layers: %d",
                state, currentY, blocksHarvested, layersCompleted);
    }

    // Getters/Setters
    public void setFarmType(FarmType type) {
        this.farmType = type;
    }

    public void setPattern(FarmPattern pattern) {
        this.pattern = pattern;
    }

    public void setMinY(int y) {
        this.minY = y;
    }

    public void setMaxY(int y) {
        this.maxY = y;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setBidirectional(boolean bidirectional) {
        this.bidirectional = bidirectional;
    }

    public void setAutoReplant(boolean auto) {
        this.autoReplant = auto;
    }
}
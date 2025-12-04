package com.donut.client.macros.mining;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * TunnelsMiner - Creates efficient mining tunnels
 * Features: Straight tunnels, branch mining, automatic pillar placement
 */
public class TunnelsMiner extends Macro {

    private final MinecraftClient mc;

    // State
    private TunnelState state = TunnelState.SETUP;
    private BlockPos tunnelStart = null;
    private BlockPos currentTarget = null;
    private int tunnelLength = 0;
    private int branchCount = 0;

    // Settings
    private TunnelType tunnelType = TunnelType.STRAIGHT;
    private int targetLength = 100;
    private int tunnelHeight = 3;
    private int tunnelWidth = 3;
    private int branchSpacing = 3; // Blocks between branches
    private boolean placePillars = true;
    private boolean placeFloor = false;
    private boolean autoLight = false;

    // Direction
    private TunnelDirection direction = TunnelDirection.NORTH;

    // Statistics
    private int blocksMined = 0;
    private int tunnelsCreated = 0;
    private int totalLength = 0;

    public enum TunnelState {
        SETUP,          // Setting up tunnel parameters
        MINING_MAIN,    // Mining main tunnel
        MINING_BRANCH,  // Mining branch tunnel
        PLACING_PILLAR, // Placing support pillar
        COMPLETE        // Tunnel complete
    }

    public enum TunnelType {
        STRAIGHT,       // Single straight tunnel
        BRANCH,         // Main tunnel with branches
        GRID,           // Grid pattern
        SPIRAL,         // Spiral outward
        LAYER           // Layer by layer
    }

    public enum TunnelDirection {
        NORTH, SOUTH, EAST, WEST
    }

    public TunnelsMiner() {
        super("Tunnels Miner", "Creates efficient mining tunnels");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Tunnels Miner] Starting...");
        state = TunnelState.SETUP;
        tunnelStart = mc.player.getBlockPos();
        tunnelLength = 0;
    }

    @Override
    public void start() {
        // Initialize macro
        state = TunnelState.SETUP;
        if (mc.player != null) {
            tunnelStart = mc.player.getBlockPos();
        }
        tunnelLength = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Tunnels Miner] Stopped");
        stopMovement();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case SETUP:
                setupTunnel();
                break;
            case MINING_MAIN:
                mineMainTunnel();
                break;
            case MINING_BRANCH:
                mineBranchTunnel();
                break;
            case PLACING_PILLAR:
                placePillar();
                break;
            case COMPLETE:
                completeTunnel();
                break;
        }
    }

    /**
     * Setup tunnel parameters
     */
    private void setupTunnel() {
        System.out.println("[Tunnels] Starting tunnel: " + tunnelType);
        System.out.println("[Tunnels] Direction: " + direction);
        System.out.println("[Tunnels] Length: " + targetLength);

        state = TunnelState.MINING_MAIN;
        tunnelLength = 0;
    }

    /**
     * Mine main tunnel
     */
    private void mineMainTunnel() {
        // Check if tunnel complete
        if (tunnelLength >= targetLength) {
            state = TunnelState.COMPLETE;
            return;
        }

        // Calculate next section to mine
        BlockPos nextSection = getNextTunnelSection();

        if (nextSection != null) {
            // Mine the section (height x width)
            mineSection(nextSection);
            tunnelLength++;

            // Place pillar if needed
            if (placePillars && tunnelLength % 5 == 0) {
                state = TunnelState.PLACING_PILLAR;
                return;
            }

            // Create branch if needed
            if (tunnelType == TunnelType.BRANCH && tunnelLength % branchSpacing == 0) {
                state = TunnelState.MINING_BRANCH;
                branchCount++;
                return;
            }
        }
    }

    /**
     * Get next tunnel section position
     */
    private BlockPos getNextTunnelSection() {
        if (tunnelStart == null) return null;

        int offset = tunnelLength;

        switch (direction) {
            case NORTH:
                return tunnelStart.add(0, 0, -offset);
            case SOUTH:
                return tunnelStart.add(0, 0, offset);
            case EAST:
                return tunnelStart.add(offset, 0, 0);
            case WEST:
                return tunnelStart.add(-offset, 0, 0);
        }

        return tunnelStart;
    }

    /**
     * Mine a section (height x width x 1)
     */
    private void mineSection(BlockPos center) {
        int halfWidth = tunnelWidth / 2;
        int halfHeight = tunnelHeight / 2;

        // Mine all blocks in section
        for (int y = -halfHeight; y <= halfHeight; y++) {
            for (int x = -halfWidth; x <= halfWidth; x++) {
                BlockPos pos = center.add(x, y, 0);

                Block block = mc.world.getBlockState(pos).getBlock();
                if (!block.equals(Blocks.AIR)) {
                    // Break block
                    breakBlock(pos);
                    blocksMined++;
                }
            }
        }

        // Place floor if enabled
        if (placeFloor) {
            placeFloorSection(center);
        }

        // Place torches if enabled
        if (autoLight && tunnelLength % 8 == 0) {
            placeTorch(center);
        }
    }

    /**
     * Mine branch tunnel
     */
    private void mineBranchTunnel() {
        System.out.println("[Tunnels] Mining branch " + branchCount);

        // TODO: Implement branch mining logic
        // Mine perpendicular tunnel from main tunnel

        // Return to main tunnel
        state = TunnelState.MINING_MAIN;
    }

    /**
     * Place support pillar
     */
    private void placePillar() {
        System.out.println("[Tunnels] Placing pillar at length " + tunnelLength);

        BlockPos pillarPos = getNextTunnelSection();
        if (pillarPos != null) {
            // Place pillar blocks (cobblestone or stone)
            for (int y = -1; y < tunnelHeight; y++) {
                placeBlock(pillarPos.add(0, y, 0), Blocks.COBBLESTONE);
            }
        }

        state = TunnelState.MINING_MAIN;
    }

    /**
     * Place floor section
     */
    private void placeFloorSection(BlockPos center) {
        int halfWidth = tunnelWidth / 2;

        for (int x = -halfWidth; x <= halfWidth; x++) {
            BlockPos floorPos = center.add(x, -tunnelHeight/2 - 1, 0);
            placeBlock(floorPos, Blocks.STONE);
        }
    }

    /**
     * Place torch
     */
    private void placeTorch(BlockPos center) {
        BlockPos torchPos = center.add(0, 0, 0);
        placeBlock(torchPos, Blocks.TORCH);
    }

    /**
     * Break block
     */
    private void breakBlock(BlockPos pos) {
        // TODO: Implement actual block breaking
        System.out.println("[Tunnels] Breaking block at: " + pos);
    }

    /**
     * Place block
     */
    private void placeBlock(BlockPos pos, Block block) {
        // TODO: Implement actual block placement
        System.out.println("[Tunnels] Placing " + block + " at: " + pos);
    }

    /**
     * Complete tunnel
     */
    private void completeTunnel() {
        System.out.println("========================================");
        System.out.println("TUNNEL COMPLETE");
        System.out.println("Type: " + tunnelType);
        System.out.println("Length: " + tunnelLength + " blocks");
        System.out.println("Blocks Mined: " + blocksMined);
        System.out.println("Branches: " + branchCount);
        System.out.println("========================================");

        tunnelsCreated++;
        totalLength += tunnelLength;

        // Reset or continue
        state = TunnelState.SETUP;
        tunnelStart = mc.player.getBlockPos();
        tunnelLength = 0;
    }

    /**
     * Stop all movement
     */
    private void stopMovement() {
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        }
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | Length: %d/%d | Blocks: %d",
                state, tunnelLength, targetLength, blocksMined);
    }

    /**
     * Get statistics
     */
    public String getStats() {
        return String.format(
                "Tunnels: %d | Total Length: %d | Blocks: %d | Runtime: %s",
                tunnelsCreated, totalLength, blocksMined, getRuntimeFormatted()
        );
    }

    // ==================== GETTERS/SETTERS ====================

    public void setTunnelType(TunnelType type) {
        this.tunnelType = type;
    }

    public void setTargetLength(int length) {
        this.targetLength = length;
    }

    public void setTunnelHeight(int height) {
        this.tunnelHeight = height;
    }

    public void setTunnelWidth(int width) {
        this.tunnelWidth = width;
    }

    public void setBranchSpacing(int spacing) {
        this.branchSpacing = spacing;
    }

    public void setPlacePillars(boolean place) {
        this.placePillars = place;
    }

    public void setPlaceFloor(boolean place) {
        this.placeFloor = place;
    }

    public void setAutoLight(boolean auto) {
        this.autoLight = auto;
    }

    public void setDirection(TunnelDirection dir) {
        this.direction = dir;
    }
}
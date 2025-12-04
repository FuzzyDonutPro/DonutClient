package com.donut.client.macros.farming;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * BuildersMacro - Automated builder's wand usage
 * Features: Auto-fill, pattern building, mass placement
 */
public class BuildersMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private MacroState state = MacroState.IDLE;
    private List<BlockPos> buildQueue = new ArrayList<>();
    private int currentIndex = 0;

    // Settings
    private BuildMode mode = BuildMode.FILL;
    private BlockPos startPos = null;
    private BlockPos endPos = null;
    private String blockType = "DIRT";

    // Settings
    private int buildDelay = 100; // ms between placements
    private boolean useFastPlace = true;

    // Statistics
    private int blocksPlaced = 0;

    public enum MacroState {
        IDLE,
        GENERATING,
        BUILDING,
        COMPLETE
    }

    public enum BuildMode {
        FILL,       // Fill area
        WALLS,      // Build walls only
        FLOOR,      // Build floor
        CEILING,    // Build ceiling
        OUTLINE,    // Build outline only
        PATTERN     // Custom pattern
    }

    public BuildersMacro() {
        super("Builders Macro", "Automated builder's wand usage");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = MacroState.GENERATING;
        buildQueue.clear();
        currentIndex = 0;
        blocksPlaced = 0;
        System.out.println("[Builders Macro] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Builders Macro] Starting...");
        System.out.println("[Builders Macro] Mode: " + mode);
        System.out.println("[Builders Macro] Block: " + blockType);

        if (startPos == null || endPos == null) {
            System.out.println("[Builders Macro] ERROR: Positions not set!");
            onDisable();
            return;
        }

        state = MacroState.GENERATING;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Builders Macro] Stopped");
        printStatistics();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                // Do nothing
                break;
            case GENERATING:
                generateBuildQueue();
                break;
            case BUILDING:
                build();
                break;
            case COMPLETE:
                complete();
                break;
        }
    }

    /**
     * Generate build queue based on mode
     */
    private void generateBuildQueue() {
        if (startPos == null || endPos == null) return;

        buildQueue.clear();

        // Calculate bounds
        int minX = Math.min(startPos.getX(), endPos.getX());
        int maxX = Math.max(startPos.getX(), endPos.getX());
        int minY = Math.min(startPos.getY(), endPos.getY());
        int maxY = Math.max(startPos.getY(), endPos.getY());
        int minZ = Math.min(startPos.getZ(), endPos.getZ());
        int maxZ = Math.max(startPos.getZ(), endPos.getZ());

        switch (mode) {
            case FILL:
                // Fill entire area
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            buildQueue.add(new BlockPos(x, y, z));
                        }
                    }
                }
                break;

            case WALLS:
                // Build walls only
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            // Only edges
                            if (x == minX || x == maxX || z == minZ || z == maxZ) {
                                buildQueue.add(new BlockPos(x, y, z));
                            }
                        }
                    }
                }
                break;

            case FLOOR:
                // Build floor only
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        buildQueue.add(new BlockPos(x, minY, z));
                    }
                }
                break;

            case CEILING:
                // Build ceiling only
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        buildQueue.add(new BlockPos(x, maxY, z));
                    }
                }
                break;

            case OUTLINE:
                // Build outline only (edges of box)
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            // Only corners and edges
                            int edgeCount = 0;
                            if (x == minX || x == maxX) edgeCount++;
                            if (y == minY || y == maxY) edgeCount++;
                            if (z == minZ || z == maxZ) edgeCount++;

                            if (edgeCount >= 2) {
                                buildQueue.add(new BlockPos(x, y, z));
                            }
                        }
                    }
                }
                break;
        }

        System.out.println("[Builders Macro] Generated " + buildQueue.size() + " blocks to place");

        currentIndex = 0;
        state = MacroState.BUILDING;
    }

    /**
     * Build blocks
     */
    private void build() {
        if (currentIndex >= buildQueue.size()) {
            state = MacroState.COMPLETE;
            return;
        }

        BlockPos pos = buildQueue.get(currentIndex);

        // Place block
        placeBlock(pos);
        blocksPlaced++;

        if (blocksPlaced % 100 == 0) {
            System.out.println("[Builders Macro] Progress: " + blocksPlaced + "/" + buildQueue.size());
        }

        // Move to next
        currentIndex++;

        // Delay between placements
        if (!useFastPlace) {
            try {
                Thread.sleep(buildDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Place block at position
     */
    private void placeBlock(BlockPos pos) {
        // TODO: Use builder's wand or place block
        // Right-click with wand/block
    }

    /**
     * Complete macro
     */
    private void complete() {
        System.out.println("========================================");
        System.out.println("🔨 BUILDERS MACRO COMPLETE 🔨");
        System.out.println("Blocks Placed: " + blocksPlaced);
        System.out.println("Mode: " + mode);
        System.out.println("========================================");

        onDisable();
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("BUILDERS MACRO STATISTICS");
        System.out.println("========================================");
        System.out.println("Mode: " + mode);
        System.out.println("Block Type: " + blockType);
        System.out.println("Blocks Placed: " + blocksPlaced);
        System.out.println("Total Blocks: " + buildQueue.size());

        if (buildQueue.size() > 0) {
            double completion = (double) blocksPlaced / buildQueue.size() * 100;
            System.out.println("Completion: " + String.format("%.1f%%", completion));
        }

        System.out.println("Runtime: " + getRuntimeFormatted());

        long seconds = getRuntime() / 1000;
        if (seconds > 0) {
            int blocksPerSecond = (int)(blocksPlaced / seconds);
            System.out.println("Rate: " + blocksPerSecond + " blocks/second");
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | %s | Progress: %d/%d",
                state, mode, blocksPlaced, buildQueue.size());
    }

    // ==================== GETTERS/SETTERS ====================

    public void setMode(BuildMode mode) {
        this.mode = mode;
    }

    public void setPositions(BlockPos start, BlockPos end) {
        this.startPos = start;
        this.endPos = end;
    }

    public void setBlockType(String block) {
        this.blockType = block;
    }

    public void setBuildDelay(int delay) {
        this.buildDelay = delay;
    }

    public void setUseFastPlace(boolean fast) {
        this.useFastPlace = fast;
    }
}
package com.donut.client.macros.foraging;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * ForagingMacro - Universal foraging macro for all wood types
 * Intelligent tree detection and chopping
 */
public class ForagingMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private ForagingState state = ForagingState.IDLE;
    private BlockPos currentTree = null;
    private List<BlockPos> treeLogs = new ArrayList<>();
    private int logIndex = 0;

    // Settings
    private int searchRadius = 30;
    private boolean autoRotate = true;
    private boolean breakLeaves = false;
    private boolean replantSaplings = false;
    private TreeType targetTreeType = TreeType.ANY;

    // Statistics
    private int logsChopped = 0;
    private int treesChopped = 0;
    private Map<TreeType, Integer> treesByType = new HashMap<>();

    public enum ForagingState {
        IDLE, SEARCHING, MOVING_TO_TREE, CHOPPING, COLLECTING
    }

    public enum TreeType {
        ANY,        // Any tree
        OAK,        // Oak wood
        SPRUCE,     // Spruce wood
        BIRCH,      // Birch wood
        JUNGLE,     // Jungle wood
        ACACIA,     // Acacia wood
        DARK_OAK,   // Dark oak wood
        CHERRY      // Cherry wood (1.20+)
    }

    public ForagingMacro() {
        super("Foraging", "Universal foraging macro for all wood types");
        this.mc = MinecraftClient.getInstance();

        // Initialize counters
        for (TreeType type : TreeType.values()) {
            treesByType.put(type, 0);
        }
    }

    @Override
    public void start() {
        state = ForagingState.IDLE;
        currentTree = null;
        treeLogs.clear();
        logsChopped = 0;
        treesChopped = 0;
        System.out.println("[Foraging] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Foraging] Starting foraging...");
        state = ForagingState.SEARCHING;
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                break;
            case SEARCHING:
                searchForTrees();
                break;
            case MOVING_TO_TREE:
                moveToTree();
                break;
            case CHOPPING:
                chopTree();
                break;
            case COLLECTING:
                collectItems();
                break;
        }
    }

    /**
     * Search for nearby trees
     */
    private void searchForTrees() {
        if (mc.world == null || mc.player == null) return;

        BlockPos nearestTree = null;
        double nearestDistance = Double.MAX_VALUE;

        BlockPos playerPos = mc.player.getBlockPos();

        // Scan area for logs
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (isLog(block)) {
                        // Check if it's the target tree type
                        TreeType type = getTreeType(block);
                        if (targetTreeType == TreeType.ANY || type == targetTreeType) {
                            double distance = mc.player.getPos().distanceTo(pos.toCenterPos());

                            if (distance < nearestDistance) {
                                nearestTree = pos;
                                nearestDistance = distance;
                            }
                        }
                    }
                }
            }
        }

        if (nearestTree != null) {
            currentTree = nearestTree;
            System.out.println("[Foraging] Found tree at: " + nearestTree);
            state = ForagingState.MOVING_TO_TREE;
        } else {
            System.out.println("[Foraging] No trees found in range");
        }
    }

    /**
     * Check if block is a log
     */
    private boolean isLog(Block block) {
        return block == Blocks.OAK_LOG ||
                block == Blocks.SPRUCE_LOG ||
                block == Blocks.BIRCH_LOG ||
                block == Blocks.JUNGLE_LOG ||
                block == Blocks.ACACIA_LOG ||
                block == Blocks.DARK_OAK_LOG ||
                block == Blocks.CHERRY_LOG ||
                block == Blocks.MANGROVE_LOG;
    }

    /**
     * Get tree type from log block
     */
    private TreeType getTreeType(Block block) {
        if (block == Blocks.OAK_LOG) return TreeType.OAK;
        if (block == Blocks.SPRUCE_LOG) return TreeType.SPRUCE;
        if (block == Blocks.BIRCH_LOG) return TreeType.BIRCH;
        if (block == Blocks.JUNGLE_LOG) return TreeType.JUNGLE;
        if (block == Blocks.ACACIA_LOG) return TreeType.ACACIA;
        if (block == Blocks.DARK_OAK_LOG) return TreeType.DARK_OAK;
        if (block == Blocks.CHERRY_LOG) return TreeType.CHERRY;
        return TreeType.OAK;
    }

    /**
     * Move to tree
     */
    private void moveToTree() {
        if (currentTree == null) {
            state = ForagingState.SEARCHING;
            return;
        }

        if (mc.player == null) return;

        double distance = mc.player.getPos().distanceTo(currentTree.toCenterPos());

        if (distance <= 5.0) {
            // Close enough, start chopping
            System.out.println("[Foraging] Reached tree, analyzing...");
            analyzeTree();
            state = ForagingState.CHOPPING;
        } else {
            // Move towards tree
            if (autoRotate) {
                lookAt(currentTree.toCenterPos());
            }
            // TODO: Use pathfinding
        }
    }

    /**
     * Analyze tree structure
     */
    private void analyzeTree() {
        treeLogs.clear();
        logIndex = 0;

        if (currentTree == null || mc.world == null) return;

        // Find all connected logs
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(currentTree);
        visited.add(currentTree);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            treeLogs.add(pos);

            // Check adjacent blocks
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos adjacent = pos.add(x, y, z);

                        if (!visited.contains(adjacent)) {
                            Block block = mc.world.getBlockState(adjacent).getBlock();

                            if (isLog(block)) {
                                queue.add(adjacent);
                                visited.add(adjacent);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("[Foraging] Tree has " + treeLogs.size() + " logs");
    }

    /**
     * Chop tree
     */
    private void chopTree() {
        if (treeLogs.isEmpty() || logIndex >= treeLogs.size()) {
            // Tree chopped
            TreeType type = getTreeType(mc.world.getBlockState(currentTree).getBlock());
            treesByType.put(type, treesByType.get(type) + 1);
            treesChopped++;

            System.out.println("[Foraging] Tree chopped! Total: " + treesChopped);

            currentTree = null;
            state = ForagingState.COLLECTING;
            return;
        }

        // Chop current log
        BlockPos log = treeLogs.get(logIndex);

        if (mc.world.getBlockState(log).getBlock() == Blocks.AIR) {
            // Log already broken, move to next
            logIndex++;
            return;
        }

        // Look at log
        if (autoRotate) {
            lookAt(log.toCenterPos());
        }

        // Break log
        breakBlock(log);
        logsChopped++;

        logIndex++;
    }

    /**
     * Break block
     */
    private void breakBlock(BlockPos pos) {
        // TODO: Simulate left click to break block
        System.out.println("[Foraging] Breaking log at: " + pos);
    }

    /**
     * Collect items
     */
    private void collectItems() {
        System.out.println("[Foraging] Collecting drops...");

        // Wait for items to spawn
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Replant if enabled
        if (replantSaplings) {
            replantSapling();
        }

        // Search for next tree
        state = ForagingState.SEARCHING;
    }

    /**
     * Replant sapling
     */
    private void replantSapling() {
        // TODO: Place sapling at tree base
        System.out.println("[Foraging] Replanting sapling...");
    }

    /**
     * Look at position
     */
    private void lookAt(Vec3d pos) {
        if (mc.player == null) return;

        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = pos.subtract(eyes).normalize();

        double yaw = Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        double pitch = -Math.toDegrees(Math.asin(dir.y));

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    /**
     * Get status info
     */
    public String getStatusInfo() {
        return String.format("%s | Trees: %d | Logs: %d | Rate: %.1f/hr",
                state, treesChopped, logsChopped, getTreesPerHour());
    }

    /**
     * Get trees per hour
     */
    public double getTreesPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) treesChopped / (runtime / 3600000.0);
    }

    /**
     * Get current state (PUBLIC GETTER)
     */
    public ForagingState getForagingState() {
        return state;
    }

    // Getters/Setters
    public void setSearchRadius(int radius) {
        this.searchRadius = radius;
    }

    public void setAutoRotate(boolean auto) {
        this.autoRotate = auto;
    }

    public void setBreakLeaves(boolean breakLeaves) {
        this.breakLeaves = breakLeaves;
    }

    public void setReplantSaplings(boolean replant) {
        this.replantSaplings = replant;
    }

    public void setTargetTreeType(TreeType type) {
        this.targetTreeType = type;
    }

    public Map<TreeType, Integer> getTreesByType() {
        return new HashMap<>(treesByType);
    }
}
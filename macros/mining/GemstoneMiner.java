package com.donut.client.macros.mining;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * GemstoneMiner - Mines all 8 gemstone types in Crystal Hollows
 * Gemstones appear as stained glass blocks and stained glass panes
 */
public class GemstoneMiner extends Macro {

    private final MinecraftClient mc;

    // State
    private MiningState state = MiningState.SCANNING;
    private BlockPos targetBlock = null;
    private List<BlockPos> vein = new ArrayList<>();
    private int veinIndex = 0;

    // Settings
    private GemstoneType targetGemstone = GemstoneType.RUBY;
    private MiningPattern pattern = MiningPattern.VEIN;
    private boolean perfectGemsOnly = false;
    private boolean flawlessGemsOnly = false;
    private int minVeinSize = 3;
    private boolean avoidLava = true;
    private boolean avoidMobs = true;
    private int scanRadius = 30;
    private double breakRange = 5.0;

    // Auto-features
    private boolean autoSell = false;
    private boolean useDrill = true;
    private boolean useAbility = true;
    private boolean powderGrind = false;

    // Statistics
    private Map<GemstoneType, Integer> gemstonesMined = new HashMap<>();
    private Map<GemstoneType, Integer> perfectGems = new HashMap<>();
    private Map<GemstoneType, Integer> flawlessGems = new HashMap<>();
    private long powderCollected = 0;
    private int totalGemstones = 0;

    public enum MiningState {
        SCANNING,      // Looking for gemstones
        MOVING,        // Moving to gemstone
        MINING,        // Breaking gemstone
        SELLING        // Selling at NPC
    }

    public enum GemstoneType {
        RUBY(Blocks.RED_STAINED_GLASS, Blocks.RED_STAINED_GLASS_PANE, "Ruby"),
        AMBER(Blocks.ORANGE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS_PANE, "Amber"),
        AMETHYST(Blocks.PURPLE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS_PANE, "Amethyst"),
        JADE(Blocks.LIME_STAINED_GLASS, Blocks.LIME_STAINED_GLASS_PANE, "Jade"),
        SAPPHIRE(Blocks.BLUE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS_PANE, "Sapphire"),
        TOPAZ(Blocks.YELLOW_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS_PANE, "Topaz"),
        JASPER(Blocks.MAGENTA_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS_PANE, "Jasper"),
        OPAL(Blocks.WHITE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS_PANE, "Opal");

        public final Block glassBlock;
        public final Block glassPane;
        public final String displayName;

        GemstoneType(Block glassBlock, Block glassPane, String displayName) {
            this.glassBlock = glassBlock;
            this.glassPane = glassPane;
            this.displayName = displayName;
        }

        public boolean matches(Block block) {
            return block == glassBlock || block == glassPane;
        }
    }

    public enum MiningPattern {
        VEIN,          // Follow gemstone veins (most efficient)
        SPIRAL,        // Spiral outward from center
        GRID,          // Systematic grid pattern
        ROUTE,         // Pre-defined optimal route
        RANDOM         // Random walk
    }

    public GemstoneMiner() {
        super("Gemstone Miner", "Mines all 8 gemstone types in Crystal Hollows");
        this.mc = MinecraftClient.getInstance();

        // Initialize statistics
        for (GemstoneType type : GemstoneType.values()) {
            gemstonesMined.put(type, 0);
            perfectGems.put(type, 0);
            flawlessGems.put(type, 0);
        }
    }

    @Override
    public void start() {
        state = MiningState.SCANNING;
        vein.clear();
        System.out.println("[Gemstone Miner] Initialized - Target: " + targetGemstone.displayName);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Gemstone Miner] Starting...");
        System.out.println("[Gemstone Miner] Target: " + targetGemstone.displayName);
        System.out.println("[Gemstone Miner] Pattern: " + pattern);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Gemstone Miner] Stopped");
        printStatistics();
        stopMovement();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case SCANNING:
                scanForGemstones();
                break;
            case MOVING:
                moveToTarget();
                break;
            case MINING:
                mineGemstone();
                break;
            case SELLING:
                sellItems();
                break;
        }

        // Check inventory
        if (isInventoryFull() && autoSell) {
            state = MiningState.SELLING;
        }
    }

    /**
     * Scan for gemstone blocks (stained glass)
     */
    private void scanForGemstones() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestBlock = null;
        double bestScore = -1;

        // Scan area for target gemstone
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius/2; y <= scanRadius/2; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    // Check if it's the target gemstone (stained glass or pane)
                    if (targetGemstone.matches(block)) {
                        // Calculate score based on various factors
                        double score = calculateBlockScore(pos);

                        if (score > bestScore) {
                            bestBlock = pos;
                            bestScore = score;
                        }
                    }
                }
            }
        }

        if (bestBlock != null) {
            targetBlock = bestBlock;

            // If vein mining, find entire vein
            if (pattern == MiningPattern.VEIN) {
                findVein(bestBlock);
                veinIndex = 0;
            }

            state = MiningState.MOVING;
            System.out.println("[Gemstone] Found " + targetGemstone.displayName + " vein at: " + bestBlock);
        }
    }

    /**
     * Calculate score for a block (higher = better)
     */
    private double calculateBlockScore(BlockPos pos) {
        double score = 100.0;

        // Distance factor (closer = better)
        double distance = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos));
        score -= distance * 0.1;

        // Avoid lava
        if (avoidLava && isNearLava(pos)) {
            score -= 50;
        }

        // Avoid mobs
        if (avoidMobs && isNearMobs(pos)) {
            score -= 30;
        }

        // Prefer larger veins
        if (pattern == MiningPattern.VEIN) {
            int veinSize = estimateVeinSize(pos);
            if (veinSize < minVeinSize) {
                score -= 40; // Skip small veins
            } else {
                score += veinSize * 2; // Prefer larger veins
            }
        }

        return score;
    }

    /**
     * Check if position is near lava
     */
    private boolean isNearLava(BlockPos pos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    Block block = mc.world.getBlockState(checkPos).getBlock();
                    if (block == Blocks.LAVA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if position is near mobs
     */
    private boolean isNearMobs(BlockPos pos) {
        // TODO: Check for hostile entities near position
        return false;
    }

    /**
     * Estimate vein size without full scan
     */
    private int estimateVeinSize(BlockPos start) {
        int count = 0;
        Block targetBlock = mc.world.getBlockState(start).getBlock();

        // Quick check of adjacent blocks
        for (BlockPos adjacent : getAdjacentBlocks(start)) {
            if (mc.world.getBlockState(adjacent).getBlock() == targetBlock) {
                count++;
            }
        }

        return count;
    }

    /**
     * Find entire vein of gemstones
     */
    private void findVein(BlockPos start) {
        vein.clear();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start);

        Block startBlock = mc.world.getBlockState(start).getBlock();

        // BFS to find connected gemstone blocks (both glass and panes)
        while (!queue.isEmpty() && vein.size() < 200) {
            BlockPos current = queue.poll();
            vein.add(current);

            // Check 6 adjacent blocks
            for (BlockPos neighbor : getAdjacentBlocks(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);

                    Block block = mc.world.getBlockState(neighbor).getBlock();
                    // Match both glass blocks and panes of same color
                    if (targetGemstone.matches(block)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        // Sort vein for efficient mining
        optimizeVeinOrder();

        System.out.println("[Gemstone] Found vein with " + vein.size() + " blocks");

        // Skip if vein too small
        if (vein.size() < minVeinSize) {
            System.out.println("[Gemstone] Vein too small (" + vein.size() + " < " + minVeinSize + "), skipping");
            vein.clear();
            targetBlock = null;
            state = MiningState.SCANNING;
        }
    }

    /**
     * Optimize vein mining order
     */
    private void optimizeVeinOrder() {
        // Sort by distance from player for efficiency
        BlockPos playerPos = mc.player.getBlockPos();
        vein.sort(Comparator.comparingDouble(
                pos -> pos.getSquaredDistance(playerPos)
        ));
    }

    /**
     * Get 6 adjacent blocks
     */
    private List<BlockPos> getAdjacentBlocks(BlockPos pos) {
        return Arrays.asList(
                pos.up(), pos.down(),
                pos.north(), pos.south(),
                pos.east(), pos.west()
        );
    }

    /**
     * Move to target block
     */
    private void moveToTarget() {
        if (targetBlock == null) {
            state = MiningState.SCANNING;
            return;
        }

        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = Vec3d.ofCenter(targetBlock);
        double distance = playerPos.distanceTo(targetPos);

        // Check if in range to mine
        if (distance <= breakRange) {
            state = MiningState.MINING;
            stopMovement();
            return;
        }

        // Calculate direction
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double dy = targetPos.y - playerPos.y;

        // Set yaw to face target
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        mc.player.setYaw(yaw);

        // Set pitch to look at target
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));
        mc.player.setPitch(pitch);

        // Move forward
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(true);

            // Jump if blocked
            if (mc.player.horizontalCollision || shouldJump()) {
                mc.options.jumpKey.setPressed(true);
            } else {
                mc.options.jumpKey.setPressed(false);
            }
        }
    }

    /**
     * Check if should jump
     */
    private boolean shouldJump() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos front = playerPos.offset(mc.player.getHorizontalFacing());

        // Check if block in front at foot level is solid
        return !mc.world.getBlockState(front).isAir();
    }

    /**
     * Mine gemstone block
     */
    private void mineGemstone() {
        if (targetBlock == null) {
            state = MiningState.SCANNING;
            return;
        }

        // Check if block still exists
        Block block = mc.world.getBlockState(targetBlock).getBlock();
        if (!targetGemstone.matches(block)) {
            // Block broken, track stats
            int count = gemstonesMined.get(targetGemstone);
            gemstonesMined.put(targetGemstone, count + 1);
            totalGemstones++;

            // TODO: Check gem quality (perfect/flawless) from item pickup

            // Move to next block in vein
            if (pattern == MiningPattern.VEIN && veinIndex < vein.size() - 1) {
                veinIndex++;
                targetBlock = vein.get(veinIndex);
                state = MiningState.MOVING;
            } else {
                targetBlock = null;
                vein.clear();
                state = MiningState.SCANNING;
            }
            return;
        }

        // Look at block
        Vec3d targetPos = Vec3d.ofCenter(targetBlock);
        lookAt(targetPos);

        // Break block
        breakBlock(targetBlock);

        // Use ability if enabled
        if (useAbility && shouldUseAbility()) {
            usePickaxeAbility();
        }
    }

    /**
     * Check if should use pickaxe ability
     */
    private boolean shouldUseAbility() {
        // Use ability every 5 blocks for efficiency
        return totalGemstones % 5 == 0;
    }

    /**
     * Use pickaxe ability
     */
    private void usePickaxeAbility() {
        // TODO: Right-click with drill/pickaxe to use ability
        System.out.println("[Gemstone] Using pickaxe ability");
    }

    /**
     * Look at position
     */
    private void lookAt(Vec3d target) {
        Vec3d playerPos = mc.player.getEyePos();

        double dx = target.x - playerPos.x;
        double dy = target.y - playerPos.y;
        double dz = target.z - playerPos.z;

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    /**
     * Break block
     */
    private void breakBlock(BlockPos pos) {
        // TODO: Implement actual block breaking with packets
        System.out.println("[Gemstone] Breaking " + targetGemstone.displayName + " at: " + pos);
    }

    /**
     * Sell items at NPC
     */
    private void sellItems() {
        System.out.println("[Gemstone] Selling gemstones...");

        // TODO: Walk to NPC and sell
        // For now, just return to mining
        state = MiningState.SCANNING;
    }

    /**
     * Check if inventory is full
     */
    private boolean isInventoryFull() {
        if (mc.player == null || mc.player.getInventory() == null) return false;

        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                emptySlots++;
            }
        }

        return emptySlots < 3;
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
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("GEMSTONE MINER STATISTICS");
        System.out.println("========================================");
        System.out.println("Total Gemstones: " + totalGemstones);

        for (GemstoneType type : GemstoneType.values()) {
            int count = gemstonesMined.get(type);
            if (count > 0) {
                int perfect = perfectGems.get(type);
                int flawless = flawlessGems.get(type);
                System.out.println(type.displayName + ": " + count +
                        " (Perfect: " + perfect + ", Flawless: " + flawless + ")");
            }
        }

        System.out.println("Powder: " + powderCollected);
        System.out.println("Runtime: " + getRuntimeFormatted());

        // Calculate rate
        long seconds = getRuntime() / 1000;
        if (seconds > 0) {
            long gemsPerHour = totalGemstones * 3600 / seconds;
            System.out.println("Rate: " + gemsPerHour + " gems/hour");
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | %s: %d | Total: %d",
                state, targetGemstone.displayName,
                gemstonesMined.get(targetGemstone), totalGemstones);
    }

    // ==================== GETTERS/SETTERS ====================

    public void setTargetGemstone(GemstoneType type) {
        this.targetGemstone = type;
        System.out.println("[Gemstone] Target changed to: " + type.displayName);
    }

    public void setPattern(MiningPattern pattern) {
        this.pattern = pattern;
    }

    public void setPerfectGemsOnly(boolean only) {
        this.perfectGemsOnly = only;
    }

    public void setFlawlessGemsOnly(boolean only) {
        this.flawlessGemsOnly = only;
    }

    public void setMinVeinSize(int size) {
        this.minVeinSize = size;
    }

    public void setAvoidLava(boolean avoid) {
        this.avoidLava = avoid;
    }

    public void setAvoidMobs(boolean avoid) {
        this.avoidMobs = avoid;
    }

    public void setScanRadius(int radius) {
        this.scanRadius = radius;
    }

    public void setBreakRange(double range) {
        this.breakRange = range;
    }

    public void setAutoSell(boolean auto) {
        this.autoSell = auto;
    }

    public void setUseDrill(boolean use) {
        this.useDrill = use;
    }

    public void setUseAbility(boolean use) {
        this.useAbility = use;
    }

    public void setPowderGrind(boolean grind) {
        this.powderGrind = grind;
    }
}
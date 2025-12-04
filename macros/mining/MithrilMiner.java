package com.donut.client.macros.mining;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * MithrilMiner - Mines mithril in Dwarven Mines
 * Features: Vein mining, titanium detection, auto-sell, powder grinding
 */
public class MithrilMiner extends Macro {

    private final MinecraftClient mc;

    // State
    private MiningState state = MiningState.SCANNING;
    private BlockPos targetBlock = null;
    private List<BlockPos> vein = new ArrayList<>();
    private int veinIndex = 0;

    // Settings
    private boolean veinMine = true;
    private boolean titaniumPriority = true;
    private boolean autoSell = false;
    private boolean powderGrind = false;
    private int scanRadius = 30;
    private double breakRange = 5.0;

    // Statistics
    private int mithrilMined = 0;
    private int titaniumMined = 0;
    private int gemstoneMined = 0;
    private long powderCollected = 0;

    // Mining patterns
    private MiningPattern pattern = MiningPattern.CLOSEST;

    public enum MiningState {
        SCANNING,      // Looking for mithril
        MOVING,        // Moving to mithril
        MINING,        // Breaking mithril
        SELLING        // Selling at NPC
    }

    public enum MiningPattern {
        CLOSEST,       // Mine nearest blocks
        VEIN,          // Follow mithril veins
        SPIRAL,        // Spiral outward
        LAYER          // Mine by Y layers
    }

    public MithrilMiner() {
        super("Mithril Miner", "Mines mithril and titanium in Dwarven Mines");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Mithril Miner] Starting...");
        state = MiningState.SCANNING;
        vein.clear();
    }

    @Override
    public void start() {
        // Initialize macro
        state = MiningState.SCANNING;
        vein.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Mithril Miner] Stopped");
        stopMovement();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case SCANNING:
                scanForMithril();
                break;
            case MOVING:
                moveToTarget();
                break;
            case MINING:
                mineMithril();
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
     * Scan for mithril blocks
     */
    private void scanForMithril() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestBlock = null;
        double bestDistance = Double.MAX_VALUE;
        boolean foundTitanium = false;

        // Scan area for mithril/titanium
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius/2; y <= scanRadius/2; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    // Check for titanium (prioritize if enabled)
                    if (titaniumPriority && isTitanium(block)) {
                        double distance = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos));
                        if (distance < bestDistance) {
                            bestBlock = pos;
                            bestDistance = distance;
                            foundTitanium = true;
                        }
                    }

                    // Check for mithril (if no titanium found)
                    if (!foundTitanium && isMithril(block)) {
                        double distance = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos));
                        if (distance < bestDistance) {
                            bestBlock = pos;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }

        if (bestBlock != null) {
            targetBlock = bestBlock;

            // If vein mining, find entire vein
            if (veinMine) {
                findVein(bestBlock);
                veinIndex = 0;
            }

            state = MiningState.MOVING;
            System.out.println("[Mithril] Found block at: " + bestBlock + (foundTitanium ? " (TITANIUM!)" : ""));
        }
    }

    /**
     * Check if block is mithril
     */
    private boolean isMithril(Block block) {
        // Mithril appears as light blue wool/concrete in Dwarven Mines
        return block == Blocks.PRISMARINE ||
                block == Blocks.LIGHT_BLUE_WOOL ||
                block == Blocks.LIGHT_BLUE_CONCRETE;
    }

    /**
     * Check if block is titanium
     */
    private boolean isTitanium(Block block) {
        // Titanium appears as white concrete/wool
        return block == Blocks.WHITE_CONCRETE ||
                block == Blocks.WHITE_WOOL ||
                block == Blocks.CALCITE;
    }

    /**
     * Find entire vein of mithril
     */
    private void findVein(BlockPos start) {
        vein.clear();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start);

        Block targetType = mc.world.getBlockState(start).getBlock();

        // BFS to find connected mithril blocks
        while (!queue.isEmpty() && vein.size() < 100) {
            BlockPos current = queue.poll();
            vein.add(current);

            // Check 6 adjacent blocks
            for (BlockPos neighbor : getAdjacentBlocks(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);

                    Block block = mc.world.getBlockState(neighbor).getBlock();
                    if (block == targetType) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        // Sort vein by distance from player for efficiency
        BlockPos playerPos = mc.player.getBlockPos();
        vein.sort(Comparator.comparingDouble(
                pos -> pos.getSquaredDistance(playerPos)
        ));

        System.out.println("[Mithril] Found vein with " + vein.size() + " blocks");
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
     * Mine mithril block
     */
    private void mineMithril() {
        if (targetBlock == null) {
            state = MiningState.SCANNING;
            return;
        }

        // Check if block still exists
        Block block = mc.world.getBlockState(targetBlock).getBlock();
        if (!isMithril(block) && !isTitanium(block)) {
            // Block broken, track stats
            if (isTitanium(block)) {
                titaniumMined++;
            } else {
                mithrilMined++;
            }

            // Move to next block in vein
            if (veinMine && veinIndex < vein.size() - 1) {
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

        // Start breaking
        breakBlock(targetBlock);
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
        // For now, placeholder
        System.out.println("[Mithril] Breaking block at: " + pos);
    }

    /**
     * Sell items at NPC
     */
    private void sellItems() {
        System.out.println("[Mithril] Selling items...");

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

        return emptySlots < 3; // Consider full if less than 3 empty slots
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
        return String.format("%s | Mithril: %d | Titanium: %d | Powder: %d",
                state, mithrilMined, titaniumMined, powderCollected);
    }

    /**
     * Get statistics
     */
    public String getStats() {
        long runtime = getRuntime() / 1000;
        long blocksPerHour = runtime > 0 ? (mithrilMined + titaniumMined) * 3600 / runtime : 0;

        return String.format(
                "Mithril: %d | Titanium: %d | Gemstones: %d | Powder: %d\n" +
                        "Blocks/hr: %d | Runtime: %s",
                mithrilMined, titaniumMined, gemstoneMined, powderCollected,
                blocksPerHour, getRuntimeFormatted()
        );
    }

    // ==================== GETTERS/SETTERS ====================

    public void setVeinMine(boolean vein) {
        this.veinMine = vein;
    }

    public void setTitaniumPriority(boolean priority) {
        this.titaniumPriority = priority;
    }

    public void setAutoSell(boolean auto) {
        this.autoSell = auto;
    }

    public void setPowderGrind(boolean grind) {
        this.powderGrind = grind;
    }

    public void setScanRadius(int radius) {
        this.scanRadius = radius;
    }

    public void setBreakRange(double range) {
        this.breakRange = range;
    }

    public void setPattern(MiningPattern pattern) {
        this.pattern = pattern;
    }
}
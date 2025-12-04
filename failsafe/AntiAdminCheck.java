package com.donut.client.failsafe;

import com.donut.client.macros.MacroManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.MinecraftClient;

import java.util.*;

public class AntiAdminCheck {

    private static AntiAdminCheck instance;
    private final MinecraftClient mc;

    // Detection settings
    private boolean enabled = true;
    private boolean checkBedrock = true;
    private boolean checkDirt = true;
    private boolean checkBarriers = true;
    private boolean checkStructure = true;
    private boolean checkTeleport = true;

    // Bedrock detection
    private Set<BlockPos> knownBedrock = new HashSet<>();
    private long lastBedrockCheck = 0;
    private long bedrockCheckInterval = 5000; // 5 seconds

    // Dirt/Suspicious block detection
    private Set<BlockPos> suspiciousBlocks = new HashSet<>();
    private Map<Block, Integer> blockHistory = new HashMap<>();

    // Teleport detection
    private BlockPos lastPosition = null;
    private long lastTeleportCheck = 0;
    private int teleportThreshold = 10; // blocks

    // Structure detection (admin test areas)
    private boolean inSuspiciousArea = false;
    private long suspiciousAreaTime = 0;

    // State
    private boolean adminCheckDetected = false;
    private String detectionReason = "";
    private long detectionTime = 0;

    private AntiAdminCheck() {
        this.mc = MinecraftClient.getInstance();
    }

    public static AntiAdminCheck getInstance() {
        if (instance == null) {
            instance = new AntiAdminCheck();
        }
        return instance;
    }

    /**
     * Main detection tick
     */
    public void tick() {
        if (!enabled || mc.player == null || mc.world == null) {
            return;
        }

        // Check for bedrock boxes
        if (checkBedrock) {
            detectBedrockBox();
        }

        // Check for suspicious dirt/blocks
        if (checkDirt) {
            detectSuspiciousBlocks();
        }

        // Check for barrier blocks
        if (checkBarriers) {
            detectBarriers();
        }

        // Check for suspicious structures
        if (checkStructure) {
            detectSuspiciousStructure();
        }

        // Check for teleportation
        if (checkTeleport) {
            detectTeleportation();
        }
    }

    /**
     * Detect bedrock box (admin trap)
     */
    private void detectBedrockBox() {
        long now = System.currentTimeMillis();

        if (now - lastBedrockCheck < bedrockCheckInterval) {
            return;
        }
        lastBedrockCheck = now;

        BlockPos playerPos = mc.player.getBlockPos();
        int bedrockCount = 0;
        List<BlockPos> newBedrock = new ArrayList<>();

        // Check surrounding blocks for bedrock
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    // Skip center (player position)
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (block == Blocks.BEDROCK) {
                        bedrockCount++;

                        // Check if this is NEW bedrock (not in known list)
                        if (!knownBedrock.contains(pos)) {
                            newBedrock.add(pos);
                        }
                    }
                }
            }
        }

        // Update known bedrock
        knownBedrock.clear();
        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK) {
                        knownBedrock.add(pos);
                    }
                }
            }
        }

        // Detect bedrock box
        if (newBedrock.size() > 10) {
            // Many new bedrock blocks appeared
            System.out.println("⚠ BEDROCK BOX DETECTED ⚠");
            System.out.println("New bedrock blocks: " + newBedrock.size());
            triggerAdminCheck("Bedrock box detected (" + newBedrock.size() + " new blocks)");
        }

        // Check if completely surrounded
        if (bedrockCount > 20) {
            System.out.println("⚠ ENCLOSED IN BEDROCK ⚠");
            triggerAdminCheck("Enclosed in bedrock (" + bedrockCount + " blocks)");
        }
    }

    /**
     * Detect suspicious blocks (dirt in unnatural places)
     */
    private void detectSuspiciousBlocks() {
        BlockPos playerPos = mc.player.getBlockPos();
        int suspiciousCount = 0;

        // Check for dirt/suspicious blocks in unusual places
        for (int x = -5; x <= 5; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    // Check for dirt/grass in unusual locations
                    if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
                        // Check if in unnatural location (e.g., mid-air, in caves)
                        if (isUnnatural(pos, block)) {
                            suspiciousCount++;
                            suspiciousBlocks.add(pos);
                        }
                    }

                    // Check for other admin test blocks
                    if (block == Blocks.STONE_BRICKS ||
                            block == Blocks.QUARTZ_BLOCK ||
                            block == Blocks.SEA_LANTERN) {

                        if (isUnnatural(pos, block)) {
                            suspiciousCount++;
                        }
                    }
                }
            }
        }

        if (suspiciousCount > 5) {
            System.out.println("⚠ SUSPICIOUS BLOCKS DETECTED ⚠");
            System.out.println("Count: " + suspiciousCount);
            triggerAdminCheck("Suspicious blocks detected (" + suspiciousCount + " blocks)");
        }
    }

    /**
     * Check if block placement is unnatural
     */
    private boolean isUnnatural(BlockPos pos, Block block) {
        // Check if block is floating (no support)
        BlockPos below = pos.down();
        Block blockBelow = mc.world.getBlockState(below).getBlock();

        if (blockBelow == Blocks.AIR) {
            return true; // Floating block
        }

        // Check if surrounded by air (isolated block)
        int airCount = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos neighbor = pos.add(dx, 0, dz);
                if (mc.world.getBlockState(neighbor).getBlock() == Blocks.AIR) {
                    airCount++;
                }
            }
        }

        if (airCount > 6) {
            return true; // Mostly isolated
        }

        // Check if block appeared recently in an unusual pattern
        // (admins often place test blocks in specific patterns)

        return false;
    }

    /**
     * Detect barrier blocks (invisible admin blocks)
     */
    private void detectBarriers() {
        BlockPos playerPos = mc.player.getBlockPos();

        // Check for barrier blocks
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (block == Blocks.BARRIER) {
                        System.out.println("⚠ BARRIER BLOCK DETECTED ⚠");
                        System.out.println("Position: " + pos);
                        triggerAdminCheck("Barrier block detected at " + pos);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Detect suspicious structures (admin test areas)
     */
    private void detectSuspiciousStructure() {
        BlockPos playerPos = mc.player.getBlockPos();

        // Check for perfect cubes/rooms (admin test chambers)
        if (isInPerfectRoom(playerPos)) {
            long now = System.currentTimeMillis();

            if (!inSuspiciousArea) {
                inSuspiciousArea = true;
                suspiciousAreaTime = now;
            }

            // If in suspicious area for more than 5 seconds
            if (now - suspiciousAreaTime > 5000) {
                System.out.println("⚠ SUSPICIOUS STRUCTURE DETECTED ⚠");
                triggerAdminCheck("Trapped in suspicious structure");
            }
        } else {
            inSuspiciousArea = false;
        }
    }

    /**
     * Check if player is in a perfect room (suspicious)
     */
    private boolean isInPerfectRoom(BlockPos pos) {
        // Check if surrounded by same block type in perfect cube
        Block wallBlock = null;
        int wallCount = 0;

        // Check walls
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    // Only check outer layer
                    if (Math.abs(dx) != 3 && Math.abs(dy) != 3 && Math.abs(dz) != 3) {
                        continue;
                    }

                    BlockPos checkPos = pos.add(dx, dy, dz);
                    Block block = mc.world.getBlockState(checkPos).getBlock();

                    if (block != Blocks.AIR) {
                        if (wallBlock == null) {
                            wallBlock = block;
                        }

                        if (block == wallBlock) {
                            wallCount++;
                        }
                    }
                }
            }
        }

        // If most walls are same block type, it's suspicious
        return wallCount > 100;
    }

    /**
     * Detect teleportation (admin moved you)
     */
    private void detectTeleportation() {
        if (mc.player == null) return;

        BlockPos currentPos = mc.player.getBlockPos();

        if (lastPosition != null) {
            double distance = Math.sqrt(currentPos.getSquaredDistance(lastPosition));

            // If moved more than threshold instantly
            if (distance > teleportThreshold && mc.player.getVelocity().length() < 1.0) {
                System.out.println("⚠ TELEPORTATION DETECTED ⚠");
                System.out.println("Distance: " + String.format("%.1f", distance) + " blocks");
                triggerAdminCheck("Teleported " + String.format("%.1f", distance) + " blocks");
            }
        }

        lastPosition = currentPos;
    }

    /**
     * Trigger admin check response
     */
    private void triggerAdminCheck(String reason) {
        if (adminCheckDetected) {
            return; // Already triggered
        }

        adminCheckDetected = true;
        detectionReason = reason;
        detectionTime = System.currentTimeMillis();

        System.out.println("========================================");
        System.out.println("⚠ ADMIN CHECK DETECTED ⚠");
        System.out.println("Reason: " + reason);
        System.out.println("Position: " + mc.player.getBlockPos());
        System.out.println("========================================");

        // Stop all macros immediately
        MacroManager manager = MacroManager.getInstance();
        manager.disableAll();

        // Execute response
        respondToAdminCheck();
    }

    /**
     * Respond to admin check
     */
    private void respondToAdminCheck() {
        System.out.println("[Admin Check] Executing emergency response...");

        // Option 1: Disconnect immediately
        disconnectSafely();

        // Option 2: Act confused (pretend you don't know what's happening)
        // TODO: Look around, type in chat "what happened?", etc.

        // Option 3: Stand still and do nothing
        // TODO: Stop all movement
    }

    /**
     * Disconnect from server safely
     */
    private void disconnectSafely() {
        System.out.println("[Admin Check] Disconnecting for safety...");

        // TODO: Send disconnect packet
        // TODO: Add delay to seem natural?
    }

    /**
     * Check if currently in admin check
     */
    public boolean isInAdminCheck() {
        return adminCheckDetected;
    }

    /**
     * Get detection reason
     */
    public String getDetectionReason() {
        return detectionReason;
    }

    /**
     * Reset detection state
     */
    public void reset() {
        adminCheckDetected = false;
        detectionReason = "";
        suspiciousBlocks.clear();
        inSuspiciousArea = false;
    }

    /**
     * Get detection report
     */
    public String getReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Admin Check Detection Report ===\n");
        report.append("Status: ").append(adminCheckDetected ? "DETECTED" : "Clear").append("\n");

        if (adminCheckDetected) {
            report.append("Reason: ").append(detectionReason).append("\n");
            report.append("Time: ").append(new Date(detectionTime)).append("\n");
        }

        report.append("\nKnown Bedrock: ").append(knownBedrock.size()).append(" blocks\n");
        report.append("Suspicious Blocks: ").append(suspiciousBlocks.size()).append(" blocks\n");
        report.append("In Suspicious Area: ").append(inSuspiciousArea).append("\n");

        return report.toString();
    }

    // Settings
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setCheckBedrock(boolean check) {
        this.checkBedrock = check;
    }

    public void setCheckDirt(boolean check) {
        this.checkDirt = check;
    }

    public void setCheckBarriers(boolean check) {
        this.checkBarriers = check;
    }

    public void setCheckStructure(boolean check) {
        this.checkStructure = check;
    }

    public void setCheckTeleport(boolean check) {
        this.checkTeleport = check;
    }

    public void setTeleportThreshold(int threshold) {
        this.teleportThreshold = threshold;
    }
}
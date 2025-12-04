package com.donut.client.macros.events;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * DianaEventMacro - Automated Diana mythological event
 * Features: Griffin burrow detection, inquisitor detection, automated digging
 */
public class DianaEventMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private MacroState state = MacroState.IDLE;
    private BurrowType currentBurrowType = null;
    private BlockPos targetBurrow = null;

    // Burrow detection
    private List<Burrow> detectedBurrows = new ArrayList<>();
    private BlockPos lastBurrow = null;

    // Settings
    private boolean autoDig = true;
    private boolean avoidLava = true;
    private boolean prioritizeInquisitors = true;
    private boolean useWarp = true;
    private int maxBurrows = 100;
    private double scanRadius = 50.0;

    // Inquisitor detection
    private boolean inquisitorNearby = false;
    private Entity inquisitorEntity = null;

    // Statistics
    private int burrowsDug = 0;
    private int mobsKilled = 0;
    private int inquisiorsFound = 0;
    private int coinsEarned = 0;
    private Map<String, Integer> lootCounts = new HashMap<>();

    public enum MacroState {
        IDLE,               // Not active
        SCANNING,           // Scanning for burrows
        NAVIGATING,         // Moving to burrow
        DIGGING,            // Digging burrow
        FIGHTING,           // Fighting mob
        LOOTING,            // Collecting loot
        COMPLETE            // Done
    }

    public enum BurrowType {
        START,              // Green particles - start of chain
        MOB,                // Red particles - mob spawn
        TREASURE,           // Gold particles - treasure chest
        INQUISITOR          // Special - rare spawn
    }

    public DianaEventMacro() {
        super("Diana Event Macro", "Automated Diana mythological event");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = MacroState.SCANNING;
        detectedBurrows.clear();
        targetBurrow = null;
        System.out.println("[Diana] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Diana] Starting...");
        System.out.println("[Diana] Max burrows: " + maxBurrows);
        System.out.println("[Diana] Scan radius: " + scanRadius);

        state = MacroState.SCANNING;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Diana] Stopped");
        printStatistics();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Always check for inquisitors
        if (prioritizeInquisitors) {
            detectInquisitor();
        }

        switch (state) {
            case IDLE:
                // Do nothing
                break;
            case SCANNING:
                scanForBurrows();
                break;
            case NAVIGATING:
                navigateToBurrow();
                break;
            case DIGGING:
                digBurrow();
                break;
            case FIGHTING:
                fightMob();
                break;
            case LOOTING:
                collectLoot();
                break;
            case COMPLETE:
                complete();
                break;
        }
    }

    /**
     * Scan for griffin burrows
     */
    private void scanForBurrows() {
        if (mc.world == null) return;

        detectedBurrows.clear();

        // Detect burrows by particles
        // TODO: Scan for particle effects
        // Green particles = start burrow
        // Red particles = mob burrow
        // Gold particles = treasure burrow

        // Detect burrows by sound
        // TODO: Listen for burrow sounds

        // Use Ancestral Spade ability
        if (mc.player != null && mc.options != null) {
            // Right-click with spade
            // Shows directional waypoint to nearest burrow
            // TODO: Detect waypoint and calculate position
        }

        // Simulated burrow detection
        if (detectedBurrows.isEmpty() && Math.random() < 0.1) {
            // Generate random burrow nearby
            BlockPos playerPos = mc.player.getBlockPos();
            int x = playerPos.getX() + (int)(Math.random() * 40 - 20);
            int z = playerPos.getZ() + (int)(Math.random() * 40 - 20);
            int y = playerPos.getY();

            BlockPos burrowPos = new BlockPos(x, y, z);
            BurrowType type = BurrowType.values()[(int)(Math.random() * 3)];

            detectedBurrows.add(new Burrow(burrowPos, type));
        }

        // Select nearest burrow
        if (!detectedBurrows.isEmpty()) {
            targetBurrow = findNearestBurrow();
            currentBurrowType = getBurrowType(targetBurrow);

            System.out.println("[Diana] Burrow detected: " + currentBurrowType +
                    " at " + targetBurrow);

            state = MacroState.NAVIGATING;
        }
    }

    /**
     * Find nearest burrow
     */
    private BlockPos findNearestBurrow() {
        if (detectedBurrows.isEmpty()) return null;

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Burrow burrow : detectedBurrows) {
            double dist = mc.player.getBlockPos().getSquaredDistance(burrow.position);

            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = burrow.position;
            }
        }

        return nearest;
    }

    /**
     * Get burrow type at position
     */
    private BurrowType getBurrowType(BlockPos pos) {
        for (Burrow burrow : detectedBurrows) {
            if (burrow.position.equals(pos)) {
                return burrow.type;
            }
        }
        return BurrowType.START;
    }

    /**
     * Navigate to burrow
     */
    private void navigateToBurrow() {
        if (targetBurrow == null) {
            state = MacroState.SCANNING;
            return;
        }

        Vec3d targetPos = Vec3d.ofCenter(targetBurrow);
        Vec3d playerPos = mc.player.getPos();

        double distance = playerPos.distanceTo(targetPos);

        // Check if arrived
        if (distance < 2.0) {
            System.out.println("[Diana] Arrived at burrow");
            state = MacroState.DIGGING;
            return;
        }

        // Calculate direction
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double dy = targetPos.y - playerPos.y;

        // Set yaw
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        mc.player.setYaw(yaw);

        // Set pitch
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));
        mc.player.setPitch(pitch);

        // Move forward
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(true);

            // Jump if blocked
            if (mc.player.horizontalCollision || dy > 1.0) {
                mc.options.jumpKey.setPressed(true);
            } else {
                mc.options.jumpKey.setPressed(false);
            }
        }
    }

    /**
     * Dig burrow
     */
    private void digBurrow() {
        if (!autoDig) {
            state = MacroState.SCANNING;
            return;
        }

        System.out.println("[Diana] Digging burrow...");

        // Right-click on burrow with Ancestral Spade
        // TODO: Detect burrow block
        // TODO: Right-click to dig

        // Simulated digging
        burrowsDug++;
        lastBurrow = targetBurrow;

        // Check burrow type
        if (currentBurrowType == BurrowType.MOB) {
            System.out.println("[Diana] Mob spawned!");
            state = MacroState.FIGHTING;
        } else if (currentBurrowType == BurrowType.TREASURE) {
            System.out.println("[Diana] Treasure found!");
            state = MacroState.LOOTING;
        } else {
            // Start burrow - leads to next burrow
            System.out.println("[Diana] Start burrow - scanning for next");
            targetBurrow = null;
            state = MacroState.SCANNING;
        }

        // Check if reached max
        if (burrowsDug >= maxBurrows) {
            state = MacroState.COMPLETE;
        }
    }

    /**
     * Fight mob from burrow
     */
    private void fightMob() {
        System.out.println("[Diana] Fighting mob...");

        // TODO: Detect mob
        // TODO: Attack mob
        // TODO: Wait for mob to die

        // Simulated combat
        if (Math.random() < 0.1) {
            mobsKilled++;
            System.out.println("[Diana] Mob killed!");
            state = MacroState.LOOTING;
        }
    }

    /**
     * Collect loot
     */
    private void collectLoot() {
        System.out.println("[Diana] Collecting loot...");

        // TODO: Pick up items
        // TODO: Open chests

        // Simulated loot
        String[] loot = {"Enchanted Book", "Minos Relic", "Daedalus Stick", "Coins"};
        String item = loot[(int)(Math.random() * loot.length)];

        lootCounts.put(item, lootCounts.getOrDefault(item, 0) + 1);

        if (item.equals("Coins")) {
            int coins = 1000 + (int)(Math.random() * 5000);
            coinsEarned += coins;
        }

        System.out.println("[Diana] Found: " + item);

        // Reset and scan for next burrow
        targetBurrow = null;
        state = MacroState.SCANNING;
    }

    /**
     * Detect inquisitor
     */
    private void detectInquisitor() {
        if (mc.world == null) return;

        // Inquisitors are rare mobs that spawn during Diana event
        // They have specific names and drop valuable loot

        for (Entity entity : mc.world.getEntities()) {
            String name = entity.getName().getString();

            // Check for Minos Inquisitor
            if (name.contains("Inquisitor") || name.contains("Minos")) {
                if (!inquisitorNearby) {
                    inquisitorNearby = true;
                    inquisitorEntity = entity;
                    inquisiorsFound++;

                    System.out.println("========================================");
                    System.out.println("⚠️ INQUISITOR DETECTED! ⚠️");
                    System.out.println("Position: " + entity.getBlockPos());
                    System.out.println("========================================");

                    // Alert in chat
                    if (mc.player != null) {
                        // TODO: Send chat message to alert party
                    }
                }
                return;
            }
        }

        inquisitorNearby = false;
        inquisitorEntity = null;
    }

    /**
     * Complete macro
     */
    private void complete() {
        System.out.println("========================================");
        System.out.println("🏛️ DIANA EVENT COMPLETE 🏛️");
        System.out.println("Burrows Dug: " + burrowsDug);
        System.out.println("Mobs Killed: " + mobsKilled);
        System.out.println("Inquisitors: " + inquisiorsFound);
        System.out.println("========================================");

        onDisable();
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("DIANA EVENT STATISTICS");
        System.out.println("========================================");
        System.out.println("Burrows Dug: " + burrowsDug);
        System.out.println("Mobs Killed: " + mobsKilled);
        System.out.println("Inquisitors Found: " + inquisiorsFound);
        System.out.println("Coins Earned: " + coinsEarned);
        System.out.println();

        if (!lootCounts.isEmpty()) {
            System.out.println("Loot Collected:");
            for (Map.Entry<String, Integer> entry : lootCounts.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
        }

        System.out.println("\nRuntime: " + getRuntimeFormatted());

        long seconds = getRuntime() / 1000;
        if (seconds > 0) {
            int burrowsPerHour = (int)(burrowsDug * 3600L / seconds);
            System.out.println("Rate: " + burrowsPerHour + " burrows/hour");
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | Burrows: %d/%d | Mobs: %d | Inquisitors: %d",
                state, burrowsDug, maxBurrows, mobsKilled, inquisiorsFound);
    }

    // ==================== DATA CLASSES ====================

    public static class Burrow {
        public BlockPos position;
        public BurrowType type;
        public long detectedTime;

        public Burrow(BlockPos position, BurrowType type) {
            this.position = position;
            this.type = type;
            this.detectedTime = System.currentTimeMillis();
        }
    }

    // ==================== GETTERS/SETTERS ====================

    public void setAutoDig(boolean auto) {
        this.autoDig = auto;
    }

    public void setAvoidLava(boolean avoid) {
        this.avoidLava = avoid;
    }

    public void setPrioritizeInquisitors(boolean prioritize) {
        this.prioritizeInquisitors = prioritize;
    }

    public void setUseWarp(boolean warp) {
        this.useWarp = warp;
    }

    public void setMaxBurrows(int max) {
        this.maxBurrows = max;
    }

    public void setScanRadius(double radius) {
        this.scanRadius = radius;
    }

    public boolean isInquisitorNearby() {
        return inquisitorNearby;
    }
}
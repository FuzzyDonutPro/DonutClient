package com.donut.client.macros.hunting;

import net.minecraft.client.MinecraftClient;

import java.util.*;

/**
 * BaitManager - Manages hunting bait inventory and usage
 * Tracks bait types and auto-refills
 */
public class BaitManager {

    private final MinecraftClient mc;

    // Bait inventory
    private Map<BaitType, Integer> baitCounts = new HashMap<>();
    private BaitType currentBait = BaitType.CARROT;

    // Settings
    private boolean autoRefill = true;
    private int refillThreshold = 64; // Refill when below this amount

    // Statistics
    private int baitUsed = 0;
    private Map<BaitType, Integer> baitUsageStats = new HashMap<>();

    public enum BaitType {
        CARROT("Carrot", AnimalTracker.AnimalType.PIG, AnimalTracker.AnimalType.RABBIT),
        WHEAT("Wheat", AnimalTracker.AnimalType.COW, AnimalTracker.AnimalType.SHEEP),
        SEEDS("Seeds", AnimalTracker.AnimalType.CHICKEN),
        SUGAR("Sugar", AnimalTracker.AnimalType.HORSE),
        GOLDEN_CARROT("Golden Carrot", AnimalTracker.AnimalType.HORSE),
        HAY_BALE("Hay Bale", AnimalTracker.AnimalType.HORSE, AnimalTracker.AnimalType.DONKEY);

        public final String name;
        public final AnimalTracker.AnimalType[] attractedAnimals;

        BaitType(String name, AnimalTracker.AnimalType... attracted) {
            this.name = name;
            this.attractedAnimals = attracted;
        }
    }

    public BaitManager() {
        this.mc = MinecraftClient.getInstance();

        // Initialize counts
        for (BaitType type : BaitType.values()) {
            baitCounts.put(type, 0);
            baitUsageStats.put(type, 0);
        }
    }

    /**
     * Update bait counts from inventory
     */
    public void updateInventory() {
        if (mc.player == null) return;

        // Reset counts
        for (BaitType type : BaitType.values()) {
            baitCounts.put(type, 0);
        }

        // Count bait in inventory
        // TODO: Scan inventory for bait items

        System.out.println("[Bait Manager] Inventory updated");
    }

    /**
     * Use bait
     */
    public boolean useBait(BaitType type) {
        int count = baitCounts.get(type);

        if (count <= 0) {
            System.out.println("[Bait Manager] Out of " + type.name + "!");
            return false;
        }

        // Use bait
        baitCounts.put(type, count - 1);
        baitUsed++;
        baitUsageStats.put(type, baitUsageStats.get(type) + 1);

        System.out.println("[Bait Manager] Used " + type.name + " (" + (count - 1) + " remaining)");

        // Check if refill needed
        if (autoRefill && count - 1 < refillThreshold) {
            refillBait(type);
        }

        return true;
    }

    /**
     * Refill bait from storage
     */
    private void refillBait(BaitType type) {
        System.out.println("[Bait Manager] Refilling " + type.name + "...");

        // TODO: Get bait from chest/storage
    }

    /**
     * Get best bait for animal type
     */
    public BaitType getBestBait(AnimalTracker.AnimalType animalType) {
        // Find bait that attracts this animal
        for (BaitType bait : BaitType.values()) {
            for (AnimalTracker.AnimalType attracted : bait.attractedAnimals) {
                if (attracted == animalType) {
                    // Check if we have this bait
                    if (baitCounts.get(bait) > 0) {
                        return bait;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get bait count
     */
    public int getBaitCount(BaitType type) {
        return baitCounts.get(type);
    }

    /**
     * Check if has bait
     */
    public boolean hasBait(BaitType type) {
        return baitCounts.get(type) > 0;
    }

    /**
     * Get total bait count
     */
    public int getTotalBait() {
        int total = 0;
        for (int count : baitCounts.values()) {
            total += count;
        }
        return total;
    }

    /**
     * Get bait usage stats
     */
    public Map<BaitType, Integer> getUsageStats() {
        return new HashMap<>(baitUsageStats);
    }

    /**
     * Get status
     */
    public String getStatusInfo() {
        return String.format("Current: %s (%d) | Total Used: %d",
                currentBait.name, baitCounts.get(currentBait), baitUsed);
    }

    // Getters/Setters
    public void setCurrentBait(BaitType type) {
        this.currentBait = type;
    }

    public BaitType getCurrentBait() {
        return currentBait;
    }

    public void setAutoRefill(boolean auto) {
        this.autoRefill = auto;
    }

    public void setRefillThreshold(int threshold) {
        this.refillThreshold = threshold;
    }
}
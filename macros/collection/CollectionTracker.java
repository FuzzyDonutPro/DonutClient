package com.donut.client.macros.collection;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;

import java.util.*;

/**
 * CollectionTracker - Tracks collection progress and goals
 * Features: Real-time tracking, goals, notifications, stats per hour
 */
public class CollectionTracker extends Macro {

    private final MinecraftClient mc;

    // Collection data
    private Map<String, CollectionData> collections = new HashMap<>();

    // Settings
    private boolean showNotifications = true;
    private boolean trackRates = true;
    private boolean autoSync = true; // Sync with Hypixel API

    // Goals
    private Map<String, Integer> collectionGoals = new HashMap<>();

    public CollectionTracker() {
        super("Collection Tracker", "Track collection progress and goals");
        this.mc = MinecraftClient.getInstance();
        initializeCollections();
    }

    @Override
    public void start() {
        System.out.println("[Collection Tracker] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Collection Tracker] Enabled");
        System.out.println("[Collection Tracker] Tracking " + collections.size() + " collections");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Collection Tracker] Disabled");
        printSummary();
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Update collection rates
        if (trackRates) {
            updateRates();
        }

        // Check for collection milestones
        if (showNotifications) {
            checkMilestones();
        }
    }

    /**
     * Initialize all Hypixel collections
     */
    private void initializeCollections() {
        // Farming collections
        addCollection("Wheat", CollectionCategory.FARMING);
        addCollection("Carrot", CollectionCategory.FARMING);
        addCollection("Potato", CollectionCategory.FARMING);
        addCollection("Pumpkin", CollectionCategory.FARMING);
        addCollection("Melon", CollectionCategory.FARMING);
        addCollection("Sugar Cane", CollectionCategory.FARMING);
        addCollection("Cocoa Beans", CollectionCategory.FARMING);
        addCollection("Cactus", CollectionCategory.FARMING);
        addCollection("Mushroom", CollectionCategory.FARMING);
        addCollection("Nether Wart", CollectionCategory.FARMING);

        // Mining collections
        addCollection("Cobblestone", CollectionCategory.MINING);
        addCollection("Coal", CollectionCategory.MINING);
        addCollection("Iron", CollectionCategory.MINING);
        addCollection("Gold", CollectionCategory.MINING);
        addCollection("Diamond", CollectionCategory.MINING);
        addCollection("Lapis", CollectionCategory.MINING);
        addCollection("Emerald", CollectionCategory.MINING);
        addCollection("Redstone", CollectionCategory.MINING);
        addCollection("Quartz", CollectionCategory.MINING);
        addCollection("Obsidian", CollectionCategory.MINING);
        addCollection("Glowstone", CollectionCategory.MINING);
        addCollection("Gravel", CollectionCategory.MINING);
        addCollection("Ice", CollectionCategory.MINING);
        addCollection("Netherrack", CollectionCategory.MINING);
        addCollection("Sand", CollectionCategory.MINING);
        addCollection("End Stone", CollectionCategory.MINING);
        addCollection("Mithril", CollectionCategory.MINING);
        addCollection("Hard Stone", CollectionCategory.MINING);
        addCollection("Gemstone", CollectionCategory.MINING);

        // Combat collections
        addCollection("Rotten Flesh", CollectionCategory.COMBAT);
        addCollection("Bone", CollectionCategory.COMBAT);
        addCollection("String", CollectionCategory.COMBAT);
        addCollection("Spider Eye", CollectionCategory.COMBAT);
        addCollection("Gunpowder", CollectionCategory.COMBAT);
        addCollection("Ender Pearl", CollectionCategory.COMBAT);
        addCollection("Ghast Tear", CollectionCategory.COMBAT);
        addCollection("Slimeball", CollectionCategory.COMBAT);
        addCollection("Blaze Rod", CollectionCategory.COMBAT);
        addCollection("Magma Cream", CollectionCategory.COMBAT);

        // Foraging collections
        addCollection("Oak Wood", CollectionCategory.FORAGING);
        addCollection("Spruce Wood", CollectionCategory.FORAGING);
        addCollection("Birch Wood", CollectionCategory.FORAGING);
        addCollection("Dark Oak Wood", CollectionCategory.FORAGING);
        addCollection("Acacia Wood", CollectionCategory.FORAGING);
        addCollection("Jungle Wood", CollectionCategory.FORAGING);

        // Fishing collections
        addCollection("Raw Fish", CollectionCategory.FISHING);
        addCollection("Raw Salmon", CollectionCategory.FISHING);
        addCollection("Clownfish", CollectionCategory.FISHING);
        addCollection("Pufferfish", CollectionCategory.FISHING);
        addCollection("Prismarine Shard", CollectionCategory.FISHING);
        addCollection("Prismarine Crystals", CollectionCategory.FISHING);
        addCollection("Clay", CollectionCategory.FISHING);
        addCollection("Ink Sack", CollectionCategory.FISHING);
        addCollection("Lily Pad", CollectionCategory.FISHING);
        addCollection("Sponge", CollectionCategory.FISHING);
    }

    /**
     * Add collection to tracker
     */
    private void addCollection(String name, CollectionCategory category) {
        collections.put(name, new CollectionData(name, category));
    }

    /**
     * Update collection amount
     */
    public void updateCollection(String name, int amount) {
        CollectionData data = collections.get(name);
        if (data != null) {
            int previous = data.amount;
            data.amount = amount;
            data.lastUpdate = System.currentTimeMillis();

            int gained = amount - previous;
            if (gained > 0) {
                data.sessionGained += gained;

                if (showNotifications) {
                    System.out.println("[Collection] " + name + ": " + previous + " → " + amount + " (+" + gained + ")");
                }
            }
        }
    }

    /**
     * Increment collection by amount
     */
    public void incrementCollection(String name, int amount) {
        CollectionData data = collections.get(name);
        if (data != null) {
            data.amount += amount;
            data.sessionGained += amount;
            data.lastUpdate = System.currentTimeMillis();
        }
    }

    /**
     * Set collection goal
     */
    public void setGoal(String name, int goal) {
        collectionGoals.put(name, goal);
        System.out.println("[Collection] Goal set: " + name + " → " + goal);
    }

    /**
     * Update collection rates
     */
    private void updateRates() {
        long now = System.currentTimeMillis();

        for (CollectionData data : collections.values()) {
            if (data.lastUpdate > 0 && data.sessionGained > 0) {
                long elapsed = now - startTime;
                if (elapsed > 0) {
                    data.ratePerHour = (int) (data.sessionGained * 3600000L / elapsed);
                }
            }
        }
    }

    /**
     * Check for milestones
     */
    private void checkMilestones() {
        for (Map.Entry<String, Integer> entry : collectionGoals.entrySet()) {
            String name = entry.getKey();
            int goal = entry.getValue();

            CollectionData data = collections.get(name);
            if (data != null && data.amount >= goal && !data.goalReached) {
                data.goalReached = true;
                System.out.println("========================================");
                System.out.println("🎉 COLLECTION GOAL REACHED!");
                System.out.println(name + ": " + data.amount + " / " + goal);
                System.out.println("========================================");
            }
        }
    }

    /**
     * Get collection info
     */
    public CollectionData getCollection(String name) {
        return collections.get(name);
    }

    /**
     * Get all collections in category
     */
    public List<CollectionData> getCollectionsByCategory(CollectionCategory category) {
        List<CollectionData> result = new ArrayList<>();
        for (CollectionData data : collections.values()) {
            if (data.category == category) {
                result.add(data);
            }
        }
        return result;
    }

    /**
     * Print summary
     */
    private void printSummary() {
        System.out.println("========================================");
        System.out.println("COLLECTION TRACKER SUMMARY");
        System.out.println("========================================");

        for (CollectionCategory category : CollectionCategory.values()) {
            System.out.println("\n" + category + ":");

            List<CollectionData> categoryCollections = getCollectionsByCategory(category);
            for (CollectionData data : categoryCollections) {
                if (data.sessionGained > 0) {
                    System.out.println(String.format("  %s: +%d (%d/hr)",
                            data.name, data.sessionGained, data.ratePerHour));
                }
            }
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        int activeCollections = 0;
        for (CollectionData data : collections.values()) {
            if (data.sessionGained > 0) activeCollections++;
        }

        return String.format("TRACKING | Active: %d | Goals: %d",
                activeCollections, collectionGoals.size());
    }

    // ==================== DATA CLASSES ====================

    public enum CollectionCategory {
        FARMING,
        MINING,
        COMBAT,
        FORAGING,
        FISHING
    }

    public static class CollectionData {
        public String name;
        public CollectionCategory category;
        public int amount = 0;
        public int sessionGained = 0;
        public int ratePerHour = 0;
        public long lastUpdate = 0;
        public boolean goalReached = false;

        public CollectionData(String name, CollectionCategory category) {
            this.name = name;
            this.category = category;
        }
    }

    // ==================== GETTERS/SETTERS ====================

    public void setShowNotifications(boolean show) {
        this.showNotifications = show;
    }

    public void setTrackRates(boolean track) {
        this.trackRates = track;
    }

    public void setAutoSync(boolean sync) {
        this.autoSync = sync;
    }
}
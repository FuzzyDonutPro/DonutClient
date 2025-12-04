package com.donut.client.macros.fishing;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;

import java.util.*;

/**
 * LavaFishing - Lava fishing in Crimson Isle
 * Trophy fish, Magma Fish, special lava catches
 */
public class LavaFishing extends Macro {

    private final MinecraftClient mc;
    private final AutoFisher autoFisher;

    // State
    private LavaFishingState state = LavaFishingState.IDLE;
    private long lastTrophyTime = 0;

    // Settings
    private boolean autoKillMobs = true;
    private boolean autoReinforced = true; // Auto use reinforced rod
    private int trophyAlertDelay = 5000; // Alert if no trophy in 5s

    // Statistics
    private int trophyFishCaught = 0;
    private int magmaFishCaught = 0;
    private int flamingWormsCaught = 0;
    private Map<TrophyRarity, Integer> trophyRarities = new HashMap<>();

    public enum LavaFishingState {
        IDLE, FISHING, KILLING_MOB, COLLECTING
    }

    public enum TrophyRarity {
        BRONZE, SILVER, GOLD, DIAMOND
    }

    public LavaFishing() {
        super("Lava Fishing", "Trophy fishing in Crimson Isle");
        this.mc = MinecraftClient.getInstance();
        this.autoFisher = new AutoFisher();

        // Initialize trophy counters
        for (TrophyRarity rarity : TrophyRarity.values()) {
            trophyRarities.put(rarity, 0);
        }
    }

    @Override
    public void start() {
        state = LavaFishingState.IDLE;
        trophyFishCaught = 0;
        magmaFishCaught = 0;
        flamingWormsCaught = 0;
        System.out.println("[Lava Fishing] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // Check for lava rod
        if (!hasLavaRod()) {
            System.out.println("[Lava Fishing] ERROR: Need Lava Rod or Inferno Rod!");
            onDisable();
            return;
        }

        System.out.println("[Lava Fishing] Starting lava fishing...");
        state = LavaFishingState.FISHING;
        autoFisher.start();
        autoFisher.onEnable();
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        switch (state) {
            case IDLE:
                break;
            case FISHING:
                fish();
                break;
            case KILLING_MOB:
                killMob();
                break;
            case COLLECTING:
                collectLoot();
                break;
        }

        // Check for trophy drought
        checkTrophyDrought();
    }

    /**
     * Main fishing logic
     */
    private void fish() {
        // Run auto fisher
        autoFisher.onTick();

        // Check for hostile mobs
        if (autoKillMobs && hostileMobNearby()) {
            state = LavaFishingState.KILLING_MOB;
            autoFisher.onDisable();
        }

        // Check if caught something
        if (caughtSomething()) {
            analyzeLavaCatch();
        }
    }

    /**
     * Check if has lava fishing rod
     */
    private boolean hasLavaRod() {
        if (mc.player == null) return false;

        // Check main hand
        String itemName = mc.player.getMainHandStack().getName().getString();
        return itemName.contains("Lava") ||
                itemName.contains("Inferno") ||
                itemName.contains("Hellfire");
    }

    /**
     * Check for hostile mobs nearby
     */
    private boolean hostileMobNearby() {
        // TODO: Check for Lava Blaze, Magma Cube, etc.
        return false;
    }

    /**
     * Kill hostile mob
     */
    private void killMob() {
        // TODO: Attack nearest hostile mob
        System.out.println("[Lava Fishing] Killing mob...");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Resume fishing
        state = LavaFishingState.FISHING;
        autoFisher.onEnable();
    }

    /**
     * Check if caught something
     */
    private boolean caughtSomething() {
        // TODO: Check for new items in inventory
        return false;
    }

    /**
     * Analyze lava catch
     */
    private void analyzeLavaCatch() {
        // TODO: Check what was caught
        // Trophy fish, Magma fish, Flaming worms, etc.

        String caughtItem = getLastCaughtItem();

        if (isTrophyFish(caughtItem)) {
            TrophyRarity rarity = getTrophyRarity(caughtItem);
            trophyFishCaught++;
            trophyRarities.put(rarity, trophyRarities.get(rarity) + 1);
            lastTrophyTime = System.currentTimeMillis();

            System.out.println("[Lava Fishing] ★ TROPHY: " + caughtItem + " (" + rarity + ") ★");

            if (rarity == TrophyRarity.GOLD || rarity == TrophyRarity.DIAMOND) {
                System.out.println("[Lava Fishing] ★★★ RARE TROPHY! ★★★");
            }
        } else if (caughtItem.contains("Magma")) {
            magmaFishCaught++;
            System.out.println("[Lava Fishing] Magma Fish caught");
        } else if (caughtItem.contains("Flaming Worm")) {
            flamingWormsCaught++;
            System.out.println("[Lava Fishing] Flaming Worm caught");
        }
    }

    /**
     * Get last caught item
     */
    private String getLastCaughtItem() {
        // TODO: Get from inventory changes
        return "Trophy Fish";
    }

    /**
     * Check if trophy fish
     */
    private boolean isTrophyFish(String item) {
        return item.contains("Trophy") ||
                item.contains("Moldfin") ||
                item.contains("Slugfish") ||
                item.contains("Flyfish") ||
                item.contains("Obfuscated Fish");
    }

    /**
     * Get trophy rarity
     */
    private TrophyRarity getTrophyRarity(String item) {
        // TODO: Parse from item lore
        if (item.contains("DIAMOND")) return TrophyRarity.DIAMOND;
        if (item.contains("GOLD")) return TrophyRarity.GOLD;
        if (item.contains("SILVER")) return TrophyRarity.SILVER;
        return TrophyRarity.BRONZE;
    }

    /**
     * Check for trophy drought (no trophy in X time)
     */
    private void checkTrophyDrought() {
        if (lastTrophyTime == 0) return;

        long timeSinceTrophy = System.currentTimeMillis() - lastTrophyTime;

        if (timeSinceTrophy > trophyAlertDelay) {
            System.out.println("[Lava Fishing] WARNING: No trophy in " + (timeSinceTrophy / 1000) + "s");
        }
    }

    /**
     * Collect loot
     */
    private void collectLoot() {
        System.out.println("[Lava Fishing] Collecting loot...");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        state = LavaFishingState.FISHING;
    }

    /**
     * Get status info (not an override)
     */
    public String getStatusInfo() {
        return String.format("%s | Trophy: %d | Magma: %d | Gold: %d | Diamond: %d",
                state, trophyFishCaught, magmaFishCaught,
                trophyRarities.get(TrophyRarity.GOLD),
                trophyRarities.get(TrophyRarity.DIAMOND));
    }

    /**
     * Get trophy fish per hour
     */
    public double getTrophyPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) trophyFishCaught / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setAutoKillMobs(boolean auto) {
        this.autoKillMobs = auto;
    }

    public void setAutoReinforced(boolean auto) {
        this.autoReinforced = auto;
    }

    public void setTrophyAlertDelay(int delay) {
        this.trophyAlertDelay = delay;
    }

    public Map<TrophyRarity, Integer> getTrophyRarities() {
        return new HashMap<>(trophyRarities);
    }
}
package com.donut.client.macros.collection;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * FairySoulMacro - Automated fairy soul collection
 * Features: Route optimization, auto-navigation, progress tracking
 */
public class FairySoulMacro extends Macro {

    private final MinecraftClient mc;
    private final FairySoulFinder finder;

    // State
    private MacroState state = MacroState.SCANNING;
    private List<BlockPos> soulRoute = new ArrayList<>();
    private int routeIndex = 0;

    // Settings
    private String currentIsland = "Hub";
    private boolean useOptimalRoute = true;
    private boolean skipCollected = true;

    // Statistics
    private int soulsCollectedThisSession = 0;

    public enum MacroState {
        SCANNING,       // Scanning for souls
        NAVIGATING,     // Moving to next soul
        COLLECTING,     // At soul location
        COMPLETE        // All souls collected
    }

    public FairySoulMacro() {
        super("Fairy Soul Macro", "Automated fairy soul collection");
        this.mc = MinecraftClient.getInstance();
        this.finder = new FairySoulFinder();
    }

    @Override
    public void start() {
        state = MacroState.SCANNING;
        soulRoute.clear();
        routeIndex = 0;
        System.out.println("[Fairy Soul Macro] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Fairy Soul Macro] Starting...");
        System.out.println("[Fairy Soul Macro] Island: " + currentIsland);

        // Enable finder
        finder.onEnable();

        // Load soul locations for current island
        loadSoulLocations();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Fairy Soul Macro] Stopped");

        // Disable finder
        finder.onDisable();

        printSummary();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Tick finder
        finder.onTick();

        switch (state) {
            case SCANNING:
                scanForSouls();
                break;
            case NAVIGATING:
                navigateToNextSoul();
                break;
            case COLLECTING:
                collectSoul();
                break;
            case COMPLETE:
                complete();
                break;
        }
    }

    /**
     * Load fairy soul locations for current island
     */
    private void loadSoulLocations() {
        soulRoute.clear();

        // TODO: Load known soul locations from file/database
        // For now, use finder to detect souls dynamically

        System.out.println("[Fairy Soul Macro] Loaded " + soulRoute.size() + " soul locations");

        if (!soulRoute.isEmpty()) {
            state = MacroState.NAVIGATING;
        }
    }

    /**
     * Scan for souls using finder
     */
    private void scanForSouls() {
        // Check if finder found a soul
        BlockPos nearestSoul = finder.getNearestSoulPosition();

        if (nearestSoul != null) {
            soulRoute.add(nearestSoul);
            state = MacroState.NAVIGATING;
            routeIndex = 0;
        }
    }

    /**
     * Navigate to next soul in route
     */
    private void navigateToNextSoul() {
        if (routeIndex >= soulRoute.size()) {
            state = MacroState.COMPLETE;
            return;
        }

        BlockPos targetSoul = soulRoute.get(routeIndex);
        double distance = mc.player.getBlockPos().getSquaredDistance(targetSoul);

        // Check if arrived
        if (distance < 9) { // Within 3 blocks
            state = MacroState.COLLECTING;
            return;
        }

        // Use finder's navigation
        finder.setAutoNavigate(true);
    }

    /**
     * Collect soul
     */
    private void collectSoul() {
        // Wait for collection
        // TODO: Detect collection completion

        // Move to next soul
        routeIndex++;
        soulsCollectedThisSession++;

        System.out.println("[Fairy Soul Macro] Soul collected! (" + soulsCollectedThisSession + "/" + soulRoute.size() + ")");

        if (routeIndex < soulRoute.size()) {
            state = MacroState.NAVIGATING;
        } else {
            state = MacroState.COMPLETE;
        }
    }

    /**
     * Complete macro
     */
    private void complete() {
        System.out.println("========================================");
        System.out.println("✨ FAIRY SOUL MACRO COMPLETE ✨");
        System.out.println("Souls Collected: " + soulsCollectedThisSession);
        System.out.println("Island: " + currentIsland);
        System.out.println("========================================");

        onDisable();
    }

    /**
     * Print summary
     */
    private void printSummary() {
        System.out.println("========================================");
        System.out.println("FAIRY SOUL MACRO SUMMARY");
        System.out.println("========================================");
        System.out.println("Island: " + currentIsland);
        System.out.println("Souls Collected: " + soulsCollectedThisSession);
        System.out.println("Route Progress: " + routeIndex + "/" + soulRoute.size());
        System.out.println("Runtime: " + getRuntimeFormatted());
        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | Progress: %d/%d | Collected: %d",
                state, routeIndex, soulRoute.size(), soulsCollectedThisSession);
    }

    // ==================== GETTERS/SETTERS ====================

    public void setCurrentIsland(String island) {
        this.currentIsland = island;
    }

    public void setUseOptimalRoute(boolean optimal) {
        this.useOptimalRoute = optimal;
    }

    public void setSkipCollected(boolean skip) {
        this.skipCollected = skip;
    }

    public FairySoulFinder getFinder() {
        return finder;
    }
}
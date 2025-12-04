package com.donut.client.macros.farming;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;

import java.util.*;

/**
 * VisitorMacro - Automated garden visitor handling
 * Features: Auto-accept visitors, fulfill requests, claim rewards
 */
public class VisitorMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private MacroState state = MacroState.IDLE;
    private Visitor currentVisitor = null;

    // Settings
    private boolean autoAccept = true;
    private boolean autoFulfill = true;
    private boolean onlyRare = false; // Only accept rare visitors
    private int maxVisitors = 5;

    // Statistics
    private int visitorsAccepted = 0;
    private int requestsFulfilled = 0;
    private int rewardsClaimed = 0;
    private long coinsEarned = 0;

    public enum MacroState {
        IDLE,
        CHECKING,
        ACCEPTING,
        FULFILLING,
        CLAIMING,
        COMPLETE
    }

    public enum VisitorRarity {
        COMMON,
        UNCOMMON,
        RARE,
        LEGENDARY,
        SPECIAL
    }

    public VisitorMacro() {
        super("Visitor Macro", "Automated garden visitor handling");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = MacroState.CHECKING;
        currentVisitor = null;
        visitorsAccepted = 0;
        requestsFulfilled = 0;
        rewardsClaimed = 0;
        coinsEarned = 0;
        System.out.println("[Visitor Macro] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Visitor Macro] Starting...");
        System.out.println("[Visitor Macro] Max visitors: " + maxVisitors);
        System.out.println("[Visitor Macro] Only rare: " + onlyRare);

        state = MacroState.CHECKING;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Visitor Macro] Stopped");
        printStatistics();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                // Do nothing
                break;
            case CHECKING:
                checkForVisitors();
                break;
            case ACCEPTING:
                acceptVisitor();
                break;
            case FULFILLING:
                fulfillRequest();
                break;
            case CLAIMING:
                claimReward();
                break;
            case COMPLETE:
                complete();
                break;
        }
    }

    /**
     * Check for visitors
     */
    private void checkForVisitors() {
        // Check if max visitors reached
        if (visitorsAccepted >= maxVisitors) {
            state = MacroState.COMPLETE;
            return;
        }

        // TODO: Check visitor queue
        // Look for NPCs near garden spawn

        Visitor visitor = detectVisitor();

        if (visitor != null) {
            currentVisitor = visitor;

            // Check rarity filter
            if (onlyRare && !isRareVisitor(visitor)) {
                System.out.println("[Visitor Macro] Skipping common visitor: " + visitor.name);
                state = MacroState.CHECKING;
                return;
            }

            System.out.println("[Visitor Macro] Visitor detected: " + visitor.name +
                    " (" + visitor.rarity + ")");

            if (autoAccept) {
                state = MacroState.ACCEPTING;
            }
        }
    }

    /**
     * Detect visitor
     */
    private Visitor detectVisitor() {
        // TODO: Scan for visitor NPCs
        // Check scoreboard for visitor count
        return null;
    }

    /**
     * Check if visitor is rare
     */
    private boolean isRareVisitor(Visitor visitor) {
        return visitor.rarity == VisitorRarity.RARE ||
                visitor.rarity == VisitorRarity.LEGENDARY ||
                visitor.rarity == VisitorRarity.SPECIAL;
    }

    /**
     * Accept visitor
     */
    private void acceptVisitor() {
        if (currentVisitor == null) {
            state = MacroState.CHECKING;
            return;
        }

        // Click on visitor NPC
        System.out.println("[Visitor Macro] Accepting visitor: " + currentVisitor.name);

        // TODO: Right-click NPC
        // TODO: Click accept button in GUI

        visitorsAccepted++;

        if (autoFulfill) {
            state = MacroState.FULFILLING;
        } else {
            state = MacroState.CHECKING;
        }
    }

    /**
     * Fulfill visitor request
     */
    private void fulfillRequest() {
        if (currentVisitor == null) {
            state = MacroState.CHECKING;
            return;
        }

        System.out.println("[Visitor Macro] Fulfilling request for: " + currentVisitor.name);

        // Check if we have required items
        if (!hasRequiredItems(currentVisitor)) {
            System.out.println("[Visitor Macro] Missing items for request");
            state = MacroState.CHECKING;
            currentVisitor = null;
            return;
        }

        // Give items to visitor
        // TODO: Click on items in inventory
        // TODO: Confirm in GUI

        requestsFulfilled++;
        state = MacroState.CLAIMING;
    }

    /**
     * Check if player has required items
     */
    private boolean hasRequiredItems(Visitor visitor) {
        // TODO: Check inventory for items
        // visitor.requiredItems
        return true; // Simulated
    }

    /**
     * Claim reward
     */
    private void claimReward() {
        if (currentVisitor == null) {
            state = MacroState.CHECKING;
            return;
        }

        System.out.println("[Visitor Macro] Claiming reward from: " + currentVisitor.name);

        // TODO: Click claim button
        // TODO: Collect items/coins

        rewardsClaimed++;
        coinsEarned += currentVisitor.coinReward;

        System.out.println("========================================");
        System.out.println("✅ VISITOR COMPLETE");
        System.out.println("Visitor: " + currentVisitor.name);
        System.out.println("Reward: " + currentVisitor.coinReward + " coins");
        System.out.println("Total: " + visitorsAccepted + "/" + maxVisitors);
        System.out.println("========================================");

        currentVisitor = null;
        state = MacroState.CHECKING;
    }

    /**
     * Complete macro
     */
    private void complete() {
        System.out.println("========================================");
        System.out.println("👥 VISITOR MACRO COMPLETE 👥");
        System.out.println("Visitors: " + visitorsAccepted);
        System.out.println("Fulfilled: " + requestsFulfilled);
        System.out.println("Coins: " + coinsEarned);
        System.out.println("========================================");

        onDisable();
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("VISITOR MACRO STATISTICS");
        System.out.println("========================================");
        System.out.println("Visitors Accepted: " + visitorsAccepted);
        System.out.println("Requests Fulfilled: " + requestsFulfilled);
        System.out.println("Rewards Claimed: " + rewardsClaimed);
        System.out.println("Coins Earned: " + coinsEarned);
        System.out.println("Runtime: " + getRuntimeFormatted());

        long seconds = getRuntime() / 1000;
        if (seconds > 0) {
            int visitorsPerHour = (int)(visitorsAccepted * 3600L / seconds);
            long coinsPerHour = coinsEarned * 3600L / seconds;
            System.out.println("Rate: " + visitorsPerHour + " visitors/hour");
            System.out.println("Coins/Hour: " + coinsPerHour);
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | Visitors: %d/%d | Fulfilled: %d | Coins: %d",
                state, visitorsAccepted, maxVisitors, requestsFulfilled, coinsEarned);
    }

    // ==================== DATA CLASSES ====================

    public static class Visitor {
        public String name;
        public VisitorRarity rarity;
        public List<String> requiredItems;
        public long coinReward;
        public int gardenXP;

        public Visitor(String name, VisitorRarity rarity, long coinReward) {
            this.name = name;
            this.rarity = rarity;
            this.coinReward = coinReward;
            this.requiredItems = new ArrayList<>();
        }
    }

    // ==================== GETTERS/SETTERS ====================

    public void setAutoAccept(boolean auto) {
        this.autoAccept = auto;
    }

    public void setAutoFulfill(boolean auto) {
        this.autoFulfill = auto;
    }

    public void setOnlyRare(boolean only) {
        this.onlyRare = only;
    }

    public void setMaxVisitors(int max) {
        this.maxVisitors = max;
    }
}
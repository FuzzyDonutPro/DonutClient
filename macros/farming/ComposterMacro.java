package com.donut.client.macros.farming;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * ComposterMacro - Automated composter usage
 * Features: Auto-fill, auto-collect, inventory management
 */
public class ComposterMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private MacroState state = MacroState.IDLE;
    private BlockPos composterPos = null;

    // Settings
    private boolean autoFill = true;
    private boolean autoCollect = true;
    private int fillDelay = 50; // ms between fills

    // Compostable items priority
    private List<String> compostPriority = new ArrayList<>();

    // Statistics
    private int itemsComposted = 0;
    private int bonemealCollected = 0;

    public enum MacroState {
        IDLE,
        FILLING,
        WAITING,
        COLLECTING,
        COMPLETE
    }

    public ComposterMacro() {
        super("Composter Macro", "Automated composter usage");
        this.mc = MinecraftClient.getInstance();
        initializeCompostPriority();
    }

    @Override
    public void start() {
        state = MacroState.IDLE;
        itemsComposted = 0;
        bonemealCollected = 0;
        System.out.println("[Composter] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Composter] Starting...");

        if (composterPos == null) {
            System.out.println("[Composter] ERROR: Composter position not set!");
            onDisable();
            return;
        }

        state = MacroState.FILLING;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Composter] Stopped");
        printStatistics();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                // Do nothing
                break;
            case FILLING:
                fill();
                break;
            case WAITING:
                waitForCompost();
                break;
            case COLLECTING:
                collect();
                break;
            case COMPLETE:
                complete();
                break;
        }
    }

    /**
     * Initialize compost priority list
     */
    private void initializeCompostPriority() {
        // Low value items first
        compostPriority.add("WHEAT");
        compostPriority.add("CARROT");
        compostPriority.add("POTATO");
        compostPriority.add("BEETROOT");
        compostPriority.add("MELON_SLICE");
        compostPriority.add("PUMPKIN");
        compostPriority.add("BROWN_MUSHROOM");
        compostPriority.add("RED_MUSHROOM");
        compostPriority.add("CACTUS");
        compostPriority.add("SUGAR_CANE");
    }

    /**
     * Fill composter
     */
    private void fill() {
        if (composterPos == null) return;

        // Check if composter is full
        if (isComposterFull()) {
            state = MacroState.WAITING;
            return;
        }

        // Find compostable item in inventory
        String item = findCompostableItem();

        if (item == null) {
            System.out.println("[Composter] No compostable items in inventory");
            state = MacroState.COMPLETE;
            return;
        }

        // Right-click composter with item
        addToComposter(item);
        itemsComposted++;

        // Delay between fills
        try {
            Thread.sleep(fillDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if composter is full
     */
    private boolean isComposterFull() {
        // TODO: Check composter fill level
        // Full = ready to collect bonemeal
        return false;
    }

    /**
     * Find compostable item in inventory
     */
    private String findCompostableItem() {
        // Check inventory for items in priority order
        for (String item : compostPriority) {
            if (hasItemInInventory(item)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Check if player has item in inventory
     */
    private boolean hasItemInInventory(String itemName) {
        // TODO: Check inventory
        return false;
    }

    /**
     * Add item to composter
     */
    private void addToComposter(String item) {
        // TODO: Right-click composter with item
        System.out.println("[Composter] Added: " + item);
    }

    /**
     * Wait for compost to complete
     */
    private void waitForCompost() {
        // Check if ready to collect
        if (isBonemealReady()) {
            state = MacroState.COLLECTING;
        }
    }

    /**
     * Check if bonemeal is ready
     */
    private boolean isBonemealReady() {
        // TODO: Check if composter has bonemeal
        return false;
    }

    /**
     * Collect bonemeal
     */
    private void collect() {
        if (composterPos == null) return;

        // Right-click to collect bonemeal
        collectBonemeal();
        bonemealCollected++;

        System.out.println("[Composter] Collected bonemeal! Total: " + bonemealCollected);

        // Continue filling
        if (autoFill) {
            state = MacroState.FILLING;
        } else {
            state = MacroState.COMPLETE;
        }
    }

    /**
     * Collect bonemeal from composter
     */
    private void collectBonemeal() {
        // TODO: Right-click composter to collect
    }

    /**
     * Complete macro
     */
    private void complete() {
        System.out.println("========================================");
        System.out.println("♻️ COMPOSTER COMPLETE ♻️");
        System.out.println("Items Composted: " + itemsComposted);
        System.out.println("Bonemeal Collected: " + bonemealCollected);
        System.out.println("========================================");

        onDisable();
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("COMPOSTER STATISTICS");
        System.out.println("========================================");
        System.out.println("Items Composted: " + itemsComposted);
        System.out.println("Bonemeal Collected: " + bonemealCollected);
        System.out.println("Runtime: " + getRuntimeFormatted());

        long seconds = getRuntime() / 1000;
        if (seconds > 0) {
            int itemsPerHour = (int)(itemsComposted * 3600L / seconds);
            int bonemealPerHour = (int)(bonemealCollected * 3600L / seconds);
            System.out.println("Rate: " + itemsPerHour + " items/hour");
            System.out.println("Bonemeal/Hour: " + bonemealPerHour);
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | Items: %d | Bonemeal: %d",
                state, itemsComposted, bonemealCollected);
    }

    // ==================== GETTERS/SETTERS ====================

    public void setComposterPos(BlockPos pos) {
        this.composterPos = pos;
    }

    public void setAutoFill(boolean auto) {
        this.autoFill = auto;
    }

    public void setAutoCollect(boolean auto) {
        this.autoCollect = auto;
    }

    public void setFillDelay(int delay) {
        this.fillDelay = delay;
    }

    public void addCompostItem(String item) {
        if (!compostPriority.contains(item)) {
            compostPriority.add(item);
        }
    }

    public void removeCompostItem(String item) {
        compostPriority.remove(item);
    }
}
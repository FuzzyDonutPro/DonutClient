package com.donut.client.macros.collection;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * MuseumDonatorMacro - Automatically donate items to museum
 */
public class MuseumDonatorMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private DonateState state = DonateState.IDLE;
    private Queue<ItemStack> donationQueue = new LinkedList<>();

    // Settings
    private boolean autoScan = true;
    private boolean onlyRare = false;
    private int scanInterval = 100; // ticks

    // Statistics
    private int itemsDonated = 0;
    private int totalValue = 0;
    private long lastScanTime = 0;

    public enum DonateState {
        IDLE, SCANNING, NAVIGATING, DONATING, COMPLETE
    }

    // Museum-worthy items (example list)
    private static final Set<Item> MUSEUM_ITEMS = new HashSet<>(Arrays.asList(
            Items.DIAMOND,
            Items.EMERALD,
            Items.ANCIENT_DEBRIS,
            Items.NETHERITE_INGOT,
            Items.DRAGON_HEAD,
            Items.ELYTRA,
            Items.TOTEM_OF_UNDYING,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.NETHER_STAR,
            Items.BEACON
    ));

    public MuseumDonatorMacro() {
        super("Museum Donator", "Automatically donate items to museum");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = DonateState.IDLE;
        donationQueue.clear();
        itemsDonated = 0;
        totalValue = 0;
        System.out.println("[Museum Donator] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = DonateState.SCANNING;
        System.out.println("[Museum Donator] Starting...");
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        switch (state) {
            case IDLE:
                if (autoScan) {
                    long now = System.currentTimeMillis();
                    if (now - lastScanTime > scanInterval * 50) {
                        state = DonateState.SCANNING;
                        lastScanTime = now;
                    }
                }
                break;
            case SCANNING:
                scanInventory();
                break;
            case NAVIGATING:
                navigateToMuseum();
                break;
            case DONATING:
                donateItems();
                break;
            case COMPLETE:
                // Done
                break;
        }
    }

    /**
     * Scan inventory for museum items
     */
    private void scanInventory() {
        if (mc.player == null) return;

        donationQueue.clear();

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (!stack.isEmpty() && isMuseumWorthy(stack)) {
                donationQueue.add(stack.copy());
            }
        }

        System.out.println("[Museum Donator] Found " + donationQueue.size() + " items to donate");

        if (donationQueue.isEmpty()) {
            state = DonateState.IDLE;
        } else {
            state = DonateState.NAVIGATING;
        }
    }

    /**
     * Check if item is museum-worthy
     */
    private boolean isMuseumWorthy(ItemStack stack) {
        Item item = stack.getItem();

        // Check if in museum items list
        if (MUSEUM_ITEMS.contains(item)) {
            return true;
        }

        // Check for enchanted items
        if (stack.hasEnchantments()) {
            return true;
        }

        // Check for named items (FIXED: use getName() check)
        try {
            String itemName = stack.getName().getString();
            String defaultName = stack.getItem().getName().getString();
            if (!itemName.equals(defaultName)) {
                return true; // Has custom name
            }
        } catch (Exception e) {
            // Ignore
        }

        return false;
    }

    /**
     * Navigate to museum
     */
    private void navigateToMuseum() {
        // TODO: Actual navigation logic
        System.out.println("[Museum Donator] Navigating to museum...");
        state = DonateState.DONATING;
    }

    /**
     * Donate items
     */
    private void donateItems() {
        if (donationQueue.isEmpty()) {
            state = DonateState.COMPLETE;
            printStatistics();
            return;
        }

        ItemStack item = donationQueue.poll();

        // TODO: Actually donate the item
        System.out.println("[Museum Donator] Donating: " + item.getName().getString());

        itemsDonated++;
        totalValue += calculateValue(item);
    }

    /**
     * Calculate item value
     */
    private int calculateValue(ItemStack stack) {
        // Simple value calculation
        Item item = stack.getItem();

        if (item == Items.NETHERITE_INGOT) return 10000;
        if (item == Items.DIAMOND) return 1000;
        if (item == Items.EMERALD) return 500;
        if (item == Items.NETHER_STAR) return 50000;
        if (stack.hasEnchantments()) return 2000;

        return 100;
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("MUSEUM DONATOR STATISTICS");
        System.out.println("========================================");
        System.out.println("Items Donated: " + itemsDonated);
        System.out.println("Total Value: " + totalValue + " coins");
        System.out.println("Runtime: " + getRuntimeFormatted());
        System.out.println("========================================");
    }

    /**
     * Get status
     */
    public String getStatus() {
        return String.format("%s | Donated: %d | Queue: %d | Value: %d",
                state, itemsDonated, donationQueue.size(), totalValue);
    }

    // Getters/Setters
    public void setAutoScan(boolean auto) {
        this.autoScan = auto;
    }

    public void setOnlyRare(boolean onlyRare) {
        this.onlyRare = onlyRare;
    }

    public void setScanInterval(int interval) {
        this.scanInterval = interval;
    }
}
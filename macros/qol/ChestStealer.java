package com.donut.client.macros.qol;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.util.*;

/**
 * ChestStealer - Quickly steal items from chests
 * Features: Smart filtering, trash items, auto-close
 */
public class ChestStealer extends Macro {

    private final MinecraftClient mc;

    // Settings
    private int delayMs = 50; // Delay between taking items
    private boolean autoClose = true;
    private boolean ignoreTrash = true;
    private boolean smartMode = true; // Only take valuable items

    // Trash items to ignore
    private static final Set<String> TRASH_ITEMS = new HashSet<>(Arrays.asList(
            "dirt", "cobblestone", "stone", "wooden_pickaxe",
            "wooden_sword", "wooden_axe", "leather_helmet",
            "leather_chestplate", "leather_leggings", "leather_boots"
    ));

    // Valuable items to prioritize
    private static final Set<String> VALUABLE_ITEMS = new HashSet<>(Arrays.asList(
            "diamond", "emerald", "gold", "iron", "enchanted",
            "potion", "apple", "pearl"
    ));

    // State
    private boolean stealing = false;
    private long lastStealTime = 0;
    private int itemsStolen = 0;

    public ChestStealer() {
        super("Chest Stealer", "Quickly loot items from chests");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        stealing = false;
        itemsStolen = 0;
        System.out.println("[Chest Stealer] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Chest Stealer] Enabled");
        System.out.println("[Chest Stealer] Delay: " + delayMs + "ms");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Chest Stealer] Disabled");
        System.out.println("[Chest Stealer] Items stolen: " + itemsStolen);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Check if chest screen is open
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            stealing = false;
            return;
        }

        GenericContainerScreen screen = (GenericContainerScreen) mc.currentScreen;

        if (!stealing) {
            stealing = true;
            System.out.println("[Chest Stealer] Chest opened, stealing...");
        }

        // Check delay
        long now = System.currentTimeMillis();
        if (now - lastStealTime < delayMs) {
            return;
        }

        // Find items to steal
        boolean foundItem = false;

        for (Slot slot : screen.getScreenHandler().slots) {
            // Skip player inventory slots
            if (slot.id >= screen.getScreenHandler().getRows() * 9) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            // Check if should take item
            if (shouldTakeItem(stack)) {
                takeItem(slot);
                foundItem = true;
                lastStealTime = now;
                break; // One item per tick
            }
        }

        // If no items left, close chest
        if (!foundItem && autoClose) {
            closeChest();
        }
    }

    /**
     * Check if should take item
     */
    private boolean shouldTakeItem(ItemStack stack) {
        String itemName = stack.getItem().toString().toLowerCase();

        // Ignore trash items
        if (ignoreTrash) {
            for (String trash : TRASH_ITEMS) {
                if (itemName.contains(trash)) {
                    return false;
                }
            }
        }

        // Smart mode: only take valuable items
        if (smartMode) {
            boolean isValuable = false;
            for (String valuable : VALUABLE_ITEMS) {
                if (itemName.contains(valuable)) {
                    isValuable = true;
                    break;
                }
            }

            if (!isValuable) {
                return false;
            }
        }

        return true;
    }

    /**
     * Take item from slot
     */
    private void takeItem(Slot slot) {
        if (mc.interactionManager == null) return;

        // TODO: Click slot to take item
        // mc.interactionManager.clickSlot(...)

        itemsStolen++;
        System.out.println("[Chest Stealer] Took: " + slot.getStack().getName().getString());
    }

    /**
     * Close chest
     */
    private void closeChest() {
        System.out.println("[Chest Stealer] Chest empty, closing");

        if (mc.player != null) {
            mc.player.closeHandledScreen();
        }

        stealing = false;
    }

    /**
     * Get status display
     */
    public String getStatus() {
        if (stealing) {
            return String.format("STEALING | Items: %d", itemsStolen);
        }
        return "WAITING";
    }

    // ==================== GETTERS/SETTERS ====================

    public void setDelayMs(int delay) {
        this.delayMs = Math.max(0, delay);
    }

    public void setAutoClose(boolean auto) {
        this.autoClose = auto;
    }

    public void setIgnoreTrash(boolean ignore) {
        this.ignoreTrash = ignore;
    }

    public void setSmartMode(boolean smart) {
        this.smartMode = smart;
    }

    public void addTrashItem(String item) {
        TRASH_ITEMS.add(item.toLowerCase());
    }

    public void addValuableItem(String item) {
        VALUABLE_ITEMS.add(item.toLowerCase());
    }
}
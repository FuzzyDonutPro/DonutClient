package com.donut.client.macros.qol;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;

/**
 * AutoSprint - Automatically sprint when moving
 * Features: Toggle sprint, only when moving forward, hunger aware
 */
public class AutoSprint extends Macro {

    private final MinecraftClient mc;

    // Settings
    private boolean onlyForward = true; // Only sprint when moving forward
    private boolean hungerAware = true; // Don't sprint if hunger too low
    private double minHunger = 6.0; // Minimum hunger level

    public AutoSprint() {
        super("Auto Sprint", "Automatically sprint when moving");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        System.out.println("[Auto Sprint] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Auto Sprint] Enabled");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Auto Sprint] Disabled");

        // Stop sprinting
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Check hunger
        if (hungerAware && mc.player.getHungerManager().getFoodLevel() < minHunger) {
            mc.player.setSprinting(false);
            return;
        }

        // Check if should sprint
        boolean shouldSprint = false;

        if (onlyForward) {
            // Only sprint when moving forward
            shouldSprint = mc.options.forwardKey.isPressed();
        } else {
            // Sprint when moving in any direction
            shouldSprint = mc.options.forwardKey.isPressed() ||
                    mc.options.backKey.isPressed() ||
                    mc.options.leftKey.isPressed() ||
                    mc.options.rightKey.isPressed();
        }

        // Set sprint state
        if (shouldSprint && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        } else if (!shouldSprint && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }

    /**
     * Get status display
     */
    public String getStatus() {
        if (mc.player != null && mc.player.isSprinting()) {
            return "SPRINTING";
        }
        return "READY";
    }

    // ==================== GETTERS/SETTERS ====================

    public void setOnlyForward(boolean only) {
        this.onlyForward = only;
    }

    public void setHungerAware(boolean aware) {
        this.hungerAware = aware;
    }

    public void setMinHunger(double hunger) {
        this.minHunger = hunger;
    }
}
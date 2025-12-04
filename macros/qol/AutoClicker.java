package com.donut.client.macros.qol;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;

/**
 * AutoClicker - Automatically click (left/right)
 * Features: Configurable CPS, randomization, toggle modes
 */
public class AutoClicker extends Macro {

    private final MinecraftClient mc;

    // Settings
    private ClickMode clickMode = ClickMode.LEFT;
    private int clicksPerSecond = 10;
    private boolean randomize = true;
    private int randomVariation = 2; // +/- 2 CPS
    private boolean holdOnly = false; // Only click when key held

    // State
    private long lastClickTime = 0;
    private int currentCPS = 10;

    public enum ClickMode {
        LEFT,   // Left click (attack/mine)
        RIGHT,  // Right click (use/place)
        BOTH    // Both (alternate)
    }

    public AutoClicker() {
        super("Auto Clicker", "Automatically click left/right");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        lastClickTime = 0;
        updateCPS();
        System.out.println("[Auto Clicker] Initialized - CPS: " + clicksPerSecond);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Auto Clicker] Enabled");
        System.out.println("[Auto Clicker] Mode: " + clickMode);
        System.out.println("[Auto Clicker] CPS: " + clicksPerSecond);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Auto Clicker] Disabled");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) return;

        // Check if should click
        if (holdOnly) {
            boolean keyHeld = false;

            switch (clickMode) {
                case LEFT:
                    keyHeld = mc.options.attackKey.isPressed();
                    break;
                case RIGHT:
                    keyHeld = mc.options.useKey.isPressed();
                    break;
                case BOTH:
                    keyHeld = mc.options.attackKey.isPressed() || mc.options.useKey.isPressed();
                    break;
            }

            if (!keyHeld) return;
        }

        // Check if should click now
        long now = System.currentTimeMillis();
        long clickInterval = 1000 / currentCPS;

        if (now - lastClickTime >= clickInterval) {
            performClick();
            lastClickTime = now;

            // Randomize next click
            if (randomize) {
                updateCPS();
            }
        }
    }

    /**
     * Perform click action
     */
    private void performClick() {
        if (mc.interactionManager == null) return;

        switch (clickMode) {
            case LEFT:
                // TODO: Left click (attack/break)
                break;
            case RIGHT:
                // TODO: Right click (use/place)
                break;
            case BOTH:
                // Alternate between left and right
                if (System.currentTimeMillis() % 2 == 0) {
                    // Left click
                } else {
                    // Right click
                }
                break;
        }
    }

    /**
     * Update CPS with randomization
     */
    private void updateCPS() {
        if (randomize) {
            int variation = (int) (Math.random() * randomVariation * 2) - randomVariation;
            currentCPS = Math.max(1, clicksPerSecond + variation);
        } else {
            currentCPS = clicksPerSecond;
        }
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("CLICKING | CPS: %d | Mode: %s", currentCPS, clickMode);
    }

    // ==================== GETTERS/SETTERS ====================

    public void setClickMode(ClickMode mode) {
        this.clickMode = mode;
    }

    public void setClicksPerSecond(int cps) {
        this.clicksPerSecond = Math.max(1, Math.min(20, cps));
        updateCPS();
    }

    public void setRandomize(boolean random) {
        this.randomize = random;
    }

    public void setRandomVariation(int variation) {
        this.randomVariation = variation;
    }

    public void setHoldOnly(boolean hold) {
        this.holdOnly = hold;
    }
}
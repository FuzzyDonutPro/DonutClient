package com.donut.client.macros.qol;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;

import java.util.Random;

/**
 * AutoReconnect - Automatically reconnect when disconnected
 * Features: Configurable delay, random variation, retry limit
 */
public class AutoReconnect extends Macro {

    private final MinecraftClient mc;
    private final Random random;

    // Settings
    private int reconnectDelay = 5000; // 5 seconds
    private int randomVariation = 2000; // +/- 2 seconds
    private int maxRetries = 5;
    private boolean onlyOnKick = true; // Only reconnect on kick, not manual disconnect

    // State
    private boolean disconnected = false;
    private long disconnectTime = 0;
    private int retryCount = 0;
    private String lastServer = "mc.hypixel.net";

    public AutoReconnect() {
        super("Auto Reconnect", "Automatically reconnect when disconnected");
        this.mc = MinecraftClient.getInstance();
        this.random = new Random();
    }

    @Override
    public void start() {
        disconnected = false;
        retryCount = 0;
        System.out.println("[Auto Reconnect] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Auto Reconnect] Enabled");
        System.out.println("[Auto Reconnect] Delay: " + reconnectDelay + "ms");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Auto Reconnect] Disabled");
    }

    @Override
    public void onTick() {
        if (mc == null) return;

        // Check if on disconnect screen
        if (mc.currentScreen instanceof DisconnectedScreen) {
            if (!disconnected) {
                // Just disconnected
                disconnected = true;
                disconnectTime = System.currentTimeMillis();
                System.out.println("[Auto Reconnect] Disconnected detected");
            }

            // Check if should reconnect
            long elapsed = System.currentTimeMillis() - disconnectTime;
            int actualDelay = reconnectDelay + random.nextInt(randomVariation * 2) - randomVariation;

            if (elapsed >= actualDelay) {
                if (retryCount < maxRetries) {
                    reconnect();
                } else {
                    System.out.println("[Auto Reconnect] Max retries reached");
                    onDisable();
                }
            }
        } else if (disconnected && mc.world != null) {
            // Successfully reconnected
            System.out.println("[Auto Reconnect] Reconnected successfully!");
            disconnected = false;
            retryCount = 0;
        }
    }

    /**
     * Attempt to reconnect
     */
    private void reconnect() {
        retryCount++;
        System.out.println("[Auto Reconnect] Reconnecting... (Attempt " + retryCount + "/" + maxRetries + ")");

        // TODO: Implement actual reconnection
        // This would involve:
        // 1. Getting server address from disconnect screen
        // 2. Navigating back to multiplayer screen
        // 3. Connecting to server

        // Reset disconnect state
        disconnected = false;
        disconnectTime = 0;
    }

    /**
     * Get status display
     */
    public String getStatus() {
        if (disconnected) {
            return String.format("WAITING | Retry: %d/%d", retryCount, maxRetries);
        }
        return "MONITORING";
    }

    // ==================== GETTERS/SETTERS ====================

    public void setReconnectDelay(int delay) {
        this.reconnectDelay = delay;
    }

    public void setRandomVariation(int variation) {
        this.randomVariation = variation;
    }

    public void setMaxRetries(int max) {
        this.maxRetries = max;
    }

    public void setOnlyOnKick(boolean only) {
        this.onlyOnKick = only;
    }
}
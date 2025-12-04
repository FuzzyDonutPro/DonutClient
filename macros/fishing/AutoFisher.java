package com.donut.client.macros.fishing;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;

/**
 * Example macro with settings
 */
public class AutoFisher extends Macro {

    private final MinecraftClient client = MinecraftClient.getInstance();

    // State
    private long lastCastTime = 0;
    private boolean waitingForBite = false;
    private int fishCaught = 0;

    public AutoFisher() {
        super("Auto Fisher", "Automatically casts and reels fishing rod");
    }

    @Override
    protected void initializeSettings() {
        // Create settings
        createSettings();

        // Add settings
        settings.addBooleanSetting("autoCast", "Automatically cast rod", true);
        settings.addBooleanSetting("autoReel", "Automatically reel when fish bites", true);
        settings.addIntSetting("castDelay", "Delay between casts (ms)", 500, 100, 2000);
        settings.addIntSetting("reelDelay", "Delay before reeling (ms)", 100, 0, 500);
        settings.addBooleanSetting("failsafe", "Enable failsafe detection", true);
        settings.addIntSetting("maxFishTime", "Max time waiting for fish (seconds)", 30, 10, 120);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        setThrottleInterval(5); // Check every 5 ticks
        lastCastTime = 0;
        waitingForBite = false;
        fishCaught = 0;
    }

    @Override
    public void onTick() {
        if (client.player == null || client.world == null) return;

        // Check if we should cast
        if (settings.getBoolean("autoCast") && shouldCast()) {
            castRod();
        }

        // Check if we should reel
        if (settings.getBoolean("autoReel") && shouldReel()) {
            reelRod();
        }

        // Failsafe check
        if (settings.getBoolean("failsafe")) {
            checkFailsafe();
        }
    }

    private boolean shouldCast() {
        long now = System.currentTimeMillis();
        int castDelay = settings.getInt("castDelay");

        return !waitingForBite && (now - lastCastTime > castDelay);
    }

    private void castRod() {
        // Cast rod logic here
        lastCastTime = System.currentTimeMillis();
        waitingForBite = true;
        log("Cast rod");
    }

    private boolean shouldReel() {
        if (!waitingForBite) return false;

        // Check for bite (bobber velocity change)
        // This is simplified - real implementation would check bobber entity
        return detectBite();
    }

    private void reelRod() {
        int reelDelay = settings.getInt("reelDelay");

        try {
            Thread.sleep(reelDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Reel rod logic here
        waitingForBite = false;
        fishCaught++;
        log("Caught fish! Total: " + fishCaught);
    }

    private boolean detectBite() {
        // Simplified bite detection
        // Real implementation would check bobber.getVelocity().y < -0.15
        return false;
    }

    private void checkFailsafe() {
        long maxFishTime = settings.getInt("maxFishTime") * 1000L;
        long waitTime = System.currentTimeMillis() - lastCastTime;

        if (waitingForBite && waitTime > maxFishTime) {
            log("Failsafe triggered - re-casting");
            waitingForBite = false;
            lastCastTime = 0;
        }
    }

    @Override
    public String getStatusInfo() {
        return "Fish caught: " + fishCaught + " | Runtime: " + getRuntimeFormatted();
    }

    @Override
    public double getProgress() {
        return waitingForBite ? 0.5 : 0.0;
    }
}
package com.donut.client.failsafe;

import com.donut.client.utils.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.*;

public class FailsafeManager {
    private final MinecraftClient mc;
    private final List<BaseFailsafe> failsafes;
    private final Map<String, Integer> triggerCounts;
    private boolean enabled;

    // Failsafe actions
    public enum FailsafeAction {
        LOG,        // Just log the event
        PAUSE,      // Pause current macro
        STOP,       // Stop current macro
        DISCONNECT  // Disconnect from server
    }

    // Failsafe severity levels
    public enum FailsafeSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public FailsafeManager() {
        this.mc = MinecraftClient.getInstance();
        this.failsafes = new ArrayList<>();
        this.triggerCounts = new HashMap<>();
        this.enabled = true;

        // Initialize failsafes
        initializeFailsafes();
    }

    private void initializeFailsafes() {
        // Register all failsafes
        registerFailsafe(new PlayerDetector());
        registerFailsafe(new StaffDetector());
        registerFailsafe(new HealthMonitor());
        registerFailsafe(new VoidDetector());
        registerFailsafe(new StuckDetector());
        registerFailsafe(new DamageDetector());

        ChatUtils.log("Registered " + failsafes.size() + " failsafes");
    }

    public void tick() {
        if (!enabled || mc.player == null || mc.world == null) {
            return;
        }

        // Check all failsafes
        for (BaseFailsafe failsafe : failsafes) {
            if (failsafe.check()) {
                handleFailsafe(failsafe);
            }
        }
    }

    private void handleFailsafe(BaseFailsafe failsafe) {
        String name = failsafe.getName();
        FailsafeSeverity severity = failsafe.getSeverity();
        FailsafeAction action = failsafe.getAction();

        // Increment trigger count
        triggerCounts.put(name, triggerCounts.getOrDefault(name, 0) + 1);

        // Log the event
        String message = String.format("[FAILSAFE] %s triggered! Severity: %s, Action: %s",
                name, severity, action);
        ChatUtils.log(message);

        // Execute action based on severity
        switch (action) {
            case LOG:
                ChatUtils.sendWarning("Failsafe: " + name);
                break;

            case PAUSE:
                ChatUtils.sendWarning("Failsafe triggered: " + name + " - Pausing macro");
                pauseMacro();
                break;

            case STOP:
                ChatUtils.sendError("Failsafe triggered: " + name + " - Stopping macro");
                stopMacro();
                break;

            case DISCONNECT:
                ChatUtils.sendError("CRITICAL FAILSAFE: " + name + " - Disconnecting!");
                emergencyDisconnect();
                break;
        }
    }

    private void pauseMacro() {
        // TODO: Integrate with MacroManager
        // DonutClient.getInstance().getMacroManager().pauseCurrentMacro();
    }

    private void stopMacro() {
        // TODO: Integrate with MacroManager
        // DonutClient.getInstance().getMacroManager().stopCurrentMacro();
    }

    private void emergencyDisconnect() {
        try {
            if (mc.world != null) {
                mc.world.disconnect();
            }
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().getConnection().disconnect(Text.literal("Failsafe triggered"));
            }
        } catch (Exception e) {
            ChatUtils.log("Error during emergency disconnect: " + e.getMessage());
        }
    }

    public void registerFailsafe(BaseFailsafe failsafe) {
        failsafes.add(failsafe);
    }

    public void unregisterFailsafe(BaseFailsafe failsafe) {
        failsafes.remove(failsafe);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        ChatUtils.sendInfo("Failsafes " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<BaseFailsafe> getFailsafes() {
        return new ArrayList<>(failsafes);
    }

    public int getTriggerCount(String failsafeName) {
        return triggerCounts.getOrDefault(failsafeName, 0);
    }

    public void resetTriggerCounts() {
        triggerCounts.clear();
    }

    public Map<String, Integer> getAllTriggerCounts() {
        return new HashMap<>(triggerCounts);
    }
}
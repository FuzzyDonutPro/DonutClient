package com.donut.client.failsafe;

import com.donut.client.failsafe.FailsafeManager.FailsafeAction;
import com.donut.client.failsafe.FailsafeManager.FailsafeSeverity;
import net.minecraft.client.MinecraftClient;

public abstract class BaseFailsafe {
    protected final MinecraftClient mc;
    protected final String name;
    protected FailsafeSeverity severity;
    protected FailsafeAction action;
    protected boolean enabled;

    public BaseFailsafe(String name, FailsafeSeverity severity, FailsafeAction action) {
        this.mc = MinecraftClient.getInstance();
        this.name = name;
        this.severity = severity;
        this.action = action;
        this.enabled = true;
    }

    /**
     * Check if the failsafe condition is met
     * @return true if failsafe should trigger
     */
    public abstract boolean check();

    /**
     * Called when the failsafe is triggered
     */
    public void onTrigger() {
        // Override in subclasses if needed
    }

    /**
     * Reset the failsafe state
     */
    public void reset() {
        // Override in subclasses if needed
    }

    public String getName() {
        return name;
    }

    public FailsafeSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(FailsafeSeverity severity) {
        this.severity = severity;
    }

    public FailsafeAction getAction() {
        return action;
    }

    public void setAction(FailsafeAction action) {
        this.action = action;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
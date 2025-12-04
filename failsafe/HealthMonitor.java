package com.donut.client.failsafe;

import com.donut.client.failsafe.FailsafeManager.FailsafeAction;
import com.donut.client.failsafe.FailsafeManager.FailsafeSeverity;

public class HealthMonitor extends BaseFailsafe {
    private float healthThreshold = 6.0f; // 3 hearts (30% of 20 HP)

    public HealthMonitor() {
        super("Health Monitor", FailsafeSeverity.MEDIUM, FailsafeAction.PAUSE);
    }

    @Override
    public boolean check() {
        if (!enabled || mc.player == null) {
            return false;
        }

        float currentHealth = mc.player.getHealth();

        // Update severity based on health
        if (currentHealth <= 2.0f) { // 1 heart
            setSeverity(FailsafeSeverity.CRITICAL);
            setAction(FailsafeAction.STOP);
        } else if (currentHealth <= 4.0f) { // 2 hearts
            setSeverity(FailsafeSeverity.HIGH);
            setAction(FailsafeAction.STOP);
        } else if (currentHealth <= healthThreshold) {
            setSeverity(FailsafeSeverity.MEDIUM);
            setAction(FailsafeAction.PAUSE);
        }

        return currentHealth <= healthThreshold;
    }

    public float getHealthThreshold() {
        return healthThreshold;
    }

    public void setHealthThreshold(float threshold) {
        this.healthThreshold = threshold;
    }
}
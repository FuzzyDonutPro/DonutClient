package com.donut.client.failsafe;

import com.donut.client.failsafe.FailsafeManager.FailsafeAction;
import com.donut.client.failsafe.FailsafeManager.FailsafeSeverity;

import java.util.LinkedList;
import java.util.Queue;

public class DamageDetector extends BaseFailsafe {
    private final Queue<Long> damageTimestamps;
    private static final int MAX_DAMAGE_EVENTS = 5;
    private static final long TIME_WINDOW = 10000; // 10 seconds
    private float lastHealth = -1;

    public DamageDetector() {
        super("Damage Detector", FailsafeSeverity.MEDIUM, FailsafeAction.PAUSE);
        this.damageTimestamps = new LinkedList<>();
    }

    @Override
    public boolean check() {
        if (!enabled || mc.player == null) {
            return false;
        }

        float currentHealth = mc.player.getHealth();

        // Initialize last health
        if (lastHealth == -1) {
            lastHealth = currentHealth;
            return false;
        }

        // Check if player took damage
        if (currentHealth < lastHealth) {
            long currentTime = System.currentTimeMillis();
            damageTimestamps.add(currentTime);

            // Remove old damage events outside time window
            while (!damageTimestamps.isEmpty() &&
                    currentTime - damageTimestamps.peek() > TIME_WINDOW) {
                damageTimestamps.poll();
            }

            // Check if too many damage events
            if (damageTimestamps.size() >= MAX_DAMAGE_EVENTS) {
                // Update severity based on damage frequency
                if (damageTimestamps.size() >= 8) {
                    setSeverity(FailsafeSeverity.HIGH);
                    setAction(FailsafeAction.STOP);
                } else if (damageTimestamps.size() >= 6) {
                    setSeverity(FailsafeSeverity.MEDIUM);
                    setAction(FailsafeAction.PAUSE);
                }

                lastHealth = currentHealth;
                return true;
            }
        }

        lastHealth = currentHealth;
        return false;
    }

    @Override
    public void reset() {
        damageTimestamps.clear();
        lastHealth = -1;
    }
}
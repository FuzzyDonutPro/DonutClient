package com.donut.client.failsafe;

import com.donut.client.failsafe.FailsafeManager.FailsafeAction;
import com.donut.client.failsafe.FailsafeManager.FailsafeSeverity;
import net.minecraft.util.math.Vec3d;

public class StuckDetector extends BaseFailsafe {
    private Vec3d lastPosition = null;
    private long lastCheckTime = 0;
    private long stuckStartTime = 0;
    private boolean currentlyStuck = false;

    private static final long CHECK_INTERVAL = 2000; // Check every 2 seconds
    private static final long STUCK_THRESHOLD = 15000; // 15 seconds to trigger
    private static final double MOVEMENT_THRESHOLD = 1.0; // Must move at least 1 block

    public StuckDetector() {
        super("Stuck Detector", FailsafeSeverity.LOW, FailsafeAction.LOG);
    }

    @Override
    public boolean check() {
        if (!enabled || mc.player == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();

        // Only check every CHECK_INTERVAL milliseconds
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            return currentlyStuck;
        }

        lastCheckTime = currentTime;
        Vec3d currentPosition = mc.player.getPos();

        // Initialize on first check
        if (lastPosition == null) {
            lastPosition = currentPosition;
            stuckStartTime = currentTime;
            currentlyStuck = false;
            return false;
        }

        double distanceMoved = currentPosition.distanceTo(lastPosition);

        if (distanceMoved < MOVEMENT_THRESHOLD) {
            // Player hasn't moved much since last check
            if (!currentlyStuck) {
                // Just started being stuck
                stuckStartTime = currentTime;
                currentlyStuck = false;
            } else {
                // Still stuck, check duration
                long stuckDuration = currentTime - stuckStartTime;

                if (stuckDuration > STUCK_THRESHOLD) {
                    // Update severity based on how long stuck
                    if (stuckDuration > 60000) { // 60 seconds
                        setSeverity(FailsafeSeverity.HIGH);
                        setAction(FailsafeAction.STOP);
                    } else if (stuckDuration > 30000) { // 30 seconds
                        setSeverity(FailsafeSeverity.MEDIUM);
                        setAction(FailsafeAction.PAUSE);
                    } else {
                        setSeverity(FailsafeSeverity.LOW);
                        setAction(FailsafeAction.LOG);
                    }

                    currentlyStuck = true;
                    lastPosition = currentPosition;
                    return true;
                }
            }
        } else {
            // Player moved significantly, reset
            stuckStartTime = currentTime;
            currentlyStuck = false;
        }

        lastPosition = currentPosition;
        return false;
    }

    @Override
    public void reset() {
        lastPosition = null;
        lastCheckTime = 0;
        stuckStartTime = 0;
        currentlyStuck = false;
    }

    public long getStuckDuration() {
        if (!currentlyStuck || stuckStartTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - stuckStartTime;
    }
}
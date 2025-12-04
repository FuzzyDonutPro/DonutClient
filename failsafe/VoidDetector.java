package com.donut.client.failsafe;

import com.donut.client.failsafe.FailsafeManager.FailsafeAction;
import com.donut.client.failsafe.FailsafeManager.FailsafeSeverity;

public class VoidDetector extends BaseFailsafe {
    private static final int VOID_Y_LEVEL = -64;
    private static final int TRIGGER_DISTANCE = 5;

    public VoidDetector() {
        super("Void Detector", FailsafeSeverity.MEDIUM, FailsafeAction.STOP);
    }

    @Override
    public boolean check() {
        if (!enabled || mc.player == null) {
            return false;
        }

        double playerY = mc.player.getY();

        // Check if player is falling into the void
        if (playerY < VOID_Y_LEVEL + TRIGGER_DISTANCE) {
            // Update severity based on how close to void
            double distanceToVoid = playerY - VOID_Y_LEVEL;

            if (distanceToVoid <= 1) {
                setSeverity(FailsafeSeverity.CRITICAL);
                setAction(FailsafeAction.DISCONNECT);
            } else if (distanceToVoid <= 3) {
                setSeverity(FailsafeSeverity.HIGH);
                setAction(FailsafeAction.STOP);
            } else {
                setSeverity(FailsafeSeverity.MEDIUM);
                setAction(FailsafeAction.PAUSE);
            }

            return true;
        }

        return false;
    }
}
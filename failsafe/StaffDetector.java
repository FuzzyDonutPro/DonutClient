package com.donut.client.failsafe;

import com.donut.client.failsafe.FailsafeManager.FailsafeAction;
import com.donut.client.failsafe.FailsafeManager.FailsafeSeverity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Arrays;
import java.util.List;

public class StaffDetector extends BaseFailsafe {
    private static final List<String> STAFF_RANKS = Arrays.asList(
            "[ADMIN]", "[GM]", "[MOD]", "[HELPER]", "[YOUTUBE]", "[YOUTUBER]"
    );

    private double detectionRadius = 50.0;

    public StaffDetector() {
        super("Staff Detector", FailsafeSeverity.CRITICAL, FailsafeAction.DISCONNECT);
    }

    @Override
    public boolean check() {
        if (!enabled || mc.player == null || mc.world == null) {
            return false;
        }

        // Check all nearby players for staff ranks
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            // Check if within detection radius
            if (mc.player.squaredDistanceTo(player) > detectionRadius * detectionRadius) {
                continue;
            }

            String displayName = player.getDisplayName().getString();

            // Check if display name contains any staff rank
            for (String rank : STAFF_RANKS) {
                if (displayName.contains(rank)) {
                    return true;
                }
            }
        }

        return false;
    }

    public double getDetectionRadius() {
        return detectionRadius;
    }

    public void setDetectionRadius(double radius) {
        this.detectionRadius = radius;
    }
}
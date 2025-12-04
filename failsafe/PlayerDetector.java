package com.donut.client.failsafe;

import com.donut.client.failsafe.FailsafeManager.FailsafeAction;
import com.donut.client.failsafe.FailsafeManager.FailsafeSeverity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerDetector extends BaseFailsafe {
    private double detectionRadius = 15.0;
    private final Set<String> ignoreList;

    public PlayerDetector() {
        super("Player Detector", FailsafeSeverity.MEDIUM, FailsafeAction.PAUSE);
        this.ignoreList = new HashSet<>();
    }

    @Override
    public boolean check() {
        if (!enabled || mc.player == null || mc.world == null) {
            return false;
        }

        // Get all nearby players
        List<PlayerEntity> nearbyPlayers = mc.world.getPlayers().stream()
                .filter(player -> player != mc.player)
                .filter(player -> !ignoreList.contains(player.getGameProfile().getName()))
                .filter(player -> mc.player.squaredDistanceTo(player) <= detectionRadius * detectionRadius)
                .collect(Collectors.toList());

        return !nearbyPlayers.isEmpty();
    }

    public void ignorePlayer(String username) {
        ignoreList.add(username);
    }

    public void unignorePlayer(String username) {
        ignoreList.remove(username);
    }

    public void clearIgnoreList() {
        ignoreList.clear();
    }

    public List<String> getNearbyPlayerNames() {
        if (mc.player == null || mc.world == null) {
            return List.of();
        }

        return mc.world.getPlayers().stream()
                .filter(player -> player != mc.player)
                .filter(player -> !ignoreList.contains(player.getGameProfile().getName()))
                .filter(player -> mc.player.squaredDistanceTo(player) <= detectionRadius * detectionRadius)
                .map(player -> player.getGameProfile().getName())
                .collect(Collectors.toList());
    }

    public double getDetectionRadius() {
        return detectionRadius;
    }

    public void setDetectionRadius(double radius) {
        this.detectionRadius = radius;
    }
}
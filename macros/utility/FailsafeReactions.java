package com.donut.client.macros.utility;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public class FailsafeReactions extends Macro {

    private static FailsafeReactions instance;

    private final MinecraftClient mc;
    private final Random random = new Random();
    private FailsafeState state = FailsafeState.MONITORING;
    private long failsafeStartTime = 0;
    private double playerDetectionRadius = 15.0;
    private long failsafeDuration = 5000;
    private int failsafesTriggered = 0;

    public enum FailsafeState {
        MONITORING, TRIGGERED, REACTING
    }

    public enum FailsafeType {
        PLAYER_NEARBY, STAFF_DETECTED, RANDOM_CHECK
    }

    public FailsafeReactions() {
        super("Failsafe Reactions", "Anti-ban failsafe");
        this.mc = MinecraftClient.getInstance();
        instance = this;
    }

    public static FailsafeReactions getInstance() {
        if (instance == null) {
            instance = new FailsafeReactions();
        }
        return instance;
    }

    @Override
    public void start() {
        state = FailsafeState.MONITORING;
        System.out.println("[Failsafe] Initialized");
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        switch (state) {
            case MONITORING:
                checkForPlayers();
                randomCheck();
                break;
            case TRIGGERED:
                handleFailsafe();
                break;
            case REACTING:
                performRandomMovement();
                checkFailsafeComplete();
                break;
        }
    }

    private void checkForPlayers() {
        List<AbstractClientPlayerEntity> nearbyPlayers = mc.world.getPlayers();
        for (AbstractClientPlayerEntity player : nearbyPlayers) {
            if (player == mc.player) continue;
            double distance = mc.player.getPos().distanceTo(player.getPos());
            if (distance < playerDetectionRadius) {
                triggerFailsafe(FailsafeType.PLAYER_NEARBY);
                return;
            }
        }
    }

    private void randomCheck() {
        if (random.nextDouble() < 0.001) {
            triggerFailsafe(FailsafeType.RANDOM_CHECK);
        }
    }

    public void triggerFailsafe(FailsafeType type) {
        state = FailsafeState.TRIGGERED;
        failsafeStartTime = System.currentTimeMillis();
        failsafesTriggered++;
        System.out.println("[Failsafe] TRIGGERED: " + type);
    }

    public void triggerRandomReaction(String reason) {
        System.out.println("[Failsafe] Random reaction: " + reason);
        triggerFailsafe(FailsafeType.STAFF_DETECTED);
    }

    private void handleFailsafe() {
        System.out.println("[Failsafe] Reacting...");
        state = FailsafeState.REACTING;
    }

    private void performRandomMovement() {
        if (mc.player == null) return;

        long elapsed = System.currentTimeMillis() - failsafeStartTime;
        float progress = (float) elapsed / failsafeDuration;

        if (progress < 0.3f) {
            lookAround();
        } else if (progress < 0.6f) {
            walkRandomly();
        } else {
            if (random.nextFloat() < 0.1f) {
                mc.player.jump();
            }
        }
    }

    private void checkFailsafeComplete() {
        long elapsed = System.currentTimeMillis() - failsafeStartTime;
        if (elapsed >= failsafeDuration) {
            System.out.println("[Failsafe] Complete");
            state = FailsafeState.MONITORING;
        }
    }

    private void lookAround() {
        float yaw = mc.player.getYaw() + (random.nextFloat() - 0.5f) * 10;
        float pitch = mc.player.getPitch() + (random.nextFloat() - 0.5f) * 5;
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private void spinAround() {
        long elapsed = System.currentTimeMillis() - failsafeStartTime;
        float progress = (float) elapsed / failsafeDuration;
        float targetYaw = mc.player.getYaw() + (360 * progress);
        mc.player.setYaw(targetYaw);
    }

    private void walkRandomly() {
        float angle = random.nextFloat() * 360;
        double distance = 0.1;
        double x = mc.player.getX() + Math.cos(angle) * distance;
        double z = mc.player.getZ() + Math.sin(angle) * distance;
        Vec3d target = new Vec3d(x, mc.player.getY(), z);
        Vec3d current = mc.player.getPos();
        Vec3d dir = target.subtract(current).normalize();
        double yaw = Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        mc.player.setYaw((float) yaw);
    }
}
package com.donut.client.macros.rift;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * RiftEyeCollector - Collects eyes in the Rift
 * Used for accessing special areas
 */
public class RiftEyeCollector extends Macro {

    private final MinecraftClient mc;

    // State
    private EyeState state = EyeState.IDLE;
    private int currentEyeIndex = 0;
    private List<RiftEye> eyes = new ArrayList<>();

    // Settings
    private boolean autoCollect = true;
    private EyeType targetEyeType = EyeType.ALL;

    // Statistics
    private int eyesCollected = 0;
    private Map<EyeType, Integer> eyesByType = new HashMap<>();

    public enum EyeState {
        IDLE, NAVIGATING, COLLECTING, CHECKING
    }

    public enum EyeType {
        ALL,            // Collect all types
        YELLOW,         // Yellow eyes (Dreadfarm)
        BLUE,           // Blue eyes (Stillgore)
        RED,            // Red eyes (Colosseum)
        GREEN           // Green eyes (Living Cave)
    }

    public RiftEyeCollector() {
        super("Rift Eye Collector", "Collects eyes throughout the Rift");
        this.mc = MinecraftClient.getInstance();

        // Initialize counters
        for (EyeType type : EyeType.values()) {
            if (type != EyeType.ALL) {
                eyesByType.put(type, 0);
            }
        }

        // Load eye locations
        loadEyeLocations();
    }

    @Override
    public void start() {
        state = EyeState.IDLE;
        currentEyeIndex = 0;
        eyesCollected = 0;
        System.out.println("[Eye Collector] Initialized - " + eyes.size() + " eyes loaded");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Eye Collector] Starting collection...");
        state = EyeState.NAVIGATING;
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                break;
            case NAVIGATING:
                navigateToEye();
                break;
            case COLLECTING:
                collectEye();
                break;
            case CHECKING:
                checkCompletion();
                break;
        }
    }

    /**
     * Load all eye locations
     */
    private void loadEyeLocations() {
        // Yellow eyes (Dreadfarm) - 4 eyes
        eyes.add(new RiftEye("Dreadfarm Barn Eye", new BlockPos(25, 78, -130), EyeType.YELLOW));
        eyes.add(new RiftEye("Dreadfarm Field Eye", new BlockPos(35, 72, -125), EyeType.YELLOW));
        eyes.add(new RiftEye("Dreadfarm House Eye", new BlockPos(30, 80, -120), EyeType.YELLOW));
        eyes.add(new RiftEye("Dreadfarm Well Eye", new BlockPos(38, 74, -115), EyeType.YELLOW));

        // Blue eyes (Stillgore Château) - 5 eyes
        eyes.add(new RiftEye("Château Entrance Eye", new BlockPos(150, 82, 200), EyeType.BLUE));
        eyes.add(new RiftEye("Château Ballroom Eye", new BlockPos(160, 88, 210), EyeType.BLUE));
        eyes.add(new RiftEye("Château Library Eye", new BlockPos(155, 92, 205), EyeType.BLUE));
        eyes.add(new RiftEye("Château Tower Eye", new BlockPos(165, 102, 215), EyeType.BLUE));
        eyes.add(new RiftEye("Château Garden Eye", new BlockPos(145, 80, 195), EyeType.BLUE));

        // Red eyes (Colosseum) - 3 eyes
        eyes.add(new RiftEye("Colosseum Arena Eye", new BlockPos(0, 77, 300), EyeType.RED));
        eyes.add(new RiftEye("Colosseum Stands Eye", new BlockPos(10, 87, 310), EyeType.RED));
        eyes.add(new RiftEye("Colosseum Underground Eye", new BlockPos(5, 67, 305), EyeType.RED));

        // Green eyes (Living Cave) - 4 eyes
        eyes.add(new RiftEye("Cave Entrance Eye", new BlockPos(100, 52, -200), EyeType.GREEN));
        eyes.add(new RiftEye("Cave Depths Eye", new BlockPos(110, 47, -210), EyeType.GREEN));
        eyes.add(new RiftEye("Cave Secret Eye", new BlockPos(105, 42, -205), EyeType.GREEN));
        eyes.add(new RiftEye("Cave Crystal Eye", new BlockPos(115, 50, -215), EyeType.GREEN));

        System.out.println("[Eye Collector] Loaded " + eyes.size() + " eye locations");
    }

    /**
     * Navigate to next eye
     */
    private void navigateToEye() {
        if (currentEyeIndex >= eyes.size()) {
            System.out.println("[Eye Collector] All eyes collected!");
            state = EyeState.CHECKING;
            return;
        }

        RiftEye eye = eyes.get(currentEyeIndex);

        // Skip if not target type
        if (targetEyeType != EyeType.ALL && eye.type != targetEyeType) {
            currentEyeIndex++;
            return;
        }

        if (mc.player == null) return;

        double distance = mc.player.getPos().distanceTo(eye.position.toCenterPos());

        if (distance <= 3.0) {
            // Close enough to collect
            state = EyeState.COLLECTING;
        } else {
            // Navigate to eye
            lookAt(eye.position.toCenterPos());
            System.out.println("[Eye Collector] Navigating to: " + eye.name + " (" + String.format("%.1f", distance) + "m)");
            // TODO: Use pathfinding
        }
    }

    /**
     * Collect eye
     */
    private void collectEye() {
        RiftEye eye = eyes.get(currentEyeIndex);

        System.out.println("[Eye Collector] Collecting: " + eye.name);

        // TODO: Right click on eye entity

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Mark as collected
        eyesByType.put(eye.type, eyesByType.get(eye.type) + 1);
        eyesCollected++;

        System.out.println("[Eye Collector] Collected " + eye.type + " eye! (" + eyesCollected + "/" + eyes.size() + ")");

        // Move to next eye
        currentEyeIndex++;
        state = EyeState.NAVIGATING;
    }

    /**
     * Check completion
     */
    private void checkCompletion() {
        System.out.println("[Eye Collector] Collection complete!");
        System.out.println("[Eye Collector] Total collected: " + eyesCollected + "/" + eyes.size());

        for (EyeType type : EyeType.values()) {
            if (type != EyeType.ALL) {
                System.out.println("[Eye Collector] " + type + " eyes: " + eyesByType.get(type));
            }
        }

        state = EyeState.IDLE;
        onDisable();
    }

    /**
     * Look at position
     */
    private void lookAt(Vec3d pos) {
        if (mc.player == null) return;

        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = pos.subtract(eyes).normalize();

        double yaw = Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        double pitch = -Math.toDegrees(Math.asin(dir.y));

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    /**
     * Get status info
     */
    public String getStatusInfo() {
        String currentEye = currentEyeIndex < eyes.size() ? eyes.get(currentEyeIndex).name : "Complete";
        return String.format("%s | Current: %s | Collected: %d/%d",
                state, currentEye, eyesCollected, eyes.size());
    }

    /**
     * Get progress percentage
     */
    public double getProgress() {
        return (double) eyesCollected / eyes.size() * 100;
    }

    // Getters/Setters
    public void setAutoCollect(boolean auto) {
        this.autoCollect = auto;
    }

    public void setTargetEyeType(EyeType type) {
        this.targetEyeType = type;
    }

    public Map<EyeType, Integer> getEyesByType() {
        return new HashMap<>(eyesByType);
    }

    /**
     * Rift Eye class
     */
    public static class RiftEye {
        public final String name;
        public final BlockPos position;
        public final EyeType type;

        public RiftEye(String name, BlockPos position, EyeType type) {
            this.name = name;
            this.position = position;
            this.type = type;
        }

        @Override
        public String toString() {
            return name + " (" + type + ") at " + position;
        }
    }
}
package com.donut.client.macros.hunting;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * HuntingMacro - Universal animal hunting macro
 * Tracks, lures, and kills animals
 */
public class HuntingMacro extends Macro {

    private final MinecraftClient mc;
    private final AnimalTracker tracker;
    private final BaitManager baitManager;

    // State
    private HuntingState state = HuntingState.IDLE;
    private AnimalTracker.TrackedAnimal currentTarget = null;
    private AnimalTracker.AnimalType targetAnimalType = AnimalTracker.AnimalType.COW;

    // Settings
    private boolean useBait = true;
    private boolean autoRotate = true;
    private int killRadius = 4;

    // Statistics
    private int animalsKilled = 0;
    private Map<AnimalTracker.AnimalType, Integer> killsByType = new HashMap<>();

    public enum HuntingState {
        IDLE, SCANNING, MOVING_TO_ANIMAL, USING_BAIT, KILLING, COLLECTING
    }

    public HuntingMacro() {
        super("Hunting", "Universal animal hunting macro");
        this.mc = MinecraftClient.getInstance();
        this.tracker = new AnimalTracker();
        this.baitManager = new BaitManager();

        // Initialize kill counters
        for (AnimalTracker.AnimalType type : AnimalTracker.AnimalType.values()) {
            killsByType.put(type, 0);
        }
    }

    @Override
    public void start() {
        state = HuntingState.IDLE;
        currentTarget = null;
        animalsKilled = 0;
        System.out.println("[Hunting] Initialized - Target: " + targetAnimalType);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Hunting] Starting hunting...");
        state = HuntingState.SCANNING;
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        // Update tracking
        tracker.update();

        switch (state) {
            case IDLE:
                break;
            case SCANNING:
                scanForAnimals();
                break;
            case MOVING_TO_ANIMAL:
                moveToAnimal();
                break;
            case USING_BAIT:
                useBait();
                break;
            case KILLING:
                killAnimal();
                break;
            case COLLECTING:
                collectDrops();
                break;
        }
    }

    /**
     * Scan for target animals
     */
    private void scanForAnimals() {
        AnimalTracker.TrackedAnimal nearest = tracker.getNearestAnimal(targetAnimalType);

        if (nearest == null) {
            System.out.println("[Hunting] No " + targetAnimalType + " found nearby");
            return;
        }

        currentTarget = nearest;
        System.out.println("[Hunting] Target found: " + nearest);
        state = HuntingState.MOVING_TO_ANIMAL;
    }

    /**
     * Move to animal
     */
    private void moveToAnimal() {
        if (currentTarget == null || !currentTarget.isAlive()) {
            currentTarget = null;
            state = HuntingState.SCANNING;
            return;
        }

        if (mc.player == null) return;

        double distance = mc.player.distanceTo(currentTarget.entity);

        if (distance <= killRadius) {
            // Close enough
            if (useBait && baitManager.hasBait(baitManager.getCurrentBait())) {
                state = HuntingState.USING_BAIT;
            } else {
                state = HuntingState.KILLING;
            }
        } else {
            // Move towards animal
            if (autoRotate) {
                lookAt(currentTarget.entity.getPos());
            }
            // TODO: Use pathfinding
        }
    }

    /**
     * Use bait on animal
     */
    private void useBait() {
        if (currentTarget == null || !currentTarget.isAlive()) {
            currentTarget = null;
            state = HuntingState.SCANNING;
            return;
        }

        // Get best bait for this animal
        BaitManager.BaitType bait = baitManager.getBestBait(currentTarget.type);

        if (bait != null) {
            System.out.println("[Hunting] Using " + bait.name + "...");
            baitManager.useBait(bait);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        state = HuntingState.KILLING;
    }

    /**
     * Kill animal
     */
    private void killAnimal() {
        if (currentTarget == null || !currentTarget.isAlive()) {
            // Animal killed
            if (currentTarget != null) {
                killsByType.put(currentTarget.type, killsByType.get(currentTarget.type) + 1);
                animalsKilled++;

                System.out.println("[Hunting] Killed " + currentTarget.type + "! Total: " + animalsKilled);

                // Record spawn location
                tracker.recordSpawn(currentTarget.position);
            }

            currentTarget = null;
            state = HuntingState.COLLECTING;
            return;
        }

        // Look at animal
        if (autoRotate) {
            lookAt(currentTarget.entity.getPos());
        }

        // Attack
        attack();
    }

    /**
     * Collect drops
     */
    private void collectDrops() {
        System.out.println("[Hunting] Collecting drops...");

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Scan for next animal
        state = HuntingState.SCANNING;
    }

    /**
     * Attack animal
     */
    private void attack() {
        // TODO: Simulate left click
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
        return String.format("%s | Target: %s | Killed: %d | Rate: %.1f/hr",
                state, targetAnimalType, animalsKilled, getKillsPerHour());
    }

    /**
     * Get kills per hour
     */
    public double getKillsPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) animalsKilled / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setTargetAnimalType(AnimalTracker.AnimalType type) {
        this.targetAnimalType = type;
    }

    public void setUseBait(boolean use) {
        this.useBait = use;
    }

    public void setAutoRotate(boolean auto) {
        this.autoRotate = auto;
    }

    public void setKillRadius(int radius) {
        this.killRadius = radius;
    }

    public AnimalTracker getTracker() {
        return tracker;
    }

    public BaitManager getBaitManager() {
        return baitManager;
    }

    public Map<AnimalTracker.AnimalType, Integer> getKillsByType() {
        return new HashMap<>(killsByType);
    }
}
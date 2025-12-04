package com.donut.client.macros.fishing;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * FishingMacro - Advanced fishing with location support and auto-walk
 */
public class FishingMacro extends Macro {

    private final MinecraftClient mc;
    private final AutoFisher autoFisher;
    private final FishingLocationManager locationManager;

    // State
    private FishingMode mode = FishingMode.IDLE;
    private FishingLocationManager.FishingLocation targetLocation = null;
    private int walkPathIndex = 0;

    // Settings
    private boolean autoWalk = true;
    private boolean autoRotate = true;
    private int walkSpeed = 1; // 1 = walking, 2 = sprinting

    public enum FishingMode {
        IDLE, WALKING_TO_SPOT, FISHING, RETURNING
    }

    public FishingMacro() {
        super("Fishing", "Advanced fishing with location management");
        this.mc = MinecraftClient.getInstance();
        this.autoFisher = new AutoFisher();
        this.locationManager = new FishingLocationManager();
    }

    @Override
    public void start() {
        mode = FishingMode.IDLE;
        walkPathIndex = 0;
        System.out.println("[Fishing Macro] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (targetLocation == null) {
            System.out.println("[Fishing Macro] No location set! Use setLocation() first.");
            onDisable();
            return;
        }

        System.out.println("[Fishing Macro] Starting at: " + targetLocation.name);

        if (autoWalk && !targetLocation.walkPath.isEmpty()) {
            mode = FishingMode.WALKING_TO_SPOT;
        } else {
            mode = FishingMode.FISHING;
            startFishing();
        }
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        switch (mode) {
            case IDLE:
                break;
            case WALKING_TO_SPOT:
                walkToSpot();
                break;
            case FISHING:
                autoFisher.onTick();
                break;
            case RETURNING:
                walkReturn();
                break;
        }
    }

    /**
     * Walk to fishing spot
     */
    private void walkToSpot() {
        if (targetLocation.walkPath.isEmpty()) {
            // No path, just go to spot
            mode = FishingMode.FISHING;
            startFishing();
            return;
        }

        if (mc.player == null) return;

        // Follow walk path
        BlockPos currentWaypoint = targetLocation.walkPath.get(walkPathIndex);
        double distance = mc.player.getPos().distanceTo(currentWaypoint.toCenterPos());

        if (distance < 2.0) {
            // Reached waypoint
            walkPathIndex++;

            if (walkPathIndex >= targetLocation.walkPath.size()) {
                // Reached fishing spot
                System.out.println("[Fishing Macro] Arrived at fishing spot!");
                mode = FishingMode.FISHING;
                startFishing();
            }
        } else {
            // Move to waypoint
            if (autoRotate) {
                lookAt(currentWaypoint.toCenterPos());
            }
            // TODO: Use pathfinding to move
        }
    }

    /**
     * Start fishing
     */
    private void startFishing() {
        System.out.println("[Fishing Macro] Starting to fish...");

        // Look at fishing spot
        if (autoRotate) {
            lookAtFishingSpot();
        }

        // Enable auto fisher
        autoFisher.start();
        autoFisher.onEnable();
    }

    /**
     * Look at fishing spot
     */
    private void lookAtFishingSpot() {
        if (targetLocation != null) {
            lookAt(targetLocation.fishingSpot.toCenterPos());
        }
    }

    /**
     * Walk return path
     */
    private void walkReturn() {
        // Walk back along path in reverse
        if (walkPathIndex <= 0) {
            System.out.println("[Fishing Macro] Returned to start");
            mode = FishingMode.IDLE;
            return;
        }

        if (mc.player == null) return;

        BlockPos currentWaypoint = targetLocation.walkPath.get(walkPathIndex);
        double distance = mc.player.getPos().distanceTo(currentWaypoint.toCenterPos());

        if (distance < 2.0) {
            walkPathIndex--;
        } else {
            if (autoRotate) {
                lookAt(currentWaypoint.toCenterPos());
            }
            // TODO: Use pathfinding
        }
    }

    /**
     * Look at position
     */
    private void lookAt(net.minecraft.util.math.Vec3d pos) {
        if (mc.player == null) return;

        net.minecraft.util.math.Vec3d eyes = mc.player.getEyePos();
        net.minecraft.util.math.Vec3d dir = pos.subtract(eyes).normalize();

        double yaw = Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        double pitch = -Math.toDegrees(Math.asin(dir.y));

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    /**
     * Set fishing location
     */
    public void setLocation(String locationName) {
        FishingLocationManager.FishingLocation location = locationManager.getLocation(locationName);

        if (location == null) {
            System.out.println("[Fishing Macro] Location not found: " + locationName);
            return;
        }

        this.targetLocation = location;
        locationManager.setCurrentLocation(location);
        System.out.println("[Fishing Macro] Set location: " + location.name);
    }

    /**
     * Set fishing location directly
     */
    public void setLocation(FishingLocationManager.FishingLocation location) {
        this.targetLocation = location;
        locationManager.setCurrentLocation(location);
    }

    /**
     * Get available locations
     */
    public List<String> getAvailableLocations() {
        List<String> names = new ArrayList<>();
        for (FishingLocationManager.FishingLocation loc : locationManager.getAllLocations()) {
            names.add(loc.name);
        }
        return names;
    }

    /**
     * Create custom location at current position
     */
    public void createCustomLocation(String name) {
        if (mc.player == null) return;

        BlockPos currentPos = mc.player.getBlockPos();
        FishingLocationManager.FishingLocation location =
                locationManager.createCustomLocation(name, currentPos, Collections.emptyList());

        System.out.println("[Fishing Macro] Created custom location: " + name);
    }

    /**
     * Get status info (not an override)
     */
    public String getStatusInfo() {
        String locationName = targetLocation != null ? targetLocation.name : "None";
        return String.format("%s | Location: %s | %s",
                mode, locationName, autoFisher.getStatusInfo());
    }

    // Getters/Setters
    public void setAutoWalk(boolean auto) {
        this.autoWalk = auto;
    }

    public void setAutoRotate(boolean auto) {
        this.autoRotate = auto;
    }

    public void setWalkSpeed(int speed) {
        this.walkSpeed = speed;
    }

    public AutoFisher getAutoFisher() {
        return autoFisher;
    }

    public FishingLocationManager getLocationManager() {
        return locationManager;
    }
}
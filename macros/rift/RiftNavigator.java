package com.donut.client.macros.rift;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * RiftNavigator - Navigation system for the Rift
 * Handles waypoints, routes, and pathfinding in the Rift dimension
 */
public class RiftNavigator {

    private final MinecraftClient mc;

    // Waypoints
    private Map<String, BlockPos> waypoints = new HashMap<>();
    private List<BlockPos> currentRoute = new ArrayList<>();
    private int currentWaypointIndex = 0;

    // Settings
    private double waypointReachDistance = 5.0;

    public RiftNavigator() {
        this.mc = MinecraftClient.getInstance();
        loadWaypoints();
    }

    /**
     * Load all Rift waypoints
     */
    private void loadWaypoints() {
        // Main areas
        waypoints.put("Wizard Tower", new BlockPos(-43, 90, 73));
        waypoints.put("Village Plaza", new BlockPos(-100, 70, -50));
        waypoints.put("West Village", new BlockPos(-150, 73, -20));
        waypoints.put("Dreadfarm", new BlockPos(30, 75, -120));
        waypoints.put("Stillgore Château", new BlockPos(150, 80, 200));
        waypoints.put("Black Lagoon", new BlockPos(-200, 65, 150));
        waypoints.put("Colosseum", new BlockPos(0, 75, 300));
        waypoints.put("Living Cave", new BlockPos(100, 50, -200));
        waypoints.put("Mirrorverse", new BlockPos(-300, 80, -300));

        // Utility locations
        waypoints.put("Barry Center", new BlockPos(-85, 72, -40));
        waypoints.put("Motes Grubber", new BlockPos(-102, 70, -48));
        waypoints.put("Rift Portal", new BlockPos(-90, 70, -55));

        System.out.println("[Rift Navigator] Loaded " + waypoints.size() + " waypoints");
    }

    /**
     * Navigate to waypoint
     */
    public boolean navigateTo(String waypointName) {
        BlockPos pos = waypoints.get(waypointName);

        if (pos == null) {
            System.out.println("[Rift Navigator] Waypoint not found: " + waypointName);
            return false;
        }

        return navigateTo(pos);
    }

    /**
     * Navigate to position
     */
    public boolean navigateTo(BlockPos pos) {
        if (mc.player == null) return false;

        double distance = mc.player.getPos().distanceTo(pos.toCenterPos());

        if (distance <= waypointReachDistance) {
            System.out.println("[Rift Navigator] Reached destination!");
            return true;
        }

        // Look at destination
        lookAt(pos.toCenterPos());

        // TODO: Use pathfinding to move

        return false;
    }

    /**
     * Set route
     */
    public void setRoute(List<BlockPos> route) {
        this.currentRoute = new ArrayList<>(route);
        this.currentWaypointIndex = 0;
        System.out.println("[Rift Navigator] Route set with " + route.size() + " waypoints");
    }

    /**
     * Follow current route
     */
    public boolean followRoute() {
        if (currentRoute.isEmpty() || currentWaypointIndex >= currentRoute.size()) {
            return true; // Route complete
        }

        BlockPos waypoint = currentRoute.get(currentWaypointIndex);

        if (navigateTo(waypoint)) {
            currentWaypointIndex++;
            System.out.println("[Rift Navigator] Waypoint reached: " + (currentWaypointIndex) + "/" + currentRoute.size());

            if (currentWaypointIndex >= currentRoute.size()) {
                System.out.println("[Rift Navigator] Route complete!");
                return true;
            }
        }

        return false;
    }

    /**
     * Get nearest waypoint
     */
    public String getNearestWaypoint() {
        if (mc.player == null) return null;

        String nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, BlockPos> entry : waypoints.entrySet()) {
            double distance = mc.player.getPos().distanceTo(entry.getValue().toCenterPos());

            if (distance < nearestDistance) {
                nearest = entry.getKey();
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    /**
     * Get distance to waypoint
     */
    public double getDistance(String waypointName) {
        if (mc.player == null) return -1;

        BlockPos pos = waypoints.get(waypointName);
        if (pos == null) return -1;

        return mc.player.getPos().distanceTo(pos.toCenterPos());
    }

    /**
     * Add custom waypoint
     */
    public void addWaypoint(String name, BlockPos pos) {
        waypoints.put(name, pos);
        System.out.println("[Rift Navigator] Added waypoint: " + name);
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
     * Get all waypoints
     */
    public Map<String, BlockPos> getWaypoints() {
        return new HashMap<>(waypoints);
    }

    /**
     * Get current route
     */
    public List<BlockPos> getCurrentRoute() {
        return new ArrayList<>(currentRoute);
    }

    /**
     * Get route progress
     */
    public double getRouteProgress() {
        if (currentRoute.isEmpty()) return 100.0;
        return (double) currentWaypointIndex / currentRoute.size() * 100;
    }

    // Getters/Setters
    public void setWaypointReachDistance(double distance) {
        this.waypointReachDistance = distance;
    }
}
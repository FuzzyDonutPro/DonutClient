package com.donut.client.macros.rift;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * RiftTimecharmFarmer - Farms Timecharms in the Rift
 * Kills specific mobs that drop timecharms
 */
public class RiftTimecharmFarmer extends Macro {

    private final MinecraftClient mc;
    private final RiftNavigator navigator;

    // State
    private FarmerState state = FarmerState.IDLE;
    private Entity currentTarget = null;
    private String farmLocation = "Colosseum";

    // Settings
    private boolean autoRotateFarms = true;
    private int killRadius = 4;
    private int searchRadius = 40;

    // Statistics
    private int mobsKilled = 0;
    private int timecharmsObtained = 0;
    private Map<TimecharmType, Integer> charmsByType = new HashMap<>();

    public enum FarmerState {
        IDLE, MOVING_TO_FARM, FARMING, COLLECTING
    }

    public enum TimecharmType {
        RIFT_TIMECHARM,             // Basic timecharm
        SUPREME_TIMECHARM,          // Rare
        DIMENSIONAL_TIMECHARM       // Very Rare
    }

    public enum FarmLocation {
        COLOSSEUM("Colosseum", "Oubliette Guard"),
        STILLGORE("Stillgore Château", "Vampire Thrall"),
        BLACK_LAGOON("Black Lagoon", "Lych"),
        LIVING_CAVE("Living Cave", "Wither Spectre");

        public final String name;
        public final String targetMob;

        FarmLocation(String name, String targetMob) {
            this.name = name;
            this.targetMob = targetMob;
        }
    }

    public RiftTimecharmFarmer() {
        super("Rift Timecharm Farmer", "Farms timecharms in the Rift");
        this.mc = MinecraftClient.getInstance();
        this.navigator = new RiftNavigator();

        // Initialize counters
        for (TimecharmType type : TimecharmType.values()) {
            charmsByType.put(type, 0);
        }
    }

    @Override
    public void start() {
        state = FarmerState.IDLE;
        currentTarget = null;
        mobsKilled = 0;
        timecharmsObtained = 0;
        System.out.println("[Timecharm Farmer] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Timecharm Farmer] Starting at: " + farmLocation);
        state = FarmerState.MOVING_TO_FARM;
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                break;
            case MOVING_TO_FARM:
                moveToFarm();
                break;
            case FARMING:
                farm();
                break;
            case COLLECTING:
                collect();
                break;
        }
    }

    /**
     * Move to farm location
     */
    private void moveToFarm() {
        if (navigator.navigateTo(farmLocation)) {
            System.out.println("[Timecharm Farmer] Arrived at " + farmLocation);
            state = FarmerState.FARMING;
        }
    }

    /**
     * Farm timecharms
     */
    private void farm() {
        // Find target mob
        Entity target = findTargetMob();

        if (target == null) {
            System.out.println("[Timecharm Farmer] No mobs found, waiting...");
            return;
        }

        currentTarget = target;

        if (mc.player == null) return;

        double distance = mc.player.distanceTo(target);

        if (distance <= killRadius) {
            // In range, attack
            lookAt(target.getPos());
            attack();

            // Check if dead
            if (!target.isAlive()) {
                mobsKilled++;
                System.out.println("[Timecharm Farmer] Mob killed! Total: " + mobsKilled);

                currentTarget = null;
                state = FarmerState.COLLECTING;
            }
        } else {
            // Move to mob
            lookAt(target.getPos());
            // TODO: Use pathfinding
        }
    }

    /**
     * Find target mob
     */
    private Entity findTargetMob() {
        if (mc.world == null || mc.player == null) return null;

        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (isTimecharmMob(entity)) {
                double distance = mc.player.distanceTo(entity);

                if (distance < searchRadius && distance < nearestDistance) {
                    nearest = entity;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    /**
     * Check if mob drops timecharms
     */
    private boolean isTimecharmMob(Entity entity) {
        String name = entity.getName().getString();

        return name.contains("Oubliette") ||
                name.contains("Vampire") ||
                name.contains("Lych") ||
                name.contains("Wither Spectre") ||
                name.contains("Blood Fiend") ||
                name.contains("Bacte");
    }

    /**
     * Collect drops
     */
    private void collect() {
        System.out.println("[Timecharm Farmer] Collecting drops...");

        // TODO: Check inventory for timecharms

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check for timecharm drop
        if (checkForTimecharm()) {
            TimecharmType type = getTimecharmType();
            charmsByType.put(type, charmsByType.get(type) + 1);
            timecharmsObtained++;

            System.out.println("[Timecharm Farmer] ★ TIMECHARM OBTAINED! ★ (" + type + ") Total: " + timecharmsObtained);
        }

        state = FarmerState.FARMING;
    }

    /**
     * Check for timecharm drop
     */
    private boolean checkForTimecharm() {
        // TODO: Check inventory
        // Random for now
        return Math.random() < 0.15; // 15% drop rate
    }

    /**
     * Get timecharm type
     */
    private TimecharmType getTimecharmType() {
        double roll = Math.random();

        if (roll < 0.02) return TimecharmType.DIMENSIONAL_TIMECHARM; // 2%
        if (roll < 0.15) return TimecharmType.SUPREME_TIMECHARM; // 13%
        return TimecharmType.RIFT_TIMECHARM; // 85%
    }

    /**
     * Attack mob
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
     * Change farm location
     */
    public void changeFarmLocation() {
        if (!autoRotateFarms) return;

        // Cycle through locations
        FarmLocation[] locations = FarmLocation.values();
        for (int i = 0; i < locations.length; i++) {
            if (locations[i].name.equals(farmLocation)) {
                // Go to next
                farmLocation = locations[(i + 1) % locations.length].name;
                System.out.println("[Timecharm Farmer] Switching to: " + farmLocation);
                state = FarmerState.MOVING_TO_FARM;
                return;
            }
        }
    }

    /**
     * Get status info
     */
    public String getStatusInfo() {
        return String.format("%s | Location: %s | Killed: %d | Charms: %d | Rate: %.1f/hr",
                state, farmLocation, mobsKilled, timecharmsObtained, getCharmsPerHour());
    }

    /**
     * Get charms per hour
     */
    public double getCharmsPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) timecharmsObtained / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setFarmLocation(String location) {
        this.farmLocation = location;
    }

    public void setAutoRotateFarms(boolean auto) {
        this.autoRotateFarms = auto;
    }

    public void setKillRadius(int radius) {
        this.killRadius = radius;
    }

    public void setSearchRadius(int radius) {
        this.searchRadius = radius;
    }

    public Map<TimecharmType, Integer> getCharmsByType() {
        return new HashMap<>(charmsByType);
    }

    public RiftNavigator getNavigator() {
        return navigator;
    }
}
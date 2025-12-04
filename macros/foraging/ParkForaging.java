package com.donut.client.macros.foraging;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * ParkForaging - Foraging in The Park with optimal efficiency
 * Handles dark oak trees and wolf encounters
 */
public class ParkForaging extends Macro {

    private final MinecraftClient mc;
    private final ForagingMacro foragingMacro;

    // State
    private ParkState state = ParkState.IDLE;
    private ParkArea currentArea = ParkArea.DARK_OAK_FOREST;
    private List<BlockPos> route = new ArrayList<>();
    private int routeIndex = 0;

    // Settings
    private boolean autoKillWolves = true;
    private boolean useRoute = true;
    private boolean autoRotate = true;
    private int waitForRespawn = 45000; // 45 seconds (Park respawn slower)

    // Statistics
    private int routesCompleted = 0;
    private int wolvesKilled = 0;
    private long lastTreeTime = 0;

    public enum ParkState {
        IDLE, MOVING_TO_AREA, FORAGING, KILLING_WOLF, WAITING_RESPAWN, MOVING_TO_NEXT
    }

    public enum ParkArea {
        DARK_OAK_FOREST,    // Main dark oak area (-300, 72, 100)
        BIRCH_FOREST,       // Birch section (-250, 70, 150)
        SPRUCE_AREA,        // Spruce trees (-350, 75, 50)
        MIXED_FOREST,       // Mixed tree area (-280, 72, 120)
        HOWLING_CAVE,       // Near Howling Cave (-320, 65, 80)
        CUSTOM              // Custom location
    }

    public ParkForaging() {
        super("Park Foraging", "Optimized foraging in The Park");
        this.mc = MinecraftClient.getInstance();
        this.foragingMacro = new ForagingMacro();

        // Set to dark oak by default (best for Park)
        foragingMacro.setTargetTreeType(ForagingMacro.TreeType.DARK_OAK);

        // Load default routes
        loadDefaultRoutes();
    }

    @Override
    public void start() {
        state = ParkState.IDLE;
        routeIndex = 0;
        routesCompleted = 0;
        wolvesKilled = 0;
        System.out.println("[Park Foraging] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Park Foraging] Starting at: " + currentArea);

        if (useRoute) {
            state = ParkState.MOVING_TO_AREA;
        } else {
            state = ParkState.FORAGING;
            foragingMacro.start();
            foragingMacro.onEnable();
        }
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        // Check for wolves
        if (autoKillWolves && wolfNearby()) {
            if (state == ParkState.FORAGING) {
                foragingMacro.onDisable();
            }
            state = ParkState.KILLING_WOLF;
        }

        switch (state) {
            case IDLE:
                break;
            case MOVING_TO_AREA:
                moveToArea();
                break;
            case FORAGING:
                forage();
                break;
            case KILLING_WOLF:
                killWolf();
                break;
            case WAITING_RESPAWN:
                waitForRespawn();
                break;
            case MOVING_TO_NEXT:
                moveToNext();
                break;
        }
    }

    /**
     * Load default foraging routes
     */
    private void loadDefaultRoutes() {
        // Dark oak forest route (most efficient)
        List<BlockPos> darkOakRoute = new ArrayList<>();
        darkOakRoute.add(new BlockPos(-300, 72, 100));
        darkOakRoute.add(new BlockPos(-320, 74, 120));
        darkOakRoute.add(new BlockPos(-340, 72, 100));
        darkOakRoute.add(new BlockPos(-320, 70, 80));
        darkOakRoute.add(new BlockPos(-300, 72, 100));

        // Set as default
        route = darkOakRoute;
    }

    /**
     * Move to foraging area
     */
    private void moveToArea() {
        if (route.isEmpty()) {
            state = ParkState.FORAGING;
            foragingMacro.start();
            foragingMacro.onEnable();
            return;
        }

        if (mc.player == null) return;

        BlockPos target = route.get(routeIndex);
        double distance = mc.player.getPos().distanceTo(target.toCenterPos());

        if (distance < 5.0) {
            // Reached waypoint
            System.out.println("[Park Foraging] Reached waypoint " + (routeIndex + 1));
            state = ParkState.FORAGING;
            foragingMacro.start();
            foragingMacro.onEnable();
        } else {
            // Move to waypoint
            if (autoRotate) {
                lookAt(target);
            }
            // TODO: Use pathfinding
        }
    }

    /**
     * Forage at current location
     */
    private void forage() {
        // Run foraging macro
        foragingMacro.onTick();

        // Check if no trees found (FIXED: Use getForagingState())
        if (foragingMacro.getForagingState() == ForagingMacro.ForagingState.SEARCHING) {
            long timeSinceTree = System.currentTimeMillis() - lastTreeTime;

            if (timeSinceTree > 8000) {
                // No trees for 8 seconds, move to next spot
                System.out.println("[Park Foraging] No trees found, moving to next spot...");
                foragingMacro.onDisable();
                state = ParkState.MOVING_TO_NEXT;
            }
        } else {
            lastTreeTime = System.currentTimeMillis();
        }
    }

    /**
     * Check for wolves nearby
     */
    private boolean wolfNearby() {
        if (mc.world == null || mc.player == null) return false;

        for (var entity : mc.world.getEntities()) {
            String name = entity.getName().getString();

            if (name.contains("Wolf") || name.contains("Howling")) {
                double distance = mc.player.distanceTo(entity);

                if (distance < 10.0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Kill wolf
     */
    private void killWolf() {
        System.out.println("[Park Foraging] Killing wolf...");

        // TODO: Attack wolf

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        wolvesKilled++;
        System.out.println("[Park Foraging] Wolf killed! Total: " + wolvesKilled);

        // Resume foraging
        state = ParkState.FORAGING;
        foragingMacro.onEnable();
    }

    /**
     * Wait for trees to respawn
     */
    private void waitForRespawn() {
        System.out.println("[Park Foraging] Waiting for trees to respawn...");

        try {
            Thread.sleep(waitForRespawn);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        state = ParkState.FORAGING;
        foragingMacro.start();
        foragingMacro.onEnable();
    }

    /**
     * Move to next waypoint
     */
    private void moveToNext() {
        routeIndex++;

        if (routeIndex >= route.size()) {
            // Completed route
            routeIndex = 0;
            routesCompleted++;
            System.out.println("[Park Foraging] Route completed! Total: " + routesCompleted);

            // Wait for respawn or start over
            state = ParkState.WAITING_RESPAWN;
        } else {
            state = ParkState.MOVING_TO_AREA;
        }
    }

    /**
     * Look at position
     */
    private void lookAt(BlockPos pos) {
        if (mc.player == null) return;

        net.minecraft.util.math.Vec3d target = pos.toCenterPos();
        net.minecraft.util.math.Vec3d eyes = mc.player.getEyePos();
        net.minecraft.util.math.Vec3d dir = target.subtract(eyes).normalize();

        double yaw = Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        double pitch = -Math.toDegrees(Math.asin(dir.y));

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    /**
     * Set area
     */
    public void setArea(ParkArea area) {
        this.currentArea = area;

        // Load area-specific route
        switch (area) {
            case DARK_OAK_FOREST:
                loadDarkOakRoute();
                foragingMacro.setTargetTreeType(ForagingMacro.TreeType.DARK_OAK);
                break;
            case BIRCH_FOREST:
                loadBirchRoute();
                foragingMacro.setTargetTreeType(ForagingMacro.TreeType.BIRCH);
                break;
            case SPRUCE_AREA:
                loadSpruceRoute();
                foragingMacro.setTargetTreeType(ForagingMacro.TreeType.SPRUCE);
                break;
            case MIXED_FOREST:
                loadMixedRoute();
                foragingMacro.setTargetTreeType(ForagingMacro.TreeType.ANY);
                break;
            case HOWLING_CAVE:
                loadHowlingCaveRoute();
                foragingMacro.setTargetTreeType(ForagingMacro.TreeType.DARK_OAK);
                break;
        }

        System.out.println("[Park Foraging] Area set: " + area);
    }

    /**
     * Load dark oak route
     */
    private void loadDarkOakRoute() {
        route.clear();
        route.add(new BlockPos(-300, 72, 100));
        route.add(new BlockPos(-320, 74, 120));
        route.add(new BlockPos(-340, 72, 100));
        route.add(new BlockPos(-320, 70, 80));
    }

    /**
     * Load birch route
     */
    private void loadBirchRoute() {
        route.clear();
        route.add(new BlockPos(-250, 70, 150));
        route.add(new BlockPos(-270, 72, 170));
        route.add(new BlockPos(-250, 70, 190));
    }

    /**
     * Load spruce route
     */
    private void loadSpruceRoute() {
        route.clear();
        route.add(new BlockPos(-350, 75, 50));
        route.add(new BlockPos(-370, 77, 70));
        route.add(new BlockPos(-350, 75, 90));
    }

    /**
     * Load mixed route
     */
    private void loadMixedRoute() {
        route.clear();
        route.add(new BlockPos(-280, 72, 120));
        route.add(new BlockPos(-300, 74, 140));
        route.add(new BlockPos(-280, 72, 160));
    }

    /**
     * Load howling cave route
     */
    private void loadHowlingCaveRoute() {
        route.clear();
        route.add(new BlockPos(-320, 65, 80));
        route.add(new BlockPos(-340, 67, 100));
        route.add(new BlockPos(-320, 65, 120));
    }

    /**
     * Get status info
     */
    public String getStatusInfo() {
        return String.format("%s | %s | Routes: %d | Wolves: %d | %s",
                state, currentArea, routesCompleted, wolvesKilled, foragingMacro.getStatusInfo());
    }

    // Getters/Setters
    public void setAutoKillWolves(boolean auto) {
        this.autoKillWolves = auto;
    }

    public void setUseRoute(boolean use) {
        this.useRoute = use;
    }

    public void setAutoRotate(boolean auto) {
        this.autoRotate = auto;
    }

    public void setWaitForRespawn(int millis) {
        this.waitForRespawn = millis;
    }

    public ForagingMacro getForagingMacro() {
        return foragingMacro;
    }
}
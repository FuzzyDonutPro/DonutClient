package com.donut.client.macros.foraging;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * HubForaging - Foraging in Hub with optimal routes
 * Focuses on Hub forest area with respawn detection
 */
public class HubForaging extends Macro {

    private final MinecraftClient mc;
    private final ForagingMacro foragingMacro;

    // State
    private HubState state = HubState.IDLE;
    private HubLocation currentLocation = HubLocation.FOREST;
    private List<BlockPos> route = new ArrayList<>();
    private int routeIndex = 0;

    // Settings
    private boolean useRoute = true;
    private boolean autoRotate = true;
    private int waitForRespawn = 30000; // 30 seconds

    // Statistics
    private int routesCompleted = 0;
    private long lastTreeTime = 0;

    public enum HubState {
        IDLE, MOVING_TO_LOCATION, FORAGING, WAITING_RESPAWN, MOVING_TO_NEXT
    }

    public enum HubLocation {
        FOREST,         // Main forest (-200, 70, -100)
        WEST_VILLAGE,   // West village trees (-350, 70, -50)
        MOUNTAIN,       // Mountain area (-150, 90, -200)
        BARN_AREA,      // Near barn (-80, 76, -240)
        CUSTOM          // Custom location
    }

    public HubForaging() {
        super("Hub Foraging", "Optimized foraging in Hub");
        this.mc = MinecraftClient.getInstance();
        this.foragingMacro = new ForagingMacro();

        // Load default routes
        loadDefaultRoutes();
    }

    @Override
    public void start() {
        state = HubState.IDLE;
        routeIndex = 0;
        routesCompleted = 0;
        System.out.println("[Hub Foraging] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Hub Foraging] Starting at: " + currentLocation);

        if (useRoute) {
            state = HubState.MOVING_TO_LOCATION;
        } else {
            state = HubState.FORAGING;
            foragingMacro.start();
            foragingMacro.onEnable();
        }
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        switch (state) {
            case IDLE:
                break;
            case MOVING_TO_LOCATION:
                moveToLocation();
                break;
            case FORAGING:
                forage();
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
        // Forest route
        List<BlockPos> forestRoute = new ArrayList<>();
        forestRoute.add(new BlockPos(-200, 70, -100));
        forestRoute.add(new BlockPos(-220, 72, -120));
        forestRoute.add(new BlockPos(-240, 70, -100));
        forestRoute.add(new BlockPos(-220, 68, -80));

        // Set as default
        route = forestRoute;
    }

    /**
     * Move to foraging location
     */
    private void moveToLocation() {
        if (route.isEmpty()) {
            state = HubState.FORAGING;
            foragingMacro.start();
            foragingMacro.onEnable();
            return;
        }

        if (mc.player == null) return;

        BlockPos target = route.get(routeIndex);
        double distance = mc.player.getPos().distanceTo(target.toCenterPos());

        if (distance < 5.0) {
            // Reached waypoint
            System.out.println("[Hub Foraging] Reached waypoint " + (routeIndex + 1));
            state = HubState.FORAGING;
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

            if (timeSinceTree > 5000) {
                // No trees for 5 seconds, move to next spot
                System.out.println("[Hub Foraging] No trees found, moving to next spot...");
                foragingMacro.onDisable();
                state = HubState.MOVING_TO_NEXT;
            }
        } else {
            lastTreeTime = System.currentTimeMillis();
        }
    }

    /**
     * Wait for trees to respawn
     */
    private void waitForRespawn() {
        System.out.println("[Hub Foraging] Waiting for trees to respawn...");

        try {
            Thread.sleep(waitForRespawn);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        state = HubState.FORAGING;
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
            System.out.println("[Hub Foraging] Route completed! Total: " + routesCompleted);

            // Wait for respawn or start over
            state = HubState.WAITING_RESPAWN;
        } else {
            state = HubState.MOVING_TO_LOCATION;
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
     * Set location
     */
    public void setLocation(HubLocation location) {
        this.currentLocation = location;

        // Load location-specific route
        switch (location) {
            case FOREST:
                loadForestRoute();
                break;
            case WEST_VILLAGE:
                loadWestVillageRoute();
                break;
            case MOUNTAIN:
                loadMountainRoute();
                break;
            case BARN_AREA:
                loadBarnRoute();
                break;
        }

        System.out.println("[Hub Foraging] Location set: " + location);
    }

    /**
     * Load forest route
     */
    private void loadForestRoute() {
        route.clear();
        route.add(new BlockPos(-200, 70, -100));
        route.add(new BlockPos(-220, 72, -120));
        route.add(new BlockPos(-240, 70, -100));
        route.add(new BlockPos(-220, 68, -80));
    }

    /**
     * Load west village route
     */
    private void loadWestVillageRoute() {
        route.clear();
        route.add(new BlockPos(-350, 70, -50));
        route.add(new BlockPos(-370, 72, -70));
        route.add(new BlockPos(-350, 70, -90));
    }

    /**
     * Load mountain route
     */
    private void loadMountainRoute() {
        route.clear();
        route.add(new BlockPos(-150, 90, -200));
        route.add(new BlockPos(-170, 95, -220));
        route.add(new BlockPos(-150, 92, -240));
    }

    /**
     * Load barn route
     */
    private void loadBarnRoute() {
        route.clear();
        route.add(new BlockPos(-80, 76, -240));
        route.add(new BlockPos(-100, 78, -260));
        route.add(new BlockPos(-80, 76, -280));
    }

    /**
     * Get status info
     */
    public String getStatusInfo() {
        return String.format("%s | %s | Routes: %d | %s",
                state, currentLocation, routesCompleted, foragingMacro.getStatusInfo());
    }

    // Getters/Setters
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
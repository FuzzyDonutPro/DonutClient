package com.donut.client.macros.fishing;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * WormFishing - Auto fish and kill worms with Hyperion
 * Solo and Party modes with loot share
 */
public class WormFishing extends Macro {

    private final MinecraftClient mc;
    private final AutoFisher autoFisher;

    // State
    private WormState state = WormState.IDLE;
    private Entity currentWorm = null;
    private WormMode mode = WormMode.SOLO;

    // Settings
    private boolean autoSwapWeapon = true;
    private boolean useLootShare = false;
    private int wormDetectionRadius = 15;

    // Statistics
    private int wormsKilled = 0;
    private int flamingosCaught = 0;
    private long coinsEarned = 0;
    private Map<WormType, Integer> wormKills = new HashMap<>();

    public enum WormState {
        IDLE, FISHING, WORM_DETECTED, SWAPPING_WEAPON, KILLING_WORM,
        SWAPPING_BACK, COLLECTING, SHARING_LOOT
    }

    public enum WormMode {
        SOLO,   // Solo farming
        PARTY   // Party with loot share
    }

    public enum WormType {
        LAVA_WORM,      // Lava worm (common)
        FLAMING_WORM,   // Flaming worm (rare)
        FIRE_EEL,       // Fire eel (uncommon)
        TAURUS          // Taurus (very rare)
    }

    public WormFishing() {
        super("Worm Fishing", "Auto fish and kill worms with weapon");
        this.mc = MinecraftClient.getInstance();
        this.autoFisher = new AutoFisher();

        // Initialize counters
        for (WormType type : WormType.values()) {
            wormKills.put(type, 0);
        }
    }

    @Override
    public void start() {
        state = WormState.IDLE;
        currentWorm = null;
        wormsKilled = 0;
        flamingosCaught = 0;
        System.out.println("[Worm Fishing] Initialized - Mode: " + mode);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // Check requirements
        if (!hasLavaRod()) {
            System.out.println("[Worm Fishing] ERROR: Need Lava Rod!");
            onDisable();
            return;
        }

        if (!hasWeapon()) {
            System.out.println("[Worm Fishing] WARNING: No weapon found!");
        }

        System.out.println("[Worm Fishing] Starting worm fishing...");
        state = WormState.FISHING;
        autoFisher.start();
        autoFisher.onEnable();
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                break;
            case FISHING:
                fish();
                break;
            case WORM_DETECTED:
                handleWormDetected();
                break;
            case SWAPPING_WEAPON:
                swapWeapon();
                break;
            case KILLING_WORM:
                killWorm();
                break;
            case SWAPPING_BACK:
                swapBack();
                break;
            case COLLECTING:
                collectLoot();
                break;
            case SHARING_LOOT:
                shareLoot();
                break;
        }
    }

    /**
     * Fish and detect worms
     */
    private void fish() {
        // Run auto fisher
        autoFisher.onTick();

        // Check for worm spawn
        Entity worm = findWorm();

        if (worm != null) {
            WormType type = identifyWorm(worm);
            System.out.println("[Worm Fishing] WORM DETECTED: " + type);

            if (type == WormType.FLAMING_WORM || type == WormType.TAURUS) {
                System.out.println("[Worm Fishing] ★★★ RARE WORM! ★★★");
            }

            currentWorm = worm;
            autoFisher.onDisable();
            state = WormState.WORM_DETECTED;
        }
    }

    /**
     * Check for lava fishing rod
     */
    private boolean hasLavaRod() {
        if (mc.player == null) return false;

        String itemName = mc.player.getMainHandStack().getName().getString();
        return itemName.contains("Lava") || itemName.contains("Inferno");
    }

    /**
     * Check for weapon (Hyperion, etc.)
     */
    private boolean hasWeapon() {
        // TODO: Check hotbar for Hyperion, Valkyrie, etc.
        return true;
    }

    /**
     * Find worm entity
     */
    private Entity findWorm() {
        if (mc.world == null || mc.player == null) return null;

        for (Entity entity : mc.world.getEntities()) {
            if (isWorm(entity)) {
                double distance = mc.player.distanceTo(entity);

                if (distance < wormDetectionRadius) {
                    return entity;
                }
            }
        }

        return null;
    }

    /**
     * Check if entity is worm
     */
    private boolean isWorm(Entity entity) {
        String name = entity.getName().getString();
        return name.contains("Worm") ||
                name.contains("Fire Eel") ||
                name.contains("Taurus");
    }

    /**
     * Identify worm type
     */
    private WormType identifyWorm(Entity worm) {
        String name = worm.getName().getString();

        if (name.contains("Flaming Worm")) return WormType.FLAMING_WORM;
        if (name.contains("Fire Eel")) return WormType.FIRE_EEL;
        if (name.contains("Taurus")) return WormType.TAURUS;
        return WormType.LAVA_WORM;
    }

    /**
     * Handle worm detected
     */
    private void handleWormDetected() {
        if (currentWorm == null || !currentWorm.isAlive()) {
            // Worm despawned
            currentWorm = null;
            state = WormState.FISHING;
            autoFisher.onEnable();
            return;
        }

        // Swap to weapon if enabled
        if (autoSwapWeapon) {
            state = WormState.SWAPPING_WEAPON;
        } else {
            state = WormState.KILLING_WORM;
        }
    }

    /**
     * Swap to weapon
     */
    private void swapWeapon() {
        System.out.println("[Worm Fishing] Swapping to weapon...");

        // TODO: Find Hyperion in hotbar and swap to it
        // Press number key 1-9

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        state = WormState.KILLING_WORM;
    }

    /**
     * Kill worm
     */
    private void killWorm() {
        if (currentWorm == null || !currentWorm.isAlive()) {
            // Worm killed
            WormType type = identifyWorm(currentWorm);
            wormKills.put(type, wormKills.get(type) + 1);
            wormsKilled++;

            System.out.println("[Worm Fishing] Worm killed! Total: " + wormsKilled);

            currentWorm = null;

            if (autoSwapWeapon) {
                state = WormState.SWAPPING_BACK;
            } else {
                state = WormState.COLLECTING;
            }
            return;
        }

        // Look at worm
        lookAt(currentWorm.getPos());

        // Attack with weapon
        attackWithWeapon();
    }

    /**
     * Attack with weapon (Hyperion, etc.)
     */
    private void attackWithWeapon() {
        // TODO: Right click for Hyperion ability
        // Or left click for melee
    }

    /**
     * Swap back to fishing rod
     */
    private void swapBack() {
        System.out.println("[Worm Fishing] Swapping back to rod...");

        // TODO: Swap back to fishing rod slot

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        state = WormState.COLLECTING;
    }

    /**
     * Collect loot
     */
    private void collectLoot() {
        System.out.println("[Worm Fishing] Collecting loot...");

        // TODO: Collect nearby items

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check for loot share
        if (mode == WormMode.PARTY && useLootShare) {
            state = WormState.SHARING_LOOT;
        } else {
            state = WormState.FISHING;
            autoFisher.onEnable();
        }
    }

    /**
     * Share loot with party
     */
    private void shareLoot() {
        System.out.println("[Worm Fishing] Sharing loot...");

        // TODO: Use loot share mechanic
        // Split coins with party members

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        state = WormState.FISHING;
        autoFisher.onEnable();
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
     * Get status info (not an override)
     */
    public String getStatusInfo() {
        return String.format("%s | %s | Worms: %d | Flaming: %d | Rate: %.1f/hr",
                state, mode, wormsKilled,
                wormKills.get(WormType.FLAMING_WORM),
                getWormsPerHour());
    }

    /**
     * Get worms per hour
     */
    public double getWormsPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) wormsKilled / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setMode(WormMode mode) {
        this.mode = mode;
    }

    public void setAutoSwapWeapon(boolean auto) {
        this.autoSwapWeapon = auto;
    }

    public void setUseLootShare(boolean use) {
        this.useLootShare = use;
    }

    public void setWormDetectionRadius(int radius) {
        this.wormDetectionRadius = radius;
    }

    public Map<WormType, Integer> getWormKills() {
        return new HashMap<>(wormKills);
    }
}
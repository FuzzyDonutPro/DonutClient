package com.donut.client.macros.hunting;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * MythologicalHunting - Diana mythological creature hunting
 * Tracks burrows, digs, and kills mythological mobs
 */
public class MythologicalHunting extends Macro {

    private final MinecraftClient mc;

    // State
    private MythState state = MythState.IDLE;
    private BlockPos currentBurrow = null;
    private Entity currentCreature = null;

    // Settings
    private boolean autoWarp = true;
    private boolean autoSell = false;
    private int burrowSearchRadius = 50;

    // Statistics
    private int burrowsDug = 0;
    private int creaturesKilled = 0;
    private Map<CreatureType, Integer> killsByType = new HashMap<>();

    public enum MythState {
        IDLE, SEARCHING_BURROW, MOVING_TO_BURROW, DIGGING, CREATURE_SPAWNED,
        FIGHTING, COLLECTING, WARPING
    }

    public enum CreatureType {
        MINOS_CHAMPION,     // Common
        MINOS_HUNTER,       // Uncommon
        MINOS_INQUISITOR,   // Rare (alert!)
        SIAMESE_LYNX,       // Common
        MINOTAUR,           // Uncommon
        GAIA_CONSTRUCT      // Rare (alert!)
    }

    public MythologicalHunting() {
        super("Mythological Hunting", "Diana mythological creature hunting");
        this.mc = MinecraftClient.getInstance();

        // Initialize kill counters
        for (CreatureType type : CreatureType.values()) {
            killsByType.put(type, 0);
        }
    }

    @Override
    public void start() {
        state = MythState.IDLE;
        currentBurrow = null;
        currentCreature = null;
        burrowsDug = 0;
        creaturesKilled = 0;
        System.out.println("[Myth Hunting] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Myth Hunting] Starting mythological hunting...");
        state = MythState.SEARCHING_BURROW;
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                break;
            case SEARCHING_BURROW:
                searchForBurrow();
                break;
            case MOVING_TO_BURROW:
                moveToBurrow();
                break;
            case DIGGING:
                digBurrow();
                break;
            case CREATURE_SPAWNED:
                handleCreatureSpawn();
                break;
            case FIGHTING:
                fightCreature();
                break;
            case COLLECTING:
                collectLoot();
                break;
            case WARPING:
                warpToHub();
                break;
        }
    }

    /**
     * Search for burrows
     */
    private void searchForBurrow() {
        if (mc.world == null || mc.player == null) return;

        BlockPos nearestBurrow = null;
        double nearestDistance = Double.MAX_VALUE;

        BlockPos playerPos = mc.player.getBlockPos();

        // Scan for burrow particles
        // TODO: Detect brown particle effects that indicate burrows

        // For now, scan for coarse dirt blocks (burrow indicator)
        for (int x = -burrowSearchRadius; x <= burrowSearchRadius; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -burrowSearchRadius; z <= burrowSearchRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    // TODO: Check if position has burrow particles
                    if (isBurrowLocation(pos)) {
                        double distance = mc.player.getPos().distanceTo(pos.toCenterPos());

                        if (distance < nearestDistance) {
                            nearestBurrow = pos;
                            nearestDistance = distance;
                        }
                    }
                }
            }
        }

        if (nearestBurrow != null) {
            currentBurrow = nearestBurrow;
            System.out.println("[Myth Hunting] Found burrow at: " + nearestBurrow);
            state = MythState.MOVING_TO_BURROW;
        } else {
            System.out.println("[Myth Hunting] No burrows found, continuing search...");
        }
    }

    /**
     * Check if location is a burrow
     */
    private boolean isBurrowLocation(BlockPos pos) {
        // TODO: Check for brown particles
        // TODO: Check for coarse dirt
        return false;
    }

    /**
     * Move to burrow
     */
    private void moveToBurrow() {
        if (currentBurrow == null) {
            state = MythState.SEARCHING_BURROW;
            return;
        }

        if (mc.player == null) return;

        double distance = mc.player.getPos().distanceTo(currentBurrow.toCenterPos());

        if (distance <= 3.0) {
            // Close enough to dig
            System.out.println("[Myth Hunting] Reached burrow!");
            state = MythState.DIGGING;
        } else {
            // Move to burrow
            lookAt(currentBurrow.toCenterPos());
            // TODO: Use pathfinding
        }
    }

    /**
     * Dig burrow
     */
    private void digBurrow() {
        System.out.println("[Myth Hunting] Digging burrow...");

        // TODO: Right click with spade

        burrowsDug++;

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        state = MythState.CREATURE_SPAWNED;
    }

    /**
     * Handle creature spawn
     */
    private void handleCreatureSpawn() {
        // Check for spawned creature
        Entity creature = findMythCreature();

        if (creature != null) {
            currentCreature = creature;
            CreatureType type = identifyCreature(creature);

            System.out.println("[Myth Hunting] Creature spawned: " + type);

            if (type == CreatureType.MINOS_INQUISITOR || type == CreatureType.GAIA_CONSTRUCT) {
                System.out.println("[Myth Hunting] ★★★ RARE SPAWN! ★★★");
            }

            state = MythState.FIGHTING;
        } else {
            // No creature, just items
            state = MythState.COLLECTING;
        }
    }

    /**
     * Find mythological creature
     */
    private Entity findMythCreature() {
        if (mc.world == null || mc.player == null) return null;

        for (Entity entity : mc.world.getEntities()) {
            String name = entity.getName().getString();

            if (isMythCreature(name)) {
                double distance = mc.player.distanceTo(entity);

                if (distance < 20.0) {
                    return entity;
                }
            }
        }

        return null;
    }

    /**
     * Check if entity is myth creature
     */
    private boolean isMythCreature(String name) {
        return name.contains("Minos") ||
                name.contains("Lynx") ||
                name.contains("Minotaur") ||
                name.contains("Gaia Construct");
    }

    /**
     * Identify creature type
     */
    private CreatureType identifyCreature(Entity creature) {
        String name = creature.getName().getString();

        if (name.contains("Minos Champion")) return CreatureType.MINOS_CHAMPION;
        if (name.contains("Minos Hunter")) return CreatureType.MINOS_HUNTER;
        if (name.contains("Minos Inquisitor")) return CreatureType.MINOS_INQUISITOR;
        if (name.contains("Siamese Lynx")) return CreatureType.SIAMESE_LYNX;
        if (name.contains("Minotaur")) return CreatureType.MINOTAUR;
        if (name.contains("Gaia Construct")) return CreatureType.GAIA_CONSTRUCT;

        return CreatureType.MINOS_CHAMPION;
    }

    /**
     * Fight creature
     */
    private void fightCreature() {
        if (currentCreature == null || !currentCreature.isAlive()) {
            // Creature killed
            if (currentCreature != null) {
                CreatureType type = identifyCreature(currentCreature);
                killsByType.put(type, killsByType.get(type) + 1);
                creaturesKilled++;

                System.out.println("[Myth Hunting] Killed " + type + "! Total: " + creaturesKilled);
            }

            currentCreature = null;
            state = MythState.COLLECTING;
            return;
        }

        // Look at creature
        lookAt(currentCreature.getPos());

        // Attack
        attack();
    }

    /**
     * Collect loot
     */
    private void collectLoot() {
        System.out.println("[Myth Hunting] Collecting loot...");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Search for next burrow
        currentBurrow = null;
        state = MythState.SEARCHING_BURROW;
    }

    /**
     * Warp to hub
     */
    private void warpToHub() {
        System.out.println("[Myth Hunting] Warping to hub...");

        // TODO: Use /warp hub command

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        state = MythState.SEARCHING_BURROW;
    }

    /**
     * Attack creature
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
        return String.format("%s | Burrows: %d | Creatures: %d | Rate: %.1f/hr",
                state, burrowsDug, creaturesKilled, getBurrowsPerHour());
    }

    /**
     * Get burrows per hour
     */
    public double getBurrowsPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) burrowsDug / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setAutoWarp(boolean auto) {
        this.autoWarp = auto;
    }

    public void setAutoSell(boolean auto) {
        this.autoSell = auto;
    }

    public void setBurrowSearchRadius(int radius) {
        this.burrowSearchRadius = radius;
    }

    public Map<CreatureType, Integer> getKillsByType() {
        return new HashMap<>(killsByType);
    }
}
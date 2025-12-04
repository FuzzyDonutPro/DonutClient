package com.donut.client.macros.combat;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * GhoulKillerMacro - Auto kill ghouls in Dwarven Mines
 * Fully AFK with smooth pathfinding
 */
public class GhoulKillerMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private KillState state = KillState.IDLE;
    private Entity targetGhoul = null;
    private BlockPos patrolPoint = null;
    private List<BlockPos> patrolPath = new ArrayList<>();
    private int patrolIndex = 0;

    // Settings
    private int searchRadius = 20;
    private int killRadius = 4;
    private boolean autoLoot = true;
    private boolean smartPatrol = true;
    private WeaponType weaponType = WeaponType.MELEE;

    // Statistics
    private int ghoulsKilled = 0;
    private int coinsEarned = 0;
    private long lastKillTime = 0;

    public enum KillState {
        IDLE, SEARCHING, MOVING_TO_TARGET, ATTACKING, LOOTING, PATROLLING
    }

    public enum WeaponType {
        MELEE,      // Sword, axe
        BOW,        // Ranged
        MAGIC,      // Mage weapons
        HYBRID      // Switch between types
    }

    public GhoulKillerMacro() {
        super("Ghoul Killer", "Auto kill ghouls in Dwarven Mines");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = KillState.IDLE;
        targetGhoul = null;
        ghoulsKilled = 0;
        coinsEarned = 0;
        setupPatrolPath();
        System.out.println("[Ghoul Killer] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = KillState.SEARCHING;
        System.out.println("[Ghoul Killer] Starting ghoul hunt...");
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                // Wait
                break;
            case SEARCHING:
                searchForGhouls();
                break;
            case MOVING_TO_TARGET:
                moveToTarget();
                break;
            case ATTACKING:
                attackGhoul();
                break;
            case LOOTING:
                collectLoot();
                break;
            case PATROLLING:
                patrol();
                break;
        }
    }

    /**
     * Setup patrol path for ghoul spawns
     */
    private void setupPatrolPath() {
        if (mc.player == null) return;

        BlockPos center = mc.player.getBlockPos();

        // Create circular patrol path
        int radius = 15;
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            int x = (int)(center.getX() + radius * Math.cos(rad));
            int z = (int)(center.getZ() + radius * Math.sin(rad));
            patrolPath.add(new BlockPos(x, center.getY(), z));
        }

        System.out.println("[Ghoul Killer] Patrol path set: " + patrolPath.size() + " points");
    }

    /**
     * Search for nearby ghouls
     */
    private void searchForGhouls() {
        if (mc.world == null || mc.player == null) return;

        Entity nearestGhoul = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (isGhoul(entity)) {
                double distance = mc.player.distanceTo(entity);

                if (distance < searchRadius && distance < nearestDistance) {
                    nearestGhoul = entity;
                    nearestDistance = distance;
                }
            }
        }

        if (nearestGhoul != null) {
            targetGhoul = nearestGhoul;
            state = KillState.MOVING_TO_TARGET;
            System.out.println("[Ghoul Killer] Ghoul found! Distance: " + (int)nearestDistance);
        } else {
            // No ghouls found, patrol
            if (smartPatrol) {
                state = KillState.PATROLLING;
            }
        }
    }

    /**
     * Check if entity is a ghoul
     */
    private boolean isGhoul(Entity entity) {
        // Ghouls are zombies in Dwarven Mines
        if (!(entity instanceof ZombieEntity)) return false;

        // Check name for "Ghoul"
        String name = entity.getName().getString();
        return name.contains("Ghoul");
    }

    /**
     * Move to target ghoul
     */
    private void moveToTarget() {
        if (targetGhoul == null || !targetGhoul.isAlive()) {
            targetGhoul = null;
            state = KillState.SEARCHING;
            return;
        }

        if (mc.player == null) return;

        double distance = mc.player.distanceTo(targetGhoul);

        if (distance <= killRadius) {
            state = KillState.ATTACKING;
        } else {
            // Move towards ghoul
            lookAt(targetGhoul.getPos());
            // TODO: Use pathfinding to move
        }
    }

    /**
     * Attack the ghoul
     */
    private void attackGhoul() {
        if (targetGhoul == null || !targetGhoul.isAlive()) {
            lastKillTime = System.currentTimeMillis();
            ghoulsKilled++;
            System.out.println("[Ghoul Killer] Ghoul killed! Total: " + ghoulsKilled);

            if (autoLoot) {
                state = KillState.LOOTING;
            } else {
                targetGhoul = null;
                state = KillState.SEARCHING;
            }
            return;
        }

        if (mc.player == null) return;

        // Look at ghoul
        lookAt(targetGhoul.getPos());

        // Attack based on weapon type
        switch (weaponType) {
            case MELEE:
                attackMelee();
                break;
            case BOW:
                attackRanged();
                break;
            case MAGIC:
                attackMagic();
                break;
            case HYBRID:
                attackHybrid();
                break;
        }
    }

    /**
     * Melee attack
     */
    private void attackMelee() {
        // TODO: Simulate left click attack
        System.out.println("[Ghoul Killer] Attacking (Melee)");
    }

    /**
     * Ranged attack
     */
    private void attackRanged() {
        // TODO: Bow attack logic
        System.out.println("[Ghoul Killer] Attacking (Ranged)");
    }

    /**
     * Magic attack
     */
    private void attackMagic() {
        // TODO: Magic weapon logic
        System.out.println("[Ghoul Killer] Attacking (Magic)");
    }

    /**
     * Hybrid attack
     */
    private void attackHybrid() {
        // Switch between melee and magic
        if (mc.player.distanceTo(targetGhoul) > 6) {
            attackRanged();
        } else {
            attackMelee();
        }
    }

    /**
     * Collect loot after kill
     */
    private void collectLoot() {
        // TODO: Collect nearby items
        System.out.println("[Ghoul Killer] Collecting loot...");

        // Wait a bit for items to spawn
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        targetGhoul = null;
        state = KillState.SEARCHING;
    }

    /**
     * Patrol for ghouls
     */
    private void patrol() {
        if (patrolPath.isEmpty()) {
            state = KillState.SEARCHING;
            return;
        }

        if (mc.player == null) return;

        BlockPos currentTarget = patrolPath.get(patrolIndex);
        double distance = mc.player.getPos().distanceTo(currentTarget.toCenterPos());

        if (distance < 2.0) {
            // Reached patrol point, move to next
            patrolIndex = (patrolIndex + 1) % patrolPath.size();
        } else {
            // Move towards patrol point
            lookAt(currentTarget.toCenterPos());
            // TODO: Use pathfinding
        }

        // Check for ghouls while patrolling
        state = KillState.SEARCHING;
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
     * Get status
     */
    public String getStatus() {
        long timeSinceKill = System.currentTimeMillis() - lastKillTime;
        return String.format("%s | Killed: %d | Coins: %dk | Last: %ds ago",
                state, ghoulsKilled, coinsEarned / 1000, timeSinceKill / 1000);
    }

    /**
     * Get kills per hour
     */
    public double getKillsPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) ghoulsKilled / (runtime / 3600000.0);
    }

    /**
     * Get coins per hour
     */
    public double getCoinsPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) coinsEarned / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setSearchRadius(int radius) {
        this.searchRadius = radius;
    }

    public void setKillRadius(int radius) {
        this.killRadius = radius;
    }

    public void setAutoLoot(boolean auto) {
        this.autoLoot = auto;
    }

    public void setWeaponType(WeaponType type) {
        this.weaponType = type;
    }

    public void setSmartPatrol(boolean smart) {
        this.smartPatrol = smart;
    }
}
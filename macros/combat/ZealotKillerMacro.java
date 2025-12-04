package com.donut.client.macros.combat;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.math.Vec3d;

/**
 * ZealotKillerMacro - Auto kill zealots in The End
 * Special Zealot (blue) detection
 */
public class ZealotKillerMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private KillState state = KillState.IDLE;
    private Entity targetZealot = null;
    private boolean specialZealot = false;

    // Settings
    private int searchRadius = 25;
    private boolean prioritizeSpecial = true;
    private boolean autoLoot = true;
    private boolean eyeHunter = true; // Hunt for Summoning Eyes

    // Statistics
    private int zealotsKilled = 0;
    private int specialZealotsKilled = 0;
    private int summoningEyes = 0;
    private long coinsEarned = 0;

    public enum KillState {
        IDLE, SEARCHING, MOVING, ATTACKING, LOOTING
    }

    public ZealotKillerMacro() {
        super("Zealot Killer", "Auto kill zealots and hunt Summoning Eyes");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = KillState.IDLE;
        targetZealot = null;
        zealotsKilled = 0;
        specialZealotsKilled = 0;
        summoningEyes = 0;
        System.out.println("[Zealot Killer] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = KillState.SEARCHING;
        System.out.println("[Zealot Killer] Starting zealot hunt...");
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                break;
            case SEARCHING:
                searchForZealots();
                break;
            case MOVING:
                moveToTarget();
                break;
            case ATTACKING:
                attackZealot();
                break;
            case LOOTING:
                collectLoot();
                break;
        }
    }

    /**
     * Search for zealots
     */
    private void searchForZealots() {
        if (mc.world == null || mc.player == null) return;

        Entity nearestZealot = null;
        Entity nearestSpecial = null;
        double nearestDistance = Double.MAX_VALUE;
        double nearestSpecialDistance = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (isZealot(entity)) {
                double distance = mc.player.distanceTo(entity);

                if (isSpecialZealot(entity)) {
                    if (distance < searchRadius && distance < nearestSpecialDistance) {
                        nearestSpecial = entity;
                        nearestSpecialDistance = distance;
                    }
                } else {
                    if (distance < searchRadius && distance < nearestDistance) {
                        nearestZealot = entity;
                        nearestDistance = distance;
                    }
                }
            }
        }

        // Prioritize special zealots
        if (prioritizeSpecial && nearestSpecial != null) {
            targetZealot = nearestSpecial;
            specialZealot = true;
            state = KillState.MOVING;
            System.out.println("[Zealot Killer] SPECIAL ZEALOT FOUND!");
        } else if (nearestZealot != null) {
            targetZealot = nearestZealot;
            specialZealot = false;
            state = KillState.MOVING;
        }
    }

    /**
     * Check if entity is zealot
     */
    private boolean isZealot(Entity entity) {
        if (!(entity instanceof EndermanEntity)) return false;

        String name = entity.getName().getString();
        return name.contains("Zealot");
    }

    /**
     * Check if zealot is special (blue/glowing)
     */
    private boolean isSpecialZealot(Entity entity) {
        String name = entity.getName().getString();
        // Special zealots usually have a different name or tag
        return name.contains("Special") || name.contains("Rare") || entity.isGlowing();
    }

    /**
     * Move to target zealot
     */
    private void moveToTarget() {
        if (targetZealot == null || !targetZealot.isAlive()) {
            targetZealot = null;
            state = KillState.SEARCHING;
            return;
        }

        if (mc.player == null) return;

        double distance = mc.player.distanceTo(targetZealot);

        if (distance <= 4.0) {
            state = KillState.ATTACKING;
        } else {
            // Move towards zealot
            lookAt(targetZealot.getPos());
            // TODO: Use pathfinding
        }
    }

    /**
     * Attack zealot
     */
    private void attackZealot() {
        if (targetZealot == null || !targetZealot.isAlive()) {
            if (specialZealot) {
                specialZealotsKilled++;
                System.out.println("[Zealot Killer] SPECIAL ZEALOT KILLED!");

                // Check for summoning eye
                if (eyeHunter) {
                    checkForSummoningEye();
                }
            }

            zealotsKilled++;
            System.out.println("[Zealot Killer] Zealot killed! Total: " + zealotsKilled);

            if (autoLoot) {
                state = KillState.LOOTING;
            } else {
                targetZealot = null;
                state = KillState.SEARCHING;
            }
            return;
        }

        if (mc.player == null) return;

        // Look at zealot
        lookAt(targetZealot.getPos());

        // Attack
        attack();
    }

    /**
     * Check for summoning eye drop
     */
    private void checkForSummoningEye() {
        // TODO: Check for item entities nearby
        // Look for "Summoning Eye" item
        summoningEyes++;
        System.out.println("[Zealot Killer] ★ SUMMONING EYE DROPPED! ★ Total: " + summoningEyes);
    }

    /**
     * Collect loot
     */
    private void collectLoot() {
        System.out.println("[Zealot Killer] Collecting loot...");

        // Wait for items to spawn
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        targetZealot = null;
        state = KillState.SEARCHING;
    }

    /**
     * Attack current target
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
     * Get status
     */
    public String getStatus() {
        return String.format("%s | Killed: %d | Special: %d | Eyes: %d",
                state, zealotsKilled, specialZealotsKilled, summoningEyes);
    }

    /**
     * Get kills per hour
     */
    public double getKillsPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) zealotsKilled / (runtime / 3600000.0);
    }

    /**
     * Get eyes per hour
     */
    public double getEyesPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) summoningEyes / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setSearchRadius(int radius) {
        this.searchRadius = radius;
    }

    public void setPrioritizeSpecial(boolean prioritize) {
        this.prioritizeSpecial = prioritize;
    }

    public void setAutoLoot(boolean auto) {
        this.autoLoot = auto;
    }

    public void setEyeHunter(boolean hunt) {
        this.eyeHunter = hunt;
    }
}
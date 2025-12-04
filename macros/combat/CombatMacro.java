package com.donut.client.macros.combat;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * CombatMacro - General mob grinding and combat
 * Features: Smart targeting, auto-loot, combat AI, health management
 */
public class CombatMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private CombatState state = CombatState.IDLE;
    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0;

    // Targeting settings
    private TargetPriority targetPriority = TargetPriority.NEAREST;
    private double attackRange = 4.5;
    private double scanRadius = 30.0;
    private boolean targetPlayers = false;
    private boolean targetPassive = false;
    private boolean targetHostile = true;

    // Target filters
    private Set<String> targetWhitelist = new HashSet<>();
    private Set<String> targetBlacklist = new HashSet<>();

    // Combat settings
    private boolean autoSword = true;
    private boolean autoAbility = true;
    private boolean criticalHits = true;
    private boolean autoBlock = false;
    private int attacksPerSecond = 12;
    private boolean faceTarget = true;

    // Movement settings
    private boolean strafeMovement = true;
    private boolean jumpAttack = false;
    private boolean dodgeProjectiles = true;
    private boolean keepDistance = false;
    private double minDistance = 2.0;

    // Health management
    private boolean autoHeal = true;
    private double healThreshold = 14.0; // Hearts
    private boolean autoEat = true;
    private boolean autoPotion = true;
    private boolean retreatLowHealth = true;

    // Loot settings
    private boolean autoLoot = true;
    private double lootRange = 5.0;
    private boolean filterLoot = false;
    private Set<String> lootWhitelist = new HashSet<>();

    // Area control
    private boolean stayInArea = false;
    private BlockPos areaCenter = null;
    private int areaRadius = 20;
    private boolean stopIfOut = false;

    // Statistics
    private int killCount = 0;
    private int hitCount = 0;
    private int missCount = 0;
    private double damageDealt = 0;
    private double damageTaken = 0;

    // Internal
    private long strafeDirection = 1; // 1 or -1
    private long lastStrafeChange = 0;
    private int comboHits = 0;

    public enum CombatState {
        IDLE,          // No target
        SCANNING,      // Looking for target
        MOVING,        // Moving to target
        ATTACKING,     // In combat
        LOOTING,       // Collecting drops
        HEALING,       // Healing up
        DODGING        // Dodging projectiles
    }

    public enum TargetPriority {
        NEAREST,        // Closest target
        LOWEST_HEALTH,  // Weakest target
        HIGHEST_HEALTH, // Tankiest target
        HIGHEST_THREAT  // Most dangerous (health/distance ratio)
    }

    public CombatMacro() {
        super("Combat Macro", "General mob grinding and combat");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = CombatState.SCANNING;
        currentTarget = null;
        System.out.println("[Combat] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Combat] Starting...");
        System.out.println("[Combat] Priority: " + targetPriority);
        System.out.println("[Combat] Range: " + attackRange);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Combat] Stopped");
        printStatistics();
        stopMovement();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Check area bounds
        if (stayInArea && !isInArea()) {
            if (stopIfOut) {
                System.out.println("[Combat] Out of area, stopping");
                onDisable();
                return;
            }
            returnToArea();
        }

        // Check health
        if (autoHeal && mc.player.getHealth() < healThreshold) {
            state = CombatState.HEALING;
        }

        switch (state) {
            case IDLE:
            case SCANNING:
                scanForTargets();
                break;
            case MOVING:
                moveToTarget();
                break;
            case ATTACKING:
                attackTarget();
                break;
            case LOOTING:
                collectLoot();
                break;
            case HEALING:
                heal();
                break;
            case DODGING:
                dodge();
                break;
        }
    }

    /**
     * Scan for valid targets
     */
    private void scanForTargets() {
        if (mc.player == null || mc.world == null) return;

        List<LivingEntity> validTargets = new ArrayList<>();

        // Find all entities in range
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity == mc.player) continue;

            LivingEntity living = (LivingEntity) entity;

            // Check distance
            double distance = mc.player.distanceTo(living);
            if (distance > scanRadius) continue;

            // Check if valid target
            if (isValidTarget(living)) {
                validTargets.add(living);
            }
        }

        if (!validTargets.isEmpty()) {
            // Select best target based on priority
            currentTarget = selectBestTarget(validTargets);
            state = CombatState.MOVING;
            System.out.println("[Combat] Target acquired: " + currentTarget.getName().getString());
        }
    }

    /**
     * Check if entity is valid target
     */
    private boolean isValidTarget(LivingEntity entity) {
        // Check if dead
        if (!entity.isAlive() || entity.getHealth() <= 0) return false;

        // Check entity type
        if (entity instanceof PlayerEntity && !targetPlayers) return false;
        if (entity instanceof PassiveEntity && !targetPassive) return false;
        if (entity instanceof HostileEntity && !targetHostile) return false;

        String name = entity.getName().getString().toLowerCase();

        // Check whitelist (if not empty, only target whitelisted)
        if (!targetWhitelist.isEmpty()) {
            boolean whitelisted = false;
            for (String allowed : targetWhitelist) {
                if (name.contains(allowed.toLowerCase())) {
                    whitelisted = true;
                    break;
                }
            }
            if (!whitelisted) return false;
        }

        // Check blacklist
        for (String blocked : targetBlacklist) {
            if (name.contains(blocked.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Select best target from list
     */
    private LivingEntity selectBestTarget(List<LivingEntity> targets) {
        if (targets.isEmpty()) return null;

        switch (targetPriority) {
            case NEAREST:
                return targets.stream()
                        .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                        .orElse(null);

            case LOWEST_HEALTH:
                return targets.stream()
                        .min(Comparator.comparingDouble(LivingEntity::getHealth))
                        .orElse(null);

            case HIGHEST_HEALTH:
                return targets.stream()
                        .max(Comparator.comparingDouble(LivingEntity::getHealth))
                        .orElse(null);

            case HIGHEST_THREAT:
                return targets.stream()
                        .max(Comparator.comparingDouble(this::calculateThreat))
                        .orElse(null);
        }

        return targets.get(0);
    }

    /**
     * Calculate threat level
     */
    private double calculateThreat(LivingEntity entity) {
        double distance = mc.player.distanceTo(entity);
        double health = entity.getHealth();
        // Higher health + closer = higher threat
        return health / Math.max(distance, 1.0);
    }

    /**
     * Move to target
     */
    private void moveToTarget() {
        if (currentTarget == null || !currentTarget.isAlive()) {
            currentTarget = null;
            state = CombatState.SCANNING;
            return;
        }

        double distance = mc.player.distanceTo(currentTarget);

        // Check if in attack range
        if (distance <= attackRange) {
            state = CombatState.ATTACKING;
            stopMovement();
            return;
        }

        // Move towards target
        Vec3d targetPos = currentTarget.getPos();
        Vec3d playerPos = mc.player.getPos();

        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;

        // Set yaw
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        mc.player.setYaw(yaw);

        // Move forward
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(true);

            // Jump if blocked
            if (mc.player.horizontalCollision) {
                mc.options.jumpKey.setPressed(true);
            }
        }
    }

    /**
     * Attack target
     */
    private void attackTarget() {
        if (currentTarget == null || !currentTarget.isAlive()) {
            killCount++;
            currentTarget = null;

            if (autoLoot) {
                state = CombatState.LOOTING;
            } else {
                state = CombatState.SCANNING;
            }
            return;
        }

        // Check if target out of range
        double distance = mc.player.distanceTo(currentTarget);
        if (distance > attackRange + 1) {
            state = CombatState.MOVING;
            return;
        }

        // Face target
        if (faceTarget) {
            lookAtEntity(currentTarget);
        }

        // Strafe movement
        if (strafeMovement) {
            performStrafe();
        }

        // Keep distance
        if (keepDistance && distance < minDistance) {
            moveAway();
        }

        // Attack based on APS
        long now = System.currentTimeMillis();
        long attackInterval = 1000 / attacksPerSecond;

        if (now - lastAttackTime >= attackInterval) {
            performAttack();
            lastAttackTime = now;
        }
    }

    /**
     * Perform attack
     */
    private void performAttack() {
        if (mc.player == null || currentTarget == null) return;

        // Critical hit (sprint + hit)
        if (criticalHits && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }

        // Jump attack
        if (jumpAttack && mc.player.isOnGround()) {
            if (mc.options != null) {
                mc.options.jumpKey.setPressed(true);
            }
        }

        // TODO: Actual attack with packets
        System.out.println("[Combat] Attacking " + currentTarget.getName().getString());
        hitCount++;
        comboHits++;

        // Use ability after combo
        if (autoAbility && comboHits >= 5) {
            useWeaponAbility();
            comboHits = 0;
        }
    }

    /**
     * Look at entity
     */
    private void lookAtEntity(LivingEntity entity) {
        Vec3d targetPos = entity.getEyePos();
        Vec3d playerPos = mc.player.getEyePos();

        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    /**
     * Perform strafe movement
     */
    private void performStrafe() {
        long now = System.currentTimeMillis();

        // Change direction every second
        if (now - lastStrafeChange > 1000) {
            strafeDirection *= -1;
            lastStrafeChange = now;
        }

        if (mc.options != null) {
            if (strafeDirection > 0) {
                mc.options.rightKey.setPressed(true);
                mc.options.leftKey.setPressed(false);
            } else {
                mc.options.leftKey.setPressed(true);
                mc.options.rightKey.setPressed(false);
            }
        }
    }

    /**
     * Move away from target
     */
    private void moveAway() {
        if (mc.options != null) {
            mc.options.backKey.setPressed(true);
            mc.options.forwardKey.setPressed(false);
        }
    }

    /**
     * Use weapon ability
     */
    private void useWeaponAbility() {
        // TODO: Right-click weapon
        System.out.println("[Combat] Using weapon ability");
    }

    /**
     * Collect loot
     */
    private void collectLoot() {
        // TODO: Scan for item entities and collect
        System.out.println("[Combat] Collecting loot");
        state = CombatState.SCANNING;
    }

    /**
     * Heal player
     */
    private void heal() {
        System.out.println("[Combat] Healing...");

        // TODO: Eat food or use potion

        // Return to combat when healed
        if (mc.player.getHealth() >= healThreshold + 4) {
            state = CombatState.SCANNING;
        }
    }

    /**
     * Dodge projectiles
     */
    private void dodge() {
        // TODO: Detect and dodge projectiles
        state = CombatState.ATTACKING;
    }

    /**
     * Check if in area
     */
    private boolean isInArea() {
        if (!stayInArea || areaCenter == null) return true;

        BlockPos playerPos = mc.player.getBlockPos();
        return playerPos.getSquaredDistance(areaCenter) <= areaRadius * areaRadius;
    }

    /**
     * Return to area center
     */
    private void returnToArea() {
        // TODO: Pathfind back to area
    }

    /**
     * Stop all movement
     */
    private void stopMovement() {
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        }
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        long seconds = getRuntime() / 1000;
        long killsPerHour = seconds > 0 ? killCount * 3600 / seconds : 0;
        double accuracy = hitCount + missCount > 0 ?
                (double) hitCount / (hitCount + missCount) * 100 : 0;

        System.out.println("========================================");
        System.out.println("COMBAT STATISTICS");
        System.out.println("========================================");
        System.out.println("Kills: " + killCount + " (" + killsPerHour + "/hr)");
        System.out.println("Hits: " + hitCount + " | Misses: " + missCount);
        System.out.println("Accuracy: " + String.format("%.1f%%", accuracy));
        System.out.println("Damage Dealt: " + String.format("%.1f", damageDealt));
        System.out.println("Damage Taken: " + String.format("%.1f", damageTaken));
        System.out.println("Runtime: " + getRuntimeFormatted());
        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        if (currentTarget != null) {
            return String.format("ATTACKING | Kills: %d (%d/hr) | Accuracy: %.1f%% | HP: %.1f",
                    killCount, getKillsPerHour(), getAccuracy(), mc.player.getHealth());
        }
        return String.format("%s | Kills: %d | HP: %.1f", state, killCount, mc.player.getHealth());
    }

    private long getKillsPerHour() {
        long seconds = getRuntime() / 1000;
        return seconds > 0 ? killCount * 3600 / seconds : 0;
    }

    private double getAccuracy() {
        int total = hitCount + missCount;
        return total > 0 ? (double) hitCount / total * 100 : 0;
    }

    // ==================== SETTERS ====================

    public void setTargetPriority(TargetPriority priority) {
        this.targetPriority = priority;
    }

    public void setAttackRange(double range) {
        this.attackRange = range;
    }

    public void setScanRadius(double radius) {
        this.scanRadius = radius;
    }

    public void setTargetHostile(boolean target) {
        this.targetHostile = target;
    }

    public void setTargetPassive(boolean target) {
        this.targetPassive = target;
    }

    public void addTargetName(String name) {
        targetWhitelist.add(name);
    }

    public void addIgnoreName(String name) {
        targetBlacklist.add(name);
    }

    public void setAutoSword(boolean auto) {
        this.autoSword = auto;
    }

    public void setAutoAbility(boolean auto) {
        this.autoAbility = auto;
    }

    public void setCriticalHits(boolean crit) {
        this.criticalHits = crit;
    }

    public void setAttacksPerSecond(int aps) {
        this.attacksPerSecond = aps;
    }

    public void setStrafeMovement(boolean strafe) {
        this.strafeMovement = strafe;
    }

    public void setJumpAttack(boolean jump) {
        this.jumpAttack = jump;
    }

    public void setKeepDistance(boolean keep) {
        this.keepDistance = keep;
    }

    public void setAutoHeal(boolean auto) {
        this.autoHeal = auto;
    }

    public void setHealThreshold(double threshold) {
        this.healThreshold = threshold;
    }

    public void setRetreatLowHealth(boolean retreat) {
        this.retreatLowHealth = retreat;
    }

    public void setAutoLoot(boolean auto) {
        this.autoLoot = auto;
    }

    public void setStayInArea(boolean stay) {
        this.stayInArea = stay;
    }

    public void setAreaCenter(BlockPos center) {
        this.areaCenter = center;
    }

    public void setAreaRadius(int radius) {
        this.areaRadius = radius;
    }
}
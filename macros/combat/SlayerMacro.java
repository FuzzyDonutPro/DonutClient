package com.donut.client.macros.combat;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * SlayerMacro - Auto-completes slayer quests
 * Supports: All 6 slayer types with auto-quest, auto-kill, auto-loot
 */
public class SlayerMacro extends Macro {

    private final MinecraftClient mc;

    // State
    private SlayerState state = SlayerState.IDLE;
    private LivingEntity currentTarget = null;
    private LivingEntity bossEntity = null;

    // Settings
    private SlayerType slayerType = SlayerType.REVENANT;
    private int slayerTier = 4;
    private boolean autoStart = true;
    private boolean autoLoot = true;
    private boolean autoSell = false;
    private double attackRange = 4.5;
    private int maxQuests = 0; // 0 = infinite

    // Combat settings
    private boolean useAbilities = true;
    private boolean dodgeAttacks = true;
    private boolean autoArmor = true;

    // Statistics
    private int questsCompleted = 0;
    private int mobsKilled = 0;
    private int bossesKilled = 0;
    private long combatXP = 0;

    public enum SlayerState {
        IDLE,           // No quest active
        STARTING_QUEST, // Starting new quest
        KILLING_MOBS,   // Killing mobs for combat XP
        KILLING_BOSS,   // Fighting slayer boss
        LOOTING,        // Collecting drops
        SELLING         // Selling items
    }

    public enum SlayerType {
        REVENANT("Revenant Horror", "Zombie"),
        TARANTULA("Tarantula Broodfather", "Spider"),
        SVEN("Sven Packmaster", "Wolf"),
        VOIDGLOOM("Voidgloom Seraph", "Enderman"),
        INFERNO("Inferno Demonlord", "Blaze"),
        BLOODFIEND("Riftstalker Bloodfiend", "Vampire");

        public final String fullName;
        public final String mobType;

        SlayerType(String fullName, String mobType) {
            this.fullName = fullName;
            this.mobType = mobType;
        }
    }

    public SlayerMacro() {
        super("Slayer Macro", "Auto-completes slayer quests for all types");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        state = SlayerState.IDLE;
        currentTarget = null;
        bossEntity = null;
        System.out.println("[Slayer] Initialized - Type: " + slayerType.fullName);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Slayer] Starting...");
        System.out.println("[Slayer] Type: " + slayerType.fullName);
        System.out.println("[Slayer] Tier: " + slayerTier);

        if (autoStart) {
            state = SlayerState.STARTING_QUEST;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Slayer] Stopped");
        printStatistics();
        stopMovement();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Check if max quests reached
        if (maxQuests > 0 && questsCompleted >= maxQuests) {
            System.out.println("[Slayer] Max quests reached!");
            onDisable();
            return;
        }

        switch (state) {
            case IDLE:
                if (autoStart) {
                    state = SlayerState.STARTING_QUEST;
                }
                break;
            case STARTING_QUEST:
                startQuest();
                break;
            case KILLING_MOBS:
                killMobs();
                break;
            case KILLING_BOSS:
                killBoss();
                break;
            case LOOTING:
                collectLoot();
                break;
            case SELLING:
                sellItems();
                break;
        }
    }

    /**
     * Start slayer quest at NPC
     */
    private void startQuest() {
        System.out.println("[Slayer] Starting quest: " + slayerType.fullName + " T" + slayerTier);

        // TODO: Find slayer NPC
        // TODO: Click NPC and start quest

        // For now, simulate quest start
        state = SlayerState.KILLING_MOBS;
    }

    /**
     * Kill mobs for combat XP
     */
    private void killMobs() {
        // Check for boss spawn
        if (detectBoss()) {
            System.out.println("[Slayer] BOSS SPAWNED!");
            state = SlayerState.KILLING_BOSS;
            return;
        }

        // Find and attack slayer mobs
        if (currentTarget == null || !currentTarget.isAlive()) {
            currentTarget = findSlayerMob();

            if (currentTarget != null) {
                System.out.println("[Slayer] Target acquired: " + currentTarget.getName().getString());
            }
        }

        if (currentTarget != null) {
            attackTarget(currentTarget);
        } else {
            // No mobs found, scan area
            scanForMobs();
        }
    }

    /**
     * Find slayer mob
     */
    private LivingEntity findSlayerMob() {
        if (mc.world == null) return null;

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity == mc.player) continue;

            LivingEntity living = (LivingEntity) entity;
            String name = living.getName().getString().toLowerCase();

            // Check if it's a slayer mob
            if (!isSlayerMob(name)) continue;

            // Check if it's the boss (skip for now)
            if (isBoss(name)) continue;

            double distance = mc.player.distanceTo(living);
            if (distance < nearestDistance && distance < 30) {
                nearest = living;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    /**
     * Check if mob name matches slayer type
     */
    private boolean isSlayerMob(String name) {
        String type = slayerType.mobType.toLowerCase();

        // Check for slayer-specific names
        switch (slayerType) {
            case REVENANT:
                return name.contains("zombie") || name.contains("revenant");
            case TARANTULA:
                return name.contains("spider") || name.contains("tarantula");
            case SVEN:
                return name.contains("wolf") || name.contains("sven");
            case VOIDGLOOM:
                return name.contains("enderman") || name.contains("voidgloom");
            case INFERNO:
                return name.contains("blaze") || name.contains("inferno");
            case BLOODFIEND:
                return name.contains("vampire") || name.contains("bloodfiend");
        }

        return false;
    }

    /**
     * Detect boss spawn
     */
    private boolean detectBoss() {
        if (mc.world == null) return false;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;

            LivingEntity living = (LivingEntity) entity;
            String name = living.getName().getString();

            if (isBoss(name)) {
                bossEntity = living;
                return true;
            }
        }

        return false;
    }

    /**
     * Check if entity is boss
     */
    private boolean isBoss(String name) {
        // Boss names contain special symbols
        if (name.contains("⚡") || name.contains("❤") || name.contains("☠")) {
            return true;
        }

        // Check for boss names
        String lower = name.toLowerCase();
        switch (slayerType) {
            case REVENANT:
                return lower.contains("revenant horror");
            case TARANTULA:
                return lower.contains("tarantula broodfather");
            case SVEN:
                return lower.contains("sven packmaster");
            case VOIDGLOOM:
                return lower.contains("voidgloom seraph");
            case INFERNO:
                return lower.contains("inferno demonlord");
            case BLOODFIEND:
                return lower.contains("riftstalker bloodfiend");
        }

        // Check health (bosses have high HP)
        return false;
    }

    /**
     * Kill boss
     */
    private void killBoss() {
        if (bossEntity == null || !bossEntity.isAlive()) {
            System.out.println("[Slayer] Boss killed!");
            bossesKilled++;
            questsCompleted++;

            if (autoLoot) {
                state = SlayerState.LOOTING;
            } else if (autoStart) {
                state = SlayerState.STARTING_QUEST;
            } else {
                state = SlayerState.IDLE;
            }

            bossEntity = null;
            return;
        }

        // Attack boss
        attackTarget(bossEntity);

        // Use abilities
        if (useAbilities) {
            useSlayerAbility();
        }

        // Dodge attacks
        if (dodgeAttacks) {
            performDodge();
        }
    }

    /**
     * Attack target entity
     */
    private void attackTarget(LivingEntity target) {
        if (target == null || mc.player == null) return;

        double distance = mc.player.distanceTo(target);

        // Move closer if too far
        if (distance > attackRange) {
            moveTowards(target.getPos());
            return;
        }

        // Look at target
        lookAt(target);

        // Attack
        performAttack(target);
    }

    /**
     * Move towards position
     */
    private void moveTowards(Vec3d target) {
        if (mc.player == null || mc.options == null) return;

        Vec3d playerPos = mc.player.getPos();
        double dx = target.x - playerPos.x;
        double dz = target.z - playerPos.z;

        // Set yaw
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        mc.player.setYaw(yaw);

        // Move forward
        mc.options.forwardKey.setPressed(true);

        // Jump if blocked
        if (mc.player.horizontalCollision) {
            mc.options.jumpKey.setPressed(true);
        } else {
            mc.options.jumpKey.setPressed(false);
        }
    }

    /**
     * Look at entity
     */
    private void lookAt(LivingEntity entity) {
        if (mc.player == null) return;

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
     * Perform attack
     */
    private void performAttack(LivingEntity target) {
        // TODO: Implement actual attack with packets
        System.out.println("[Slayer] Attacking " + target.getName().getString());
        mobsKilled++;
    }

    /**
     * Use slayer ability
     */
    private void useSlayerAbility() {
        // TODO: Use weapon ability (right-click)
        System.out.println("[Slayer] Using ability");
    }

    /**
     * Perform dodge movement
     */
    private void performDodge() {
        if (mc.options == null) return;

        // Strafe to dodge
        if (System.currentTimeMillis() % 1000 < 500) {
            mc.options.leftKey.setPressed(true);
            mc.options.rightKey.setPressed(false);
        } else {
            mc.options.rightKey.setPressed(true);
            mc.options.leftKey.setPressed(false);
        }
    }

    /**
     * Scan for mobs
     */
    private void scanForMobs() {
        // Move around to find mobs
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(true);
        }
    }

    /**
     * Collect loot
     */
    private void collectLoot() {
        System.out.println("[Slayer] Collecting loot...");

        // TODO: Scan for item entities and collect

        // After looting, start new quest or idle
        if (autoStart) {
            state = SlayerState.STARTING_QUEST;
        } else {
            state = SlayerState.IDLE;
        }
    }

    /**
     * Sell items at NPC
     */
    private void sellItems() {
        System.out.println("[Slayer] Selling items...");

        // TODO: Walk to NPC and sell

        state = SlayerState.STARTING_QUEST;
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
        long questsPerHour = seconds > 0 ? questsCompleted * 3600 / seconds : 0;

        System.out.println("========================================");
        System.out.println("SLAYER STATISTICS");
        System.out.println("========================================");
        System.out.println("Slayer: " + slayerType.fullName);
        System.out.println("Tier: " + slayerTier);
        System.out.println("Quests Completed: " + questsCompleted + " (" + questsPerHour + "/hr)");
        System.out.println("Mobs Killed: " + mobsKilled);
        System.out.println("Bosses Killed: " + bossesKilled);
        System.out.println("Runtime: " + getRuntimeFormatted());
        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("%s | %s T%d | Quests: %d | Mobs: %d",
                state, slayerType.mobType, slayerTier, questsCompleted, mobsKilled);
    }

    // ==================== GETTERS/SETTERS ====================

    public void setSlayerType(SlayerType type) {
        this.slayerType = type;
        System.out.println("[Slayer] Type changed to: " + type.fullName);
    }

    public void setSlayerTier(int tier) {
        this.slayerTier = Math.max(1, Math.min(5, tier));
    }

    public void setAutoStart(boolean auto) {
        this.autoStart = auto;
    }

    public void setAutoLoot(boolean auto) {
        this.autoLoot = auto;
    }

    public void setAutoSell(boolean auto) {
        this.autoSell = auto;
    }

    public void setAttackRange(double range) {
        this.attackRange = range;
    }

    public void setMaxQuests(int max) {
        this.maxQuests = max;
    }

    public void setUseAbilities(boolean use) {
        this.useAbilities = use;
    }

    public void setDodgeAttacks(boolean dodge) {
        this.dodgeAttacks = dodge;
    }

    public void setAutoArmor(boolean auto) {
        this.autoArmor = auto;
    }
}
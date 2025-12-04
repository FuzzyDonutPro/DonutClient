package com.donut.client.macros.fishing;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * SeaCreatureMacro - Auto kill sea creatures from fishing
 * Detects spawns, kills, collects loot
 */
public class SeaCreatureMacro extends Macro {

    private final MinecraftClient mc;
    private final AutoFisher autoFisher;

    // State
    private CreatureState state = CreatureState.IDLE;
    private Entity currentCreature = null;

    // Settings
    private boolean autoFish = true;
    private boolean autoCombat = true;
    private boolean useAbilities = true;
    private int detectionRadius = 20;

    // Statistics
    private int creaturesKilled = 0;
    private Map<CreatureType, Integer> creatureKills = new HashMap<>();
    private int rareSpawns = 0;

    public enum CreatureState {
        IDLE, FISHING, CREATURE_DETECTED, FIGHTING, COLLECTING
    }

    public enum CreatureType {
        // Common
        SQUID, GUARDIAN, WATER_HYDRA,

        // Uncommon
        SEA_WALKER, NIGHT_SQUID, SEA_GUARDIAN,

        // Rare
        SEA_WITCH, SEA_ARCHER, RIDER_OF_THE_DEEP,

        // Very Rare
        CATFISH, CARROT_KING, SEA_EMPEROR,

        // Legendary
        YETI, GREAT_WHITE_SHARK, THUNDER,

        // Mythic
        REINDRAKE
    }

    public SeaCreatureMacro() {
        super("Sea Creature", "Auto kill sea creatures from fishing");
        this.mc = MinecraftClient.getInstance();
        this.autoFisher = new AutoFisher();

        // Initialize kill counters
        for (CreatureType type : CreatureType.values()) {
            creatureKills.put(type, 0);
        }
    }

    @Override
    public void start() {
        state = CreatureState.IDLE;
        currentCreature = null;
        creaturesKilled = 0;
        rareSpawns = 0;
        System.out.println("[Sea Creature] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Sea Creature] Starting...");

        if (autoFish) {
            state = CreatureState.FISHING;
            autoFisher.start();
            autoFisher.onEnable();
        } else {
            state = CreatureState.CREATURE_DETECTED;
        }
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
            case CREATURE_DETECTED:
                detectCreature();
                break;
            case FIGHTING:
                fightCreature();
                break;
            case COLLECTING:
                collectLoot();
                break;
        }
    }

    /**
     * Fish and detect sea creatures
     */
    private void fish() {
        // Run auto fisher
        autoFisher.onTick();

        // Check for sea creature spawn
        Entity creature = findSeaCreature();

        if (creature != null) {
            System.out.println("[Sea Creature] DETECTED: " + creature.getName().getString());
            currentCreature = creature;

            // Pause fishing
            autoFisher.onDisable();

            state = CreatureState.CREATURE_DETECTED;
        }
    }

    /**
     * Detect sea creature type
     */
    private void detectCreature() {
        if (currentCreature == null || !currentCreature.isAlive()) {
            // Creature disappeared, resume fishing
            currentCreature = null;
            if (autoFish) {
                state = CreatureState.FISHING;
                autoFisher.onEnable();
            } else {
                state = CreatureState.IDLE;
            }
            return;
        }

        // Identify creature type
        CreatureType type = identifyCreature(currentCreature);
        System.out.println("[Sea Creature] Type: " + type);

        // Check if rare
        if (isRareCreature(type)) {
            rareSpawns++;
            System.out.println("[Sea Creature] ★★★ RARE SPAWN! ★★★");
        }

        // Start combat
        if (autoCombat) {
            state = CreatureState.FIGHTING;
        }
    }

    /**
     * Find sea creature nearby
     */
    private Entity findSeaCreature() {
        if (mc.world == null || mc.player == null) return null;

        for (Entity entity : mc.world.getEntities()) {
            if (isSeaCreature(entity)) {
                double distance = mc.player.distanceTo(entity);

                if (distance < detectionRadius) {
                    return entity;
                }
            }
        }

        return null;
    }

    /**
     * Check if entity is sea creature
     */
    private boolean isSeaCreature(Entity entity) {
        String name = entity.getName().getString();

        // Check for sea creature names
        return name.contains("Sea") ||
                name.contains("Squid") ||
                name.contains("Guardian") ||
                name.contains("Hydra") ||
                name.contains("Yeti") ||
                name.contains("Shark") ||
                name.contains("Thunder") ||
                name.contains("Reindrake");
    }

    /**
     * Identify creature type
     */
    private CreatureType identifyCreature(Entity creature) {
        String name = creature.getName().getString();

        // Common
        if (name.contains("Squid") && !name.contains("Night")) return CreatureType.SQUID;
        if (name.contains("Guardian") && !name.contains("Sea")) return CreatureType.GUARDIAN;
        if (name.contains("Water Hydra")) return CreatureType.WATER_HYDRA;

        // Uncommon
        if (name.contains("Sea Walker")) return CreatureType.SEA_WALKER;
        if (name.contains("Night Squid")) return CreatureType.NIGHT_SQUID;
        if (name.contains("Sea Guardian")) return CreatureType.SEA_GUARDIAN;

        // Rare
        if (name.contains("Sea Witch")) return CreatureType.SEA_WITCH;
        if (name.contains("Sea Archer")) return CreatureType.SEA_ARCHER;
        if (name.contains("Rider")) return CreatureType.RIDER_OF_THE_DEEP;

        // Very Rare
        if (name.contains("Catfish")) return CreatureType.CATFISH;
        if (name.contains("Carrot King")) return CreatureType.CARROT_KING;
        if (name.contains("Sea Emperor")) return CreatureType.SEA_EMPEROR;

        // Legendary
        if (name.contains("Yeti")) return CreatureType.YETI;
        if (name.contains("Shark")) return CreatureType.GREAT_WHITE_SHARK;
        if (name.contains("Thunder")) return CreatureType.THUNDER;

        // Mythic
        if (name.contains("Reindrake")) return CreatureType.REINDRAKE;

        return CreatureType.SQUID; // Default
    }

    /**
     * Check if rare creature
     */
    private boolean isRareCreature(CreatureType type) {
        return type == CreatureType.YETI ||
                type == CreatureType.GREAT_WHITE_SHARK ||
                type == CreatureType.THUNDER ||
                type == CreatureType.REINDRAKE;
    }

    /**
     * Fight sea creature
     */
    private void fightCreature() {
        if (currentCreature == null || !currentCreature.isAlive()) {
            // Creature killed
            CreatureType type = identifyCreature(currentCreature);
            creatureKills.put(type, creatureKills.get(type) + 1);
            creaturesKilled++;

            System.out.println("[Sea Creature] Killed! Total: " + creaturesKilled);

            currentCreature = null;
            state = CreatureState.COLLECTING;
            return;
        }

        // Look at creature
        lookAt(currentCreature.getPos());

        // Attack
        attack();

        // Use abilities if enabled
        if (useAbilities) {
            useAbility();
        }
    }

    /**
     * Collect loot
     */
    private void collectLoot() {
        System.out.println("[Sea Creature] Collecting loot...");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Resume fishing if enabled
        if (autoFish) {
            state = CreatureState.FISHING;
            autoFisher.onEnable();
        } else {
            state = CreatureState.IDLE;
        }
    }

    /**
     * Attack creature
     */
    private void attack() {
        // TODO: Simulate left click
    }

    /**
     * Use ability
     */
    private void useAbility() {
        // TODO: Right click for abilities
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
        return String.format("%s | Killed: %d | Rare: %d | Rate: %.1f/hr",
                state, creaturesKilled, rareSpawns, getKillsPerHour());
    }

    /**
     * Get kills per hour
     */
    public double getKillsPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) creaturesKilled / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setAutoFish(boolean auto) {
        this.autoFish = auto;
    }

    public void setAutoCombat(boolean auto) {
        this.autoCombat = auto;
    }

    public void setUseAbilities(boolean use) {
        this.useAbilities = use;
    }

    public void setDetectionRadius(int radius) {
        this.detectionRadius = radius;
    }

    public Map<CreatureType, Integer> getCreatureKills() {
        return new HashMap<>(creatureKills);
    }
}
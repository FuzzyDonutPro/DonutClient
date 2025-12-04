package com.donut.client.macros.rift;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * MontezumaSoulPieces - Collects Montezuma Soul Pieces
 * Hunts rare mobs that drop soul pieces in the Rift
 */
public class MontezumaSoulPieces extends Macro {

    private final MinecraftClient mc;

    // State
    private MontezumaState state = MontezumaState.IDLE;
    private String currentLocation = "Colosseum";

    // Settings
    private boolean autoKillMobs = true;
    private boolean prioritizeRares = true;
    private int searchRadius = 40;

    // Statistics
    private int mobsKilled = 0;
    private int soulPiecesObtained = 0;
    private Map<MobType, Integer> killsByType = new HashMap<>();

    public enum MontezumaState {
        IDLE, SEARCHING_LOCATION, HUNTING_MOBS, COLLECTING_DROPS
    }

    public enum MobType {
        // Colosseum mobs (best for soul pieces)
        BLOBBERCYST,        // Common
        BACTE,              // Uncommon
        LUMINA_MOTH,        // Rare
        OUBLIETTE_GUARD,    // Very Rare

        // Other rift mobs
        LEECH_SUPREME,
        BLOOD_FIEND,
        SMOLDERING_BLAZE
    }

    public MontezumaSoulPieces() {
        super("Montezuma Soul Pieces", "Hunts mobs for Montezuma Soul Pieces");
        this.mc = MinecraftClient.getInstance();

        // Initialize kill counters
        for (MobType type : MobType.values()) {
            killsByType.put(type, 0);
        }
    }

    @Override
    public void start() {
        state = MontezumaState.IDLE;
        mobsKilled = 0;
        soulPiecesObtained = 0;
        System.out.println("[Montezuma] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Montezuma] Starting soul piece hunting...");
        state = MontezumaState.SEARCHING_LOCATION;
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                break;
            case SEARCHING_LOCATION:
                searchLocation();
                break;
            case HUNTING_MOBS:
                huntMobs();
                break;
            case COLLECTING_DROPS:
                collectDrops();
                break;
        }
    }

    /**
     * Search for best hunting location
     */
    private void searchLocation() {
        System.out.println("[Montezuma] Searching for mobs at: " + currentLocation);

        // Check if at Colosseum (best spot)
        if (!isAtColosseum()) {
            System.out.println("[Montezuma] Navigating to Colosseum...");
            navigateToColosseum();
            return;
        }

        state = MontezumaState.HUNTING_MOBS;
    }

    /**
     * Check if at Colosseum
     */
    private boolean isAtColosseum() {
        if (mc.player == null) return false;

        BlockPos pos = mc.player.getBlockPos();

        // Colosseum area coords
        return pos.getX() >= -20 && pos.getX() <= 20 &&
                pos.getZ() >= 280 && pos.getZ() <= 320;
    }

    /**
     * Navigate to Colosseum
     */
    private void navigateToColosseum() {
        BlockPos colosseumCenter = new BlockPos(0, 75, 300);

        if (mc.player == null) return;

        double distance = mc.player.getPos().distanceTo(colosseumCenter.toCenterPos());

        if (distance <= 10.0) {
            System.out.println("[Montezuma] Arrived at Colosseum!");
            state = MontezumaState.HUNTING_MOBS;
        } else {
            lookAt(colosseumCenter.toCenterPos());
            // TODO: Use pathfinding
        }
    }

    /**
     * Hunt mobs
     */
    private void huntMobs() {
        // Find nearest mob
        var mob = findNearestMob();

        if (mob == null) {
            System.out.println("[Montezuma] No mobs found, waiting...");
            return;
        }

        // Attack mob
        if (mc.player == null) return;

        double distance = mc.player.distanceTo(mob);

        if (distance <= 4.0) {
            // In range, attack
            lookAt(mob.getPos());
            attack();

            // Check if dead
            if (!mob.isAlive()) {
                MobType type = identifyMob(mob);
                killsByType.put(type, killsByType.get(type) + 1);
                mobsKilled++;

                System.out.println("[Montezuma] Killed " + type + "! Total: " + mobsKilled);

                state = MontezumaState.COLLECTING_DROPS;
            }
        } else {
            // Move to mob
            lookAt(mob.getPos());
            // TODO: Use pathfinding
        }
    }

    /**
     * Find nearest mob
     */
    private net.minecraft.entity.Entity findNearestMob() {
        if (mc.world == null || mc.player == null) return null;

        net.minecraft.entity.Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (var entity : mc.world.getEntities()) {
            if (isRiftMob(entity)) {
                double distance = mc.player.distanceTo(entity);

                if (distance < searchRadius && distance < nearestDistance) {
                    // Prioritize rare mobs
                    if (prioritizeRares && isRareMob(entity)) {
                        nearest = entity;
                        nearestDistance = distance;
                    } else if (!prioritizeRares || nearest == null) {
                        nearest = entity;
                        nearestDistance = distance;
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Check if entity is rift mob
     */
    private boolean isRiftMob(net.minecraft.entity.Entity entity) {
        String name = entity.getName().getString();

        return name.contains("Blobbercyst") ||
                name.contains("Bacte") ||
                name.contains("Lumina") ||
                name.contains("Oubliette") ||
                name.contains("Leech") ||
                name.contains("Blood") ||
                name.contains("Blaze");
    }

    /**
     * Check if rare mob
     */
    private boolean isRareMob(net.minecraft.entity.Entity entity) {
        String name = entity.getName().getString();

        return name.contains("Lumina") ||
                name.contains("Oubliette") ||
                name.contains("Leech Supreme");
    }

    /**
     * Identify mob type
     */
    private MobType identifyMob(net.minecraft.entity.Entity entity) {
        String name = entity.getName().getString();

        if (name.contains("Blobbercyst")) return MobType.BLOBBERCYST;
        if (name.contains("Bacte")) return MobType.BACTE;
        if (name.contains("Lumina")) return MobType.LUMINA_MOTH;
        if (name.contains("Oubliette")) return MobType.OUBLIETTE_GUARD;
        if (name.contains("Leech")) return MobType.LEECH_SUPREME;
        if (name.contains("Blood")) return MobType.BLOOD_FIEND;
        if (name.contains("Blaze")) return MobType.SMOLDERING_BLAZE;

        return MobType.BLOBBERCYST;
    }

    /**
     * Collect drops
     */
    private void collectDrops() {
        System.out.println("[Montezuma] Collecting drops...");

        // TODO: Check inventory for soul pieces
        // TODO: Pick up nearby items

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check for soul piece drop
        if (checkForSoulPiece()) {
            soulPiecesObtained++;
            System.out.println("[Montezuma] ★ SOUL PIECE OBTAINED! ★ Total: " + soulPiecesObtained);
        }

        state = MontezumaState.HUNTING_MOBS;
    }

    /**
     * Check for soul piece drop
     */
    private boolean checkForSoulPiece() {
        // TODO: Check inventory for "Montezuma Soul Piece"
        // Random for now
        return Math.random() < 0.05; // 5% drop rate
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
     * Get status info
     */
    public String getStatusInfo() {
        return String.format("%s | Location: %s | Killed: %d | Pieces: %d",
                state, currentLocation, mobsKilled, soulPiecesObtained);
    }

    /**
     * Get pieces per hour
     */
    public double getPiecesPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) soulPiecesObtained / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setAutoKillMobs(boolean auto) {
        this.autoKillMobs = auto;
    }

    public void setPrioritizeRares(boolean prioritize) {
        this.prioritizeRares = prioritize;
    }

    public void setSearchRadius(int radius) {
        this.searchRadius = radius;
    }

    public Map<MobType, Integer> getKillsByType() {
        return new HashMap<>(killsByType);
    }
}
package com.donut.client.macros.collection;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * FairySoulFinder - Finds and navigates to fairy souls
 * Features: ESP highlighting, waypoint navigation, distance tracking
 */
public class FairySoulFinder extends Macro {

    private final MinecraftClient mc;

    // State
    private FairySoul nearestSoul = null;
    private List<FairySoul> knownSouls = new ArrayList<>();
    private Set<BlockPos> collectedSouls = new HashSet<>();

    // Settings
    private boolean autoNavigate = true;
    private boolean highlightSouls = true;
    private double scanRadius = 50.0;
    private boolean onlyUncollected = true;

    // Statistics
    private int soulsFound = 0;
    private int soulsCollected = 0;

    public FairySoulFinder() {
        super("Fairy Soul Finder", "Find and navigate to fairy souls");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        nearestSoul = null;
        System.out.println("[Fairy Soul Finder] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Fairy Soul Finder] Enabled");
        System.out.println("[Fairy Soul Finder] Scan radius: " + scanRadius);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Fairy Soul Finder] Disabled");
        printStatistics();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Scan for fairy souls
        scanForSouls();

        // Navigate to nearest soul
        if (autoNavigate && nearestSoul != null) {
            navigateToSoul(nearestSoul);
        }

        // Check if collected soul
        checkCollection();
    }

    /**
     * Scan for fairy souls
     */
    private void scanForSouls() {
        if (mc.world == null) return;

        knownSouls.clear();
        FairySoul nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        // Scan for armor stands (fairy souls appear as armor stands)
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity)) continue;

            ArmorStandEntity stand = (ArmorStandEntity) entity;

            // Check if it's a fairy soul
            if (isFairySoul(stand)) {
                BlockPos pos = stand.getBlockPos();
                double distance = mc.player.squaredDistanceTo(stand);

                // Skip if already collected
                if (onlyUncollected && collectedSouls.contains(pos)) {
                    continue;
                }

                FairySoul soul = new FairySoul(pos, distance);
                knownSouls.add(soul);

                // Track nearest
                if (distance < nearestDistance && distance < scanRadius * scanRadius) {
                    nearest = soul;
                    nearestDistance = distance;
                }
            }
        }

        // Update nearest soul
        if (nearest != nearestSoul) {
            nearestSoul = nearest;

            if (nearestSoul != null) {
                soulsFound++;
                System.out.println("[Fairy Soul] Found soul at: " + nearestSoul.position +
                        " (Distance: " + String.format("%.1f", Math.sqrt(nearestSoul.distance)) + " blocks)");
            }
        }
    }

    /**
     * Check if armor stand is a fairy soul
     */
    private boolean isFairySoul(ArmorStandEntity stand) {
        // Fairy souls have specific properties:
        // - Custom name contains "Fairy Soul" or is a particle effect
        // - No armor/items
        // - Marker (invisible hitbox)

        String name = stand.getName().getString();

        // Check for fairy soul indicators
        if (name.toLowerCase().contains("fairy") ||
                name.toLowerCase().contains("soul") ||
                name.contains("✦")) {
            return true;
        }

        // Check for particle effects (souls often have particles)
        // TODO: Detect particle effects around entity

        return false;
    }

    /**
     * Navigate to fairy soul
     */
    private void navigateToSoul(FairySoul soul) {
        if (mc.player == null || mc.options == null) return;

        Vec3d targetPos = Vec3d.ofCenter(soul.position);
        Vec3d playerPos = mc.player.getPos();

        double distance = playerPos.distanceTo(targetPos);

        // Check if close enough to collect
        if (distance < 3.0) {
            stopMovement();
            return;
        }

        // Calculate direction
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double dy = targetPos.y - playerPos.y;

        // Set yaw to face target
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        mc.player.setYaw(yaw);

        // Set pitch
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));
        mc.player.setPitch(pitch);

        // Move forward
        mc.options.forwardKey.setPressed(true);

        // Jump if blocked or going up
        if (mc.player.horizontalCollision || dy > 1.0) {
            mc.options.jumpKey.setPressed(true);
        } else {
            mc.options.jumpKey.setPressed(false);
        }
    }

    /**
     * Check if soul was collected
     */
    private void checkCollection() {
        if (nearestSoul == null) return;

        double distance = Math.sqrt(nearestSoul.distance);

        // If very close and soul disappeared
        if (distance < 3.0) {
            // Check if soul entity still exists
            boolean soulExists = false;

            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof ArmorStandEntity)) continue;

                BlockPos pos = entity.getBlockPos();
                if (pos.equals(nearestSoul.position) && isFairySoul((ArmorStandEntity) entity)) {
                    soulExists = true;
                    break;
                }
            }

            if (!soulExists) {
                // Soul was collected!
                collectedSouls.add(nearestSoul.position);
                soulsCollected++;

                System.out.println("========================================");
                System.out.println("✨ FAIRY SOUL COLLECTED! ✨");
                System.out.println("Position: " + nearestSoul.position);
                System.out.println("Total Collected: " + soulsCollected);
                System.out.println("========================================");

                nearestSoul = null;
            }
        }
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
        System.out.println("========================================");
        System.out.println("FAIRY SOUL FINDER STATISTICS");
        System.out.println("========================================");
        System.out.println("Souls Found: " + soulsFound);
        System.out.println("Souls Collected: " + soulsCollected);
        System.out.println("Known Locations: " + collectedSouls.size());
        System.out.println("Runtime: " + getRuntimeFormatted());
        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        if (nearestSoul != null) {
            double distance = Math.sqrt(nearestSoul.distance);
            return String.format("NAVIGATING | Distance: %.1f blocks | Collected: %d",
                    distance, soulsCollected);
        }

        return String.format("SCANNING | Collected: %d", soulsCollected);
    }

    /**
     * Get nearest soul position
     */
    public BlockPos getNearestSoulPosition() {
        return nearestSoul != null ? nearestSoul.position : null;
    }

    /**
     * Get all known souls
     */
    public List<FairySoul> getKnownSouls() {
        return new ArrayList<>(knownSouls);
    }

    /**
     * Mark soul as collected manually
     */
    public void markCollected(BlockPos pos) {
        collectedSouls.add(pos);
        soulsCollected++;
    }

    // ==================== DATA CLASSES ====================

    public static class FairySoul {
        public BlockPos position;
        public double distance;

        public FairySoul(BlockPos position, double distance) {
            this.position = position;
            this.distance = distance;
        }
    }

    // ==================== GETTERS/SETTERS ====================

    public void setAutoNavigate(boolean auto) {
        this.autoNavigate = auto;
    }

    public void setHighlightSouls(boolean highlight) {
        this.highlightSouls = highlight;
    }

    public void setScanRadius(double radius) {
        this.scanRadius = radius;
    }

    public void setOnlyUncollected(boolean only) {
        this.onlyUncollected = only;
    }
}
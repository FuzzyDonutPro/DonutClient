package com.donut.client.macros.hunting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * TrapPlacer - Automatic trap placement for hunting
 * Places and manages animal traps
 */
public class TrapPlacer {

    private final MinecraftClient mc;

    // Trap locations
    private List<Trap> placedTraps = new ArrayList<>();
    private List<BlockPos> trapLocations = new ArrayList<>();

    // Settings
    private int trapSpacing = 10;
    private int maxTraps = 20;
    private TrapType currentTrapType = TrapType.BASIC;

    // Statistics
    private int trapsPlaced = 0;
    private int trapsTriggered = 0;

    public enum TrapType {
        BASIC,      // Basic trap
        ADVANCED,   // Advanced trap
        EXPERT      // Expert trap
    }

    public TrapPlacer() {
        this.mc = MinecraftClient.getInstance();
    }

    /**
     * Place trap at location
     */
    public boolean placeTrap(BlockPos pos) {
        if (placedTraps.size() >= maxTraps) {
            System.out.println("[Trap Placer] Max traps reached!");
            return false;
        }

        // Check if too close to existing trap
        for (Trap trap : placedTraps) {
            double distance = Math.sqrt(trap.position.getSquaredDistance(pos));

            if (distance < trapSpacing) {
                System.out.println("[Trap Placer] Too close to existing trap!");
                return false;
            }
        }

        // Place trap
        Trap trap = new Trap(pos, currentTrapType, System.currentTimeMillis());
        placedTraps.add(trap);
        trapLocations.add(pos);
        trapsPlaced++;

        System.out.println("[Trap Placer] Placed " + currentTrapType + " trap at " + pos);

        return true;
    }

    /**
     * Place trap at current location
     */
    public boolean placeHere() {
        if (mc.player == null) return false;

        BlockPos pos = mc.player.getBlockPos();
        return placeTrap(pos);
    }

    /**
     * Place trap grid
     */
    public void placeGrid(BlockPos center, int radius, int spacing) {
        System.out.println("[Trap Placer] Placing trap grid...");

        int placed = 0;

        for (int x = -radius; x <= radius; x += spacing) {
            for (int z = -radius; z <= radius; z += spacing) {
                BlockPos pos = center.add(x, 0, z);

                if (placeTrap(pos)) {
                    placed++;
                }

                if (placedTraps.size() >= maxTraps) {
                    break;
                }
            }

            if (placedTraps.size() >= maxTraps) {
                break;
            }
        }

        System.out.println("[Trap Placer] Placed " + placed + " traps in grid");
    }

    /**
     * Check trap status
     */
    public void checkTraps() {
        for (Trap trap : placedTraps) {
            if (!trap.isActive) continue;

            // TODO: Check if trap has caught animal
            if (isTrapTriggered(trap)) {
                trap.isActive = false;
                trap.triggeredTime = System.currentTimeMillis();
                trapsTriggered++;

                System.out.println("[Trap Placer] Trap triggered at " + trap.position);
            }
        }
    }

    /**
     * Check if trap is triggered
     */
    private boolean isTrapTriggered(Trap trap) {
        // TODO: Check trap status
        return false;
    }

    /**
     * Collect trap
     */
    public boolean collectTrap(BlockPos pos) {
        Trap trap = getTrapAt(pos);

        if (trap == null) {
            return false;
        }

        placedTraps.remove(trap);
        trapLocations.remove(pos);

        System.out.println("[Trap Placer] Collected trap at " + pos);

        return true;
    }

    /**
     * Collect all traps
     */
    public void collectAllTraps() {
        System.out.println("[Trap Placer] Collecting all traps...");

        int collected = placedTraps.size();
        placedTraps.clear();
        trapLocations.clear();

        System.out.println("[Trap Placer] Collected " + collected + " traps");
    }

    /**
     * Get trap at position
     */
    public Trap getTrapAt(BlockPos pos) {
        for (Trap trap : placedTraps) {
            if (trap.position.equals(pos)) {
                return trap;
            }
        }
        return null;
    }

    /**
     * Get nearest trap
     */
    public Trap getNearestTrap() {
        if (mc.player == null || placedTraps.isEmpty()) return null;

        Trap nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Trap trap : placedTraps) {
            double distance = mc.player.getPos().distanceTo(trap.position.toCenterPos());

            if (distance < nearestDistance) {
                nearest = trap;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    /**
     * Get triggered traps
     */
    public List<Trap> getTriggeredTraps() {
        List<Trap> triggered = new ArrayList<>();

        for (Trap trap : placedTraps) {
            if (!trap.isActive) {
                triggered.add(trap);
            }
        }

        return triggered;
    }

    /**
     * Get status
     */
    public String getStatusInfo() {
        int active = 0;
        int triggered = 0;

        for (Trap trap : placedTraps) {
            if (trap.isActive) {
                active++;
            } else {
                triggered++;
            }
        }

        return String.format("Traps: %d active, %d triggered | Total placed: %d",
                active, triggered, trapsPlaced);
    }

    // Getters/Setters
    public void setTrapSpacing(int spacing) {
        this.trapSpacing = spacing;
    }

    public void setMaxTraps(int max) {
        this.maxTraps = max;
    }

    public void setCurrentTrapType(TrapType type) {
        this.currentTrapType = type;
    }

    public List<Trap> getPlacedTraps() {
        return new ArrayList<>(placedTraps);
    }

    /**
     * Trap class
     */
    public static class Trap {
        public final BlockPos position;
        public final TrapType type;
        public final long placedTime;
        public boolean isActive;
        public long triggeredTime;

        public Trap(BlockPos position, TrapType type, long placedTime) {
            this.position = position;
            this.type = type;
            this.placedTime = placedTime;
            this.isActive = true;
            this.triggeredTime = 0;
        }

        public long getActiveTime() {
            if (isActive) {
                return System.currentTimeMillis() - placedTime;
            } else {
                return triggeredTime - placedTime;
            }
        }

        @Override
        public String toString() {
            return String.format("%s trap at %s (%s)",
                    type, position, isActive ? "active" : "triggered");
        }
    }
}
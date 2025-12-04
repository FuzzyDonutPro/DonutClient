package com.donut.client.pathfinding.movements;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class Movement {
    protected final BlockPos src;
    protected final BlockPos dest;
    protected final Vec3d destVec;

    protected double cost;
    protected boolean valid;

    public Movement(BlockPos src, BlockPos dest) {
        this.src = src;
        this.dest = dest;
        this.destVec = new Vec3d(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
        this.valid = true;
        this.cost = 1.0;
    }

    /**
     * Calculate if this movement is possible
     */
    public abstract boolean calculate();

    /**
     * Get the cost of this movement
     */
    public double getCost() {
        return cost;
    }

    /**
     * Get the destination position
     */
    public BlockPos getDest() {
        return dest;
    }

    /**
     * Get the source position
     */
    public BlockPos getSrc() {
        return src;
    }

    /**
     * Check if this movement is valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Get positions to check for this movement
     */
    public abstract BlockPos[] getPositionsToCheck();

    /**
     * Get the movement type name
     */
    public abstract String getName();

    @Override
    public String toString() {
        return getName() + " from " + src.toShortString() + " to " + dest.toShortString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Movement movement = (Movement) obj;
        return src.equals(movement.src) && dest.equals(movement.dest);
    }

    @Override
    public int hashCode() {
        return 31 * src.hashCode() + dest.hashCode();
    }
}
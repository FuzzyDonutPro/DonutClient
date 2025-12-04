package com.donut.client.pathfinding;

import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * PathExecutor - Executes pathfinding routes
 */
public class PathExecutor {

    private List<BlockPos> currentPath = null;
    private int currentNodeIndex = 0;
    private boolean executing = false;

    /**
     * Tick update
     */
    public void onTick() {
        if (!executing || currentPath == null) return;

        // Move to next node logic here
    }

    /**
     * Execute a path
     */
    public void executePath(List<BlockPos> path) {
        this.currentPath = path;
        this.currentNodeIndex = 0;
        this.executing = true;
    }

    /**
     * Stop execution
     */
    public void stopExecution() {
        this.executing = false;
        this.currentPath = null;
        this.currentNodeIndex = 0;
    }

    /**
     * Check if executing
     */
    public boolean isExecuting() {
        return executing;
    }

    /**
     * Get current path
     */
    public List<BlockPos> getCurrentPath() {
        return currentPath;
    }

    /**
     * Get current node index
     */
    public int getCurrentNodeIndex() {
        return currentNodeIndex;
    }
}
package com.donut.client.pathfinding;

import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * PathFinder - A* pathfinding implementation
 */
public class PathFinder {

    private boolean allowDiagonal = true;
    private boolean allowParkour = false;

    /**
     * Set diagonal movement
     */
    public void setAllowDiagonal(boolean allow) {
        this.allowDiagonal = allow;
    }

    /**
     * Set parkour (jumping gaps)
     */
    public void setAllowParkour(boolean allow) {
        this.allowParkour = allow;
    }

    /**
     * Find path from start to end
     */
    public List<BlockPos> findPath(BlockPos start, BlockPos end) {
        // Placeholder - implement A* algorithm here
        List<BlockPos> path = new ArrayList<>();
        path.add(start);
        path.add(end);
        return path;
    }
}
package com.donut.client.pathfinding;

import com.donut.client.utils.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class PathSmoother {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Smooth a path by removing unnecessary intermediate nodes
     * Uses line-of-sight checking
     */
    public static List<Node> smoothPath(List<Node> originalPath) {
        if (originalPath == null || originalPath.size() <= 2) {
            return originalPath;
        }

        List<Node> smoothed = new ArrayList<>();
        smoothed.add(originalPath.get(0)); // Always keep start

        int currentIndex = 0;

        while (currentIndex < originalPath.size() - 1) {
            // Try to find the furthest node we can reach directly
            int furthestReachable = currentIndex + 1;

            for (int i = originalPath.size() - 1; i > currentIndex + 1; i--) {
                if (canReachDirectly(originalPath.get(currentIndex).getPos(),
                        originalPath.get(i).getPos())) {
                    furthestReachable = i;
                    break;
                }
            }

            // Add the furthest reachable node
            smoothed.add(originalPath.get(furthestReachable));
            currentIndex = furthestReachable;
        }

        return smoothed;
    }

    /**
     * Check if we can reach target directly from source
     */
    private static boolean canReachDirectly(BlockPos from, BlockPos to) {
        if (mc.world == null) return false;

        // Don't smooth if too far apart
        double distance = Math.sqrt(from.getSquaredDistance(to));
        if (distance > 10) {
            return false;
        }

        // Check line of sight
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        int steps = (int) Math.ceil(distance);

        for (int i = 1; i <= steps; i++) {
            BlockPos check = from.add(
                    dx * i / steps,
                    dy * i / steps,
                    dz * i / steps
            );

            // Check if position is blocked
            if (BlockUtils.hasCollision(check)) {
                return false;
            }

            // Check headroom
            if (BlockUtils.hasCollision(check.up())) {
                return false;
            }

            // If not flying, need ground below (unless going down)
            if (mc.player != null && !mc.player.getAbilities().flying) {
                if (dy >= 0 && !BlockUtils.isSolid(check.down())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Remove nodes that are too close together
     */
    public static List<Node> removeRedundantNodes(List<Node> path, double minDistance) {
        if (path == null || path.size() <= 2) {
            return path;
        }

        List<Node> filtered = new ArrayList<>();
        filtered.add(path.get(0)); // Always keep start

        for (int i = 1; i < path.size() - 1; i++) {
            BlockPos prev = filtered.get(filtered.size() - 1).getPos();
            BlockPos current = path.get(i).getPos();

            double distance = Math.sqrt(prev.getSquaredDistance(current));

            if (distance >= minDistance) {
                filtered.add(path.get(i));
            }
        }

        filtered.add(path.get(path.size() - 1)); // Always keep goal

        return filtered;
    }
}
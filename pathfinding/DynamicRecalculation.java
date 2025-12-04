package com.donut.client.pathfinding;

import com.donut.client.utils.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class DynamicRecalculation {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final double RECALC_DISTANCE = 5.0;
    private static final long RECALC_COOLDOWN = 2000; // 2 seconds

    private static long lastRecalcTime = 0;

    /**
     * Check if path needs recalculation
     */
    public static boolean needsRecalculation(List<Node> path, int currentIndex) {
        if (mc.player == null || path == null || path.isEmpty()) {
            return false;
        }

        // Cooldown check
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRecalcTime < RECALC_COOLDOWN) {
            return false;
        }

        // Check if current node is blocked
        if (currentIndex < path.size()) {
            Node currentNode = path.get(currentIndex);
            BlockPos pos = currentNode.getPos();

            if (BlockUtils.hasCollision(pos) || BlockUtils.hasCollision(pos.up())) {
                lastRecalcTime = currentTime;
                return true;
            }
        }

        // Check if next few nodes are blocked
        for (int i = currentIndex; i < Math.min(currentIndex + 3, path.size()); i++) {
            Node node = path.get(i);
            BlockPos pos = node.getPos();

            if (BlockUtils.hasCollision(pos) || BlockUtils.hasCollision(pos.up())) {
                lastRecalcTime = currentTime;
                return true;
            }
        }

        // Check if significantly off path
        if (isOffPath(path, currentIndex)) {
            lastRecalcTime = currentTime;
            return true;
        }

        return false;
    }

    /**
     * Check if player is too far from path
     */
    private static boolean isOffPath(List<Node> path, int currentIndex) {
        if (mc.player == null || path == null || currentIndex >= path.size()) {
            return false;
        }

        Vec3d playerPos = mc.player.getPos();

        // Check distance to current node
        Node currentNode = path.get(currentIndex);
        BlockPos nodePos = currentNode.getPos();

        double dx = nodePos.getX() - playerPos.x;
        double dy = nodePos.getY() - playerPos.y;
        double dz = nodePos.getZ() - playerPos.z;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        return distance > RECALC_DISTANCE;
    }

    /**
     * Update path dynamically based on current position
     */
    public static List<Node> updatePath(List<Node> originalPath, int currentIndex, BlockPos goal) {
        if (mc.player == null || originalPath == null || originalPath.isEmpty()) {
            return originalPath;
        }

        Vec3d playerPos = mc.player.getPos();
        BlockPos currentPos = mc.player.getBlockPos();

        // If close to current node, continue with original path
        if (currentIndex < originalPath.size()) {
            Node currentNode = originalPath.get(currentIndex);
            BlockPos nodePos = currentNode.getPos();

            double distance = Math.sqrt(currentPos.getSquaredDistance(nodePos));

            if (distance < 2.0) {
                return originalPath;
            }
        }

        // Create updated path
        List<Node> updatedPath = new ArrayList<>();

        // Add current position as start
        updatedPath.add(new Node(currentPos));

        // Keep remaining nodes from original path
        for (int i = currentIndex; i < originalPath.size(); i++) {
            updatedPath.add(originalPath.get(i));
        }

        return updatedPath;
    }

    /**
     * Try to smooth path around obstacles
     */
    public static List<Node> smoothAroundObstacles(List<Node> path) {
        if (path == null || path.size() < 3) {
            return path;
        }

        List<Node> smoothed = new ArrayList<>();
        smoothed.add(path.get(0)); // Keep start

        for (int i = 1; i < path.size() - 1; i++) {
            Node prev = smoothed.get(smoothed.size() - 1);
            Node current = path.get(i);
            Node next = path.get(i + 1);

            // Check if we can skip current node
            if (canReachDirectly(prev.getPos(), next.getPos())) {
                // Skip current node
                continue;
            } else {
                // Keep current node
                smoothed.add(current);
            }
        }

        smoothed.add(path.get(path.size() - 1)); // Keep goal

        return smoothed;
    }

    /**
     * Check if can reach target directly
     */
    private static boolean canReachDirectly(BlockPos from, BlockPos to) {
        if (mc.world == null) return false;

        double distance = Math.sqrt(from.getSquaredDistance(to));
        if (distance > 5.0) return false;

        int steps = (int) Math.ceil(distance);

        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        for (int i = 1; i < steps; i++) {
            BlockPos check = from.add(
                    dx * i / steps,
                    dy * i / steps,
                    dz * i / steps
            );

            if (BlockUtils.hasCollision(check) || BlockUtils.hasCollision(check.up())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Reset recalculation cooldown
     */
    public static void resetCooldown() {
        lastRecalcTime = 0;
    }
}
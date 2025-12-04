package com.donut.client.pathfinding;

import com.donut.client.utils.BlockUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ObstacleAvoidance {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Check if path has obstacles and try to avoid them
     */
    public static boolean hasObstacles(List<Node> path) {
        if (path == null || path.isEmpty() || mc.world == null) {
            return false;
        }

        for (Node node : path) {
            if (node == null) continue;

            BlockPos pos = node.getPos();

            // Check if node position is blocked
            if (BlockUtils.hasCollision(pos)) {
                return true;
            }

            // Check headroom
            if (BlockUtils.hasCollision(pos.up())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Try to find alternative routes around obstacles
     */
    public static List<Node> avoidObstacles(List<Node> path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        List<Node> cleanPath = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            Node node = path.get(i);
            if (node == null) continue;

            BlockPos pos = node.getPos();

            // Check if current node is blocked
            if (BlockUtils.hasCollision(pos) || BlockUtils.hasCollision(pos.up())) {
                // Try to find nearby alternative
                Node alternative = findNearbyAlternative(pos, path, i);
                if (alternative != null) {
                    cleanPath.add(alternative);
                } else {
                    // Can't find alternative, skip this node
                    continue;
                }
            } else {
                // Node is fine, keep it
                cleanPath.add(node);
            }
        }

        return cleanPath.isEmpty() ? path : cleanPath;
    }

    /**
     * Find a nearby alternative position
     */
    private static Node findNearbyAlternative(BlockPos blocked, List<Node> path, int index) {
        if (mc.world == null) return null;

        // Try positions around the blocked one
        BlockPos[] offsets = {
                blocked.add(1, 0, 0),
                blocked.add(-1, 0, 0),
                blocked.add(0, 0, 1),
                blocked.add(0, 0, -1),
                blocked.add(0, 1, 0),
                blocked.add(0, -1, 0)
        };

        for (BlockPos alt : offsets) {
            if (!BlockUtils.hasCollision(alt) && !BlockUtils.hasCollision(alt.up())) {
                // Check if there's ground below (if not flying)
                if (mc.player != null && !mc.player.getAbilities().flying) {
                    if (!BlockUtils.isSolid(alt.down())) {
                        continue;
                    }
                }

                return new Node(alt);
            }
        }

        return null;
    }

    /**
     * Check if player can reach next node safely
     */
    public static boolean canReachNext(Node current, Node next) {
        if (current == null || next == null || mc.world == null) {
            return false;
        }

        BlockPos currentPos = current.getPos();
        BlockPos nextPos = next.getPos();

        // Get Vec3d positions
        Vec3d currentVec = new Vec3d(
                currentPos.getX() + 0.5,
                currentPos.getY(),
                currentPos.getZ() + 0.5
        );

        Vec3d nextVec = new Vec3d(
                nextPos.getX() + 0.5,
                nextPos.getY(),
                nextPos.getZ() + 0.5
        );

        // Check line of sight
        return isPathClear(currentVec, nextVec);
    }

    /**
     * Check if path between two positions is clear
     */
    private static boolean isPathClear(Vec3d from, Vec3d to) {
        if (mc.world == null) return false;

        double distance = from.distanceTo(to);
        int steps = (int) Math.ceil(distance * 2);

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;

            Vec3d point = from.lerp(to, t);
            BlockPos pos = BlockPos.ofFloored(point);

            if (BlockUtils.hasCollision(pos)) {
                return false;
            }

            if (BlockUtils.hasCollision(pos.up())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Remove nodes that are blocked
     */
    public static List<Node> filterBlockedNodes(List<Node> path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        List<Node> filtered = new ArrayList<>();

        for (Node node : path) {
            if (node == null) continue;

            BlockPos pos = node.getPos();

            // Only keep nodes that are passable
            if (!BlockUtils.hasCollision(pos) && !BlockUtils.hasCollision(pos.up())) {
                filtered.add(node);
            }
        }

        return filtered;
    }
}
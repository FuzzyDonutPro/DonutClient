package com.donut.client.pathfinding.movements;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class MovementProvider {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean allowDiagonal = true;
    private boolean allowParkour = true;
    private boolean allowDescend = true;

    /**
     * Get all possible movements from a position
     */
    public List<Movement> getMovements(BlockPos from) {
        List<Movement> movements = new ArrayList<>();

        // Check if flying - completely different movement set
        if (mc.player != null && mc.player.getAbilities().flying) {
            addFlyingMovements(movements, from);
            return movements;
        }

        // Ground movement
        addStraightMovements(movements, from);

        if (allowDiagonal) {
            addDiagonalMovements(movements, from);
        }

        addAscendMovements(movements, from);

        if (allowDescend) {
            addDescendMovements(movements, from);
        }

        if (allowParkour) {
            addParkourMovements(movements, from);
        }

        return movements;
    }

    private void addFlyingMovements(List<Movement> movements, BlockPos from) {
        // Flying allows free 3D movement in all directions
        // Check nearby positions in a 3D grid

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    movements.add(new MovementFly(from, from.add(dx, dy, dz)));
                }
            }
        }

        // Also add longer distance flying (2-3 blocks)
        int[][] longDistances = {
                {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2}, // Horizontal
                {0, 2, 0}, {0, -2, 0}, // Vertical
                {2, 1, 0}, {-2, 1, 0}, {0, 1, 2}, {0, 1, -2}, // Diagonal up
                {2, -1, 0}, {-2, -1, 0}, {0, -1, 2}, {0, -1, -2} // Diagonal down
        };

        for (int[] offset : longDistances) {
            movements.add(new MovementFly(from, from.add(offset[0], offset[1], offset[2])));
        }
    }

    private void addStraightMovements(List<Movement> movements, BlockPos from) {
        // Same level
        movements.add(new MovementStraight(from, from.add(1, 0, 0)));
        movements.add(new MovementStraight(from, from.add(-1, 0, 0)));
        movements.add(new MovementStraight(from, from.add(0, 0, 1)));
        movements.add(new MovementStraight(from, from.add(0, 0, -1)));
    }

    private void addDiagonalMovements(List<Movement> movements, BlockPos from) {
        movements.add(new MovementDiagonal(from, from.add(1, 0, 1)));
        movements.add(new MovementDiagonal(from, from.add(1, 0, -1)));
        movements.add(new MovementDiagonal(from, from.add(-1, 0, 1)));
        movements.add(new MovementDiagonal(from, from.add(-1, 0, -1)));
    }

    private void addAscendMovements(List<Movement> movements, BlockPos from) {
        // REALISTIC: Only 1-2 block jumps (player can't jump higher)
        // For escaping deep holes, pathfinding will chain multiple jumps

        for (int height = 1; height <= 2; height++) {
            // Forward + up (most common)
            movements.add(new MovementAscend(from, from.add(1, height, 0)));
            movements.add(new MovementAscend(from, from.add(-1, height, 0)));
            movements.add(new MovementAscend(from, from.add(0, height, 1)));
            movements.add(new MovementAscend(from, from.add(0, height, -1)));

            // Diagonal + up
            if (allowDiagonal && height == 1) {
                // Only allow diagonal for 1 block jumps (2 block diagonal too hard)
                movements.add(new MovementAscend(from, from.add(1, height, 1)));
                movements.add(new MovementAscend(from, from.add(1, height, -1)));
                movements.add(new MovementAscend(from, from.add(-1, height, 1)));
                movements.add(new MovementAscend(from, from.add(-1, height, -1)));
            }
        }
    }

    private void addDescendMovements(List<Movement> movements, BlockPos from) {
        // Try falling 1-10 blocks down
        for (int dy = 1; dy <= 10; dy++) {
            movements.add(new MovementDescend(from, from.add(1, -dy, 0)));
            movements.add(new MovementDescend(from, from.add(-1, -dy, 0)));
            movements.add(new MovementDescend(from, from.add(0, -dy, 1)));
            movements.add(new MovementDescend(from, from.add(0, -dy, -1)));

            // Straight down
            movements.add(new MovementDescend(from, from.add(0, -dy, 0)));
        }
    }

    private void addParkourMovements(List<Movement> movements, BlockPos from) {
        // 2-4 block parkour jumps
        for (int dist = 2; dist <= 4; dist++) {
            // Same level
            movements.add(new MovementParkour(from, from.add(dist, 0, 0)));
            movements.add(new MovementParkour(from, from.add(-dist, 0, 0)));
            movements.add(new MovementParkour(from, from.add(0, 0, dist)));
            movements.add(new MovementParkour(from, from.add(0, 0, -dist)));

            // Up 1 block
            movements.add(new MovementParkour(from, from.add(dist, 1, 0)));
            movements.add(new MovementParkour(from, from.add(-dist, 1, 0)));
            movements.add(new MovementParkour(from, from.add(0, 1, dist)));
            movements.add(new MovementParkour(from, from.add(0, 1, -dist)));

            // Down 1 block
            movements.add(new MovementParkour(from, from.add(dist, -1, 0)));
            movements.add(new MovementParkour(from, from.add(-dist, -1, 0)));
            movements.add(new MovementParkour(from, from.add(0, -1, dist)));
            movements.add(new MovementParkour(from, from.add(0, -1, -dist)));
        }
    }

    // Configuration
    public void setAllowDiagonal(boolean allow) {
        this.allowDiagonal = allow;
    }

    public void setAllowParkour(boolean allow) {
        this.allowParkour = allow;
    }

    public void setAllowDescend(boolean allow) {
        this.allowDescend = allow;
    }
}
package com.donut.client.pathfinding;

import net.minecraft.util.math.BlockPos;

public class Node {
    private final BlockPos pos;
    private Node parent;

    private double gScore; // Cost from start to this node
    private double hScore; // Estimated cost from this node to goal (heuristic)

    public Node(BlockPos pos) {
        this.pos = pos;
        this.gScore = Double.MAX_VALUE;
        this.hScore = 0;
        this.parent = null;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public double getGScore() {
        return gScore;
    }

    public void setGScore(double gScore) {
        this.gScore = gScore;
    }

    public double getHScore() {
        return hScore;
    }

    public void setHScore(double hScore) {
        this.hScore = hScore;
    }

    /**
     * F-score = G-score + H-score
     * Total estimated cost of path through this node
     */
    public double getFScore() {
        return gScore + hScore;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return pos.equals(node.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public String toString() {
        return "Node{pos=" + pos + ", g=" + String.format("%.1f", gScore) +
                ", h=" + String.format("%.1f", hScore) + ", f=" + String.format("%.1f", getFScore()) + "}";
    }
}
package com.donut.client.pathfinding;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class PathRenderer {
    private final MinecraftClient mc;

    private boolean enabled = true;
    private boolean renderBoxes = true;
    private boolean renderLines = true;

    public PathRenderer() {
        this.mc = MinecraftClient.getInstance();
    }

    public void render(MatrixStack matrices, List<Node> path, int currentNodeIndex, VertexConsumerProvider vertexConsumers) {
        if (!enabled || path == null || path.isEmpty() || mc.player == null) {
            return;
        }

        if (renderLines) {
            renderPathLines(matrices, path, currentNodeIndex, vertexConsumers);
        }

        if (renderBoxes) {
            renderPathBoxes(matrices, path, currentNodeIndex, vertexConsumers);
        }
    }

    private void renderPathLines(MatrixStack matrices, List<Node> path, int currentIndex, VertexConsumerProvider vertexConsumers) {
        if (path.size() < 2) return;

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        for (int i = 0; i < path.size() - 1; i++) {
            BlockPos pos1 = path.get(i).getPos();
            BlockPos pos2 = path.get(i + 1).getPos();

            Vec3d start = new Vec3d(
                    pos1.getX() + 0.5 - cameraPos.x,
                    pos1.getY() + 0.5 - cameraPos.y,
                    pos1.getZ() + 0.5 - cameraPos.z
            );
            Vec3d end = new Vec3d(
                    pos2.getX() + 0.5 - cameraPos.x,
                    pos2.getY() + 0.5 - cameraPos.y,
                    pos2.getZ() + 0.5 - cameraPos.z
            );

            int color;
            if (i < currentIndex) {
                color = 0x8000FF00;
            } else if (i == currentIndex) {
                color = 0xFFFFFF00;
            } else {
                color = 0xB300FFFF;
            }

            buffer.vertex(entry, (float)start.x, (float)start.y, (float)start.z)
                    .color(color)
                    .normal(entry, 0f, 1f, 0f);
            buffer.vertex(entry, (float)end.x, (float)end.y, (float)end.z)
                    .color(color)
                    .normal(entry, 0f, 1f, 0f);
        }
    }

    private void renderPathBoxes(MatrixStack matrices, List<Node> path, int currentIndex, VertexConsumerProvider vertexConsumers) {
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        MatrixStack.Entry entry = matrices.peek();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = path.get(i).getPos();

            int color;
            if (i < currentIndex) {
                color = 0x33008000;
            } else if (i == currentIndex) {
                color = 0x66FFFF00;
            } else if (i == path.size() - 1) {
                color = 0x80FF0000;
            } else {
                color = 0x4D00CCFF;
            }

            renderBox(entry, pos, cameraPos, color, buffer);
        }
    }

    private void renderBox(MatrixStack.Entry entry, BlockPos pos, Vec3d cameraPos, int color, VertexConsumer buffer) {
        float x1 = (float)(pos.getX() - cameraPos.x);
        float y1 = (float)(pos.getY() - cameraPos.y);
        float z1 = (float)(pos.getZ() - cameraPos.z);
        float x2 = x1 + 1;
        float y2 = y1 + 1;
        float z2 = z1 + 1;

        // Bottom
        buffer.vertex(entry, x1, y1, z1).color(color);
        buffer.vertex(entry, x2, y1, z1).color(color);
        buffer.vertex(entry, x2, y1, z2).color(color);
        buffer.vertex(entry, x1, y1, z2).color(color);

        // Top
        buffer.vertex(entry, x1, y2, z1).color(color);
        buffer.vertex(entry, x1, y2, z2).color(color);
        buffer.vertex(entry, x2, y2, z2).color(color);
        buffer.vertex(entry, x2, y2, z1).color(color);

        // North
        buffer.vertex(entry, x1, y1, z1).color(color);
        buffer.vertex(entry, x1, y2, z1).color(color);
        buffer.vertex(entry, x2, y2, z1).color(color);
        buffer.vertex(entry, x2, y1, z1).color(color);

        // South
        buffer.vertex(entry, x2, y1, z2).color(color);
        buffer.vertex(entry, x2, y2, z2).color(color);
        buffer.vertex(entry, x1, y2, z2).color(color);
        buffer.vertex(entry, x1, y1, z2).color(color);

        // West
        buffer.vertex(entry, x1, y1, z1).color(color);
        buffer.vertex(entry, x1, y1, z2).color(color);
        buffer.vertex(entry, x1, y2, z2).color(color);
        buffer.vertex(entry, x1, y2, z1).color(color);

        // East
        buffer.vertex(entry, x2, y1, z2).color(color);
        buffer.vertex(entry, x2, y1, z1).color(color);
        buffer.vertex(entry, x2, y2, z1).color(color);
        buffer.vertex(entry, x2, y2, z2).color(color);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRenderBoxes(boolean render) {
        this.renderBoxes = render;
    }

    public void setRenderLines(boolean render) {
        this.renderLines = render;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
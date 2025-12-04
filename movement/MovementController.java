package com.donut.client.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class MovementController {
    private final MinecraftClient mc;

    private boolean moveForward;
    private boolean moveBackward;
    private boolean moveLeft;
    private boolean moveRight;
    private boolean jump;
    private boolean sneak;

    public MovementController() {
        this.mc = MinecraftClient.getInstance();
    }

    public void tick() {
        if (mc.player == null || mc.options == null) return;

        // Apply movement states to keybindings
        KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), moveForward);
        KeyBinding.setKeyPressed(mc.options.backKey.getDefaultKey(), moveBackward);
        KeyBinding.setKeyPressed(mc.options.leftKey.getDefaultKey(), moveLeft);
        KeyBinding.setKeyPressed(mc.options.rightKey.getDefaultKey(), moveRight);
        KeyBinding.setKeyPressed(mc.options.jumpKey.getDefaultKey(), jump);
        KeyBinding.setKeyPressed(mc.options.sneakKey.getDefaultKey(), sneak);
    }

    public void setForward(boolean state) {
        this.moveForward = state;
    }

    public void setBackward(boolean state) {
        this.moveBackward = state;
    }

    public void setLeft(boolean state) {
        this.moveLeft = state;
    }

    public void setRight(boolean state) {
        this.moveRight = state;
    }

    public void setJump(boolean state) {
        this.jump = state;
    }

    public void setSneak(boolean state) {
        this.sneak = state;
    }

    public void stopMovement() {
        setForward(false);
        setBackward(false);
        setLeft(false);
        setRight(false);
        setJump(false);
        setSneak(false);

        // Immediately release all keys
        if (mc.options != null) {
            KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.backKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.leftKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.rightKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.jumpKey.getDefaultKey(), false);
            KeyBinding.setKeyPressed(mc.options.sneakKey.getDefaultKey(), false);
        }
    }

    public boolean isMovingForward() {
        return moveForward;
    }

    public boolean isMovingBackward() {
        return moveBackward;
    }

    public boolean isMovingLeft() {
        return moveLeft;
    }

    public boolean isMovingRight() {
        return moveRight;
    }

    public boolean isJumping() {
        return jump;
    }

    public boolean isSneaking() {
        return sneak;
    }
}
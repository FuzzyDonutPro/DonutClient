package com.donut.client.movement;

/**
 * RotationHandler - Manages player rotation
 */
public class RotationHandler {

    private float rotationSpeed = 10.0f;

    /**
     * Tick update
     */
    public void onTick() {
        // Smooth rotation logic here
    }

    /**
     * Set rotation speed
     */
    public void setRotationSpeed(float speed) {
        this.rotationSpeed = Math.max(1.0f, Math.min(180.0f, speed));
    }

    /**
     * Get rotation speed
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }
}
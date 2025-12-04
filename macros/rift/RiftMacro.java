package com.donut.client.macros.rift;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;

/**
 * RiftMacro - Universal Rift automation
 * Simplified version - just runs one collector at a time
 */
public class RiftMacro extends Macro {

    private final MinecraftClient mc;

    // Current active macro
    private Macro currentMacro = null;

    // Settings
    private RiftTask currentTask = RiftTask.COLLECT_SOULS;
    private boolean autoSwitchTasks = true;

    // Statistics
    private int tasksCompleted = 0;

    public enum RiftTask {
        COLLECT_SOULS,      // Enigma Souls
        COLLECT_EYES,       // Rift Eyes
        HUNT_MONTEZUMA      // Soul Pieces
    }

    public RiftMacro() {
        super("Rift Macro", "Universal Rift automation");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        tasksCompleted = 0;
        System.out.println("[Rift Macro] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Rift Macro] Starting task: " + currentTask);
        startCurrentTask();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // Stop current macro
        if (currentMacro != null && currentMacro.isEnabled()) {
            currentMacro.onDisable();
        }
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        // Run current macro
        if (currentMacro != null && currentMacro.isEnabled()) {
            currentMacro.onTick();

            // Check if task complete
            checkTaskCompletion();
        }
    }

    /**
     * Start current task
     */
    private void startCurrentTask() {
        // Stop previous macro
        if (currentMacro != null && currentMacro.isEnabled()) {
            currentMacro.onDisable();
        }

        // Create and start new macro
        switch (currentTask) {
            case COLLECT_SOULS:
                currentMacro = new EnigmaSoulCollector();
                break;
            case COLLECT_EYES:
                currentMacro = new RiftEyeCollector();
                break;
            case HUNT_MONTEZUMA:
                currentMacro = new MontezumaSoulPieces();
                break;
        }

        if (currentMacro != null) {
            currentMacro.start();
            currentMacro.onEnable();
        }
    }

    /**
     * Check if task is complete
     */
    private void checkTaskCompletion() {
        boolean completed = false;

        // Check completion based on task
        if (currentMacro instanceof EnigmaSoulCollector) {
            EnigmaSoulCollector collector = (EnigmaSoulCollector) currentMacro;
            completed = collector.getProgress() >= 100;
        } else if (currentMacro instanceof RiftEyeCollector) {
            RiftEyeCollector collector = (RiftEyeCollector) currentMacro;
            completed = collector.getProgress() >= 100;
        }

        if (completed) {
            tasksCompleted++;
            System.out.println("[Rift Macro] Task completed! (" + tasksCompleted + ")");

            if (autoSwitchTasks) {
                nextTask();
            } else {
                onDisable();
            }
        }
    }

    /**
     * Move to next task
     */
    private void nextTask() {
        switch (currentTask) {
            case COLLECT_SOULS:
                currentTask = RiftTask.COLLECT_EYES;
                System.out.println("[Rift Macro] Switching to: Collect Eyes");
                startCurrentTask();
                break;
            case COLLECT_EYES:
                currentTask = RiftTask.HUNT_MONTEZUMA;
                System.out.println("[Rift Macro] Switching to: Hunt Montezuma");
                startCurrentTask();
                break;
            case HUNT_MONTEZUMA:
                System.out.println("[Rift Macro] All tasks completed!");
                onDisable();
                break;
        }
    }

    /**
     * Get status info
     */
    public String getStatusInfo() {
        if (currentMacro == null) {
            return "Idle";
        }

        String macroStatus = "";

        if (currentMacro instanceof EnigmaSoulCollector) {
            macroStatus = ((EnigmaSoulCollector) currentMacro).getStatusInfo();
        } else if (currentMacro instanceof RiftEyeCollector) {
            macroStatus = ((RiftEyeCollector) currentMacro).getStatusInfo();
        } else if (currentMacro instanceof MontezumaSoulPieces) {
            macroStatus = ((MontezumaSoulPieces) currentMacro).getStatusInfo();
        }

        return String.format("Task: %s | Completed: %d | %s",
                currentTask, tasksCompleted, macroStatus);
    }

    // Getters/Setters
    public void setCurrentTask(RiftTask task) {
        this.currentTask = task;
    }

    public void setAutoSwitchTasks(boolean auto) {
        this.autoSwitchTasks = auto;
    }

    public Macro getCurrentMacro() {
        return currentMacro;
    }
}
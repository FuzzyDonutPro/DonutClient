package com.donut.client.macros;

import net.minecraft.client.MinecraftClient;

/**
 * Base class for all macros in Donut Client
 * All macros must extend this class
 */
public abstract class BaseMacro {

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    protected String name;
    protected String description;
    protected boolean enabled;
    protected boolean running;
    protected MacroState state;

    // Statistics
    protected long startTime;
    protected long totalRuntime;
    protected int itemsCollected;
    protected double coinsEarned;

    public enum MacroState {
        IDLE,
        RUNNING,
        PAUSED,
        STOPPED,
        ERROR
    }

    public BaseMacro(String name, String description) {
        this.name = name;
        this.description = description;
        this.enabled = false;
        this.running = false;
        this.state = MacroState.IDLE;
        this.startTime = 0;
        this.totalRuntime = 0;
        this.itemsCollected = 0;
        this.coinsEarned = 0;
    }

    /**
     * Initialize the macro
     */
    public abstract void initialize();

    /**
     * Start the macro
     */
    public void start() {
        if (running) return;

        System.out.println("[Donut Client] Starting macro: " + name);
        running = true;
        state = MacroState.RUNNING;
        startTime = System.currentTimeMillis();
        onStart();
    }

    /**
     * Stop the macro
     */
    public void stop() {
        if (!running) return;

        System.out.println("[Donut Client] Stopping macro: " + name);
        running = false;
        state = MacroState.STOPPED;

        if (startTime > 0) {
            totalRuntime += System.currentTimeMillis() - startTime;
        }

        onStop();
    }

    /**
     * Pause the macro
     */
    public void pause() {
        if (!running) return;

        state = MacroState.PAUSED;
        onPause();
    }

    /**
     * Resume the macro
     */
    public void resume() {
        if (state != MacroState.PAUSED) return;

        state = MacroState.RUNNING;
        onResume();
    }

    /**
     * Tick method - called every game tick
     */
    public void tick() {
        if (!running || state != MacroState.RUNNING) return;
        if (mc.player == null || mc.world == null) return;

        try {
            onTick();
        } catch (Exception e) {
            System.err.println("[Donut Client] Error in macro " + name + ": " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                System.err.println("  at " + element.toString());
            }
            state = MacroState.ERROR;
        }
    }

    /**
     * Called when macro starts
     */
    protected abstract void onStart();

    /**
     * Called when macro stops
     */
    protected abstract void onStop();

    /**
     * Called when macro is paused
     */
    protected void onPause() {
        // Override if needed
    }

    /**
     * Called when macro is resumed
     */
    protected void onResume() {
        // Override if needed
    }

    /**
     * Called every tick while macro is running
     */
    protected abstract void onTick();

    /**
     * Reset statistics
     */
    public void resetStats() {
        totalRuntime = 0;
        itemsCollected = 0;
        coinsEarned = 0;
    }

    /**
     * Get macro status string
     */
    public String getStatus() {
        return String.format("%s [%s] - Items: %d, Coins: %.2f, Runtime: %s",
                name, state, itemsCollected, coinsEarned, formatTime(getTotalRuntime()));
    }

    /**
     * Format time in human readable format
     */
    protected String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Get total runtime including current session
     */
    public long getTotalRuntime() {
        long runtime = totalRuntime;
        if (running && startTime > 0) {
            runtime += System.currentTimeMillis() - startTime;
        }
        return runtime;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isRunning() { return running; }
    public MacroState getState() { return state; }
    public int getItemsCollected() { return itemsCollected; }
    public double getCoinsEarned() { return coinsEarned; }

    protected void addItemsCollected(int amount) { this.itemsCollected += amount; }
    protected void addCoinsEarned(double amount) { this.coinsEarned += amount; }
}
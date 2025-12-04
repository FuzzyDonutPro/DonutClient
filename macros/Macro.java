package com.donut.client.macros;

/**
 * MACRO WITH SETTINGS SUPPORT
 * - Compatible with existing macros
 * - Adds optional settings system
 */
public abstract class Macro {

    protected String name;
    protected String description;
    protected boolean enabled = false;
    protected long startTime = 0;
    protected long runtime = 0;

    // Settings system (optional)
    protected MacroSettings settings = null;

    // Throttling system
    private int tickCounter = 0;
    private int throttleInterval = 10;

    // Performance monitoring
    private long lastTickDuration = 0;
    private long avgTickDuration = 0;
    private int tickCount = 0;

    private static final long LAG_THRESHOLD_MS = 5;

    public Macro() {
        this.name = getClass().getSimpleName();
        this.description = "A macro";
        initializeSettings(); // Call to let subclass add settings
    }

    public Macro(String name, String description) {
        this.name = name;
        this.description = description;
        initializeSettings();
    }

    /**
     * Override this to add settings to your macro
     */
    protected void initializeSettings() {
        // Subclasses can override to add settings
    }

    /**
     * Create settings object (call this in initializeSettings if you want settings)
     */
    protected void createSettings() {
        if (settings == null) {
            settings = new MacroSettings(name);
        }
    }

    /**
     * Get settings (null if macro has no settings)
     */
    public MacroSettings getSettings() {
        return settings;
    }

    /**
     * Check if macro has settings
     */
    public boolean hasSettings() {
        return settings != null && !settings.getAllSettings().isEmpty();
    }

    public void setThrottleInterval(int ticks) {
        this.throttleInterval = Math.max(1, ticks);
    }

    public int getThrottleInterval() {
        return throttleInterval;
    }

    public final void tick() {
        if (!enabled) return;

        tickCounter++;

        if (tickCounter % throttleInterval != 0) {
            return;
        }

        long start = System.nanoTime();

        try {
            onTick();
        } catch (Exception e) {
            System.err.println("[Macro] Error in " + name + ": " + e.getMessage());
            e.printStackTrace();
        }

        long end = System.nanoTime();
        lastTickDuration = (end - start) / 1_000_000;

        tickCount++;
        avgTickDuration = ((avgTickDuration * (tickCount - 1)) + lastTickDuration) / tickCount;

        if (lastTickDuration > LAG_THRESHOLD_MS) {
            System.out.println("[PERFORMANCE] " + name + " took " + lastTickDuration + "ms");
        }
    }

    public abstract void onTick();

    public void enable() {
        this.enabled = true;
        this.startTime = System.currentTimeMillis();
        this.tickCounter = 0;
        this.tickCount = 0;
        this.avgTickDuration = 0;
        onEnable();
    }

    public void disable() {
        this.enabled = false;
        if (startTime > 0) {
            runtime += System.currentTimeMillis() - startTime;
        }
        onDisable();
    }

    public void toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
    }

    public void start() {
        enable();
    }

    public void stop() {
        disable();
    }

    public void onEnable() {}
    public void onDisable() {}

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getRuntime() {
        if (enabled && startTime > 0) {
            return runtime + (System.currentTimeMillis() - startTime);
        }
        return runtime;
    }

    public String getRuntimeFormatted() {
        return formatTime(getRuntime());
    }

    public void log(String message) {
        System.out.println("[" + name + "] " + message);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format("%02d:%02d", minutes, seconds % 60);
        }
    }

    public String getStatusInfo() {
        if (enabled) {
            return "Running | Avg: " + avgTickDuration + "ms | Throttle: " + throttleInterval;
        }
        return "Click to toggle macro";
    }

    public double getProgress() {
        return 0.0;
    }

    public String getPerformanceStats() {
        return String.format("Last: %dms | Avg: %dms | Throttle: %d ticks",
                lastTickDuration, avgTickDuration, throttleInterval);
    }

    public void resetStats() {
        tickCount = 0;
        avgTickDuration = 0;
        lastTickDuration = 0;
    }
}
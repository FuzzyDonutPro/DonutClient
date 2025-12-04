package com.donut.client.macros;

import java.util.*;

/**
 * MacroManager - IMPROVED with better organization and features
 */
public class MacroManager {

    private static MacroManager instance;

    private Map<MacroCategory, List<Macro>> macros = new LinkedHashMap<>();
    private Macro activeMacro = null;
    private List<MacroListener> listeners = new ArrayList<>();

    // Statistics
    private int totalStarts = 0;
    private long totalRuntime = 0;

    public enum MacroCategory {
        FARMING("Farming", "🌾"),
        MINING("Mining", "⛏️"),
        COMBAT("Combat", "⚔️"),
        DUNGEONS("Dungeons", "🏰"),
        FISHING("Fishing", "🎣"),
        FORAGING("Foraging", "🪓"),
        HUNTING("Hunting", "🏹"),
        RIFT("Rift", "🌀"),
        ECONOMY("Economy", "💰");

        public final String name;
        public final String icon;

        MacroCategory(String name, String icon) {
            this.name = name;
            this.icon = icon;
        }
    }

    /**
     * Listener interface for macro events
     */
    public interface MacroListener {
        void onMacroStarted(Macro macro);
        void onMacroStopped(Macro macro);
    }

    private MacroManager() {
        for (MacroCategory category : MacroCategory.values()) {
            macros.put(category, new ArrayList<>());
        }
    }

    public static MacroManager getInstance() {
        if (instance == null) {
            instance = new MacroManager();
        }
        return instance;
    }

    /**
     * Register a macro
     */
    public void registerMacro(MacroCategory category, Macro macro) {
        macros.get(category).add(macro);
        System.out.println("[Macro Manager] Registered: " + macro.getName() + " in " + category.name);
    }

    /**
     * Start macro (stops current one)
     */
    public void startMacro(Macro macro) {
        if (activeMacro != null && activeMacro.isEnabled()) {
            stopMacro(activeMacro);
        }

        activeMacro = macro;
        macro.onEnable();
        totalStarts++;

        notifyListeners(l -> l.onMacroStarted(macro));
        System.out.println("[Macro Manager] Started: " + macro.getName());
    }

    /**
     * Stop macro
     */
    public void stopMacro(Macro macro) {
        if (macro != null && macro.isEnabled()) {
            totalRuntime += macro.getRuntime();
            macro.onDisable();

            if (activeMacro == macro) {
                activeMacro = null;
            }

            notifyListeners(l -> l.onMacroStopped(macro));
            System.out.println("[Macro Manager] Stopped: " + macro.getName());
        }
    }

    /**
     * Stop all macros
     */
    public void stopAllMacros() {
        for (List<Macro> categoryMacros : macros.values()) {
            for (Macro macro : categoryMacros) {
                if (macro.isEnabled()) {
                    stopMacro(macro);
                }
            }
        }
        activeMacro = null;
        System.out.println("[Macro Manager] Stopped all macros");
    }

    /**
     * Alias for stopAllMacros (for failsafe compatibility)
     */
    public void disableAll() {
        stopAllMacros();
    }

    /**
     * Get macros by category
     */
    public List<Macro> getMacrosByCategory(MacroCategory category) {
        return new ArrayList<>(macros.get(category));
    }

    /**
     * Get all categories
     */
    public List<MacroCategory> getCategories() {
        return new ArrayList<>(Arrays.asList(MacroCategory.values()));
    }

    /**
     * Get all macros
     */
    public Map<MacroCategory, List<Macro>> getAllMacros() {
        return new LinkedHashMap<>(macros);
    }

    /**
     * Get total macro count
     */
    public int getTotalMacroCount() {
        int total = 0;
        for (List<Macro> list : macros.values()) {
            total += list.size();
        }
        return total;
    }

    /**
     * Get active macro
     */
    public Macro getActiveMacro() {
        return activeMacro;
    }

    /**
     * Check if any macro is running
     */
    public boolean isAnyMacroRunning() {
        return activeMacro != null && activeMacro.isEnabled();
    }

    /**
     * Find macro by name
     */
    public Macro findMacroByName(String name) {
        for (List<Macro> categoryMacros : macros.values()) {
            for (Macro macro : categoryMacros) {
                if (macro.getName().equalsIgnoreCase(name)) {
                    return macro;
                }
            }
        }
        return null;
    }

    /**
     * Get macros by name pattern
     */
    public List<Macro> findMacrosByPattern(String pattern) {
        List<Macro> found = new ArrayList<>();
        String lowerPattern = pattern.toLowerCase();

        for (List<Macro> categoryMacros : macros.values()) {
            for (Macro macro : categoryMacros) {
                if (macro.getName().toLowerCase().contains(lowerPattern)) {
                    found.add(macro);
                }
            }
        }
        return found;
    }

    /**
     * Get category of a macro
     */
    public MacroCategory getCategoryOf(Macro macro) {
        for (Map.Entry<MacroCategory, List<Macro>> entry : macros.entrySet()) {
            if (entry.getValue().contains(macro)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Listener management
    public void addListener(MacroListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MacroListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(java.util.function.Consumer<MacroListener> action) {
        for (MacroListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (Exception e) {
                System.err.println("[Macro Manager] Listener error: " + e.getMessage());
            }
        }
    }

    // Statistics
    public int getTotalStarts() {
        return totalStarts;
    }

    public long getTotalRuntime() {
        return totalRuntime;
    }

    public String getFormattedTotalRuntime() {
        long seconds = totalRuntime / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%dh %dm", hours, minutes);
    }
}
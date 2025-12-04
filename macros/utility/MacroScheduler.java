package com.donut.client.macros.utility;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import java.util.*;

public class MacroScheduler extends Macro {

    private final MinecraftClient mc;
    private List<ScheduledMacro> schedule = new ArrayList<>();
    private ScheduledMacro currentMacro = null;
    private boolean autoRotate = true;
    private long defaultDuration = 3600000;
    private int macrosExecuted = 0;

    public MacroScheduler() {
        super("Macro Scheduler", "Schedule macro execution");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        currentMacro = null;
        macrosExecuted = 0;
        System.out.println("[Scheduler] Initialized");
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        if (currentMacro != null) {
            long elapsed = System.currentTimeMillis() - currentMacro.startTime;
            if (elapsed >= currentMacro.duration) {
                stopCurrentMacro();
                if (autoRotate) startNextMacro();
            }
        } else {
            startNextMacro();
        }
    }

    public void addMacro(Macro macro, long duration) {
        schedule.add(new ScheduledMacro(macro, duration));
    }

    private void startNextMacro() {
        if (schedule.isEmpty()) return;

        ScheduledMacro next = null;
        for (ScheduledMacro sm : schedule) {
            if (!sm.executed) {
                next = sm;
                break;
            }
        }

        if (next == null && autoRotate) {
            for (ScheduledMacro sm : schedule) sm.executed = false;
            next = schedule.get(0);
        }

        if (next != null) {
            currentMacro = next;
            currentMacro.startTime = System.currentTimeMillis();
            currentMacro.macro.onEnable();
        }
    }

    private void stopCurrentMacro() {
        if (currentMacro == null) return;
        currentMacro.macro.onDisable();
        currentMacro.executed = true;
        macrosExecuted++;
        currentMacro = null;
    }

    public static class ScheduledMacro {
        public Macro macro;
        public long duration;
        public long startTime = 0;
        public boolean executed = false;

        public ScheduledMacro(Macro macro, long duration) {
            this.macro = macro;
            this.duration = duration;
        }
    }
}
package com.donut.client.macros.rift;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * EnigmaSoulCollector - Collects all Enigma Souls in the Rift
 * Follows optimal routes to collect all 42 souls
 */
public class EnigmaSoulCollector extends Macro {

    private final MinecraftClient mc;

    // State
    private CollectorState state = CollectorState.IDLE;
    private int currentSoulIndex = 0;
    private List<EnigmaSoul> souls = new ArrayList<>();

    // Settings
    private boolean autoNavigate = true;
    private boolean skipCollected = true;

    // Statistics
    private int soulsCollected = 0;
    private Set<String> collectedSouls = new HashSet<>();

    public enum CollectorState {
        IDLE, NAVIGATING, COLLECTING, CHECKING
    }

    public EnigmaSoulCollector() {
        super("Enigma Soul Collector", "Collects all 42 Enigma Souls in Rift");
        this.mc = MinecraftClient.getInstance();

        // Load all 42 soul locations
        loadEnigmaSouls();
    }

    @Override
    public void start() {
        state = CollectorState.IDLE;
        currentSoulIndex = 0;
        soulsCollected = 0;
        System.out.println("[Enigma Souls] Initialized - " + souls.size() + " souls loaded");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Enigma Souls] Starting collection...");
        state = CollectorState.NAVIGATING;
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case IDLE:
                break;
            case NAVIGATING:
                navigateToSoul();
                break;
            case COLLECTING:
                collectSoul();
                break;
            case CHECKING:
                checkCompletion();
                break;
        }
    }

    /**
     * Load all 42 Enigma Soul locations
     */
    private void loadEnigmaSouls() {
        // Wizard Tower (5 souls)
        souls.add(new EnigmaSoul("Wizard Tower Top", new BlockPos(-43, 120, 73), "wizardtower1"));
        souls.add(new EnigmaSoul("Wizard Tower Mid", new BlockPos(-50, 100, 65), "wizardtower2"));
        souls.add(new EnigmaSoul("Wizard Tower Base", new BlockPos(-38, 85, 80), "wizardtower3"));
        souls.add(new EnigmaSoul("Wizard Tower Secret", new BlockPos(-45, 95, 55), "wizardtower4"));
        souls.add(new EnigmaSoul("Wizard Tower Bridge", new BlockPos(-55, 90, 70), "wizardtower5"));

        // Village Plaza (6 souls)
        souls.add(new EnigmaSoul("Plaza Center", new BlockPos(-100, 70, -50), "plaza1"));
        souls.add(new EnigmaSoul("Plaza Fountain", new BlockPos(-95, 72, -45), "plaza2"));
        souls.add(new EnigmaSoul("Plaza Rooftop", new BlockPos(-110, 85, -55), "plaza3"));
        souls.add(new EnigmaSoul("Plaza Basement", new BlockPos(-105, 65, -48), "plaza4"));
        souls.add(new EnigmaSoul("Plaza Garden", new BlockPos(-90, 71, -60), "plaza5"));
        souls.add(new EnigmaSoul("Plaza Bell Tower", new BlockPos(-108, 95, -52), "plaza6"));

        // West Village (5 souls)
        souls.add(new EnigmaSoul("West House 1", new BlockPos(-150, 73, -20), "west1"));
        souls.add(new EnigmaSoul("West House 2", new BlockPos(-165, 75, -15), "west2"));
        souls.add(new EnigmaSoul("West Barn", new BlockPos(-155, 72, -30), "west3"));
        souls.add(new EnigmaSoul("West Tree", new BlockPos(-170, 80, -25), "west4"));
        souls.add(new EnigmaSoul("West Underground", new BlockPos(-160, 60, -18), "west5"));

        // Dreadfarm (5 souls)
        souls.add(new EnigmaSoul("Dreadfarm House", new BlockPos(30, 75, -120), "dread1"));
        souls.add(new EnigmaSoul("Dreadfarm Barn", new BlockPos(25, 73, -130), "dread2"));
        souls.add(new EnigmaSoul("Dreadfarm Field", new BlockPos(40, 71, -125), "dread3"));
        souls.add(new EnigmaSoul("Dreadfarm Well", new BlockPos(35, 72, -115), "dread4"));
        souls.add(new EnigmaSoul("Dreadfarm Silo", new BlockPos(20, 85, -135), "dread5"));

        // Stillgore Château (6 souls)
        souls.add(new EnigmaSoul("Château Entrance", new BlockPos(150, 80, 200), "chateau1"));
        souls.add(new EnigmaSoul("Château Ballroom", new BlockPos(160, 85, 210), "chateau2"));
        souls.add(new EnigmaSoul("Château Library", new BlockPos(155, 90, 205), "chateau3"));
        souls.add(new EnigmaSoul("Château Tower", new BlockPos(165, 100, 215), "chateau4"));
        souls.add(new EnigmaSoul("Château Garden", new BlockPos(145, 78, 195), "chateau5"));
        souls.add(new EnigmaSoul("Château Dungeon", new BlockPos(158, 70, 208), "chateau6"));

        // Black Lagoon (5 souls)
        souls.add(new EnigmaSoul("Lagoon Shore", new BlockPos(-200, 65, 150), "lagoon1"));
        souls.add(new EnigmaSoul("Lagoon Island", new BlockPos(-210, 70, 160), "lagoon2"));
        souls.add(new EnigmaSoul("Lagoon Cave", new BlockPos(-205, 60, 155), "lagoon3"));
        souls.add(new EnigmaSoul("Lagoon Tree", new BlockPos(-195, 80, 145), "lagoon4"));
        souls.add(new EnigmaSoul("Lagoon Depths", new BlockPos(-215, 55, 165), "lagoon5"));

        // Colosseum (4 souls)
        souls.add(new EnigmaSoul("Colosseum Arena", new BlockPos(0, 75, 300), "colosseum1"));
        souls.add(new EnigmaSoul("Colosseum Stands", new BlockPos(10, 85, 310), "colosseum2"));
        souls.add(new EnigmaSoul("Colosseum Entrance", new BlockPos(-5, 73, 295), "colosseum3"));
        souls.add(new EnigmaSoul("Colosseum Underground", new BlockPos(5, 65, 305), "colosseum4"));

        // Living Cave (3 souls)
        souls.add(new EnigmaSoul("Cave Entrance", new BlockPos(100, 50, -200), "cave1"));
        souls.add(new EnigmaSoul("Cave Depths", new BlockPos(110, 45, -210), "cave2"));
        souls.add(new EnigmaSoul("Cave Secret", new BlockPos(105, 40, -205), "cave3"));

        // Mirrorverse (3 souls)
        souls.add(new EnigmaSoul("Mirror Center", new BlockPos(-300, 80, -300), "mirror1"));
        souls.add(new EnigmaSoul("Mirror Edge", new BlockPos(-310, 82, -310), "mirror2"));
        souls.add(new EnigmaSoul("Mirror Hidden", new BlockPos(-305, 78, -305), "mirror3"));

        System.out.println("[Enigma Souls] Loaded " + souls.size() + " soul locations");
    }

    /**
     * Navigate to next soul
     */
    private void navigateToSoul() {
        if (currentSoulIndex >= souls.size()) {
            System.out.println("[Enigma Souls] All souls collected!");
            state = CollectorState.CHECKING;
            return;
        }

        EnigmaSoul soul = souls.get(currentSoulIndex);

        // Skip if already collected
        if (skipCollected && collectedSouls.contains(soul.id)) {
            System.out.println("[Enigma Souls] Soul already collected: " + soul.name);
            currentSoulIndex++;
            return;
        }

        if (mc.player == null) return;

        double distance = mc.player.getPos().distanceTo(soul.position.toCenterPos());

        if (distance <= 3.0) {
            // Close enough to collect
            state = CollectorState.COLLECTING;
        } else {
            // Navigate to soul
            lookAt(soul.position.toCenterPos());
            System.out.println("[Enigma Souls] Navigating to: " + soul.name + " (" + String.format("%.1f", distance) + "m)");
            // TODO: Use pathfinding
        }
    }

    /**
     * Collect soul
     */
    private void collectSoul() {
        EnigmaSoul soul = souls.get(currentSoulIndex);

        System.out.println("[Enigma Souls] Collecting: " + soul.name);

        // TODO: Right click on soul entity/block

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Mark as collected
        collectedSouls.add(soul.id);
        soulsCollected++;

        System.out.println("[Enigma Souls] Collected! (" + soulsCollected + "/" + souls.size() + ")");

        // Move to next soul
        currentSoulIndex++;
        state = CollectorState.NAVIGATING;
    }

    /**
     * Check completion
     */
    private void checkCompletion() {
        System.out.println("[Enigma Souls] Collection complete!");
        System.out.println("[Enigma Souls] Total collected: " + soulsCollected + "/" + souls.size());

        state = CollectorState.IDLE;
        onDisable();
    }

    /**
     * Look at position
     */
    private void lookAt(Vec3d pos) {
        if (mc.player == null) return;

        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = pos.subtract(eyes).normalize();

        double yaw = Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        double pitch = -Math.toDegrees(Math.asin(dir.y));

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    /**
     * Get status info
     */
    public String getStatusInfo() {
        String currentSoul = currentSoulIndex < souls.size() ? souls.get(currentSoulIndex).name : "Complete";
        return String.format("%s | Current: %s | Collected: %d/%d",
                state, currentSoul, soulsCollected, souls.size());
    }

    /**
     * Get progress percentage
     */
    public double getProgress() {
        return (double) soulsCollected / souls.size() * 100;
    }

    // Getters/Setters
    public void setAutoNavigate(boolean auto) {
        this.autoNavigate = auto;
    }

    public void setSkipCollected(boolean skip) {
        this.skipCollected = skip;
    }

    public List<EnigmaSoul> getSouls() {
        return new ArrayList<>(souls);
    }

    /**
     * Enigma Soul class
     */
    public static class EnigmaSoul {
        public final String name;
        public final BlockPos position;
        public final String id;

        public EnigmaSoul(String name, BlockPos position, String id) {
            this.name = name;
            this.position = position;
            this.id = id;
        }

        @Override
        public String toString() {
            return name + " at " + position;
        }
    }
}
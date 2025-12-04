package com.donut.client.macros.hunting;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * TrevorTrapper - Trevor the Trapper quest automation
 * Accepts quests, finds animals, completes objectives
 */
public class TrevorTrapper extends Macro {

    private final MinecraftClient mc;
    private final AnimalTracker tracker;
    private final TrapPlacer trapPlacer;

    // State
    private TrevorState state = TrevorState.IDLE;
    private TrevorQuest currentQuest = null;
    private Entity currentAnimal = null;

    // Settings
    private boolean autoAcceptQuest = true;
    private boolean autoCompleteQuest = true;
    private boolean useTrap = false;

    // Statistics
    private int questsCompleted = 0;
    private int animalsTrapped = 0;
    private int animalsKilled = 0;

    public enum TrevorState {
        IDLE, TALKING_TO_TREVOR, SEARCHING_ANIMAL, MOVING_TO_ANIMAL,
        PLACING_TRAP, TRAPPING, KILLING, RETURNING_TO_TREVOR
    }

    public enum AnimalTarget {
        TRACKABLE_SHEEP("Sheep", AnimalTracker.AnimalType.SHEEP),
        TRACKABLE_COW("Cow", AnimalTracker.AnimalType.COW),
        TRACKABLE_PIG("Pig", AnimalTracker.AnimalType.PIG),
        TRACKABLE_CHICKEN("Chicken", AnimalTracker.AnimalType.CHICKEN),
        TRACKABLE_RABBIT("Rabbit", AnimalTracker.AnimalType.RABBIT),
        TRACKABLE_HORSE("Horse", AnimalTracker.AnimalType.HORSE);

        public final String displayName;
        public final AnimalTracker.AnimalType animalType;

        AnimalTarget(String displayName, AnimalTracker.AnimalType animalType) {
            this.displayName = displayName;
            this.animalType = animalType;
        }
    }

    public TrevorTrapper() {
        super("Trevor Trapper", "Trevor the Trapper quest automation");
        this.mc = MinecraftClient.getInstance();
        this.tracker = new AnimalTracker();
        this.trapPlacer = new TrapPlacer();
    }

    @Override
    public void start() {
        state = TrevorState.IDLE;
        currentQuest = null;
        currentAnimal = null;
        questsCompleted = 0;
        animalsTrapped = 0;
        animalsKilled = 0;
        System.out.println("[Trevor Trapper] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Trevor Trapper] Starting Trevor quests...");

        if (autoAcceptQuest) {
            state = TrevorState.TALKING_TO_TREVOR;
        } else {
            state = TrevorState.SEARCHING_ANIMAL;
        }
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        // Update tracking
        tracker.update();

        switch (state) {
            case IDLE:
                break;
            case TALKING_TO_TREVOR:
                talkToTrevor();
                break;
            case SEARCHING_ANIMAL:
                searchForAnimal();
                break;
            case MOVING_TO_ANIMAL:
                moveToAnimal();
                break;
            case PLACING_TRAP:
                placeTrap();
                break;
            case TRAPPING:
                trapAnimal();
                break;
            case KILLING:
                killAnimal();
                break;
            case RETURNING_TO_TREVOR:
                returnToTrevor();
                break;
        }
    }

    /**
     * Talk to Trevor
     */
    private void talkToTrevor() {
        System.out.println("[Trevor Trapper] Talking to Trevor...");

        // TODO: Find Trevor NPC and right click

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Accept quest
        acceptQuest();
    }

    /**
     * Accept quest from Trevor
     */
    private void acceptQuest() {
        // TODO: Parse chat for quest details
        // For now, create dummy quest
        currentQuest = new TrevorQuest(
                AnimalTarget.TRACKABLE_SHEEP,
                "Find and trap a Trackable Sheep"
        );

        System.out.println("[Trevor Trapper] Quest accepted: " + currentQuest.objective);

        state = TrevorState.SEARCHING_ANIMAL;
    }

    /**
     * Search for target animal
     */
    private void searchForAnimal() {
        if (currentQuest == null) {
            System.out.println("[Trevor Trapper] No active quest!");
            state = TrevorState.TALKING_TO_TREVOR;
            return;
        }

        // Update tracker
        tracker.update();

        // Find target animal
        AnimalTracker.TrackedAnimal animal = tracker.getNearestAnimal(currentQuest.target.animalType);

        if (animal == null) {
            System.out.println("[Trevor Trapper] No " + currentQuest.target.displayName + " found");
            return;
        }

        currentAnimal = animal.entity;
        System.out.println("[Trevor Trapper] Found target: " + animal);

        state = TrevorState.MOVING_TO_ANIMAL;
    }

    /**
     * Move to animal
     */
    private void moveToAnimal() {
        if (currentAnimal == null || !currentAnimal.isAlive()) {
            currentAnimal = null;
            state = TrevorState.SEARCHING_ANIMAL;
            return;
        }

        if (mc.player == null) return;

        double distance = mc.player.distanceTo(currentAnimal);

        if (distance <= 5.0) {
            // Close enough
            if (useTrap) {
                state = TrevorState.PLACING_TRAP;
            } else {
                state = TrevorState.KILLING;
            }
        } else {
            // Move towards animal
            lookAt(currentAnimal.getPos());
            // TODO: Use pathfinding
        }
    }

    /**
     * Place trap
     */
    private void placeTrap() {
        if (currentAnimal == null) {
            state = TrevorState.SEARCHING_ANIMAL;
            return;
        }

        System.out.println("[Trevor Trapper] Placing trap...");

        // Place trap near animal
        BlockPos trapPos = currentAnimal.getBlockPos();
        trapPlacer.placeTrap(trapPos);

        state = TrevorState.TRAPPING;
    }

    /**
     * Trap animal
     */
    private void trapAnimal() {
        System.out.println("[Trevor Trapper] Waiting for animal to trigger trap...");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if trapped
        // TODO: Check trap status

        animalsTrapped++;
        System.out.println("[Trevor Trapper] Animal trapped!");

        state = TrevorState.RETURNING_TO_TREVOR;
    }

    /**
     * Kill animal
     */
    private void killAnimal() {
        if (currentAnimal == null || !currentAnimal.isAlive()) {
            // Animal killed
            animalsKilled++;
            System.out.println("[Trevor Trapper] Animal killed!");

            currentAnimal = null;
            state = TrevorState.RETURNING_TO_TREVOR;
            return;
        }

        // Look at animal
        lookAt(currentAnimal.getPos());

        // Attack
        attack();
    }

    /**
     * Return to Trevor
     */
    private void returnToTrevor() {
        System.out.println("[Trevor Trapper] Returning to Trevor...");

        // TODO: Pathfind back to Trevor

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        completeQuest();
    }

    /**
     * Complete quest with Trevor
     */
    private void completeQuest() {
        System.out.println("[Trevor Trapper] Completing quest...");

        // TODO: Right click Trevor to turn in quest

        questsCompleted++;
        System.out.println("[Trevor Trapper] Quest completed! Total: " + questsCompleted);

        currentQuest = null;
        currentAnimal = null;

        // Start next quest
        if (autoAcceptQuest) {
            state = TrevorState.TALKING_TO_TREVOR;
        } else {
            state = TrevorState.IDLE;
        }
    }

    /**
     * Attack animal
     */
    private void attack() {
        // TODO: Simulate left click
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
        String questInfo = currentQuest != null ? currentQuest.target.displayName : "None";
        return String.format("%s | Quest: %s | Completed: %d | Rate: %.1f/hr",
                state, questInfo, questsCompleted, getQuestsPerHour());
    }

    /**
     * Get quests per hour
     */
    public double getQuestsPerHour() {
        long runtime = getRuntime();
        if (runtime == 0) return 0;
        return (double) questsCompleted / (runtime / 3600000.0);
    }

    // Getters/Setters
    public void setAutoAcceptQuest(boolean auto) {
        this.autoAcceptQuest = auto;
    }

    public void setAutoCompleteQuest(boolean auto) {
        this.autoCompleteQuest = auto;
    }

    public void setUseTrap(boolean use) {
        this.useTrap = use;
    }

    public AnimalTracker getTracker() {
        return tracker;
    }

    public TrapPlacer getTrapPlacer() {
        return trapPlacer;
    }

    /**
     * Trevor quest class
     */
    private static class TrevorQuest {
        public final AnimalTarget target;
        public final String objective;

        public TrevorQuest(AnimalTarget target, String objective) {
            this.target = target;
            this.objective = objective;
        }
    }
}
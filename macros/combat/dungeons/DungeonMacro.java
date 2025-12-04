package com.donut.client.macros.combat.dungeons;

import com.donut.client.macros.Macro;
import com.donut.client.utils.ClientSideInputHelper;
import com.donut.client.utils.BlockScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.BiomeKeys;
import com.mojang.datafixers.util.Pair;


import java.util.List;

/**
 * AI Dungeon Macro - Automated dungeon running
 * Location: com.donut.client.macros.combat.dungeons
 */
public class DungeonMacro extends Macro {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final BlockScanner scanner = new BlockScanner();

    private DungeonState state = DungeonState.IDLE;
    private BlockPos targetBlock = null;
    private Entity targetMob = null;
    private long lastActionTime = 0;

    public enum DungeonState {
        IDLE,
        CLEARING_ROOM,
        FINDING_SECRETS,
        OPENING_CHEST,
        MOVING_TO_NEXT_ROOM
    }

    public enum DungeonFloor {
        F1, F2, F3, F4, F5, F6, F7,
        M1, M2, M3, M4, M5, M6, M7
    }

    public DungeonMacro() {
        super("AI Dungeons", "Automated dungeon running with secrets");
    }

    @Override
    protected void initializeSettings() {
        createSettings();

        // General settings
        settings.addEnumSetting("floor", "Target floor", DungeonFloor.F7);
        settings.addBooleanSetting("autoKill", "Auto kill mobs", true);
        settings.addBooleanSetting("autoSecrets", "Auto find secrets", true);
        settings.addBooleanSetting("autoChests", "Auto open chests", true);

        // Combat settings
        settings.addIntSetting("attackDelay", "Attack delay (ms)", 500, 100, 2000);
        settings.addIntSetting("mobScanRadius", "Mob scan radius", 10, 5, 20);
        settings.addIntSetting("healthThreshold", "Min health %", 30, 10, 90);

        // Secret finding settings
        settings.addIntSetting("secretScanRadius", "Secret scan radius", 15, 5, 30);
        settings.addIntSetting("secretDelay", "Secret check delay (ms)", 500, 100, 2000);

        // Movement settings
        settings.addFloatSetting("moveSpeed", "Movement speed", 1.0f, 0.5f, 2.0f);
        settings.addBooleanSetting("autoSprint", "Auto sprint", true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        setThrottleInterval(5); // Check every 5 ticks

        scanner.setScanRadius(settings.getInt("secretScanRadius"));
        scanner.setCacheDuration(40); // Cache for 2 seconds

        state = DungeonState.IDLE;
        targetBlock = null;
        targetMob = null;

        log("Dungeon macro started on floor: " + settings.getEnum("floor"));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Check health
        float healthPercent = (mc.player.getHealth() / mc.player.getMaxHealth()) * 100;
        if (healthPercent < settings.getInt("healthThreshold")) {
            log("Low health! Stopping combat");
            state = DungeonState.IDLE;
            ClientSideInputHelper.stopAllMovement();
            return;
        }

        // Auto sprint
        if (settings.getBoolean("autoSprint")) {
            ClientSideInputHelper.sprint(true);
        }

        // State machine
        switch (state) {
            case IDLE:
                determineNextAction();
                break;
            case CLEARING_ROOM:
                clearRoom();
                break;
            case FINDING_SECRETS:
                findSecrets();
                break;
            case OPENING_CHEST:
                openChest();
                break;
            case MOVING_TO_NEXT_ROOM:
                moveToNextRoom();
                break;
        }
    }

    private void determineNextAction() {
        // Priority 1: Kill mobs if auto kill enabled
        if (settings.getBoolean("autoKill")) {
            Entity mob = findNearestMob();
            if (mob != null) {
                targetMob = mob;
                state = DungeonState.CLEARING_ROOM;
                return;
            }
        }

        // Priority 2: Find secrets if enabled
        if (settings.getBoolean("autoSecrets")) {
            BlockPos secret = findSecretBlock();
            if (secret != null) {
                targetBlock = secret;
                state = DungeonState.FINDING_SECRETS;
                return;
            }
        }

        // Priority 3: Open chests
        if (settings.getBoolean("autoChests")) {
            BlockPos chest = findChest();
            if (chest != null) {
                targetBlock = chest;
                state = DungeonState.OPENING_CHEST;
                return;
            }
        }

        // Priority 4: Move to next room
        state = DungeonState.MOVING_TO_NEXT_ROOM;
    }

    private void clearRoom() {
        if (targetMob == null || !targetMob.isAlive()) {
            targetMob = null;
            state = DungeonState.IDLE;
            return;
        }

        // Check if can attack
        long now = System.currentTimeMillis();
        int attackDelay = settings.getInt("attackDelay");
        if (now - lastActionTime < attackDelay) {
            return;
        }

        // Look at mob
        BlockPos mobPos = targetMob.getBlockPos();
        ClientSideInputHelper.smoothLookAt(mobPos, 0.3f);

        // Check if looking at mob
        if (!ClientSideInputHelper.isLookingAt(mobPos, 10.0f)) {
            return;
        }

        // Check if in range
        double distance = ClientSideInputHelper.getDistanceTo(mobPos);
        if (distance > 4.0) {
            // Move closer
            ClientSideInputHelper.moveForward(true);
            return;
        }

        // Attack
        ClientSideInputHelper.leftClick(true);
        lastActionTime = now;

        // Release after short delay
        new Thread(() -> {
            try {
                Thread.sleep(50);
                ClientSideInputHelper.leftClick(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void findSecrets() {
        if (targetBlock == null) {
            state = DungeonState.IDLE;
            return;
        }

        // Check if block still exists
        if (mc.world.getBlockState(targetBlock).isAir()) {
            targetBlock = null;
            state = DungeonState.IDLE;
            return;
        }

        // Look at secret block
        ClientSideInputHelper.lookAt(targetBlock);

        // Check if looking at it
        if (!ClientSideInputHelper.isLookingAt(targetBlock, 5.0f)) {
            return;
        }

        // Check if in range
        if (!ClientSideInputHelper.canReach(targetBlock)) {
            // Move closer
            ClientSideInputHelper.moveForward(true);
            return;
        }

        // Interact with block
        long now = System.currentTimeMillis();
        if (now - lastActionTime > settings.getInt("secretDelay")) {
            ClientSideInputHelper.rightClickOnce();
            lastActionTime = now;

            log("Found secret at: " + targetBlock);
            targetBlock = null;
            state = DungeonState.IDLE;
        }
    }

    private void openChest() {
        if (targetBlock == null) {
            state = DungeonState.IDLE;
            return;
        }

        // Look at chest
        ClientSideInputHelper.lookAt(targetBlock);

        // Check if looking at it
        if (!ClientSideInputHelper.isLookingAt(targetBlock, 5.0f)) {
            return;
        }

        // Check if in range
        if (!ClientSideInputHelper.canReach(targetBlock)) {
            // Move closer
            ClientSideInputHelper.moveForward(true);
            return;
        }

        // Open chest
        ClientSideInputHelper.rightClickOnce();
        lastActionTime = System.currentTimeMillis();

        log("Opened chest at: " + targetBlock);
        targetBlock = null;
        state = DungeonState.IDLE;
    }

    private void moveToNextRoom() {
        // Simple forward movement
        ClientSideInputHelper.moveForward(true);

        // Check for new mobs/secrets periodically
        state = DungeonState.IDLE;
    }

    private Entity findNearestMob() {
        if (mc.world == null || mc.player == null) return null;

        int radius = settings.getInt("mobScanRadius");
        Box searchBox = Box.of(mc.player.getPos(), radius, radius, radius);

        List<Entity> entities = mc.world.getOtherEntities(mc.player, searchBox);

        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (!(entity instanceof MobEntity)) continue;
            if (!entity.isAlive()) continue;

            double dist = mc.player.squaredDistanceTo(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }

        return nearest;
    }

    private BlockPos findSecretBlock() {
        // Common secret blocks in dungeons
        BlockPos lever = scanner.findNearest(Blocks.LEVER);
        if (lever != null) return lever;

        BlockPos chest = scanner.findNearest(Blocks.CHEST);
        if (chest != null) return chest;

        BlockPos button = scanner.findNearest(Blocks.STONE_BUTTON);
        if (button != null) return button;

        return null;
    }

    private BlockPos findChest() {
        return scanner.findNearest(Blocks.CHEST);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ClientSideInputHelper.stopAllMovement();
        ClientSideInputHelper.leftClick(false);
        ClientSideInputHelper.rightClick(false);
    }

    @Override
    public String getStatusInfo() {
        return "State: " + state + " | Health: " +
                (int)((mc.player.getHealth() / mc.player.getMaxHealth()) * 100) + "%";
    }

    @Override
    public double getProgress() {
        // Progress based on current state
        switch (state) {
            case CLEARING_ROOM:
                return 0.25;
            case FINDING_SECRETS:
                return 0.50;
            case OPENING_CHEST:
                return 0.75;
            case MOVING_TO_NEXT_ROOM:
                return 0.90;
            default:
                return 0.0;
        }
    }
}
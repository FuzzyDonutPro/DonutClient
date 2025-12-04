package com.donut.client.macros.combat.dungeons;

import com.donut.client.macros.Macro;
import com.donut.client.utils.BlockScanner;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Secret Detector - Highlights and tracks dungeon secrets
 * Location: com.donut.client.macros.combat.dungeons
 */
public class SecretDetector extends Macro {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final BlockScanner scanner = new BlockScanner();

    private final List<BlockPos> foundSecrets = new ArrayList<>();
    private int secretCount = 0;

    public SecretDetector() {
        super("Secret Detector", "Detects and highlights dungeon secrets");
    }

    @Override
    protected void initializeSettings() {
        createSettings();

        settings.addIntSetting("scanRadius", "Scan radius", 15, 5, 30);
        settings.addBooleanSetting("detectLevers", "Detect levers", true);
        settings.addBooleanSetting("detectChests", "Detect chests", true);
        settings.addBooleanSetting("detectButtons", "Detect buttons", true);
        settings.addBooleanSetting("detectBats", "Detect bat spawners", true);
        settings.addBooleanSetting("playSound", "Play sound on find", true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        setThrottleInterval(20); // Check every second

        scanner.setScanRadius(settings.getInt("scanRadius"));
        scanner.setCacheDuration(60); // Cache for 3 seconds

        foundSecrets.clear();
        secretCount = 0;

        log("Secret detector started");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Scan for secrets
        List<BlockPos> newSecrets = scanForSecrets();

        // Check for new secrets
        for (BlockPos secret : newSecrets) {
            if (!foundSecrets.contains(secret)) {
                foundSecrets.add(secret);
                secretCount++;

                log("Found secret #" + secretCount + " at: " + secret);

                if (settings.getBoolean("playSound")) {
                    // Play sound (client-side)
                    mc.player.playSound(
                            net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                            1.0f, 1.0f
                    );
                }
            }
        }
    }

    private List<BlockPos> scanForSecrets() {
        List<BlockPos> secrets = new ArrayList<>();

        if (settings.getBoolean("detectLevers")) {
            BlockPos lever = scanner.findNearest(Blocks.LEVER);
            if (lever != null) secrets.add(lever);
        }

        if (settings.getBoolean("detectChests")) {
            BlockPos chest = scanner.findNearest(Blocks.CHEST);
            if (chest != null) secrets.add(chest);
        }

        if (settings.getBoolean("detectButtons")) {
            BlockPos button = scanner.findNearest(Blocks.STONE_BUTTON);
            if (button != null) secrets.add(button);
        }

        return secrets;
    }

    @Override
    public String getStatusInfo() {
        return "Secrets found: " + secretCount;
    }

    @Override
    public double getProgress() {
        // Progress based on typical secret count (assume 5-7 secrets per room)
        return Math.min(1.0, secretCount / 7.0);
    }
}
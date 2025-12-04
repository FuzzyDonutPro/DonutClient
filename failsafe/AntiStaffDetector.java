package com.donut.client.failsafe;

import com.donut.client.macros.MacroManager;
import com.donut.client.macros.utility.FailsafeReactions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Advanced staff detection system
 * Detects staff members by behavior, rank tags, and patterns
 */
public class AntiStaffDetector {

    private static AntiStaffDetector instance;
    private final MinecraftClient mc;

    // Known staff ranks/tags
    private static final Set<String> STAFF_RANKS = Set.of(
            "[ADMIN]", "[GM]", "[MOD]", "[HELPER]",
            "[YOUTUBE]", "[YOUTUBER]", "§c[ADMIN]", "§c[GM]",
            "§2[MOD]", "§9[HELPER]", "§c[YOUTUBE]"
    );

    // Suspicious player tracking
    private Map<UUID, StaffProfile> suspiciousPlayers = new HashMap<>();
    private Set<UUID> confirmedStaff = new HashSet<>();
    private Set<String> staffNames = new HashSet<>();

    // Detection settings
    private boolean enabled = true;
    private int suspicionThreshold = 5;
    private boolean autoDisconnect = true;
    private boolean whisperDetection = true;
    private boolean vanishDetection = true;

    // State
    private UUID currentStaffNearby = null;
    private long staffDetectedTime = 0;

    private AntiStaffDetector() {
        this.mc = MinecraftClient.getInstance();
        loadKnownStaff();
    }

    public static AntiStaffDetector getInstance() {
        if (instance == null) {
            instance = new AntiStaffDetector();
        }
        return instance;
    }

    /**
     * Main detection tick
     */
    public void tick() {
        if (!enabled || mc.player == null || mc.world == null) {
            return;
        }

        // Check nearby players
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            checkPlayer(player);
        }

        // Check for vanished staff
        if (vanishDetection) {
            detectVanishedStaff();
        }

        // Update suspicion levels
        updateSuspicionLevels();
    }

    /**
     * Check if player is staff
     */
    private void checkPlayer(PlayerEntity player) {
        UUID uuid = player.getUuid();
        String name = player.getName().getString();
        String displayName = player.getDisplayName().getString();

        // Check if already confirmed staff
        if (confirmedStaff.contains(uuid)) {
            handleStaffNearby(player);
            return;
        }

        // Get or create profile
        StaffProfile profile = suspiciousPlayers.computeIfAbsent(uuid,
                k -> new StaffProfile(uuid, name));

        int suspicionPoints = 0;

        // Check 1: Rank tags in name
        for (String rank : STAFF_RANKS) {
            if (displayName.contains(rank) || name.contains(rank)) {
                suspicionPoints += 10;
                profile.addIndicator("Has staff rank tag: " + rank);
                confirmedStaff.add(uuid);
                staffNames.add(name);
                handleStaffNearby(player);
                return;
            }
        }

        // Check 2: Known staff list
        if (staffNames.contains(name)) {
            suspicionPoints += 10;
            confirmedStaff.add(uuid);
            handleStaffNearby(player);
            return;
        }

        // Check 3: Flying without creative mode
        if (player.getAbilities().flying && !player.getAbilities().creativeMode) {
            suspicionPoints += 3;
            profile.addIndicator("Flying without creative");
        }

        // Check 4: Spectator mode (staff checking)
        if (player.isSpectator()) {
            suspicionPoints += 5;
            profile.addIndicator("In spectator mode");
        }

        // Check 5: Unusual speed (staff teleporting/flying)
        double speed = player.getVelocity().length();
        if (speed > 2.0 && !player.hasVehicle()) {
            suspicionPoints += 2;
            profile.addIndicator("Unusual speed: " + String.format("%.2f", speed));
        }

        // Check 6: Invisible but still detectable (vanish)
        if (player.isInvisible() && !isUsingInvisPotion(player)) {
            suspicionPoints += 4;
            profile.addIndicator("Suspicious invisibility");
        }

        // Check 7: Standing still watching you (staff observing)
        if (isWatchingPlayer(player, mc.player)) {
            profile.watchingTicks++;
            if (profile.watchingTicks > 100) { // 5 seconds
                suspicionPoints += 2;
                profile.addIndicator("Watching player for extended time");
            }
        } else {
            profile.watchingTicks = 0;
        }

        // Check 8: Appeared suddenly (staff teleported)
        if (!profile.hasSeenBefore) {
            double distance = player.distanceTo(mc.player);
            if (distance < 10) {
                suspicionPoints += 1;
                profile.addIndicator("Appeared suddenly nearby");
            }
            profile.hasSeenBefore = true;
        }

        // Update suspicion level
        profile.suspicionLevel += suspicionPoints;
        profile.lastSeen = System.currentTimeMillis();

        // Trigger if suspicion exceeds threshold
        if (profile.suspicionLevel >= suspicionThreshold) {
            System.out.println("⚠ POTENTIAL STAFF DETECTED ⚠");
            System.out.println("Player: " + name);
            System.out.println("Suspicion Level: " + profile.suspicionLevel);
            System.out.println("Indicators: " + profile.indicators);

            // Add to suspected staff
            confirmedStaff.add(uuid);
            handleStaffNearby(player);
        }
    }

    /**
     * Check if player is watching another player
     */
    private boolean isWatchingPlayer(PlayerEntity watcher, PlayerEntity target) {
        if (watcher == null || target == null) return false;

        // Get direction watcher is looking
        double dx = target.getX() - watcher.getX();
        double dz = target.getZ() - watcher.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90;

        // Normalize angles
        double watcherYaw = watcher.getYaw() % 360;
        if (watcherYaw < 0) watcherYaw += 360;
        targetYaw = targetYaw % 360;
        if (targetYaw < 0) targetYaw += 360;

        // Check if looking within 30 degrees
        double angleDiff = Math.abs(watcherYaw - targetYaw);
        if (angleDiff > 180) angleDiff = 360 - angleDiff;

        return angleDiff < 30;
    }

    /**
     * Check if player is using invisibility potion (legitimate)
     */
    private boolean isUsingInvisPotion(PlayerEntity player) {
        if (player == null) return false;

        // Check for potion effects - this is the legitimate way to be invisible
        return player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.INVISIBILITY);
    }


    /**
     * Detect vanished staff (server-side invisibility)
     */
    private void detectVanishedStaff() {
        // Check for phantom footsteps/sounds
        // Check for missing entity IDs in sequence
        // Check for tab list discrepancies

        // TODO: Implement vanish detection
        // This is tricky as vanished staff don't send entity packets
    }

    /**
     * Handle staff member nearby
     */
    private void handleStaffNearby(PlayerEntity staff) {
        UUID uuid = staff.getUuid();

        if (currentStaffNearby != uuid) {
            currentStaffNearby = uuid;
            staffDetectedTime = System.currentTimeMillis();

            System.out.println("========================================");
            System.out.println("⚠ STAFF MEMBER DETECTED NEARBY ⚠");
            System.out.println("Name: " + staff.getName().getString());
            System.out.println("Distance: " + String.format("%.1f", staff.distanceTo(mc.player)) + " blocks");
            System.out.println("========================================");

            // Execute response
            respondToStaff();
        }
    }

    /**
     * Respond to staff detection
     */
    private void respondToStaff() {
        System.out.println("[Staff Detection] Executing emergency response...");

        // Stop all macros
        MacroManager manager = MacroManager.getInstance();
        manager.disableAll();

        // Auto disconnect if enabled
        if (autoDisconnect) {
            System.out.println("[Staff Detection] Auto-disconnecting...");
            // Add 1-3 second random delay to seem natural
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    mc.execute(() -> {
                        if (mc.world != null) {
                            mc.world.disconnect();
                        }
                    });
                }
            }, 1000 + new Random().nextInt(2000));
        }

        // Trigger failsafe reactions
        FailsafeReactions.getInstance().triggerRandomReaction("Staff detected nearby");
    }

    /**
     * Handle chat messages for staff detection
     */
    public void onChatMessage(Text message) {
        if (!enabled) return;

        String msg = message.getString();
        String plain = msg.replaceAll("§.", ""); // Remove color codes

        // Check for staff whispers
        if (whisperDetection) {
            if (plain.startsWith("From") || plain.contains("whispers:")) {
                String sender = extractSender(plain);

                // Check if sender is staff
                if (isStaffName(sender)) {
                    System.out.println("⚠ STAFF WHISPER DETECTED ⚠");
                    System.out.println("From: " + sender);
                    System.out.println("Message: " + plain);

                    handleStaffContact(sender);
                }
            }
        }

        // Check for admin broadcasts
        if (plain.contains("[ADMIN]") || plain.contains("[GM]")) {
            System.out.println("⚠ STAFF MESSAGE IN CHAT ⚠");
        }
    }

    /**
     * Extract sender from whisper message
     */
    private String extractSender(String message) {
        // "From [RANK] Name:" format
        if (message.startsWith("From")) {
            int colonIndex = message.indexOf(":");
            if (colonIndex > 0) {
                String sender = message.substring(5, colonIndex).trim();
                // Remove rank tags
                for (String rank : STAFF_RANKS) {
                    sender = sender.replace(rank, "").trim();
                }
                return sender;
            }
        }
        return "";
    }

    /**
     * Check if name is known staff
     */
    private boolean isStaffName(String name) {
        return staffNames.contains(name);
    }

    /**
     * Handle direct staff contact
     */
    private void handleStaffContact(String staffName) {
        System.out.println("========================================");
        System.out.println("⚠ DIRECT STAFF CONTACT ⚠");
        System.out.println("Staff: " + staffName);
        System.out.println("========================================");

        // Immediate response
        MacroManager.getInstance().disableAll();

        // More aggressive response to direct contact
        if (autoDisconnect) {
            System.out.println("[Staff Contact] Immediate disconnect!");
            mc.execute(() -> {
                if (mc.world != null) {
                    mc.world.disconnect();
                }
            });
        }
    }

    /**
     * Update suspicion levels (decay over time)
     */
    private void updateSuspicionLevels() {
        long now = System.currentTimeMillis();

        suspiciousPlayers.entrySet().removeIf(entry -> {
            StaffProfile profile = entry.getValue();

            // Decay suspicion over time (if not seen recently)
            if (now - profile.lastSeen > 30000) { // 30 seconds
                profile.suspicionLevel -= 1;
            }

            // Remove if suspicion drops to 0
            return profile.suspicionLevel <= 0;
        });
    }

    /**
     * Load known staff names
     */
    private void loadKnownStaff() {
        // Add known Hypixel staff/YouTubers
        staffNames.addAll(Arrays.asList(
                "Hypixel", "Plancke", "Jayavarmen", "Minikloon",
                "Simon", "Rezzus", "Dctr", "aPunch"
                // Add more known staff names
        ));
    }

    /**
     * Staff profile for tracking
     */
    private static class StaffProfile {
        UUID uuid;
        String name;
        int suspicionLevel = 0;
        List<String> indicators = new ArrayList<>();
        long lastSeen = System.currentTimeMillis();
        boolean hasSeenBefore = false;
        int watchingTicks = 0;

        StaffProfile(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        void addIndicator(String indicator) {
            if (!indicators.contains(indicator)) {
                indicators.add(indicator);
            }
        }
    }

    // Getters
    public boolean isStaffNearby() {
        return currentStaffNearby != null;
    }

    public UUID getCurrentStaff() {
        return currentStaffNearby;
    }

    public Set<UUID> getConfirmedStaff() {
        return new HashSet<>(confirmedStaff);
    }

    // Settings
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSuspicionThreshold(int threshold) {
        this.suspicionThreshold = threshold;
    }

    public void setAutoDisconnect(boolean auto) {
        this.autoDisconnect = auto;
    }

    public void setWhisperDetection(boolean detect) {
        this.whisperDetection = detect;
    }

    public void setVanishDetection(boolean detect) {
        this.vanishDetection = detect;
    }

    public void addKnownStaff(String name) {
        staffNames.add(name);
    }

    public void reset() {
        suspiciousPlayers.clear();
        currentStaffNearby = null;
    }
}
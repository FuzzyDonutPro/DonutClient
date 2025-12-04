package com.donut.client.macros.hunting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * AnimalTracker - Tracks and locates animals for hunting
 * Maintains animal spawn locations and respawn times
 */
public class AnimalTracker {

    private final MinecraftClient mc;

    // Tracked animals
    private Map<AnimalType, List<TrackedAnimal>> trackedAnimals = new HashMap<>();
    private Map<BlockPos, Long> spawnLocations = new HashMap<>();

    // Settings
    private int trackingRadius = 50;
    private long respawnTime = 60000; // 60 seconds

    public enum AnimalType {
        COW, SHEEP, PIG, CHICKEN, RABBIT, HORSE, DONKEY, MULE
    }

    public AnimalTracker() {
        this.mc = MinecraftClient.getInstance();

        // Initialize tracking lists
        for (AnimalType type : AnimalType.values()) {
            trackedAnimals.put(type, new ArrayList<>());
        }
    }

    /**
     * Update tracked animals
     */
    public void update() {
        if (mc.world == null || mc.player == null) return;

        // Clear old tracked animals
        for (AnimalType type : AnimalType.values()) {
            trackedAnimals.get(type).clear();
        }

        // Scan for animals
        for (Entity entity : mc.world.getEntities()) {
            AnimalType type = getAnimalType(entity);

            if (type != null) {
                double distance = mc.player.distanceTo(entity);

                if (distance <= trackingRadius) {
                    TrackedAnimal animal = new TrackedAnimal(
                            entity,
                            type,
                            entity.getBlockPos(),
                            distance
                    );

                    trackedAnimals.get(type).add(animal);
                }
            }
        }

        // Sort by distance
        for (AnimalType type : AnimalType.values()) {
            trackedAnimals.get(type).sort(Comparator.comparingDouble(a -> a.distance));
        }
    }

    /**
     * Get animal type from entity
     */
    private AnimalType getAnimalType(Entity entity) {
        if (entity instanceof CowEntity) return AnimalType.COW;
        if (entity instanceof SheepEntity) return AnimalType.SHEEP;
        if (entity instanceof PigEntity) return AnimalType.PIG;
        if (entity instanceof ChickenEntity) return AnimalType.CHICKEN;
        if (entity instanceof RabbitEntity) return AnimalType.RABBIT;
        if (entity instanceof HorseEntity) return AnimalType.HORSE;
        if (entity instanceof DonkeyEntity) return AnimalType.DONKEY;
        if (entity instanceof MuleEntity) return AnimalType.MULE;
        return null;
    }

    /**
     * Get nearest animal of type
     */
    public TrackedAnimal getNearestAnimal(AnimalType type) {
        List<TrackedAnimal> animals = trackedAnimals.get(type);

        if (animals.isEmpty()) {
            return null;
        }

        return animals.get(0);
    }

    /**
     * Get all animals of type
     */
    public List<TrackedAnimal> getAnimals(AnimalType type) {
        return new ArrayList<>(trackedAnimals.get(type));
    }

    /**
     * Get all tracked animals
     */
    public Map<AnimalType, List<TrackedAnimal>> getAllAnimals() {
        Map<AnimalType, List<TrackedAnimal>> copy = new HashMap<>();

        for (AnimalType type : AnimalType.values()) {
            copy.put(type, new ArrayList<>(trackedAnimals.get(type)));
        }

        return copy;
    }

    /**
     * Get animal count by type
     */
    public int getAnimalCount(AnimalType type) {
        return trackedAnimals.get(type).size();
    }

    /**
     * Get total animal count
     */
    public int getTotalCount() {
        int total = 0;
        for (AnimalType type : AnimalType.values()) {
            total += trackedAnimals.get(type).size();
        }
        return total;
    }

    /**
     * Record spawn location
     */
    public void recordSpawn(BlockPos pos) {
        spawnLocations.put(pos, System.currentTimeMillis());
    }

    /**
     * Check if location ready to respawn
     */
    public boolean isRespawnReady(BlockPos pos) {
        Long lastSpawn = spawnLocations.get(pos);

        if (lastSpawn == null) {
            return true;
        }

        long timeSince = System.currentTimeMillis() - lastSpawn;
        return timeSince >= respawnTime;
    }

    /**
     * Get spawn locations ready for respawn
     */
    public List<BlockPos> getReadySpawnLocations() {
        List<BlockPos> ready = new ArrayList<>();

        for (BlockPos pos : spawnLocations.keySet()) {
            if (isRespawnReady(pos)) {
                ready.add(pos);
            }
        }

        return ready;
    }

    // Getters/Setters
    public void setTrackingRadius(int radius) {
        this.trackingRadius = radius;
    }

    public void setRespawnTime(long millis) {
        this.respawnTime = millis;
    }

    /**
     * Tracked animal class
     */
    public static class TrackedAnimal {
        public final Entity entity;
        public final AnimalType type;
        public final BlockPos position;
        public final double distance;

        public TrackedAnimal(Entity entity, AnimalType type, BlockPos position, double distance) {
            this.entity = entity;
            this.type = type;
            this.position = position;
            this.distance = distance;
        }

        public boolean isAlive() {
            return entity.isAlive();
        }

        @Override
        public String toString() {
            return String.format("%s at %s (%.1fm)", type, position, distance);
        }
    }
}
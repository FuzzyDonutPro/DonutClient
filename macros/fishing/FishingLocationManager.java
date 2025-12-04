package com.donut.client.macros.fishing;

import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * FishingLocationManager - FINAL FIX - createCustomLocation returns FishingLocation
 */
public class FishingLocationManager {

    public enum BiomeType {
        WATER("Water"),
        LAVA("Lava");

        public final String display;
        BiomeType(String display) { this.display = display; }
    }

    public static class FishingLocation {
        public final String name;
        public final BlockPos position;
        public final BlockPos fishingSpot;
        public final List<BlockPos> walkPath;
        public final BiomeType biome;

        public FishingLocation(String name, BlockPos position, BiomeType biome) {
            this.name = name;
            this.position = position;
            this.fishingSpot = position;
            this.walkPath = new ArrayList<>();
            this.biome = biome;
        }

        public FishingLocation(String name, BlockPos position, BlockPos fishingSpot, List<BlockPos> walkPath, BiomeType biome) {
            this.name = name;
            this.position = position;
            this.fishingSpot = fishingSpot;
            this.walkPath = walkPath != null ? walkPath : new ArrayList<>();
            this.biome = biome;
        }

        public static final FishingLocation HUB_POND = new FishingLocation("Hub Pond", new BlockPos(-265, 68, -240), BiomeType.WATER);
        public static final FishingLocation CRIMSON_ISLE = new FishingLocation("Crimson Isle", new BlockPos(-380, 100, -850), BiomeType.LAVA);
    }

    private FishingLocation currentLocation;
    private List<FishingLocation> customLocations;

    public FishingLocationManager() {
        this.currentLocation = FishingLocation.HUB_POND;
        this.customLocations = new ArrayList<>();
    }

    public FishingLocation getLocation(String name) {
        if (name.equals("Hub Pond")) return FishingLocation.HUB_POND;
        if (name.equals("Crimson Isle")) return FishingLocation.CRIMSON_ISLE;

        for (FishingLocation loc : customLocations) {
            if (loc.name.equals(name)) return loc;
        }
        return null;
    }

    public void setCurrentLocation(FishingLocation location) {
        this.currentLocation = location;
    }

    public List<FishingLocation> getAllLocations() {
        List<FishingLocation> all = new ArrayList<>();
        all.add(FishingLocation.HUB_POND);
        all.add(FishingLocation.CRIMSON_ISLE);
        all.addAll(customLocations);
        return all;
    }

    // FIXED: Now returns FishingLocation instead of void
    public FishingLocation createCustomLocation(String name, BlockPos position, List<BlockPos> walkPath) {
        FishingLocation custom = new FishingLocation(name, position, position, walkPath, BiomeType.WATER);
        customLocations.add(custom);
        return custom;  // ← FIXED: Return the created location
    }

    public FishingLocation getCurrentLocation() {
        return currentLocation;
    }

    public BlockPos getCurrentPosition() {
        return currentLocation.position;
    }

    public BiomeType getCurrentBiome() {
        return currentLocation.biome;
    }

    public boolean isLavaFishing() {
        return currentLocation.biome == BiomeType.LAVA;
    }

    public boolean isWaterFishing() {
        return currentLocation.biome == BiomeType.WATER;
    }
}
package com.donut.client.macros.mining;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public class DwarvenCommissions extends Macro {

    private final MinecraftClient mc;
    private CommissionState state = CommissionState.SCANNING;
    private List<Commission> activeCommissions = new ArrayList<>();
    private Commission currentCommission = null;
    private BlockPos targetBlock = null;
    private int maxCommissions = 4;
    private boolean prioritizePowder = true;
    private int commissionsCompleted = 0;
    private int blocksMinedTotal = 0;

    public enum CommissionState {
        SCANNING, MINING, KILLING, CLAIMING, COMPLETE
    }

    public enum CommissionType {
        MITHRIL_MINING, TITANIUM_MINING, HARD_STONE_MINING
    }

    public DwarvenCommissions() {
        super("Dwarven Commissions", "Automated commissions");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        if (mc.player == null) return;
        state = CommissionState.SCANNING;
        System.out.println("[Dwarven] Initialized");
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || mc.world == null) return;

        switch (state) {
            case SCANNING: scanCommissions(); break;
            case MINING: mineBlocks(); break;
            case CLAIMING: claimReward(); break;
        }
    }

    private void scanCommissions() {
        if (activeCommissions.isEmpty()) {
            activeCommissions.add(new Commission(CommissionType.MITHRIL_MINING, 1000, 0, 10000));
        }
        selectNextCommission();
        if (currentCommission != null) state = CommissionState.MINING;
    }

    private void selectNextCommission() {
        for (Commission comm : activeCommissions) {
            if (comm.progress < comm.goal) {
                currentCommission = comm;
                return;
            }
        }
        state = CommissionState.CLAIMING;
    }

    private void mineBlocks() {
        if (currentCommission == null) return;
        if (currentCommission.progress >= currentCommission.goal) {
            selectNextCommission();
            return;
        }

        Block targetBlockType = Blocks.PRISMARINE;
        if (targetBlock == null) {
            targetBlock = findNearestBlock(targetBlockType);
        }
        if (targetBlock != null) {
            breakBlock(targetBlock);
            currentCommission.progress++;
            blocksMinedTotal++;
            targetBlock = null;
        }
    }

    private void claimReward() {
        commissionsCompleted++;
        state = CommissionState.COMPLETE;
    }

    private BlockPos findNearestBlock(Block target) {
        BlockPos playerPos = mc.player.getBlockPos();
        for (int x = -30; x <= 30; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -30; z <= 30; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == target) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private void breakBlock(BlockPos pos) {
        if (mc.player == null) return;
        Vec3d eyes = mc.player.getEyePos();
        Vec3d target = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3d dir = target.subtract(eyes).normalize();
        double yaw = Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        double pitch = -Math.toDegrees(Math.asin(dir.y));
        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    public static class Commission {
        public CommissionType type;
        public int goal;
        public int progress;
        public int powderReward;

        public Commission(CommissionType type, int goal, int progress, int powderReward) {
            this.type = type;
            this.goal = goal;
            this.progress = progress;
            this.powderReward = powderReward;
        }
    }
}
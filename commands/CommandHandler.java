package com.donut.client.commands;

import com.donut.client.DonutClient;
import com.donut.client.utils.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.BoolArgumentType.bool;

public class CommandHandler {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // Main command
            dispatcher.register(literal("donut")
                    .executes(ctx -> {
                        showHelp();
                        return 1;
                    })

                    // /donut rotation <speed>
                    .then(literal("rotation")
                            .then(argument("speed", floatArg(1.0f, 180.0f))
                                    .executes(ctx -> {
                                        float speed = ctx.getArgument("speed", Float.class);
                                        DonutClient.getInstance().getRotationHandler().setRotationSpeed(speed);
                                        ChatUtils.sendSuccess("Rotation speed set to " + speed + "°/tick");
                                        return 1;
                                    })
                            )
                            .executes(ctx -> {
                                float current = DonutClient.getInstance().getRotationHandler().getRotationSpeed();
                                ChatUtils.sendInfo("Current rotation speed: " + current + "°/tick");
                                ChatUtils.sendInfo("Usage: /donut rotation <speed>");
                                return 1;
                            })
                    )

                    // /donut diagonal <true/false>
                    .then(literal("diagonal")
                            .then(argument("enabled", bool())
                                    .executes(ctx -> {
                                        boolean enabled = ctx.getArgument("enabled", Boolean.class);
                                        DonutClient.getInstance().getPathFinder().setAllowDiagonal(enabled);
                                        ChatUtils.sendSuccess("Diagonal movement: " + (enabled ? "Enabled" : "Disabled"));
                                        return 1;
                                    })
                            )
                    )

                    // /donut parkour <true/false>
                    .then(literal("parkour")
                            .then(argument("enabled", bool())
                                    .executes(ctx -> {
                                        boolean enabled = ctx.getArgument("enabled", Boolean.class);
                                        DonutClient.getInstance().getPathFinder().setAllowParkour(enabled);
                                        ChatUtils.sendSuccess("Parkour jumps: " + (enabled ? "Enabled" : "Disabled"));
                                        return 1;
                                    })
                            )
                    )

                    // /donut goto <x> <y> <z>
                    .then(literal("goto")
                            .then(argument("x", integer())
                                    .then(argument("y", integer())
                                            .then(argument("z", integer())
                                                    .executes(ctx -> {
                                                        int x = ctx.getArgument("x", Integer.class);
                                                        int y = ctx.getArgument("y", Integer.class);
                                                        int z = ctx.getArgument("z", Integer.class);

                                                        DonutClient client = DonutClient.getInstance();
                                                        if (client.getMinecraft().player == null) {
                                                            ChatUtils.sendError("Player is null!");
                                                            return 0;
                                                        }

                                                        var playerPos = client.getMinecraft().player.getBlockPos();
                                                        var targetPos = new net.minecraft.util.math.BlockPos(x, y, z);

                                                        ChatUtils.sendInfo("Pathfinding to " + targetPos.toShortString() + "...");

                                                        var path = client.getPathFinder().findPath(playerPos, targetPos);
                                                        if (path != null && !path.isEmpty()) {
                                                            client.getPathExecutor().executePath(path);
                                                        }

                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )

                    // /donut stop
                    .then(literal("stop")
                            .executes(ctx -> {
                                DonutClient.getInstance().getPathExecutor().stopExecution();
                                ChatUtils.sendSuccess("Pathfinding stopped!");
                                return 1;
                            })
                    )

                    // /donut status
                    .then(literal("status")
                            .executes(ctx -> {
                                showStatus();
                                return 1;
                            })
                    )
            );
        });
    }

    private static void showHelp() {
        ChatUtils.sendInfo("=== Donut Client Commands ===");
        ChatUtils.sendInfo("/donut status - Show current settings");
        ChatUtils.sendInfo("/donut rotation <speed> - Set rotation speed (1-180)");
        ChatUtils.sendInfo("/donut diagonal <true/false> - Toggle diagonal movement");
        ChatUtils.sendInfo("/donut parkour <true/false> - Toggle parkour jumps");
        ChatUtils.sendInfo("/donut smooth <true/false> - Toggle path smoothing");
        ChatUtils.sendInfo("/donut goto <x> <y> <z> - Pathfind to coordinates");
        ChatUtils.sendInfo("/donut stop - Stop pathfinding");
    }

    private static void showStatus() {
        DonutClient client = DonutClient.getInstance();

        ChatUtils.sendInfo("=== Donut Client Status ===");
        ChatUtils.sendInfo("Rotation Speed: " + client.getRotationHandler().getRotationSpeed() + "°/tick");
        ChatUtils.sendInfo("Pathfinding: " + (client.getPathExecutor().isExecuting() ? "Active" : "Inactive"));

        if (client.getPathExecutor().isExecuting()) {
            var path = client.getPathExecutor().getCurrentPath();
            int current = client.getPathExecutor().getCurrentNodeIndex();
            if (path != null) {
                ChatUtils.sendInfo("Progress: " + current + "/" + path.size() + " nodes");
            }
        }
    }
}
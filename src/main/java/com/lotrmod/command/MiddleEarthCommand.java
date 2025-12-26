package com.lotrmod.command;

import com.lotrmod.LOTRMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Command to teleport players to Middle-earth dimension
 */
public class MiddleEarthCommand {
    public static final ResourceKey<Level> MIDDLEEARTH_DIMENSION =
            ResourceKey.create(Level.DIMENSION, ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, "middleearth"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("middleearth")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            if (!(source.getEntity() instanceof ServerPlayer player)) {
                                source.sendFailure(Component.literal("This command can only be used by players"));
                                return 0;
                            }

                            ServerLevel middleEarth = source.getServer().getLevel(MIDDLEEARTH_DIMENSION);
                            if (middleEarth == null) {
                                source.sendFailure(Component.literal("Middle-earth dimension not found!"));
                                return 0;
                            }

                            // Teleport to spawn point in Middle-earth (0, highest block, 0)
                            BlockPos spawnPos = new BlockPos(0, 100, 0);

                            // Find the highest solid block at spawn
                            for (int y = middleEarth.getMaxBuildHeight() - 1; y >= middleEarth.getMinBuildHeight(); y--) {
                                BlockPos checkPos = new BlockPos(0, y, 0);
                                if (!middleEarth.getBlockState(checkPos).isAir()) {
                                    spawnPos = checkPos.above();
                                    break;
                                }
                            }

                            Vec3 destination = new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                            TeleportTransition transition = new TeleportTransition(
                                    middleEarth,
                                    destination,
                                    Vec3.ZERO,
                                    player.getYRot(),
                                    player.getXRot(),
                                    TeleportTransition.DO_NOTHING
                            );

                            player.changeDimension(transition);
                            source.sendSuccess(() -> Component.literal("Welcome to Middle-earth!"), true);

                            return 1;
                        })
        );
    }
}

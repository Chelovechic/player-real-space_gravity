package com.spacegravity.spacegravity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class SpaceGravityCommand {
    private SpaceGravityCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spaceGravity")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mode", IntegerArgumentType.integer(0, 1))
                        .executes(context -> applyMode(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "mode"),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> applyMode(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "mode"),
                                        EntityArgument.getPlayers(context, "targets")
                                )))));
    }

    private static int applyMode(CommandSourceStack source, int mode, Collection<ServerPlayer> targets) {
        boolean zeroGravityEnabled = mode == 0;

        for (ServerPlayer target : targets) {
            SpaceGravityState.setZeroGravityEnabled(target, zeroGravityEnabled);
        }

        String stateLabel = zeroGravityEnabled ? "realistic zero gravity" : "vanilla gravity";
        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            source.sendSuccess(() -> Component.literal("Set " + target.getName().getString() + " to " + stateLabel + "."), true);
        } else {
            source.sendSuccess(() -> Component.literal("Set " + targets.size() + " players to " + stateLabel + "."), true);
        }

        return targets.size();
    }
}

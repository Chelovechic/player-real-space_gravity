package com.spacegravity.spacegravity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SpaceGravityCommand {
    private SpaceGravityCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerRoot(dispatcher, "spaceGravity");
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(
                Commands.literal(name)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("mode", IntegerArgumentType.integer(0, 1))
                                .executes(context -> applyMode(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "mode")
                                )))
        );
    }

    private static int applyMode(CommandSourceStack source, int mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean enabled = mode == 0;
        SpaceGravityState.setZeroGravityEnabled(player, enabled);

        String stateLabel = enabled ? "spaceEngine gravity runtime" : "vanilla gravity";
        source.sendSuccess(() -> Component.literal("Set " + player.getName().getString() + " to " + stateLabel + "."), true);
        return 1;
    }
}

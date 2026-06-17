package com.spacegravity.spacegravity.api;

import com.spacegravity.spacegravity.SpaceGravityConfig;
import com.spacegravity.spacegravity.SpaceGravityState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class SpaceGravityApi {
    private SpaceGravityApi() {
    }

    public static boolean isZeroGravityEnabled(Player player) {
        return SpaceGravityState.isZeroGravityEnabled(player);
    }

    public static void setZeroGravityEnabled(ServerPlayer player, boolean enabled) {
        SpaceGravityState.setZeroGravityEnabled(player, enabled);
    }

    public static void enableZeroGravity(ServerPlayer player) {
        setZeroGravityEnabled(player, true);
    }

    public static void disableZeroGravity(ServerPlayer player) {
        setZeroGravityEnabled(player, false);
    }

    public static double getPushReach() {
        return SpaceGravityConfig.pushReach();
    }
}

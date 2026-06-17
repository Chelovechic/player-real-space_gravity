package com.spacegravity.spacegravity;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpaceGravityConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue PUSH_REACH;
    private static Double syncedPushReach;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        PUSH_REACH = builder
                .defineInRange("push_reach", 0.95D, 0.0D, 16.0D);
        SPEC = builder.build();
    }

    private SpaceGravityConfig() {
    }

    public static double pushReach() {
        return syncedPushReach != null ? syncedPushReach : PUSH_REACH.get();
    }

    public static void setSyncedPushReach(double pushReach) {
        syncedPushReach = pushReach;
    }

    public static void clearSyncedPushReach() {
        syncedPushReach = null;
    }
}

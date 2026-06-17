package com.spacegravity.spacegravity;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpaceGravityConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue PUSH_REACH;
    private static Double syncedPushReach;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
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

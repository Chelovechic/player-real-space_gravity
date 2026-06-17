package com.spacegravity.spacegravity;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class ZeroGravityRenderState {
    private static final double MIN_FRAME_SECONDS = 1.0D / 240.0D;
    private static final double MAX_FRAME_SECONDS = 0.05D;
    private static long lastFrameNanos;

    private ZeroGravityRenderState() {
    }

    public static double consumeFrameSeconds() {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return 1.0D / 120.0D;
        }

        double frameSeconds = (double) (now - lastFrameNanos) / 1.0E9D;
        lastFrameNanos = now;
        return Mth.clamp(frameSeconds, MIN_FRAME_SECONDS, MAX_FRAME_SECONDS);
    }

    public static float partialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    public static void reset() {
        lastFrameNanos = 0L;
    }
}

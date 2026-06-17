package com.spacegravity.spacegravity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncZeroGravityPacket(boolean zeroGravityEnabled, double pushReach) {
    public static void encode(SyncZeroGravityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.zeroGravityEnabled);
        buffer.writeDouble(packet.pushReach);
    }

    public static SyncZeroGravityPacket decode(FriendlyByteBuf buffer) {
        return new SyncZeroGravityPacket(buffer.readBoolean(), buffer.readDouble());
    }

    public static void handle(SyncZeroGravityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    SpaceGravityConfig.setSyncedPushReach(packet.pushReach);
                    ClientZeroGravityController.setZeroGravityEnabled(packet.zeroGravityEnabled);
                }));
        context.setPacketHandled(true);
    }
}

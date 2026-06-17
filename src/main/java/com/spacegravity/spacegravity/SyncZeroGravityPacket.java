package com.spacegravity.spacegravity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncZeroGravityPacket(boolean zeroGravityEnabled, double pushReach) implements CustomPacketPayload {
    public static final Type<SyncZeroGravityPacket> TYPE = new Type<>(SpaceGravityNetwork.payloadId("sync_zero_gravity"));
    public static final StreamCodec<FriendlyByteBuf, SyncZeroGravityPacket> STREAM_CODEC = StreamCodec.ofMember(
            SyncZeroGravityPacket::encode,
            SyncZeroGravityPacket::new
    );

    private SyncZeroGravityPacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean(), buffer.readDouble());
    }

    private void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.zeroGravityEnabled);
        buffer.writeDouble(this.pushReach);
    }

    public static void handle(SyncZeroGravityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            SpaceGravityConfig.setSyncedPushReach(packet.pushReach);
            ClientZeroGravityController.setZeroGravityEnabled(packet.zeroGravityEnabled);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

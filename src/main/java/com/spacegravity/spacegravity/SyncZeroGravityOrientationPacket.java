package com.spacegravity.spacegravity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncZeroGravityOrientationPacket(
        int entityId,
        boolean zeroGravityEnabled,
        float forwardX,
        float forwardY,
        float forwardZ,
        float upX,
        float upY,
        float upZ,
        int pushLimbId,
        float contactX,
        float contactY,
        float contactZ
) implements CustomPacketPayload {
    public static final Type<SyncZeroGravityOrientationPacket> TYPE = new Type<>(SpaceGravityNetwork.payloadId("sync_zero_gravity_orientation"));
    public static final StreamCodec<FriendlyByteBuf, SyncZeroGravityOrientationPacket> STREAM_CODEC = StreamCodec.ofMember(
            SyncZeroGravityOrientationPacket::encode,
            SyncZeroGravityOrientationPacket::new
    );

    public SyncZeroGravityOrientationPacket(int entityId, boolean zeroGravityEnabled, ZeroGravityOrientation.OrientationData orientation, ZeroGravityPushData pushData) {
        this(
                entityId,
                zeroGravityEnabled,
                (float) orientation.forward().x,
                (float) orientation.forward().y,
                (float) orientation.forward().z,
                (float) orientation.up().x,
                (float) orientation.up().y,
                (float) orientation.up().z,
                pushData.limb().networkId(),
                (float) pushData.contactOffset().x,
                (float) pushData.contactOffset().y,
                (float) pushData.contactOffset().z
        );
    }

    private SyncZeroGravityOrientationPacket(FriendlyByteBuf buffer) {
        this(
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public ZeroGravityOrientation.OrientationData orientation() {
        return ZeroGravityOrientation.normalize(
                new Vec3(this.forwardX, this.forwardY, this.forwardZ),
                new Vec3(this.upX, this.upY, this.upZ)
        );
    }

    public ZeroGravityPushData pushData() {
        return new ZeroGravityPushData(
                ZeroGravityPushData.ContactLimb.fromNetworkId(this.pushLimbId),
                new Vec3(this.contactX, this.contactY, this.contactZ)
        );
    }

    private void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.entityId());
        buffer.writeBoolean(this.zeroGravityEnabled());
        buffer.writeFloat(this.forwardX());
        buffer.writeFloat(this.forwardY());
        buffer.writeFloat(this.forwardZ());
        buffer.writeFloat(this.upX());
        buffer.writeFloat(this.upY());
        buffer.writeFloat(this.upZ());
        buffer.writeVarInt(this.pushLimbId());
        buffer.writeFloat(this.contactX());
        buffer.writeFloat(this.contactY());
        buffer.writeFloat(this.contactZ());
    }

    public static void handle(SyncZeroGravityOrientationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientZeroGravityController.setRemoteOrientation(packet.entityId(), packet.zeroGravityEnabled(), packet.orientation(), packet.pushData()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

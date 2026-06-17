package com.spacegravity.spacegravity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
) {
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

    public static void encode(SyncZeroGravityOrientationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeBoolean(packet.zeroGravityEnabled());
        buffer.writeFloat(packet.forwardX());
        buffer.writeFloat(packet.forwardY());
        buffer.writeFloat(packet.forwardZ());
        buffer.writeFloat(packet.upX());
        buffer.writeFloat(packet.upY());
        buffer.writeFloat(packet.upZ());
        buffer.writeVarInt(packet.pushLimbId());
        buffer.writeFloat(packet.contactX());
        buffer.writeFloat(packet.contactY());
        buffer.writeFloat(packet.contactZ());
    }

    public static SyncZeroGravityOrientationPacket decode(FriendlyByteBuf buffer) {
        return new SyncZeroGravityOrientationPacket(
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

    public static void handle(SyncZeroGravityOrientationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientZeroGravityController.setRemoteOrientation(packet.entityId(), packet.zeroGravityEnabled(), packet.orientation(), packet.pushData())));
        context.setPacketHandled(true);
    }
}

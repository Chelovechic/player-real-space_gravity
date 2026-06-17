package com.spacegravity.spacegravity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundZeroGravityInputPacket(ZeroGravityInputState inputState) {
    public static void encode(ServerboundZeroGravityInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.inputState.forwardImpulse());
        buffer.writeFloat(packet.inputState.strafeImpulse());
        buffer.writeFloat(packet.inputState.verticalImpulse());
        buffer.writeBoolean(packet.inputState.boosted());
        buffer.writeFloat(packet.inputState.forwardX());
        buffer.writeFloat(packet.inputState.forwardY());
        buffer.writeFloat(packet.inputState.forwardZ());
        buffer.writeFloat(packet.inputState.upX());
        buffer.writeFloat(packet.inputState.upY());
        buffer.writeFloat(packet.inputState.upZ());
    }

    public static ServerboundZeroGravityInputPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundZeroGravityInputPacket(new ZeroGravityInputState(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        ));
    }

    public static void handle(ServerboundZeroGravityInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                SpaceGravityState.handleClientInput(context.getSender(), packet.inputState);
            }
        });
        context.setPacketHandled(true);
    }
}

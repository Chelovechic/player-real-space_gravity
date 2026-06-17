package com.spacegravity.spacegravity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundZeroGravityInputPacket(ZeroGravityInputState inputState) implements CustomPacketPayload {
    public static final Type<ServerboundZeroGravityInputPacket> TYPE = new Type<>(SpaceGravityNetwork.payloadId("zero_gravity_input"));
    public static final StreamCodec<FriendlyByteBuf, ServerboundZeroGravityInputPacket> STREAM_CODEC = StreamCodec.ofMember(
            ServerboundZeroGravityInputPacket::encode,
            ServerboundZeroGravityInputPacket::new
    );

    private ServerboundZeroGravityInputPacket(FriendlyByteBuf buffer) {
        this(new ZeroGravityInputState(
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

    private void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.inputState.forwardImpulse());
        buffer.writeFloat(this.inputState.strafeImpulse());
        buffer.writeFloat(this.inputState.verticalImpulse());
        buffer.writeBoolean(this.inputState.boosted());
        buffer.writeFloat(this.inputState.forwardX());
        buffer.writeFloat(this.inputState.forwardY());
        buffer.writeFloat(this.inputState.forwardZ());
        buffer.writeFloat(this.inputState.upX());
        buffer.writeFloat(this.inputState.upY());
        buffer.writeFloat(this.inputState.upZ());
    }

    public static void handle(ServerboundZeroGravityInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpaceGravityState.handleClientInput(player, packet.inputState);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

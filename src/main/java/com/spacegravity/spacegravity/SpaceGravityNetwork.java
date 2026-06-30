package com.spacegravity.spacegravity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SpaceGravityNetwork {
    private static final String PROTOCOL_VERSION = "7";

    private SpaceGravityNetwork() {
    }

    public static ResourceLocation payloadId(String path) {
        return ResourceLocation.fromNamespaceAndPath(SpaceGravityMod.MODID, path);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(SyncZeroGravityPacket.TYPE, SyncZeroGravityPacket.STREAM_CODEC, SyncZeroGravityPacket::handle);
        registrar.playToClient(SyncZeroGravityOrientationPacket.TYPE, SyncZeroGravityOrientationPacket.STREAM_CODEC, SyncZeroGravityOrientationPacket::handle);
        registrar.playToServer(ServerboundZeroGravityInputPacket.TYPE, ServerboundZeroGravityInputPacket.STREAM_CODEC, ServerboundZeroGravityInputPacket::handle);
    }

    public static void syncPlayer(ServerPlayer player, boolean zeroGravityEnabled) {
        PacketDistributor.sendToPlayer(player, new SyncZeroGravityPacket(zeroGravityEnabled, SpaceGravityConfig.pushReach()));
    }

    public static void syncOrientationToTracking(ServerPlayer player, boolean zeroGravityEnabled, ZeroGravityOrientation.OrientationData orientation, ZeroGravityPushData pushData) {
        PacketDistributor.sendToPlayersTrackingEntity(player, new SyncZeroGravityOrientationPacket(player.getId(), zeroGravityEnabled, orientation, pushData));
    }

    public static void syncOrientationToPlayer(ServerPlayer recipient, ServerPlayer subject, boolean zeroGravityEnabled, ZeroGravityOrientation.OrientationData orientation, ZeroGravityPushData pushData) {
        PacketDistributor.sendToPlayer(recipient, new SyncZeroGravityOrientationPacket(subject.getId(), zeroGravityEnabled, orientation, pushData));
    }

    public static void sendInputToServer(ZeroGravityInputState inputState) {
        PacketDistributor.sendToServer(new ServerboundZeroGravityInputPacket(inputState));
    }
}

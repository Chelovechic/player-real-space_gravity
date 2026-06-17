package com.spacegravity.spacegravity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class SpaceGravityNetwork {
    private static final String PROTOCOL_VERSION = "5";
    private static int nextMessageId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SpaceGravityMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private SpaceGravityNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextMessageId++,
                SyncZeroGravityPacket.class,
                SyncZeroGravityPacket::encode,
                SyncZeroGravityPacket::decode,
                SyncZeroGravityPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextMessageId++,
                SyncZeroGravityOrientationPacket.class,
                SyncZeroGravityOrientationPacket::encode,
                SyncZeroGravityOrientationPacket::decode,
                SyncZeroGravityOrientationPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextMessageId++,
                ServerboundZeroGravityInputPacket.class,
                ServerboundZeroGravityInputPacket::encode,
                ServerboundZeroGravityInputPacket::decode,
                ServerboundZeroGravityInputPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static void syncPlayer(ServerPlayer player, boolean zeroGravityEnabled) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncZeroGravityPacket(zeroGravityEnabled, SpaceGravityConfig.pushReach()));
    }

    public static void syncOrientationToTracking(ServerPlayer player, boolean zeroGravityEnabled, ZeroGravityOrientation.OrientationData orientation, ZeroGravityPushData pushData) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), new SyncZeroGravityOrientationPacket(player.getId(), zeroGravityEnabled, orientation, pushData));
    }

    public static void syncOrientationToPlayer(ServerPlayer recipient, ServerPlayer subject, boolean zeroGravityEnabled, ZeroGravityOrientation.OrientationData orientation, ZeroGravityPushData pushData) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), new SyncZeroGravityOrientationPacket(subject.getId(), zeroGravityEnabled, orientation, pushData));
    }

    public static void sendInputToServer(ZeroGravityInputState inputState) {
        CHANNEL.sendToServer(new ServerboundZeroGravityInputPacket(inputState));
    }
}

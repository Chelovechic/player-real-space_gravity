package com.spacegravity.spacegravity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpaceGravityState {
    private static final String ROOT_TAG = SpaceGravityMod.MODID;
    private static final String ZERO_GRAVITY_ENABLED_TAG = "zeroGravityEnabled";
    private static final String SAVED_MAYFLY_TAG = "savedMayfly";
    private static final String SAVED_FLYING_TAG = "savedFlying";
    private static final Map<UUID, RuntimeState> RUNTIME_STATES = new HashMap<>();

    private SpaceGravityState() {
    }

    public static boolean isZeroGravityEnabled(Player player) {
        CompoundTag data = getOrCreateData(player);
        return data.contains(ZERO_GRAVITY_ENABLED_TAG, Tag.TAG_BYTE) && data.getBoolean(ZERO_GRAVITY_ENABLED_TAG);
    }

    public static void setZeroGravityEnabled(ServerPlayer player, boolean enabled) {
        boolean wasEnabled = isZeroGravityEnabled(player);
        writeEnabledFlag(player, enabled);

        if (enabled && !wasEnabled) {
            saveCurrentAbilities(player);
            initializeRuntime(player);
        }

        if (enabled) {
            applyZeroGravityRuntime(player);
            player.connection.resetPosition();
        } else {
            restoreSavedAbilities(player);
            player.setNoGravity(false);
            if (player.getForcedPose() == Pose.SWIMMING) {
                player.setForcedPose(null);
                player.refreshDimensions();
            }
            syncOrientation(player, false, ZeroGravityOrientation.fromVanillaAngles(player.getYRot(), player.getXRot()), ZeroGravityPushData.none());
            clearRuntime(player);
            player.connection.resetPosition();
        }

        SpaceGravityNetwork.syncPlayer(player, enabled);
        if (enabled) {
            syncOrientation(player, true, getOrCreateRuntime(player).orientation, ZeroGravityPushData.none());
        }
    }

    public static void applyPersistedState(ServerPlayer player) {
        if (isZeroGravityEnabled(player)) {
            initializeRuntime(player);
            applyZeroGravityRuntime(player);
            player.connection.resetPosition();
            syncOrientation(player, true, getOrCreateRuntime(player).orientation, ZeroGravityPushData.none());
        } else {
            restoreSavedAbilities(player);
            player.setNoGravity(false);
            if (player.getForcedPose() == Pose.SWIMMING) {
                player.setForcedPose(null);
                player.refreshDimensions();
            }
            clearRuntime(player);
        }

        SpaceGravityNetwork.syncPlayer(player, isZeroGravityEnabled(player));
    }

    public static void maintainZeroGravity(ServerPlayer player) {
        if (!isZeroGravityEnabled(player)) {
            return;
        }

        applyZeroGravityRuntime(player);
    }

    public static void captureMeasuredMovement(ServerPlayer player) {
        if (!isZeroGravityEnabled(player)) {
            return;
        }

        RuntimeState runtimeState = getOrCreateRuntime(player);
        runtimeState.measuredVelocity = ZeroGravityPhysics.measureMovement(runtimeState.lastPosition, player.position());
        runtimeState.lastPosition = player.position();
        player.setDeltaMovement(runtimeState.measuredVelocity);
    }

    public static void handleClientInput(ServerPlayer player, ZeroGravityInputState inputState) {
        if (!isZeroGravityEnabled(player) || player.isPassenger() || player.isSleeping()) {
            return;
        }

        RuntimeState runtimeState = getOrCreateRuntime(player);
        applyZeroGravityRuntime(player);
        runtimeState.orientation = inputState.orientation();
        ZeroGravityPushHelper.PushSurface pushSurface = ZeroGravityPushHelper.findNearestPushSurface(player, runtimeState.orientation);
        Vec3 thrustDirection = ZeroGravityPhysics.computeThrustDirection(inputState);
        boolean canPushOff = pushSurface.available();
        Vec3 nextVelocity = ZeroGravityPhysics.computeNextVelocity(runtimeState.measuredVelocity, inputState, canPushOff);
        ZeroGravityPushData pushData = canPushOff && thrustDirection.lengthSqr() > 1.0E-6D
                ? pushSurface.toPushData(player.getBoundingBox().getCenter())
                : ZeroGravityPushData.none();
        player.setDeltaMovement(nextVelocity);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.setOnGround(false);
        player.resetFallDistance();
        syncOrientation(player, true, runtimeState.orientation, pushData);
    }

    public static void clearRuntime(ServerPlayer player) {
        RUNTIME_STATES.remove(player.getUUID());
    }

    public static void syncTrackedOrientationTo(ServerPlayer recipient, ServerPlayer target) {
        if (!isZeroGravityEnabled(target)) {
            return;
        }

        RuntimeState runtimeState = getOrCreateRuntime(target);
        SpaceGravityNetwork.syncOrientationToPlayer(recipient, target, true, runtimeState.orientation, ZeroGravityPushData.none());
    }

    private static void applyZeroGravityRuntime(ServerPlayer player) {
        boolean abilitiesChanged = false;

        player.setNoGravity(true);
        player.setOnGround(false);
        player.resetFallDistance();
        if (player.getForcedPose() != Pose.SWIMMING) {
            player.setForcedPose(Pose.SWIMMING);
            player.refreshDimensions();
        }

        if (player.isFallFlying()) {
            player.stopFallFlying();
        }

        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            abilitiesChanged = true;
        }

        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            abilitiesChanged = true;
        }

        if (abilitiesChanged) {
            player.onUpdateAbilities();
        }
    }

    private static void saveCurrentAbilities(ServerPlayer player) {
        CompoundTag data = getOrCreateData(player);
        data.putBoolean(SAVED_MAYFLY_TAG, player.getAbilities().mayfly);
        data.putBoolean(SAVED_FLYING_TAG, player.getAbilities().flying);
        saveData(player, data);
    }

    private static void restoreSavedAbilities(ServerPlayer player) {
        CompoundTag data = getOrCreateData(player);
        boolean hasSavedMayfly = data.contains(SAVED_MAYFLY_TAG, Tag.TAG_BYTE);
        boolean hasSavedFlying = data.contains(SAVED_FLYING_TAG, Tag.TAG_BYTE);

        if (!hasSavedMayfly && !hasSavedFlying) {
            return;
        }

        boolean desiredMayfly = player.isSpectator() || player.getAbilities().instabuild || data.getBoolean(SAVED_MAYFLY_TAG);
        boolean desiredFlying = player.isSpectator() || (desiredMayfly && data.getBoolean(SAVED_FLYING_TAG));
        boolean abilitiesChanged = false;

        if (player.getAbilities().mayfly != desiredMayfly) {
            player.getAbilities().mayfly = desiredMayfly;
            abilitiesChanged = true;
        }

        if (player.getAbilities().flying != desiredFlying) {
            player.getAbilities().flying = desiredFlying;
            abilitiesChanged = true;
        }

        data.remove(SAVED_MAYFLY_TAG);
        data.remove(SAVED_FLYING_TAG);
        saveData(player, data);

        if (abilitiesChanged) {
            player.onUpdateAbilities();
        }
    }

    private static void writeEnabledFlag(ServerPlayer player, boolean enabled) {
        CompoundTag data = getOrCreateData(player);
        data.putBoolean(ZERO_GRAVITY_ENABLED_TAG, enabled);
        saveData(player, data);
    }

    private static void initializeRuntime(ServerPlayer player) {
        RuntimeState runtimeState = getOrCreateRuntime(player);
        runtimeState.measuredVelocity = Vec3.ZERO;
        runtimeState.lastPosition = player.position();
        runtimeState.orientation = ZeroGravityOrientation.fromVanillaAngles(player.getYRot(), player.getXRot());
    }

    private static RuntimeState getOrCreateRuntime(ServerPlayer player) {
        return RUNTIME_STATES.computeIfAbsent(
                player.getUUID(),
                key -> new RuntimeState(player.position(), ZeroGravityOrientation.fromVanillaAngles(player.getYRot(), player.getXRot()))
        );
    }

    private static CompoundTag getOrCreateData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistentData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }

        CompoundTag persistedRoot = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persistedRoot.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistedRoot.put(ROOT_TAG, new CompoundTag());
            persistentData.put(Player.PERSISTED_NBT_TAG, persistedRoot);
        }

        return persistedRoot.getCompound(ROOT_TAG);
    }

    private static void saveData(Player player, CompoundTag data) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedRoot = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistedRoot.put(ROOT_TAG, data);
        persistentData.put(Player.PERSISTED_NBT_TAG, persistedRoot);
    }

    private static void syncOrientation(ServerPlayer player, boolean enabled, ZeroGravityOrientation.OrientationData orientation, ZeroGravityPushData pushData) {
        SpaceGravityNetwork.syncOrientationToTracking(player, enabled, orientation, pushData);
    }

    private static final class RuntimeState {
        private Vec3 measuredVelocity = Vec3.ZERO;
        private Vec3 lastPosition;
        private ZeroGravityOrientation.OrientationData orientation;

        private RuntimeState(Vec3 lastPosition, ZeroGravityOrientation.OrientationData orientation) {
            this.lastPosition = lastPosition;
            this.orientation = orientation;
        }
    }
}

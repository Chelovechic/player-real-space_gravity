package com.spacegravity.spacegravity;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = SpaceGravityMod.MODID, value = Dist.CLIENT)
public final class ClientZeroGravityController {
    private static final double MAX_ROLL_SPEED_DEGREES_PER_SECOND = 168.0D;
    private static final double ROLL_ACCELERATION_DEGREES_PER_SECOND_SQUARED = 150.0D;
    private static final double ROLL_DECELERATION_DEGREES_PER_SECOND_SQUARED = 50.0D;
    private static final int PUSH_ANIMATION_TICKS = 6;
    private static boolean zeroGravityEnabled;
    private static Vec3 measuredVelocity = Vec3.ZERO;
    private static Vec3 lastPosition = Vec3.ZERO;
    private static int trackedPlayerId = Integer.MIN_VALUE;
    private static boolean trackingInitialized;
    private static ZeroGravityOrientation.OrientationData localOrientation = ZeroGravityOrientation.identity();
    private static boolean localOrientationInitialized;
    private static double lastMouseX;
    private static double lastMouseY;
    private static boolean mouseSampleInitialized;
    private static final Map<Integer, ZeroGravityOrientation.OrientationData> REMOTE_ORIENTATIONS = new HashMap<>();
    private static final Map<Integer, PushAnimationState> REMOTE_PUSH_ANIMATIONS = new HashMap<>();
    private static final Set<Integer> RENDER_TRANSFORMS = new HashSet<>();
    private static PushAnimationState localPushAnimation = PushAnimationState.idle();
    private static double rollVelocityDegreesPerSecond;

    private ClientZeroGravityController() {
    }

    public static void setZeroGravityEnabled(boolean enabled) {
        zeroGravityEnabled = enabled;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            if (enabled) {
                initializeTracking(player);
                initializeLocalOrientation(player);
                applyClientState(player);
                applyOrientationToPlayer(player, localOrientation);
            } else {
                clearClientState(player);
                clearOrientationForPlayer(player);
            }
        }

        if (!enabled) {
            resetTracking();
            resetLocalOrientation();
            localPushAnimation = PushAnimationState.idle();
            rollVelocityDegreesPerSecond = 0.0D;
            ZeroGravityRenderState.reset();
        }
    }

    public static void setRemoteOrientation(int entityId, boolean enabled, ZeroGravityOrientation.OrientationData orientation, ZeroGravityPushData pushData) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && entityId == minecraft.player.getId()) {
            return;
        }

        if (enabled) {
            REMOTE_ORIENTATIONS.put(entityId, orientation);
            if (pushData.active()) {
                REMOTE_PUSH_ANIMATIONS.put(entityId, PushAnimationState.active(pushData));
            }
        } else {
            REMOTE_ORIENTATIONS.remove(entityId);
            REMOTE_PUSH_ANIMATIONS.remove(entityId);
        }

        if (minecraft.level != null && minecraft.level.getEntity(entityId) instanceof Player player) {
            if (enabled) {
                applyOrientationToPlayer(player, orientation);
            } else {
                clearOrientationForPlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        if (!zeroGravityEnabled) {
            return;
        }

        ensureTracking(player);
        updateLocalOrientation(player);
        applyClientState(player);
        applyOrientationToPlayer(player, localOrientation);
        player.setOnGround(false);
        player.resetFallDistance();
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        if (!zeroGravityEnabled) {
            return;
        }

        ensureTracking(player);
        measuredVelocity = ZeroGravityPhysics.measureMovement(lastPosition, player.position());
        lastPosition = player.position();
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !zeroGravityEnabled) {
            return;
        }

        if (player.isPassenger() || player.isSleeping()) {
            return;
        }

        ensureTracking(player);
        updateLocalOrientation(player);

        Input input = event.getInput();
        ZeroGravityOrientation.OrientationData orientation = localOrientation;
        ZeroGravityInputState inputState = new ZeroGravityInputState(
                SpaceGravityKeyMappings.forwardImpulse(),
                0.0F,
                SpaceGravityKeyMappings.verticalImpulse(),
                SpaceGravityKeyMappings.boosted(),
                (float) orientation.forward().x,
                (float) orientation.forward().y,
                (float) orientation.forward().z,
                (float) orientation.up().x,
                (float) orientation.up().y,
                (float) orientation.up().z
        );

        applyClientState(player);
        applyOrientationToPlayer(player, orientation);
        player.setOnGround(false);
        player.resetFallDistance();
        ZeroGravityPushHelper.PushSurface pushSurface = ZeroGravityPushHelper.findNearestPushSurface(player, orientation);
        boolean canPushOff = pushSurface.available();
        Vec3 thrustDirection = ZeroGravityPhysics.computeThrustDirection(inputState);
        if (canPushOff && thrustDirection.lengthSqr() > 1.0E-6D) {
            localPushAnimation = PushAnimationState.active(pushSurface.toPushData(player.getBoundingBox().getCenter()));
        }

        player.setDeltaMovement(ZeroGravityPhysics.computeNextVelocity(measuredVelocity, inputState, canPushOff));
        SpaceGravityNetwork.sendInputToServer(inputState);

        clearVanillaInput(input);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            REMOTE_ORIENTATIONS.clear();
            REMOTE_PUSH_ANIMATIONS.clear();
            return;
        }

        if (zeroGravityEnabled && minecraft.player != null) {
            updateLocalOrientation(minecraft.player);
            applyOrientationToPlayer(minecraft.player, localOrientation);
            ZeroGravityClientRenderAccess.applyZeroGravityVisualState(minecraft.player);
        }

        localPushAnimation = localPushAnimation.tick();

        Iterator<Map.Entry<Integer, ZeroGravityOrientation.OrientationData>> iterator = REMOTE_ORIENTATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ZeroGravityOrientation.OrientationData> entry = iterator.next();
            if (minecraft.level.getEntity(entry.getKey()) instanceof Player player) {
                applyOrientationToPlayer(player, entry.getValue());
                ZeroGravityClientRenderAccess.applyZeroGravityVisualState(player);
            } else {
                iterator.remove();
            }
        }

        Iterator<Map.Entry<Integer, PushAnimationState>> pushIterator = REMOTE_PUSH_ANIMATIONS.entrySet().iterator();
        while (pushIterator.hasNext()) {
            Map.Entry<Integer, PushAnimationState> entry = pushIterator.next();
            PushAnimationState next = entry.getValue().tick();
            if (next.active()) {
                entry.setValue(next);
            } else {
                pushIterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        if (!zeroGravityEnabled || minecraft.player == null || camera.getEntity() != minecraft.player) {
            return;
        }

        updateLocalOrientation(minecraft.player);
        applyFrameRollInput(minecraft);
        applyOrientationToPlayer(minecraft.player, localOrientation);

        ZeroGravityOrientation.CameraAngles angles = ZeroGravityOrientation.toCameraAngles(localOrientation);
        event.setYaw(angles.yaw());
        event.setPitch(angles.pitch());
        event.setRoll(angles.roll());
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        ZeroGravityOrientation.OrientationData orientation = getOrientationForPlayer(event.getEntity());
        PushAnimationState pushAnimation = getPushAnimationForPlayer(event.getEntity());
        if (orientation == null && !pushAnimation.active()) {
            return;
        }

        event.getPoseStack().pushPose();

        if (pushAnimation.active()) {
            double progress = pushAnimation.progress(event.getPartialTick());
            Vec3 offset = pushAnimation.pushDirection().scale(0.18D * easePush(progress));
            event.getPoseStack().translate(offset.x, offset.y, offset.z);
        }

        if (orientation != null) {
            Vec3 forward = orientation.forward();
            Vec3 baseForward = ZeroGravityOrientation.horizontalForward(forward, ZeroGravityOrientation.toCameraAngles(orientation).yaw());

            if (baseForward.dot(forward) < 0.99999D) {
                event.getPoseStack().mulPose(new Quaternionf().rotationTo(
                        (float) baseForward.x,
                        (float) baseForward.y,
                        (float) baseForward.z,
                        (float) forward.x,
                        (float) forward.y,
                        (float) forward.z
                ));
            }

            float roll = ZeroGravityOrientation.toCameraAngles(orientation).roll();
            if (Math.abs(roll) > 0.01F) {
                event.getPoseStack().mulPose(new Quaternionf().fromAxisAngleRad(
                        (float) forward.x,
                        (float) forward.y,
                        (float) forward.z,
                        (float) Math.toRadians(roll)
                ));
            }
        }

        RENDER_TRANSFORMS.add(event.getEntity().getId());
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (RENDER_TRANSFORMS.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        zeroGravityEnabled = false;
        SpaceGravityConfig.clearSyncedPushReach();
        resetTracking();
        resetLocalOrientation();
        REMOTE_ORIENTATIONS.clear();
        REMOTE_PUSH_ANIMATIONS.clear();
        localPushAnimation = PushAnimationState.idle();
        rollVelocityDegreesPerSecond = 0.0D;
        ZeroGravityRenderState.reset();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        zeroGravityEnabled = false;
        SpaceGravityConfig.clearSyncedPushReach();
        resetTracking();
        resetLocalOrientation();
        REMOTE_ORIENTATIONS.clear();
        REMOTE_PUSH_ANIMATIONS.clear();
        RENDER_TRANSFORMS.clear();
        localPushAnimation = PushAnimationState.idle();
        rollVelocityDegreesPerSecond = 0.0D;
        ZeroGravityRenderState.reset();

        LocalPlayer player = event.getPlayer();
        if (player != null) {
            clearClientState(player);
            clearOrientationForPlayer(player);
        }
    }

    private static void applyClientState(LocalPlayer player) {
        player.setNoGravity(true);
        player.getAbilities().flying = false;
        player.resetFallDistance();

        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
    }

    private static void clearClientState(LocalPlayer player) {
        player.setNoGravity(false);
        ZeroGravityClientRenderAccess.clearZeroGravityVisualState(player);
    }

    private static void ensureTracking(LocalPlayer player) {
        if (!trackingInitialized || trackedPlayerId != player.getId()) {
            initializeTracking(player);
        }
    }

    private static void initializeTracking(LocalPlayer player) {
        trackedPlayerId = player.getId();
        lastPosition = player.position();
        measuredVelocity = Vec3.ZERO;
        trackingInitialized = true;
    }

    private static void resetTracking() {
        trackedPlayerId = Integer.MIN_VALUE;
        lastPosition = Vec3.ZERO;
        measuredVelocity = Vec3.ZERO;
        trackingInitialized = false;
    }

    private static void initializeLocalOrientation(LocalPlayer player) {
        localOrientation = ZeroGravityOrientation.fromVanillaAngles(player.getYRot(), player.getXRot());
        localOrientationInitialized = true;

        Minecraft minecraft = Minecraft.getInstance();
        lastMouseX = minecraft.mouseHandler.xpos();
        lastMouseY = minecraft.mouseHandler.ypos();
        mouseSampleInitialized = true;
    }

    private static void resetLocalOrientation() {
        localOrientation = ZeroGravityOrientation.identity();
        localOrientationInitialized = false;
        mouseSampleInitialized = false;
        lastMouseX = 0.0D;
        lastMouseY = 0.0D;
        rollVelocityDegreesPerSecond = 0.0D;
        ZeroGravityRenderState.reset();
    }

    private static void updateLocalOrientation(LocalPlayer player) {
        if (!zeroGravityEnabled) {
            return;
        }

        if (!localOrientationInitialized) {
            initializeLocalOrientation(player);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();

        if (!mouseSampleInitialized) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            mouseSampleInitialized = true;
            return;
        }

        double deltaX = mouseX - lastMouseX;
        double deltaY = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        if (minecraft.screen != null || !minecraft.isWindowActive() || !minecraft.mouseHandler.isMouseGrabbed()) {
            return;
        }

        if (Math.abs(deltaX) < 1.0E-6D && Math.abs(deltaY) < 1.0E-6D) {
            return;
        }

        double sensitivity = minecraft.options.sensitivity().get() * 0.6D + 0.2D;
        double factor = sensitivity * sensitivity * sensitivity * 8.0D * 0.15D;
        double yawDegrees = -deltaX * factor;
        double pitchDegrees = deltaY * factor * (minecraft.options.invertYMouse().get() ? -1.0D : 1.0D);

        localOrientation = ZeroGravityOrientation.rotate(localOrientation, yawDegrees, pitchDegrees);
    }

    private static void applyFrameRollInput(Minecraft minecraft) {
        if (minecraft.screen != null || !minecraft.isWindowActive() || !minecraft.mouseHandler.isMouseGrabbed()) {
            return;
        }

        double frameSeconds = ZeroGravityRenderState.consumeFrameSeconds();
        double rollInput = SpaceGravityKeyMappings.rollInput();
        double targetRollVelocity = rollInput * MAX_ROLL_SPEED_DEGREES_PER_SECOND;
        double acceleration = rollInput == 0.0D
                ? ROLL_DECELERATION_DEGREES_PER_SECOND_SQUARED
                : ROLL_ACCELERATION_DEGREES_PER_SECOND_SQUARED;
        rollVelocityDegreesPerSecond = approach(rollVelocityDegreesPerSecond, targetRollVelocity, acceleration * frameSeconds);

        if (Math.abs(rollVelocityDegreesPerSecond) < 1.0E-2D && Math.abs(targetRollVelocity) < 1.0E-2D) {
            rollVelocityDegreesPerSecond = 0.0D;
            return;
        }

        localOrientation = ZeroGravityOrientation.roll(localOrientation, rollVelocityDegreesPerSecond * frameSeconds);
    }

    private static double approach(double current, double target, double step) {
        if (current < target) {
            return Math.min(current + step, target);
        }

        if (current > target) {
            return Math.max(current - step, target);
        }

        return target;
    }

    private static void applyOrientationToPlayer(Player player, ZeroGravityOrientation.OrientationData orientation) {
        ZeroGravityOrientation.CameraAngles angles = ZeroGravityOrientation.toCameraAngles(orientation);
        if (player.getForcedPose() != Pose.SWIMMING) {
            player.setForcedPose(Pose.SWIMMING);
            player.refreshDimensions();
        }
        player.setYRot(angles.yaw());
        player.yRotO = angles.yaw();
        player.setXRot(angles.pitch());
        player.xRotO = angles.pitch();
        player.setYHeadRot(angles.yaw());
        player.yHeadRotO = angles.yaw();
        player.setYBodyRot(angles.yaw());
        player.yBodyRotO = angles.yaw();
    }

    private static void clearOrientationForPlayer(Player player) {
        if (player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(null);
            player.refreshDimensions();
        }
        ZeroGravityClientRenderAccess.clearZeroGravityVisualState(player);
    }

    public static boolean isZeroGravityVisualActive(Player player) {
        return getOrientationForPlayer(player) != null;
    }

    public static ZeroGravityOrientation.OrientationData getOrientationForVisual(Player player) {
        return getOrientationForPlayer(player);
    }

    public static ZeroGravityPushData getPushAnimationData(Player player) {
        return getPushAnimationForPlayer(player).pushData();
    }

    public static float getPushAnimationProgress(Player player) {
        return (float) getPushAnimationForPlayer(player).progress(ZeroGravityRenderState.partialTick());
    }

    private static ZeroGravityOrientation.OrientationData getOrientationForPlayer(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && player.getId() == minecraft.player.getId()) {
            return zeroGravityEnabled ? localOrientation : null;
        }

        return REMOTE_ORIENTATIONS.get(player.getId());
    }

    private static PushAnimationState getPushAnimationForPlayer(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && player.getId() == minecraft.player.getId()) {
            return zeroGravityEnabled ? localPushAnimation : PushAnimationState.idle();
        }

        return REMOTE_PUSH_ANIMATIONS.getOrDefault(player.getId(), PushAnimationState.idle());
    }

    private static void clearVanillaInput(Input input) {
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    private static double easePush(double progress) {
        return Math.sin(progress * Math.PI * 0.5D);
    }

    private record PushAnimationState(ZeroGravityPushData pushData, int ticksRemaining) {
        private static PushAnimationState idle() {
            return new PushAnimationState(ZeroGravityPushData.none(), 0);
        }

        private static PushAnimationState active(ZeroGravityPushData pushData) {
            return new PushAnimationState(pushData, PUSH_ANIMATION_TICKS);
        }

        private PushAnimationState tick() {
            return this.ticksRemaining > 1 ? new PushAnimationState(this.pushData, this.ticksRemaining - 1) : idle();
        }

        private boolean active() {
            return this.ticksRemaining > 0 && this.pushData.active();
        }

        private double progress(float partialTick) {
            if (!this.active()) {
                return 0.0D;
            }

            return Mth.clamp(((double) this.ticksRemaining - partialTick) / (double) PUSH_ANIMATION_TICKS, 0.0D, 1.0D);
        }

        private Vec3 pushDirection() {
            return this.pushData.pushDirection();
        }
    }
}

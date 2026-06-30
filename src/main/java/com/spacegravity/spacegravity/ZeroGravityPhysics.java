package com.spacegravity.spacegravity;

import net.minecraft.world.phys.Vec3;

public final class ZeroGravityPhysics {
    private static final double THRUST_ACCELERATION = 0.022D;
    private static final double BOOSTED_THRUST_ACCELERATION = 0.034D;
    private static final double VELOCITY_EPSILON = 1.0E-4D;

    private ZeroGravityPhysics() {
    }

    public static Vec3 computeFreeThrustVelocityDelta(ZeroGravityInputState inputState) {
        Vec3 thrustDirection = computeThrustDirection(inputState);
        if (thrustDirection.lengthSqr() < VELOCITY_EPSILON * VELOCITY_EPSILON) {
            return Vec3.ZERO;
        }

        double thrustAcceleration = inputState.boosted() ? BOOSTED_THRUST_ACCELERATION : THRUST_ACCELERATION;
        return thrustDirection.scale(thrustAcceleration);
    }

    public static Vec3 computeThrustDirection(ZeroGravityInputState inputState) {
        ZeroGravityOrientation.OrientationData orientation = inputState.orientation();
        Vec3 forward = orientation.forward();
        Vec3 right = ZeroGravityOrientation.lateral(orientation);
        Vec3 up = orientation.up();

        Vec3 thrustDirection = forward.scale(inputState.forwardImpulse())
                .add(right.scale(-inputState.strafeImpulse()))
                .add(up.scale(inputState.verticalImpulse()));

        return thrustDirection.lengthSqr() > 1.0D ? thrustDirection.normalize() : thrustDirection;
    }
}

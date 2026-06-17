package com.spacegravity.spacegravity;

import net.minecraft.world.phys.Vec3;

public final class ZeroGravityPhysics {
    private static final double THRUST_ACCELERATION = 0.022D;
    private static final double BOOSTED_THRUST_ACCELERATION = 0.034D;
    private static final double MAX_SPEED = 0.8D;
    private static final double VELOCITY_EPSILON = 1.0E-4D;

    private ZeroGravityPhysics() {
    }

    public static Vec3 computeNextVelocity(Vec3 measuredVelocity, ZeroGravityInputState inputState, boolean canPushOff) {
        Vec3 baseVelocity = clampTinyVelocity(measuredVelocity);
        Vec3 thrustDirection = computeThrustDirection(inputState);
        if (!canPushOff || thrustDirection.lengthSqr() < VELOCITY_EPSILON * VELOCITY_EPSILON) {
            return baseVelocity;
        }

        double thrustAcceleration = inputState.boosted() ? BOOSTED_THRUST_ACCELERATION : THRUST_ACCELERATION;
        Vec3 nextVelocity = baseVelocity.add(thrustDirection.scale(thrustAcceleration));

        if (nextVelocity.lengthSqr() > MAX_SPEED * MAX_SPEED) {
            nextVelocity = nextVelocity.normalize().scale(MAX_SPEED);
        }

        return clampTinyVelocity(nextVelocity);
    }

    public static Vec3 measureMovement(Vec3 from, Vec3 to) {
        return clampTinyVelocity(to.subtract(from));
    }

    public static Vec3 computeThrustDirection(ZeroGravityInputState inputState) {
        ZeroGravityOrientation.OrientationData orientation = inputState.orientation();
        Vec3 forward = orientation.forward();
        Vec3 up = orientation.up();

        Vec3 thrustDirection = forward.scale(inputState.forwardImpulse())
                .add(up.scale(inputState.verticalImpulse()));

        return thrustDirection.lengthSqr() > 1.0D ? thrustDirection.normalize() : thrustDirection;
    }

    private static Vec3 clampTinyVelocity(Vec3 velocity) {
        return velocity.lengthSqr() < VELOCITY_EPSILON * VELOCITY_EPSILON ? Vec3.ZERO : velocity;
    }
}

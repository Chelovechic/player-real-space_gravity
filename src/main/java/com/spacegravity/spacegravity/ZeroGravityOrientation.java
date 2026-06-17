package com.spacegravity.spacegravity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ZeroGravityOrientation {
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);
    private static final Vec3 DEFAULT_FORWARD = new Vec3(0.0D, 0.0D, 1.0D);
    private static final double EPSILON = 1.0E-5D;

    private ZeroGravityOrientation() {
    }

    public static OrientationData identity() {
        return new OrientationData(DEFAULT_FORWARD, WORLD_UP);
    }

    public static OrientationData fromVanillaAngles(float yRot, float xRot) {
        Vec3 forward = Vec3.directionFromRotation(xRot, yRot);
        Vec3 horizontalForward = horizontalForward(forward, yRot);
        Vec3 lateral = WORLD_UP.cross(horizontalForward);

        if (lateral.lengthSqr() < EPSILON) {
            lateral = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            lateral = lateral.normalize();
        }

        Vec3 up = forward.cross(lateral);
        if (up.lengthSqr() < EPSILON) {
            up = WORLD_UP;
        } else {
            up = up.normalize();
        }

        return normalize(forward, up);
    }

    public static OrientationData normalize(Vec3 forward, Vec3 up) {
        Vec3 normalizedForward = forward.lengthSqr() < EPSILON ? DEFAULT_FORWARD : forward.normalize();
        Vec3 lateral = up.cross(normalizedForward);

        if (lateral.lengthSqr() < EPSILON) {
            lateral = WORLD_UP.cross(normalizedForward);
        }

        if (lateral.lengthSqr() < EPSILON) {
            lateral = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            lateral = lateral.normalize();
        }

        Vec3 normalizedUp = normalizedForward.cross(lateral);
        if (normalizedUp.lengthSqr() < EPSILON) {
            normalizedUp = WORLD_UP;
        } else {
            normalizedUp = normalizedUp.normalize();
        }

        return new OrientationData(normalizedForward, normalizedUp);
    }

    public static OrientationData rotate(OrientationData orientation, double yawDegrees, double pitchDegrees) {
        Vector3f forward = orientation.forward().toVector3f();
        Vector3f up = orientation.up().toVector3f();

        if (Math.abs(yawDegrees) > EPSILON) {
            Quaternionf yawRotation = new Quaternionf().fromAxisAngleRad(
                    up.x,
                    up.y,
                    up.z,
                    (float) Math.toRadians(yawDegrees)
            );
            yawRotation.transform(forward);
        }

        Vector3f lateral = new Vector3f(up).cross(forward);
        if (lateral.lengthSquared() < EPSILON) {
            lateral.set(1.0F, 0.0F, 0.0F);
        } else {
            lateral.normalize();
        }

        if (Math.abs(pitchDegrees) > EPSILON) {
            Quaternionf pitchRotation = new Quaternionf().fromAxisAngleRad(
                    lateral.x,
                    lateral.y,
                    lateral.z,
                    (float) Math.toRadians(pitchDegrees)
            );
            pitchRotation.transform(forward);
            pitchRotation.transform(up);
        }

        return normalize(new Vec3(forward.x, forward.y, forward.z), new Vec3(up.x, up.y, up.z));
    }

    public static OrientationData roll(OrientationData orientation, double rollDegrees) {
        if (Math.abs(rollDegrees) <= EPSILON) {
            return orientation;
        }

        Vector3f forward = orientation.forward().toVector3f();
        Vector3f up = orientation.up().toVector3f();
        Quaternionf rollRotation = new Quaternionf().fromAxisAngleRad(
                forward.x,
                forward.y,
                forward.z,
                (float) Math.toRadians(rollDegrees)
        );
        rollRotation.transform(up);

        return normalize(orientation.forward(), new Vec3(up.x, up.y, up.z));
    }

    public static CameraAngles toCameraAngles(OrientationData orientation) {
        OrientationData normalized = normalize(orientation.forward(), orientation.up());
        Vec3 forward = normalized.forward();
        Vec3 up = normalized.up();

        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(forward.y, -1.0D, 1.0D)));
        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-forward.x, forward.z)));

        Vec3 baseForward = horizontalForward(forward, yaw);
        Vec3 baseLateral = WORLD_UP.cross(baseForward);
        if (baseLateral.lengthSqr() < EPSILON) {
            baseLateral = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            baseLateral = baseLateral.normalize();
        }

        Vec3 baseUp = forward.cross(baseLateral);
        if (baseUp.lengthSqr() < EPSILON) {
            baseUp = WORLD_UP;
        } else {
            baseUp = baseUp.normalize();
        }

        double sin = forward.dot(baseUp.cross(up));
        double cos = Mth.clamp(baseUp.dot(up), -1.0D, 1.0D);
        float roll = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(sin, cos)));

        return new CameraAngles(yaw, pitch, roll);
    }

    public static Vec3 lateral(OrientationData orientation) {
        Vec3 lateral = orientation.up().cross(orientation.forward());
        if (lateral.lengthSqr() < EPSILON) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }

        return lateral.normalize();
    }

    public static Vec3 horizontalForward(Vec3 forward, float yawFallback) {
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < EPSILON) {
            Vec3 yawForward = Vec3.directionFromRotation(0.0F, yawFallback);
            horizontalForward = new Vec3(yawForward.x, 0.0D, yawForward.z);
        }

        if (horizontalForward.lengthSqr() < EPSILON) {
            return DEFAULT_FORWARD;
        }

        return horizontalForward.normalize();
    }

    public record OrientationData(Vec3 forward, Vec3 up) {
    }

    public record CameraAngles(float yaw, float pitch, float roll) {
    }
}

package com.spacegravity.spacegravity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record ZeroGravityInputState(
        float forwardImpulse,
        float strafeImpulse,
        float verticalImpulse,
        boolean boosted,
        float forwardX,
        float forwardY,
        float forwardZ,
        float upX,
        float upY,
        float upZ
) {
    public ZeroGravityInputState {
        forwardImpulse = Mth.clamp(forwardImpulse, -1.0F, 1.0F);
        strafeImpulse = Mth.clamp(strafeImpulse, -1.0F, 1.0F);
        verticalImpulse = Mth.clamp(verticalImpulse, -1.0F, 1.0F);

        ZeroGravityOrientation.OrientationData orientation = ZeroGravityOrientation.normalize(
                new Vec3(forwardX, forwardY, forwardZ),
                new Vec3(upX, upY, upZ)
        );

        forwardX = (float) orientation.forward().x;
        forwardY = (float) orientation.forward().y;
        forwardZ = (float) orientation.forward().z;
        upX = (float) orientation.up().x;
        upY = (float) orientation.up().y;
        upZ = (float) orientation.up().z;
    }

    public ZeroGravityOrientation.OrientationData orientation() {
        return ZeroGravityOrientation.normalize(
                new Vec3(this.forwardX, this.forwardY, this.forwardZ),
                new Vec3(this.upX, this.upY, this.upZ)
        );
    }
}

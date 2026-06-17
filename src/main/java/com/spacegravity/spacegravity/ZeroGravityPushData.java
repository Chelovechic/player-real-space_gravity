package com.spacegravity.spacegravity;

import net.minecraft.world.phys.Vec3;

public record ZeroGravityPushData(ContactLimb limb, Vec3 contactOffset) {
    private static final double EPSILON = 1.0E-6D;
    private static final ZeroGravityPushData NONE = new ZeroGravityPushData(ContactLimb.NONE, Vec3.ZERO);

    public static ZeroGravityPushData none() {
        return NONE;
    }

    public boolean active() {
        return this.limb != ContactLimb.NONE && this.contactOffset.lengthSqr() > EPSILON;
    }

    public Vec3 pushDirection() {
        return this.active() ? this.contactOffset.normalize().scale(-1.0D) : Vec3.ZERO;
    }

    public enum ContactLimb {
        NONE(0.0D, 0.0D, 0.0D),
        LEFT_HAND(-0.36D, 0.18D, 0.64D),
        RIGHT_HAND(0.36D, 0.18D, 0.64D),
        LEFT_KNEE(-0.18D, -0.10D, -0.20D),
        RIGHT_KNEE(0.18D, -0.10D, -0.20D),
        LEFT_FOOT(-0.16D, -0.26D, -0.68D),
        RIGHT_FOOT(0.16D, -0.26D, -0.68D);

        private final Vec3 localAnchor;

        ContactLimb(double rightOffset, double upOffset, double forwardOffset) {
            this.localAnchor = new Vec3(rightOffset, upOffset, forwardOffset);
        }

        public Vec3 worldAnchor(Vec3 playerCenter, ZeroGravityOrientation.OrientationData orientation) {
            Vec3 right = ZeroGravityOrientation.lateral(orientation);
            return playerCenter.add(right.scale(this.localAnchor.x))
                    .add(orientation.up().scale(this.localAnchor.y))
                    .add(orientation.forward().scale(this.localAnchor.z));
        }

        public boolean isArm() {
            return this == LEFT_HAND || this == RIGHT_HAND;
        }

        public boolean isLeft() {
            return this == LEFT_HAND || this == LEFT_KNEE || this == LEFT_FOOT;
        }

        public boolean isFoot() {
            return this == LEFT_FOOT || this == RIGHT_FOOT;
        }

        public static ContactLimb fromNetworkId(int networkId) {
            ContactLimb[] values = values();
            return networkId >= 0 && networkId < values.length ? values[networkId] : NONE;
        }

        public int networkId() {
            return this.ordinal();
        }
    }
}

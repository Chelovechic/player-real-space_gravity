package com.spacegravity.spacegravity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class ZeroGravityPlayerModel<T extends Player> extends PlayerModel<T> {
    public ZeroGravityPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(T player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (!ClientZeroGravityController.isZeroGravityVisualActive(player)) {
            return;
        }

        applyNeutralZeroGravityPose();

        ZeroGravityOrientation.OrientationData orientation = ClientZeroGravityController.getOrientationForVisual(player);
        ZeroGravityPushData pushData = ClientZeroGravityController.getPushAnimationData(player);
        float pushProgress = ClientZeroGravityController.getPushAnimationProgress(player);
        if (orientation != null && pushProgress > 0.0F && pushData.active()) {
            applyPushOffPose(orientation, pushData, pushProgress);
        }

        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.jacket.copyFrom(this.body);
        this.hat.copyFrom(this.head);
    }

    private void applyNeutralZeroGravityPose() {
        this.head.xRot = 0.0F;
        this.head.yRot = 0.0F;
        this.head.zRot = 0.0F;

        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;

        this.rightArm.x = -5.0F;
        this.rightArm.y = 2.0F;
        this.rightArm.z = 0.0F;
        this.leftArm.x = 5.0F;
        this.leftArm.y = 2.0F;
        this.leftArm.z = 0.0F;
        this.rightLeg.x = -1.9F;
        this.rightLeg.y = 12.0F;
        this.rightLeg.z = 0.0F;
        this.leftLeg.x = 1.9F;
        this.leftLeg.y = 12.0F;
        this.leftLeg.z = 0.0F;

        this.rightArm.xRot = -0.12F;
        this.rightArm.yRot = 0.0F;
        this.rightArm.zRot = 0.12F;
        this.leftArm.xRot = -0.12F;
        this.leftArm.yRot = 0.0F;
        this.leftArm.zRot = -0.12F;

        this.rightLeg.xRot = 0.0F;
        this.rightLeg.yRot = 0.0F;
        this.rightLeg.zRot = 0.01F;
        this.leftLeg.xRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.leftLeg.zRot = -0.01F;
    }

    private void applyPushOffPose(ZeroGravityOrientation.OrientationData orientation, ZeroGravityPushData pushData, float pushProgress) {
        Vec3 right = ZeroGravityOrientation.lateral(orientation);
        Vec3 contactDirection = pushData.contactOffset().normalize();
        float forwardBias = (float) Mth.clamp(contactDirection.dot(orientation.forward()), -1.0D, 1.0D);
        float upBias = (float) Mth.clamp(contactDirection.dot(orientation.up()), -1.0D, 1.0D);
        float sideBias = (float) Mth.clamp(contactDirection.dot(right), -1.0D, 1.0D);
        float strength = Mth.clamp(pushProgress, 0.0F, 1.0F);

        if (pushData.limb().isArm()) {
            applyArmPushPose(pushData.limb(), strength, forwardBias, upBias, sideBias);
        } else {
            applyLegPushPose(pushData.limb(), strength, forwardBias, upBias, sideBias);
        }
    }

    private void applyArmPushPose(ZeroGravityPushData.ContactLimb limb, float strength, float forwardBias, float upBias, float sideBias) {
        boolean leftSide = limb.isLeft();
        float sideSign = leftSide ? -1.0F : 1.0F;
        float torsoLean = 0.12F * strength * forwardBias;
        float torsoRoll = -0.18F * strength * sideBias;
        float armPitch = -1.45F + 0.45F * forwardBias - 0.32F * upBias;
        float armYaw = sideSign * (0.20F + 0.28F * Math.abs(sideBias)) + 0.20F * sideBias;
        float armRoll = sideSign * (0.18F + 0.28F * strength) - 0.18F * upBias;

        this.body.xRot = torsoLean;
        this.body.zRot = torsoRoll;

        if (leftSide) {
            this.leftArm.xRot = armPitch;
            this.leftArm.yRot = armYaw;
            this.leftArm.zRot = armRoll;
            this.rightArm.xRot = -0.18F + 0.08F * strength;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = 0.20F;
        } else {
            this.rightArm.xRot = armPitch;
            this.rightArm.yRot = armYaw;
            this.rightArm.zRot = armRoll;
            this.leftArm.xRot = -0.18F + 0.08F * strength;
            this.leftArm.yRot = 0.0F;
            this.leftArm.zRot = -0.20F;
        }

        this.rightLeg.xRot = 0.12F * strength;
        this.leftLeg.xRot = 0.12F * strength;
    }

    private void applyLegPushPose(ZeroGravityPushData.ContactLimb limb, float strength, float forwardBias, float upBias, float sideBias) {
        boolean leftSide = limb.isLeft();
        boolean footContact = limb.isFoot();
        float sideSign = leftSide ? -1.0F : 1.0F;
        float legDrive = (footContact ? 1.15F : 0.88F) * strength + 0.22F * Math.max(-forwardBias, 0.0F);
        float legYaw = sideSign * (0.08F + 0.18F * Math.abs(sideBias)) + 0.12F * sideBias;
        float legRoll = sideSign * (0.04F + 0.06F * strength) - 0.10F * upBias;
        float armTuck = -0.28F - 0.10F * strength;

        this.body.xRot = -0.10F * strength * (1.0F - upBias * 0.3F);
        this.body.zRot = -0.14F * strength * sideBias;

        if (leftSide) {
            this.leftLeg.xRot = legDrive;
            this.leftLeg.yRot = legYaw;
            this.leftLeg.zRot = legRoll;
            this.rightLeg.xRot = 0.08F * strength;
            this.rightLeg.yRot = 0.0F;
            this.rightLeg.zRot = 0.01F;
        } else {
            this.rightLeg.xRot = legDrive;
            this.rightLeg.yRot = legYaw;
            this.rightLeg.zRot = legRoll;
            this.leftLeg.xRot = 0.08F * strength;
            this.leftLeg.yRot = 0.0F;
            this.leftLeg.zRot = -0.01F;
        }

        this.rightArm.xRot = armTuck;
        this.leftArm.xRot = armTuck;
        this.rightArm.zRot = 0.18F;
        this.leftArm.zRot = -0.18F;
    }
}

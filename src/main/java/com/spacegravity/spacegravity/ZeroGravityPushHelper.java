package com.spacegravity.spacegravity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ZeroGravityPushHelper {
    private static final double EPSILON = 1.0E-5D;
    private static final List<PushSurfaceProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private ZeroGravityPushHelper() {
    }

    public static void registerProvider(PushSurfaceProvider provider) {
        PROVIDERS.add(provider);
    }

    public static PushSurface findNearestPushSurface(Player player, ZeroGravityOrientation.OrientationData orientation) {
        AABB searchBox = player.getBoundingBox().inflate(SpaceGravityConfig.pushReach());
        Vec3 playerCenter = player.getBoundingBox().getCenter();
        BlockPos min = BlockPos.containing(searchBox.minX, searchBox.minY, searchBox.minZ);
        BlockPos max = BlockPos.containing(searchBox.maxX, searchBox.maxY, searchBox.maxZ);
        double nearestDistanceSqr = Double.MAX_VALUE;
        Vec3 nearestNormal = Vec3.ZERO;
        Vec3 nearestContactPoint = Vec3.ZERO;
        Vec3 nearestAnchorPoint = Vec3.ZERO;
        ZeroGravityPushData.ContactLimb nearestLimb = ZeroGravityPushData.ContactLimb.NONE;

        for (ZeroGravityPushData.ContactLimb limb : ZeroGravityPushData.ContactLimb.values()) {
            if (limb == ZeroGravityPushData.ContactLimb.NONE) {
                continue;
            }

            Vec3 anchorPoint = limb.worldAnchor(playerCenter, orientation);
            for (BlockPos blockPos : BlockPos.betweenClosed(min, max)) {
                BlockState blockState = player.level().getBlockState(blockPos);
                VoxelShape collisionShape = blockState.getCollisionShape(player.level(), blockPos);
                if (collisionShape.isEmpty()) {
                    continue;
                }

                for (AABB part : collisionShape.toAabbs()) {
                    AABB blockBox = part.move(blockPos);
                    if (!blockBox.intersects(searchBox)) {
                        continue;
                    }

                    Vec3 contactPoint = clampToBox(anchorPoint, blockBox);
                    Vec3 away = anchorPoint.subtract(contactPoint);
                    double distanceSqr = away.lengthSqr();
                    if (distanceSqr < EPSILON) {
                        away = anchorPoint.subtract(Vec3.atCenterOf(blockPos));
                        distanceSqr = away.lengthSqr();
                    }

                    if (distanceSqr < nearestDistanceSqr && distanceSqr > EPSILON) {
                        nearestDistanceSqr = distanceSqr;
                        nearestNormal = away.normalize();
                        nearestContactPoint = contactPoint;
                        nearestAnchorPoint = anchorPoint;
                        nearestLimb = limb;
                    }
                }
            }
        }

        PushSurface nearest = nearestDistanceSqr == Double.MAX_VALUE
                ? PushSurface.NONE
                : new PushSurface(true, nearestNormal, nearestContactPoint, nearestAnchorPoint, nearestLimb);

        for (PushSurfaceProvider provider : PROVIDERS) {
            PushSurface candidate = provider.findNearestPushSurface(player, orientation, searchBox);
            if (candidate.available() && (!nearest.available() || candidate.distanceSqr() < nearest.distanceSqr())) {
                nearest = candidate;
            }
        }

        return nearest;
    }

    private static Vec3 clampToBox(Vec3 point, AABB box) {
        return new Vec3(
                Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ)
        );
    }

    public interface PushSurfaceProvider {
        PushSurface findNearestPushSurface(Player player, ZeroGravityOrientation.OrientationData orientation, AABB searchBox);
    }

    public record PushSurface(boolean available, Vec3 normal, Vec3 contactPoint, Vec3 anchorPoint, ZeroGravityPushData.ContactLimb limb) {
        private static final PushSurface NONE = new PushSurface(false, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, ZeroGravityPushData.ContactLimb.NONE);

        public ZeroGravityPushData toPushData(Vec3 playerCenter) {
            return this.available ? new ZeroGravityPushData(this.limb, this.contactPoint.subtract(playerCenter)) : ZeroGravityPushData.none();
        }

        public double distanceSqr() {
            return this.anchorPoint.distanceToSqr(this.contactPoint);
        }
    }
}

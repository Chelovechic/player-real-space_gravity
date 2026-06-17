package com.spacegravity.spacegravity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;

public final class ZeroGravityClientRenderAccess {
    private static final Field RENDERER_MODEL_FIELD = findRendererModelField();
    private static final Field SWIM_AMOUNT_FIELD = findSwimAmountField(0);
    private static final Field SWIM_AMOUNT_OLD_FIELD = findSwimAmountField(1);

    private ZeroGravityClientRenderAccess() {
    }

    public static void installPlayerModel(PlayerRenderer renderer, EntityModel<?> model) {
        try {
            RENDERER_MODEL_FIELD.set(renderer, model);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to replace player renderer model", exception);
        }
    }

    public static void applyZeroGravityVisualState(Player player) {
        player.walkAnimation.update(0.0F, 0.0F);

        try {
            SWIM_AMOUNT_FIELD.setFloat(player, 1.0F);
            SWIM_AMOUNT_OLD_FIELD.setFloat(player, 1.0F);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to apply zero-gravity visual state", exception);
        }
    }

    public static void clearZeroGravityVisualState(Player player) {
        try {
            SWIM_AMOUNT_FIELD.setFloat(player, 0.0F);
            SWIM_AMOUNT_OLD_FIELD.setFloat(player, 0.0F);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to clear zero-gravity visual state", exception);
        }
    }

    private static Field findRendererModelField() {
        for (Field field : LivingEntityRenderer.class.getDeclaredFields()) {
            if (EntityModel.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }

        throw new IllegalStateException("Could not locate renderer model field");
    }

    private static Field findSwimAmountField(int offset) {
        Field[] fields = LivingEntity.class.getDeclaredFields();
        for (int index = 0; index < fields.length - 2; index++) {
            if (fields[index].getType() == float.class
                    && fields[index + 1].getType() == float.class
                    && Brain.class.isAssignableFrom(fields[index + 2].getType())) {
                fields[index + offset].setAccessible(true);
                return fields[index + offset];
            }
        }

        throw new IllegalStateException("Could not locate swim animation fields");
    }
}

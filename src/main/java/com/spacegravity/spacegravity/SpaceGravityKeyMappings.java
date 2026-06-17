package com.spacegravity.spacegravity;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class SpaceGravityKeyMappings {
    public static final String CATEGORY = "key.categories.space_gravity";
    public static final KeyMapping THRUST_FORWARD = key("thrust_forward", GLFW.GLFW_KEY_W);
    public static final KeyMapping THRUST_BACKWARD = key("thrust_backward", GLFW.GLFW_KEY_S);
    public static final KeyMapping THRUST_UP = key("thrust_up", GLFW.GLFW_KEY_SPACE);
    public static final KeyMapping THRUST_DOWN = key("thrust_down", GLFW.GLFW_KEY_LEFT_SHIFT);
    public static final KeyMapping BOOST = key("boost", GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyMapping ROLL_LEFT = key("roll_left", GLFW.GLFW_KEY_A);
    public static final KeyMapping ROLL_RIGHT = key("roll_right", GLFW.GLFW_KEY_D);

    private SpaceGravityKeyMappings() {
    }

    private static KeyMapping key(String name, int keyCode) {
        return new KeyMapping(
                "key.space_gravity." + name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                keyCode,
                CATEGORY
        );
    }

    public static float forwardImpulse() {
        return impulse(THRUST_FORWARD, THRUST_BACKWARD);
    }

    public static float verticalImpulse() {
        return impulse(THRUST_UP, THRUST_DOWN);
    }

    public static boolean boosted() {
        return BOOST.isDown();
    }

    public static double rollInput() {
        return impulse(ROLL_RIGHT, ROLL_LEFT);
    }

    private static float impulse(KeyMapping positive, KeyMapping negative) {
        return (positive.isDown() ? 1.0F : 0.0F) - (negative.isDown() ? 1.0F : 0.0F);
    }
}

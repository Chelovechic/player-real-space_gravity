package com.spacegravity.spacegravity;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class SpaceGravityKeyMappings {
    public static final String CATEGORY = "key.categories.space_gravity";
    public static final KeyMapping BOOST = key("boost", GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyMapping ROLL_LEFT = key("roll_left", GLFW.GLFW_KEY_Q);
    public static final KeyMapping ROLL_RIGHT = key("roll_right", GLFW.GLFW_KEY_E);

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

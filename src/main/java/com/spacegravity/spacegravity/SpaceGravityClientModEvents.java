package com.spacegravity.spacegravity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "space_gravity", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SpaceGravityClientModEvents {
    private SpaceGravityClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SpaceGravityKeyMappings.THRUST_FORWARD);
        event.register(SpaceGravityKeyMappings.THRUST_BACKWARD);
        event.register(SpaceGravityKeyMappings.THRUST_UP);
        event.register(SpaceGravityKeyMappings.THRUST_DOWN);
        event.register(SpaceGravityKeyMappings.BOOST);
        event.register(SpaceGravityKeyMappings.ROLL_LEFT);
        event.register(SpaceGravityKeyMappings.ROLL_RIGHT);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getPlayerSkin(skin);
            if (renderer == null) {
                continue;
            }

            boolean slim = "slim".equals(skin);
            ZeroGravityPlayerModel<?> model = new ZeroGravityPlayerModel<>(
                    event.getContext().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER),
                    slim
            );
            ZeroGravityClientRenderAccess.installPlayerModel(renderer, model);
        }
    }
}

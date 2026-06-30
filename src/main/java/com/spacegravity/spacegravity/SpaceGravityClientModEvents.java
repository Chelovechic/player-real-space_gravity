package com.spacegravity.spacegravity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = SpaceGravityMod.MODID, value = Dist.CLIENT)
public final class SpaceGravityClientModEvents {
    private SpaceGravityClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SpaceGravityKeyMappings.BOOST);
        event.register(SpaceGravityKeyMappings.ROLL_LEFT);
        event.register(SpaceGravityKeyMappings.ROLL_RIGHT);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer == null) {
                continue;
            }

            boolean slim = skin == PlayerSkin.Model.SLIM;
            ZeroGravityPlayerModel<?> model = new ZeroGravityPlayerModel<>(
                    event.getContext().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER),
                    slim
            );
            ZeroGravityClientRenderAccess.installPlayerModel(renderer, model);
        }
    }
}

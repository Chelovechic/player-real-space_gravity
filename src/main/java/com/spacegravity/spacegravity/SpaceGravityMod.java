package com.spacegravity.spacegravity;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SpaceGravityMod.MODID)
public final class SpaceGravityMod {
    public static final String MODID = "space_gravity";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpaceGravityMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(SpaceGravityNetwork::register);
        modContainer.registerConfig(ModConfig.Type.COMMON, SpaceGravityConfig.SPEC);
    }
}

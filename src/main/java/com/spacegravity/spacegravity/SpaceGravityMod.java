package com.spacegravity.spacegravity;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import org.slf4j.Logger;

@Mod("space_gravity")
public final class SpaceGravityMod {
    public static final String MODID = "space_gravity";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpaceGravityMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SpaceGravityConfig.SPEC);
        SpaceGravityNetwork.register();
    }
}

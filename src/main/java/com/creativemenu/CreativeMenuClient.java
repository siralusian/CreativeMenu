package com.creativemenu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = CreativeMenu.MOD_ID, dist = Dist.CLIENT)
public class CreativeMenuClient {

    public CreativeMenuClient(IEventBus modEventBus) {
        CreativeMenu.LOGGER.info("CreativeMenu client loaded.");
    }
}

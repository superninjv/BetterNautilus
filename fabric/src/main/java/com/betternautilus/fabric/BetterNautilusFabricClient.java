package com.betternautilus.fabric;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.client.NautilusStatHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class BetterNautilusFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) ->
                NautilusStatHudRenderer.onHudRender(guiGraphics, tickCounter.getGameTimeDeltaPartialTick(true)));
        BetterNautilusCommon.LOGGER.info("Better Nautilus Fabric client initialized.");
    }
}

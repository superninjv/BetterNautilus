package com.betternautilus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.betternautilus.client.NautilusStatHudRenderer;

public class BetterNautilusClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BetterNautilus.LOGGER.info("Better Nautilus client initializing...");
        HudRenderCallback.EVENT.register(NautilusStatHudRenderer::onHudRender);
        BetterNautilus.LOGGER.info("Better Nautilus client initialized.");
    }
}

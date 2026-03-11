package com.betternautilus.forge;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.client.NautilusStatHudRenderer;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

/**
 * Client-side event handler for Forge.
 *
 * Registers the nautilus stat HUD overlay using Forge 61.x's
 * AddGuiOverlayLayersEvent and ForgeLayeredDraw API.
 *
 * HOTBAR_AND_DECOS lives inside PRE_SLEEP_STACK, so the 4-arg
 * addAbove() that specifies the target stack must be used.
 */
public class BetterNautilusForgeClient {

    private static final Identifier NAUTILUS_HUD_LAYER =
            Identifier.fromNamespaceAndPath(BetterNautilusCommon.MOD_ID, "nautilus_stat_hud");

    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(
                ForgeLayeredDraw.PRE_SLEEP_STACK,
                NAUTILUS_HUD_LAYER,
                ForgeLayeredDraw.HOTBAR_AND_DECOS,
                (guiGraphics, deltaTracker) ->
                        NautilusStatHudRenderer.onHudRender(
                                guiGraphics,
                                deltaTracker.getGameTimeDeltaPartialTick(true)
                        )
        );
        BetterNautilusCommon.LOGGER.info("Registered nautilus stat HUD overlay (Forge)");
    }
}

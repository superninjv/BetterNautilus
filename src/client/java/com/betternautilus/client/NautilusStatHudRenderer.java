package com.betternautilus.client;

import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.ArrayList;
import java.util.List;

public class NautilusStatHudRenderer {

    private static final int MAX_RANGE_BLOCKS = 8;

    // Fade state
    private static float currentAlpha = 0f;
    private static final float FADE_SPEED = 0.08f;
    private static boolean targetVisible = false;

    // The last nautilus we rendered stats for (used to keep display stable)
    private static AbstractNautilusEntity lastTarget = null;

    // Diamond geometry constants
    // Each diamond is drawn in a 7x7 pixel bounding box (radius 3 from centre)
    private static final int DIAMOND_RADIUS = 3;
    private static final int DIAMOND_SIZE = DIAMOND_RADIUS * 2 + 1; // 7
    private static final int DIAMOND_SPACING = 2; // gap between diamonds
    private static final int DIAMOND_COUNT = 5;

    public static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Determine whether the crosshair is on a nautilus within range
        AbstractNautilusEntity target = getLookedAtNautilus(mc);
        targetVisible = (target != null);
        if (target != null) lastTarget = target;

        // Update fade
        if (targetVisible) {
            currentAlpha = Math.min(currentAlpha + FADE_SPEED, 0.85f);
        } else {
            currentAlpha = Math.max(currentAlpha - FADE_SPEED, 0f);
            if (currentAlpha <= 0f) {
                lastTarget = null;
                return;
            }
        }

        if (lastTarget == null || lastTarget.isRemoved()) return;

        renderStatTooltip(context, mc, lastTarget, currentAlpha);
    }

    private static AbstractNautilusEntity getLookedAtNautilus(MinecraftClient mc) {
        // First check crosshair target (works when looking at a nautilus you're not riding)
        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) hit).getEntity();
            if (entity instanceof AbstractNautilusEntity nautilus) {
                double dist = mc.player.squaredDistanceTo(nautilus);
                if (dist <= MAX_RANGE_BLOCKS * MAX_RANGE_BLOCKS) return nautilus;
            }
        }

        // Also check if the player is currently riding a nautilus
        // (crosshairTarget won't hit your own mount)
        Entity vehicle = mc.player.getVehicle();
        if (vehicle instanceof AbstractNautilusEntity nautilus) return nautilus;

        return null;
    }

    private static void renderStatTooltip(DrawContext context, MinecraftClient mc,
                                           AbstractNautilusEntity nautilus, float alpha) {
        // Build stat lines
        List<StatLine> lines = buildStatLines(nautilus);
        if (lines.isEmpty()) return;

        // Layout constants
        int lineHeight = 12; // slightly taller to fit 7px diamonds + padding
        int padding = 5;
        int labelWidth = 72;
        int diamondsTotalWidth = DIAMOND_COUNT * DIAMOND_SIZE + (DIAMOND_COUNT - 1) * DIAMOND_SPACING;
        int tooltipWidth = padding + labelWidth + diamondsTotalWidth + padding;
        int tooltipHeight = lines.size() * lineHeight + padding * 2;

        // Top-left corner with a small margin — visible when riding or looking
        int x = 6;
        int y = 6;

        int alphaInt = (int) (alpha * 255);
        alphaInt = Math.min(alphaInt, 215);

        // Background panel
        int bgColor    = (alphaInt << 24) | 0x001020; // dark navy
        int borderColor = (alphaInt << 24) | 0x2266AA; // nautilus blue
        context.fill(x - 1, y - 1, x + tooltipWidth + 1, y + tooltipHeight + 1, borderColor);
        context.fill(x, y, x + tooltipWidth, y + tooltipHeight, bgColor);

        // Header: nautilus name (custom name if set, otherwise "Nautilus")
        String name = nautilus.hasCustomName()
                ? nautilus.getCustomName().getString()
                : "Nautilus";
        int headerAlpha = Math.min(alphaInt, 255);
        context.drawText(mc.textRenderer, name,
                x + padding, y + padding - lineHeight + 1,
                composeColor(0x88CCFF, headerAlpha), false);

        // Stat lines
        int textAlpha = Math.min(alphaInt, 255);
        for (int i = 0; i < lines.size(); i++) {
            StatLine line = lines.get(i);
            int lineY = y + padding + i * lineHeight;

            // Draw label text
            int labelColor = composeColor(0xAAAAAA, textAlpha);
            context.drawText(mc.textRenderer, line.label, x + padding, lineY, labelColor, false);

            if (line.isHealthLine) {
                // Health still uses text value (current/max HP)
                int valueColor = composeColor(line.color, textAlpha);
                context.drawText(mc.textRenderer, line.textValue, x + padding + labelWidth, lineY, valueColor, false);
            } else {
                // Draw diamond rating bar
                int diamondsX = x + padding + labelWidth;
                // Centre diamonds vertically on the text line (text is ~8px tall, diamonds are 7px)
                int diamondsY = lineY + 1;
                drawDiamondRating(context, diamondsX, diamondsY, line.fillAmount, line.color, textAlpha);
            }
        }
    }

    /**
     * Draws 5 diamond/rhombus shapes in a row. Each diamond can be:
     * - fully empty (outline only)
     * - partially filled (left portion filled, right portion outline)
     * - fully filled
     *
     * @param fillAmount 0.0 to 1.0 — how much of the total 5-diamond bar is filled
     */
    private static void drawDiamondRating(DrawContext context, int startX, int startY,
                                           double fillAmount, int rgb, int alpha) {
        // fillAmount 0.0 = all empty, 1.0 = all filled
        // Map to 0.0–5.0 range for per-diamond fill
        double filledDiamonds = fillAmount * DIAMOND_COUNT;

        for (int i = 0; i < DIAMOND_COUNT; i++) {
            int dx = startX + i * (DIAMOND_SIZE + DIAMOND_SPACING);
            int dy = startY;

            // How much of THIS diamond is filled (0.0 to 1.0)
            double diamondFill;
            if (i + 1 <= filledDiamonds) {
                diamondFill = 1.0; // fully filled
            } else if (i < filledDiamonds) {
                diamondFill = filledDiamonds - i; // partially filled
            } else {
                diamondFill = 0.0; // empty
            }

            drawSingleDiamond(context, dx, dy, diamondFill, rgb, alpha);
        }
    }

    /**
     * Draws a single diamond (rhombus) shape in a 7x7 pixel area.
     * The diamond is drawn row-by-row. Each row's width expands from the top
     * centre, then contracts back — forming the classic diamond shape.
     *
     * Diamond pixel layout (radius 3):
     *    row 0:       ...X...    (1 pixel wide, centred)
     *    row 1:       ..XXX..    (3 pixels wide)
     *    row 2:       .XXXXX.    (5 pixels wide)
     *    row 3:       XXXXXXX    (7 pixels wide — widest)
     *    row 4:       .XXXXX.    (5 pixels wide)
     *    row 5:       ..XXX..    (3 pixels wide)
     *    row 6:       ...X...    (1 pixel wide)
     *
     * @param diamondFill 0.0 (empty/outline) to 1.0 (fully filled)
     */
    private static void drawSingleDiamond(DrawContext context, int left, int top,
                                           double diamondFill, int rgb, int alpha) {
        int cx = left + DIAMOND_RADIUS; // centre X of the diamond
        int outlineColor = composeColor(dimColor(rgb), alpha);
        int fillColor    = composeColor(rgb, alpha);

        for (int row = 0; row < DIAMOND_SIZE; row++) {
            // Distance from the centre row (0 at middle, DIAMOND_RADIUS at top/bottom)
            int distFromCentre = Math.abs(row - DIAMOND_RADIUS);
            // Half-width of this row (0 at tips, DIAMOND_RADIUS at widest)
            int halfWidth = DIAMOND_RADIUS - distFromCentre;

            int rowLeft  = cx - halfWidth;
            int rowRight = cx + halfWidth + 1; // exclusive for fill()
            int rowY     = top + row;
            int rowWidth = rowRight - rowLeft; // total pixel width of this row

            if (diamondFill <= 0.0) {
                // Empty diamond — just outline (draw left edge and right edge pixels)
                context.fill(rowLeft, rowY, rowLeft + 1, rowY + 1, outlineColor);
                if (rowWidth > 1) {
                    context.fill(rowRight - 1, rowY, rowRight, rowY + 1, outlineColor);
                }
            } else if (diamondFill >= 1.0) {
                // Fully filled — solid colour across the whole row
                context.fill(rowLeft, rowY, rowRight, rowY + 1, fillColor);
            } else {
                // Partially filled — fill left portion, outline right portion
                int filledPixels = Math.max(1, (int) Math.round(rowWidth * diamondFill));
                int fillEnd = rowLeft + filledPixels;

                // Filled portion
                context.fill(rowLeft, rowY, fillEnd, rowY + 1, fillColor);

                // Outline the unfilled portion (just the rightmost pixel of the row)
                if (fillEnd < rowRight) {
                    context.fill(rowRight - 1, rowY, rowRight, rowY + 1, outlineColor);
                }
            }
        }
    }

    /**
     * Returns a dimmed version of the colour for outlines (roughly 35% brightness).
     */
    private static int dimColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b =  rgb        & 0xFF;
        return ((r * 35 / 100) << 16) | ((g * 35 / 100) << 8) | (b * 35 / 100);
    }

    private static List<StatLine> buildStatLines(AbstractNautilusEntity nautilus) {
        List<StatLine> lines = new ArrayList<>();

        BetterNautilusConfig cfg = BetterNautilusConfig.get();

        // Health — show current/max as text (not diamonds)
        float currentHp = nautilus.getHealth();
        float maxHp = (float) nautilus.getAttributeValue(EntityAttributes.MAX_HEALTH);
        String healthValue = String.format("%.1f / %.1f", currentHp, maxHp);
        int healthColor = healthBarColor(currentHp, maxHp);
        lines.add(StatLine.text("❤ Health", healthValue, healthColor));

        // Speed — diamond rating using config range
        double speed = nautilus.getAttributeBaseValue(EntityAttributes.MOVEMENT_SPEED);
        double speedFill = normalize(speed, cfg.speedMin, cfg.speedMax);
        lines.add(StatLine.diamonds("⚡ Speed", speedFill, 0xFFDD44));

        // Dash strength — diamond rating using tracked attribute
        EntityAttributeInstance dashInst = nautilus.getAttributeInstance(ModAttributes.DASH_STRENGTH);
        double dashVal = (dashInst != null) ? dashInst.getValue() : cfg.dashStrengthMin;
        double dashFill = normalize(dashVal, cfg.dashStrengthMin, cfg.dashStrengthMax);
        lines.add(StatLine.diamonds("→ Dash", dashFill, 0x44DDFF));

        // Attack damage — diamond rating using tracked attribute
        EntityAttributeInstance dmgInst = nautilus.getAttributeInstance(ModAttributes.DASH_ATTACK_DAMAGE);
        double dashDmg = (dmgInst != null) ? dmgInst.getValue() : cfg.dashDamageMin;
        double dmgFill = normalize(dashDmg, cfg.dashDamageMin, cfg.dashDamageMax);
        lines.add(StatLine.diamonds("⚔ Attack", dmgFill, 0xFF6644));

        return lines;
    }

    /**
     * Normalizes a value within [min, max] to [0.0, 1.0], clamped.
     */
    private static double normalize(double value, double min, double max) {
        if (max <= min) return 0.0;
        double norm = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, norm));
    }

    private static int healthBarColor(float current, float max) {
        float ratio = (max > 0) ? current / max : 0;
        if (ratio > 0.6f) return 0x55FF55;       // green
        if (ratio > 0.3f) return 0xFFAA00;        // orange
        return 0xFF4444;                            // red
    }

    private static int composeColor(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    /**
     * StatLine now supports two modes:
     * - Text mode (for health): displays a string value
     * - Diamond mode (for speed/dash/attack): displays a diamond rating bar
     */
    private record StatLine(String label, String textValue, double fillAmount,
                             int color, boolean isHealthLine) {

        static StatLine text(String label, String value, int color) {
            return new StatLine(label, value, 0.0, color, true);
        }

        static StatLine diamonds(String label, double fillAmount, int color) {
            return new StatLine(label, "", fillAmount, color, false);
        }
    }
}

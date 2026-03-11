package com.betternautilus.client;

import com.betternautilus.attribute.ModAttributes;
import com.betternautilus.config.BetterNautilusConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a stat HUD overlay when the player looks at or rides a nautilus.
 *
 * The onHudRender method accepts (GuiGraphics, float) where the float is the
 * partial tick. Each loader adapter extracts the float from its own event type
 * before calling this method.
 */
public class NautilusStatHudRenderer {

    private static final int MAX_RANGE_BLOCKS = 8;

    // Fade state
    private static float currentAlpha = 0f;
    private static final float FADE_SPEED = 0.08f;
    private static boolean targetVisible = false;

    // The last nautilus we rendered stats for (used to keep display stable)
    private static AbstractNautilus lastTarget = null;

    // Diamond geometry constants
    private static final int DIAMOND_RADIUS = 3;
    private static final int DIAMOND_SIZE = DIAMOND_RADIUS * 2 + 1; // 7
    private static final int DIAMOND_SPACING = 2;
    private static final int DIAMOND_COUNT = 5;

    /**
     * Called by each loader's HUD render event. The partial tick float is
     * extracted by the loader-specific adapter before calling this.
     */
    public static void onHudRender(GuiGraphics context, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Determine whether the crosshair is on a nautilus within range
        AbstractNautilus target = getLookedAtNautilus(mc);
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

    private static AbstractNautilus getLookedAtNautilus(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) hit).getEntity();
            if (entity instanceof AbstractNautilus nautilus) {
                double dist = mc.player.distanceToSqr(nautilus);
                if (dist <= MAX_RANGE_BLOCKS * MAX_RANGE_BLOCKS) return nautilus;
            }
        }

        Entity vehicle = mc.player.getVehicle();
        if (vehicle instanceof AbstractNautilus nautilus) return nautilus;

        return null;
    }

    private static void renderStatTooltip(GuiGraphics context, Minecraft mc,
                                           AbstractNautilus nautilus, float alpha) {
        List<StatLine> lines = buildStatLines(nautilus);
        if (lines.isEmpty()) return;

        int lineHeight = 12;
        int padding = 5;
        int labelWidth = 72;
        int diamondsTotalWidth = DIAMOND_COUNT * DIAMOND_SIZE + (DIAMOND_COUNT - 1) * DIAMOND_SPACING;
        int tooltipWidth = padding + labelWidth + diamondsTotalWidth + padding;
        int tooltipHeight = lines.size() * lineHeight + padding * 2;

        int x = 6;
        int y = 6;

        int alphaInt = (int) (alpha * 255);
        alphaInt = Math.min(alphaInt, 215);

        int bgColor    = (alphaInt << 24) | 0x001020;
        int borderColor = (alphaInt << 24) | 0x2266AA;
        context.fill(x - 1, y - 1, x + tooltipWidth + 1, y + tooltipHeight + 1, borderColor);
        context.fill(x, y, x + tooltipWidth, y + tooltipHeight, bgColor);

        String name = nautilus.hasCustomName()
                ? nautilus.getCustomName().getString()
                : "Nautilus";
        int headerAlpha = Math.min(alphaInt, 255);
        context.drawString(mc.font, name,
                x + padding, y + padding - lineHeight + 1,
                composeColor(0x88CCFF, headerAlpha), false);

        int textAlpha = Math.min(alphaInt, 255);
        for (int i = 0; i < lines.size(); i++) {
            StatLine line = lines.get(i);
            int lineY = y + padding + i * lineHeight;

            int labelColor = composeColor(0xAAAAAA, textAlpha);
            context.drawString(mc.font, line.label, x + padding, lineY, labelColor, false);

            if (line.isHealthLine) {
                int valueColor = composeColor(line.color, textAlpha);
                context.drawString(mc.font, line.textValue, x + padding + labelWidth, lineY, valueColor, false);
            } else {
                int diamondsX = x + padding + labelWidth;
                int diamondsY = lineY + 1;
                drawDiamondRating(context, diamondsX, diamondsY, line.fillAmount, line.color, textAlpha);
            }
        }
    }

    private static void drawDiamondRating(GuiGraphics context, int startX, int startY,
                                           double fillAmount, int rgb, int alpha) {
        double filledDiamonds = fillAmount * DIAMOND_COUNT;

        for (int i = 0; i < DIAMOND_COUNT; i++) {
            int dx = startX + i * (DIAMOND_SIZE + DIAMOND_SPACING);
            int dy = startY;

            double diamondFill;
            if (i + 1 <= filledDiamonds) {
                diamondFill = 1.0;
            } else if (i < filledDiamonds) {
                diamondFill = filledDiamonds - i;
            } else {
                diamondFill = 0.0;
            }

            drawSingleDiamond(context, dx, dy, diamondFill, rgb, alpha);
        }
    }

    private static void drawSingleDiamond(GuiGraphics context, int left, int top,
                                           double diamondFill, int rgb, int alpha) {
        int cx = left + DIAMOND_RADIUS;
        int outlineColor = composeColor(dimColor(rgb), alpha);
        int fillColor    = composeColor(rgb, alpha);

        for (int row = 0; row < DIAMOND_SIZE; row++) {
            int distFromCentre = Math.abs(row - DIAMOND_RADIUS);
            int halfWidth = DIAMOND_RADIUS - distFromCentre;

            int rowLeft  = cx - halfWidth;
            int rowRight = cx + halfWidth + 1;
            int rowY     = top + row;
            int rowWidth = rowRight - rowLeft;

            if (diamondFill <= 0.0) {
                context.fill(rowLeft, rowY, rowLeft + 1, rowY + 1, outlineColor);
                if (rowWidth > 1) {
                    context.fill(rowRight - 1, rowY, rowRight, rowY + 1, outlineColor);
                }
            } else if (diamondFill >= 1.0) {
                context.fill(rowLeft, rowY, rowRight, rowY + 1, fillColor);
            } else {
                int filledPixels = Math.max(1, (int) Math.round(rowWidth * diamondFill));
                int fillEnd = rowLeft + filledPixels;

                context.fill(rowLeft, rowY, fillEnd, rowY + 1, fillColor);

                if (fillEnd < rowRight) {
                    context.fill(rowRight - 1, rowY, rowRight, rowY + 1, outlineColor);
                }
            }
        }
    }

    private static int dimColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b =  rgb        & 0xFF;
        return ((r * 35 / 100) << 16) | ((g * 35 / 100) << 8) | (b * 35 / 100);
    }

    private static List<StatLine> buildStatLines(AbstractNautilus nautilus) {
        List<StatLine> lines = new ArrayList<>();

        BetterNautilusConfig cfg = BetterNautilusConfig.get();

        float currentHp = nautilus.getHealth();
        float maxHp = (float) nautilus.getAttributeValue(Attributes.MAX_HEALTH);
        String healthValue = String.format("%.1f / %.1f", currentHp, maxHp);
        int healthColor = healthBarColor(currentHp, maxHp);
        lines.add(StatLine.text("\u2764 Health", healthValue, healthColor));

        double speed = nautilus.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        double speedFill = normalize(speed, cfg.speedMin, cfg.speedMax);
        lines.add(StatLine.diamonds("\u26A1 Speed", speedFill, 0xFFDD44));

        AttributeInstance dashInst = nautilus.getAttribute(ModAttributes.DASH_STRENGTH);
        double dashVal = (dashInst != null) ? dashInst.getValue() : cfg.dashStrengthMin;
        double dashFill = normalize(dashVal, cfg.dashStrengthMin, cfg.dashStrengthMax);
        lines.add(StatLine.diamonds("\u2192 Dash", dashFill, 0x44DDFF));

        AttributeInstance dmgInst = nautilus.getAttribute(ModAttributes.DASH_ATTACK_DAMAGE);
        double dashDmg = (dmgInst != null) ? dmgInst.getValue() : cfg.dashDamageMin;
        double dmgFill = normalize(dashDmg, cfg.dashDamageMin, cfg.dashDamageMax);
        lines.add(StatLine.diamonds("\u2694 Attack", dmgFill, 0xFF6644));

        return lines;
    }

    private static double normalize(double value, double min, double max) {
        if (max <= min) return 0.0;
        double norm = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, norm));
    }

    private static int healthBarColor(float current, float max) {
        float ratio = (max > 0) ? current / max : 0;
        if (ratio > 0.6f) return 0x55FF55;
        if (ratio > 0.3f) return 0xFFAA00;
        return 0xFF4444;
    }

    private static int composeColor(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

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

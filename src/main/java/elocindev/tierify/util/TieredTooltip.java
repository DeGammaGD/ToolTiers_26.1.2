package elocindev.tierify.util;

import java.util.List;

import org.joml.Vector2ic;

import draylar.tiered.api.BorderTemplate;
import elocindev.tierify.Tierify;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;

@Environment(EnvType.CLIENT)
@SuppressWarnings({"null"})
public class TieredTooltip {

    public static String getPlateForModifier(String modifier) {
        // The first string that gets added is the item border color, it might seem useless because it gets reset right afterwards, but the item borders mod doesn't have a data driven api and this is the only way for now according to the dev

        switch(modifier.toLowerCase()) {
            case "common":
                return "\u00A77\u00A7r"+"\u00A7F\uFFA1\u00A7r";
            case "uncommon":
                return "\u00A72\u00A7r"+"\u00A7F\uFFA2\u00A7r";
            case "rare":
                return "\u00A73\u00A7r"+"\u00A7F\uFFA3\u00A7r";
            case "epic":
                return "\u00A75\u00A7r"+"\u00A7F\uFFA4\u00A7r";
            case "legendary":
                return "\u00A76\u00A7r"+"\u00A7F\uFFA5\u00A7r";
            case "mythic":
                return "\u00A74\u00A7r"+"\u00A7F\uFFA6\u00A7r";
            default:
                return modifier;
        }
    }

    public static void renderTieredTooltipFromComponents(GuiGraphicsExtractor context, Font textRenderer, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner,
            BorderTemplate borderTemplate) {
        ClientTooltipComponent tooltipComponent2;
        int r;
        int k;
        if (components.isEmpty()) {
            return;
        }
        int i = 0;
        int j = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent tooltipComponent : components) {
            if (tooltipComponent == null) {
                continue;
            }
            k = tooltipComponent.getWidth(textRenderer);
            if (k > i) {
                i = k;
            }
            j += tooltipComponent.getHeight(textRenderer);
        }
        if (i < 64) {
            i = 64;
        }
        if (j < 16) {
            j = 16;
        }

        int l = i;
        int m = j;

        Vector2ic vector2ic = positioner.positionTooltip(context.guiWidth(), context.guiHeight(), x, y, l, m);
        int n = vector2ic.x();
        int o = vector2ic.y();

        int backgroundColor = borderTemplate.getBackgroundGradient();
        int colorStart = borderTemplate.getStartGradient();
        int colorEnd = borderTemplate.getEndGradient();

        renderTooltipBackground(context, n, o, l, m, 400, backgroundColor, colorStart, colorEnd);
        int q = o;

        for (r = 0; r < components.size(); ++r) {
            int nameCentering = 0;
            tooltipComponent2 = components.get(r);
            if (r == 0 && Tierify.CLIENT_CONFIG.centerName)
                nameCentering = i / 2 - tooltipComponent2.getWidth(textRenderer) / 2;

            tooltipComponent2.extractText(context, textRenderer, n + nameCentering, q);
            q += tooltipComponent2.getHeight(textRenderer) + (r == 0 ? 2 : 0);
        }
        q = o;
        for (r = 0; r < components.size(); ++r) {
            tooltipComponent2 = components.get(r);
            tooltipComponent2.extractImage(textRenderer, n, q, l, m, context);
            q += tooltipComponent2.getHeight(textRenderer) + (r == 0 ? 2 : 0);
        }

        int border = borderTemplate.getIndex();
        int secondHalf = border > 7 ? 1 : 0;
        if (border > 7) {
            border -= 8;
        }

        // left top corner
        context.blit(RenderPipelines.GUI_TEXTURED, borderTemplate.getIdentifier(), n - 6, o - 6, (float) (0 + secondHalf * 64), (float) (0 + border * 16), 8, 8, 128, 128);
        // right top corner
        context.blit(RenderPipelines.GUI_TEXTURED, borderTemplate.getIdentifier(), n + l - 2, o - 6, (float) (56 + secondHalf * 64), (float) (0 + border * 16), 8, 8, 128, 128);

        // left down corner
        context.blit(RenderPipelines.GUI_TEXTURED, borderTemplate.getIdentifier(), n - 6, o + m - 2, (float) (0 + secondHalf * 64), (float) (8 + border * 16), 8, 8, 128, 128);
        // right down corner
        context.blit(RenderPipelines.GUI_TEXTURED, borderTemplate.getIdentifier(), n + l - 2, o + m - 2, (float) (56 + secondHalf * 64), (float) (8 + border * 16), 8, 8, 128, 128);

        // middle header
        context.blit(RenderPipelines.GUI_TEXTURED, borderTemplate.getIdentifier(), (n - 6 + n + l + 6) / 2 - 24, o - 9, (float) (8 + secondHalf * 64), (float) (0 + border * 16), 48, 8, 128, 128);
        // bottom footer
        context.blit(RenderPipelines.GUI_TEXTURED, borderTemplate.getIdentifier(), (n - 6 + n + l + 6) / 2 - 24, o + m + 1, (float) (8 + secondHalf * 64), (float) (8 + border * 16), 48, 8, 128, 128);
    }

    private static void renderTooltipBackground(GuiGraphicsExtractor context, int x, int y, int width, int height, int z, int backgroundColor, int colorStart, int colorEnd) {
        int i = x - 3;
        int j = y - 3;
        int k = width + 6;
        int l = height + 6;
        renderHorizontalLine(context, i, j - 1, k, z, backgroundColor);
        renderHorizontalLine(context, i, j + l, k, z, backgroundColor);
        renderRectangle(context, i, j, k, l, z, backgroundColor);
        renderVerticalLine(context, i - 1, j, l, z, backgroundColor);
        renderVerticalLine(context, i + k, j, l, z, backgroundColor);
        renderBorder(context, i, j + 1, k, l, z, colorStart, colorEnd);
    }

    private static void renderBorder(GuiGraphicsExtractor context, int x, int y, int width, int height, int z, int startColor, int endColor) {
        renderVerticalLine(context, x, y, height - 2, z, startColor, endColor);
        renderVerticalLine(context, x + width - 1, y, height - 2, z, startColor, endColor);
        renderHorizontalLine(context, x, y - 1, width, z, startColor);
        renderHorizontalLine(context, x, y - 1 + height - 1, width, z, endColor);
    }

    private static void renderVerticalLine(GuiGraphicsExtractor context, int x, int y, int height, int z, int color) {
        context.fill(RenderPipelines.GUI, x, y, x + 1, y + height, color);
    }

    private static void renderVerticalLine(GuiGraphicsExtractor context, int x, int y, int height, int z, int startColor, int endColor) {
        context.fillGradient(x, y, x + 1, y + height, startColor, endColor);
    }

    private static void renderHorizontalLine(GuiGraphicsExtractor context, int x, int y, int width, int z, int color) {
        context.fill(RenderPipelines.GUI, x, y, x + width, y + 1, color);
    }

    private static void renderRectangle(GuiGraphicsExtractor context, int x, int y, int width, int height, int z, int color) {
        context.fill(RenderPipelines.GUI, x, y, x + width, y + height, color);
    }

}

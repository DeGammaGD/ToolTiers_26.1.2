package draylar.tiered.util;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import draylar.tiered.api.BorderTemplate;

public class TieredTooltip {
    
    @Deprecated()
    public static void renderTieredTooltipFromComponents(GuiGraphics context, Font textRenderer, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, BorderTemplate borderTemplate) {
        elocindev.tierify.util.TieredTooltip.renderTieredTooltipFromComponents(context, textRenderer, components, x, y, positioner, borderTemplate);
    }

}

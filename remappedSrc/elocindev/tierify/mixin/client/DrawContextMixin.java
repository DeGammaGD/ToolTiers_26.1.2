package elocindev.tierify.mixin.client;

import java.util.List;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.TierifyClient;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@Environment(EnvType.CLIENT)
@Mixin(GuiGraphics.class)
public class DrawContextMixin {

    @Shadow
    @Mutable
    @Final
    private Minecraft client;

    @Inject(method = "drawItemTooltip", at = @At("HEAD"), cancellable = true)
    private void drawItemTooltipMixin(Font textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {

        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        if (Tierify.CLIENT_CONFIG.tieredTooltip && component != null && component.copyTag().contains("Tiered")) {
            Tierify.LOGGER.info("Rendering bordered tooltip for {}", BuiltInRegistries.ITEM.getKey(stack.getItem()));
            String nbtString = component.copyTag().getCompound("Tiered").getAsString();
            for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                if (!TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack) && TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(nbtString)) {
                    TierifyClient.BORDER_TEMPLATES.get(i).addStack(stack);
                } else if (TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack)) {
                    List<Component> text = Screen.getTooltipFromItem(client, stack);

                    List<ClientTooltipComponent> list = text.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Collectors.toList());
                    stack.getTooltipImage().ifPresent(data -> list.add(1, ClientTooltipComponent.create(data)));

                    TieredTooltip.renderTieredTooltipFromComponents((GuiGraphics) (Object) this, textRenderer, list, x, y, DefaultTooltipPositioner.INSTANCE, TierifyClient.BORDER_TEMPLATES.get(i));

                    info.cancel();
                    break;
                }
            }
        }
    }

}

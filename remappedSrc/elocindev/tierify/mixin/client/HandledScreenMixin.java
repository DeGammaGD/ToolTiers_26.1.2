package elocindev.tierify.mixin.client;

import java.util.List;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import elocindev.tierify.TierifyClient;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {

    public HandledScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    protected void drawMouseoverTooltipMixin(GuiGraphics context, int x, int y, CallbackInfo info, ItemStack stack) {
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = component != null ? component.copyTag() : new CompoundTag();
        if (Tierify.CLIENT_CONFIG.tieredTooltip && component != null && root.contains("Tiered")) {

            String nbtString = root.getCompound("Tiered").getAsString();
            for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                if (!TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack) && TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(nbtString)) {
                    TierifyClient.BORDER_TEMPLATES.get(i).addStack(stack);
                } else if (TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack)) {
                    List<Component> text = Screen.getTooltipFromItem(minecraft, stack);

                    List<ClientTooltipComponent> list = text.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Collectors.toList());
                    stack.getTooltipImage().ifPresent(data -> list.add(1, ClientTooltipComponent.create(data)));

                    TieredTooltip.renderTieredTooltipFromComponents(context, this.font, list, x, y, DefaultTooltipPositioner.INSTANCE, TierifyClient.BORDER_TEMPLATES.get(i));

                    info.cancel();
                    break;
                }
            }
        }
    }

}

package elocindev.tierify.mixin.client;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@Environment(EnvType.CLIENT)
@Mixin(GuiGraphicsExtractor.class)
public class DrawContextMixin {

    @Inject(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void drawItemTooltipMixin(Font textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {
        Tierify.LOGGER.info("[TooltipDebug] GuiGraphicsExtractor#setTooltipForNextFrame(Font, ItemStack, x, y) invoked for item={}", BuiltInRegistries.ITEM.getKey(stack.getItem()));

        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        if (Tierify.CLIENT_CONFIG.tieredTooltip && component != null && component.copyTag().contains(Tierify.NBT_SUBTAG_KEY)) {
            Tierify.LOGGER.info("Rendering bordered tooltip for {}", BuiltInRegistries.ITEM.getKey(stack.getItem()));
            Optional<CompoundTag> tieredTag = component.copyTag().getCompound(Tierify.NBT_SUBTAG_KEY);
            if (tieredTag.isEmpty()) {
                Tierify.LOGGER.info("[TooltipDebug] DrawContextMixin missing Tiered compound for item={}", BuiltInRegistries.ITEM.getKey(stack.getItem()));
                return;
            }

            String tierId = tieredTag.get().getString(Tierify.NBT_SUBTAG_DATA_KEY).orElse("");
            String nbtString = tieredTag.get().toString();
            Tierify.LOGGER.info("[TooltipDebug] DrawContextMixin detected tier id={}", tierId);
            boolean rendered = false;
            for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                if (TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(tierId) || TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(nbtString)) {
                    Tierify.LOGGER.info("[TooltipDebug] DrawContextMixin matched template index={} texture={} identifier={}", TierifyClient.BORDER_TEMPLATES.get(i).getIndex(),
                            TierifyClient.BORDER_TEMPLATES.get(i).getTexture(), TierifyClient.BORDER_TEMPLATES.get(i).getIdentifier());
                    List<Component> text = Screen.getTooltipFromItem(Minecraft.getInstance(), stack);

                    List<ClientTooltipComponent> list = text.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Collectors.toList());
                    stack.getTooltipImage().ifPresent(data -> list.add(1, ClientTooltipComponent.create(data)));

                    TieredTooltip.renderTieredTooltipFromComponents((GuiGraphicsExtractor) (Object) this, textRenderer, list, x, y, DefaultTooltipPositioner.INSTANCE, TierifyClient.BORDER_TEMPLATES.get(i));
                    Tierify.LOGGER.info("[TooltipDebug] DrawContextMixin rendered custom bordered tooltip");
                    rendered = true;

                    info.cancel();
                    break;
                }
            }
            if (!rendered) {
                Tierify.LOGGER.info("[TooltipDebug] DrawContextMixin found no matching border template for item={} tier={}", BuiltInRegistries.ITEM.getKey(stack.getItem()), tierId);
            }
        }
    }

}

package elocindev.tierify.mixin.client;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import draylar.tiered.api.BorderTemplate;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
@SuppressWarnings({"null"})
public abstract class HandledScreenMixin extends Screen {

    @Shadow protected Slot hoveredSlot;

    public HandledScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    protected void extractTooltipMixin(GuiGraphicsExtractor context, int x, int y, CallbackInfo info) {
        if (this.hoveredSlot == null || !this.hoveredSlot.hasItem()) {
            return;
        }

        ItemStack stack = this.hoveredSlot.getItem();
        Tierify.LOGGER.info("[TooltipDebug] AbstractContainerScreen.extractTooltip start item={}", BuiltInRegistries.ITEM.getKey(stack.getItem()));

        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = component != null ? component.copyTag() : new CompoundTag();
        if (Tierify.CLIENT_CONFIG.tieredTooltip && component != null && root.contains(Tierify.NBT_SUBTAG_KEY)) {
            Optional<CompoundTag> tieredTag = root.getCompound(Tierify.NBT_SUBTAG_KEY);
            if (tieredTag.isEmpty()) {
                Tierify.LOGGER.info("[TooltipDebug] No Tiered compound for {}", BuiltInRegistries.ITEM.getKey(stack.getItem()));
                return;
            }

            String tierId = tieredTag.get().getString(Tierify.NBT_SUBTAG_DATA_KEY).orElse("");
            String nbtString = tieredTag.get().toString();
            Tierify.LOGGER.info("[TooltipDebug] Detected tier id={} for item={}", tierId, BuiltInRegistries.ITEM.getKey(stack.getItem()));

            BorderTemplate matchedTemplate = null;
            for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                BorderTemplate template = TierifyClient.BORDER_TEMPLATES.get(i);
                if (template.containsDecider(tierId) || template.containsDecider(nbtString)) {
                    matchedTemplate = template;
                    Tierify.LOGGER.info("[TooltipDebug] Matched template index={} texture={} identifier={}", template.getIndex(), template.getTexture(), template.getIdentifier());
                    break;
                }
            }

            if (matchedTemplate != null) {
                List<Component> text = Screen.getTooltipFromItem(Minecraft.getInstance(), stack);

                List<ClientTooltipComponent> list = text.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Collectors.toList());
                stack.getTooltipImage().ifPresent(data -> list.add(1, ClientTooltipComponent.create(data)));

                Font effectiveFont = this.font != null ? this.font : Minecraft.getInstance().font;
                TieredTooltip.renderTieredTooltipFromComponents(context, effectiveFont, list, x, y, DefaultTooltipPositioner.INSTANCE, matchedTemplate);
                Tierify.LOGGER.info("[TooltipDebug] Custom bordered tooltip rendered for item={}", BuiltInRegistries.ITEM.getKey(stack.getItem()));

                info.cancel();
            } else {
                Tierify.LOGGER.info("[TooltipDebug] No matching border template for tier id={}", tierId);
            }
        }
    }

}

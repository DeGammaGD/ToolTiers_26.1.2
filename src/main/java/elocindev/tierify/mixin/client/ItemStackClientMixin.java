package elocindev.tierify.mixin.client;

import java.util.List;

import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void getNameMixin(CallbackInfoReturnable<Text> info) {
        NbtComponent component = ((ItemStack) (Object) this).get(DataComponentTypes.CUSTOM_DATA);
        if (component != null && !component.getNbt().contains("display") && component.getNbt().contains(Tierify.NBT_SUBTAG_KEY)) {
            Identifier tier = Identifier.of(component.getNbt().getCompound(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
            PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            if (potentialAttribute != null) {
                Tierify.LOGGER.info("Tooltip name read tier {} for {}", tier, Registries.ITEM.getId(((ItemStack) (Object) this).getItem()));
                Tierify.LOGGER.info("Tooltip name attributes for {} -> {}", tier, potentialAttribute.getAttributes());
                Tierify.LOGGER.info("[TierifyDebug][Tooltip] item={} tier={} attributes={}", Registries.ITEM.getId(((ItemStack) (Object) this).getItem()), tier, potentialAttribute.getAttributes());
                MutableText text = Text.translatable(potentialAttribute.getID() + ".label");

                if (Tierify.CLIENT_CONFIG.showPlatesOnName) {
                    text = Text.literal(TieredTooltip.getPlateForModifier(text.getString()));
                }

                info.setReturnValue(text.append(" ").append(info.getReturnValue()).setStyle(potentialAttribute.getStyle()));
            }
        }
    }

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void getTooltipMixin(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> info) {
        List<Text> tooltip = info.getReturnValue();
        NbtComponent component = ((ItemStack) (Object) this).get(DataComponentTypes.CUSTOM_DATA);

        if (component != null && component.getNbt().contains(Tierify.NBT_SUBTAG_KEY)) {
            Identifier tier = Identifier.of(component.getNbt().getCompound(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
            PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            if (potentialAttribute != null) {
                Tierify.LOGGER.info("Tooltip body read tier {} for {}", tier, Registries.ITEM.getId(((ItemStack) (Object) this).getItem()));
                Tierify.LOGGER.info("Tooltip body attributes for {} -> {}", tier, potentialAttribute.getAttributes());
                Tierify.LOGGER.info("[TierifyDebug][Tooltip] item={} tier={} attributes={}", Registries.ITEM.getId(((ItemStack) (Object) this).getItem()), tier, potentialAttribute.getAttributes());
                tooltip.add(Text.literal("Tier: ").append(Text.translatable(potentialAttribute.getID() + ".label").setStyle(potentialAttribute.getStyle())));
            }
        }

        info.setReturnValue(tooltip);
    }
}

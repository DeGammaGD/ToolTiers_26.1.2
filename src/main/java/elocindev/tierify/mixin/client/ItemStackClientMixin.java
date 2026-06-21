package elocindev.tierify.mixin.client;

import java.util.List;

import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true, require = 0)
    private void getNameMixin(CallbackInfoReturnable<Component> info) {
        CustomData component = ((ItemStack) (Object) this).get(DataComponents.CUSTOM_DATA);
        CompoundTag root = component != null ? component.copyTag() : new CompoundTag();
        if (component != null && !root.contains("display") && root.contains(Tierify.NBT_SUBTAG_KEY)) {
            ResourceLocation tier = ResourceLocation.parse(root.getCompound(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
            PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            if (potentialAttribute != null) {
                Tierify.LOGGER.info("Tooltip name read tier {} for {}", tier, BuiltInRegistries.ITEM.getKey(((ItemStack) (Object) this).getItem()));
                Tierify.LOGGER.info("Tooltip name attributes for {} -> {}", tier, potentialAttribute.getAttributes());
                Tierify.LOGGER.info("[TierifyDebug][Tooltip] item={} tier={} attributes={}", BuiltInRegistries.ITEM.getKey(((ItemStack) (Object) this).getItem()), tier, potentialAttribute.getAttributes());
                MutableComponent text = Component.translatable(potentialAttribute.getID() + ".label");

                if (Tierify.CLIENT_CONFIG.showPlatesOnName) {
                    text = Component.literal(TieredTooltip.getPlateForModifier(text.getString()));
                }

                info.setReturnValue(text.append(" ").append(info.getReturnValue()).setStyle(potentialAttribute.getStyle()));
            }
        }
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true, require = 0)
    private void getTooltipMixin(Item.TooltipContext context, Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> info) {
        List<Component> tooltip = info.getReturnValue();
        CustomData component = ((ItemStack) (Object) this).get(DataComponents.CUSTOM_DATA);
        CompoundTag root = component != null ? component.copyTag() : new CompoundTag();

        if (component != null && root.contains(Tierify.NBT_SUBTAG_KEY)) {
            ResourceLocation tier = ResourceLocation.parse(root.getCompound(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
            PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            if (potentialAttribute != null) {
                Tierify.LOGGER.info("Tooltip body read tier {} for {}", tier, BuiltInRegistries.ITEM.getKey(((ItemStack) (Object) this).getItem()));
                Tierify.LOGGER.info("Tooltip body attributes for {} -> {}", tier, potentialAttribute.getAttributes());
                Tierify.LOGGER.info("[TierifyDebug][Tooltip] item={} tier={} attributes={}", BuiltInRegistries.ITEM.getKey(((ItemStack) (Object) this).getItem()), tier, potentialAttribute.getAttributes());
                tooltip.add(Component.literal("Tier: ").append(Component.translatable(potentialAttribute.getID() + ".label").setStyle(potentialAttribute.getStyle())));
            }
        }

        info.setReturnValue(tooltip);
    }
}

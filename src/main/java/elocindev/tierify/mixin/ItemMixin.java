package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

@Mixin(Item.class)
public class ItemMixin {

    // onCraft in ItemStack class does get called too and get called in CraftingResultSlot
    // but is air at onTakeItem in CraftingResultSlot when quick crafting is used
    @Inject(method = "onCraftedBy", at = @At("TAIL"), require = 0)
    private void onCraftMixin(ItemStack stack, Level world, Player player, CallbackInfo info) {
        Tierify.LOGGER.info("ItemStack created via onCraft for {}", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (!world.isClientSide() && !stack.isEmpty() && Tierify.CONFIG.craftingModifier) {
            ModifierUtils.applyTierToItem(stack);
            ModifierUtils.logTierDebug("crafting", stack);
        }
    }

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void getItemBarStepMixin(ItemStack stack, CallbackInfoReturnable<Integer> info) {
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        if (component != null && component.copyTag().contains("durable")) {
            info.setReturnValue(Math.round(13.0f - (float) stack.getDamageValue() * 13.0f / (float) stack.getMaxDamage()));
        }
    }

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void getItemBarColorMixin(ItemStack stack, CallbackInfoReturnable<Integer> info) {
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        if (component != null && component.copyTag().contains("durable")) {
            float f = Math.max(0.0f, ((float) stack.getMaxDamage() - (float) stack.getDamageValue()) / (float) stack.getMaxDamage());
            info.setReturnValue(Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f));
        }
    }

}

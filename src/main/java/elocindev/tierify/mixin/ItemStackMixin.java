package elocindev.tierify.mixin;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxDamage", at = @At("TAIL"), cancellable = true)
    private void getMaxDamageMixin(CallbackInfoReturnable<Integer> info) {
        ItemStack stack = (ItemStack) (Object) this;
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        if (component != null) {
            CompoundTag root = component.copyTag();
            if (root.contains("durable")) {
                info.setReturnValue(info.getReturnValue() + (root.getInt("durable") > 0 ? root.getInt("durable") : (int) (root.getFloat("durable") * info.getReturnValue())));
            }
        }
    }

    @Inject(method = "onCraftedBy", at = @At("TAIL"), require = 0)
    private void onCraftByPlayerMixin(Level world, Player player, int amount, CallbackInfo info) {
        ItemStack stack = (ItemStack) (Object) this;
        Tierify.LOGGER.info("ItemStack created via onCraftByPlayer for {} x{}", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()), amount);
        if (!world.isClientSide() && !stack.isEmpty() && Tierify.CONFIG.craftingModifier) {
            ModifierUtils.applyTierToItem(stack);
            ModifierUtils.logTierDebug("crafting", stack);
        }
    }
}

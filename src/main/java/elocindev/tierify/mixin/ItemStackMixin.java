package elocindev.tierify.mixin;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

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
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component != null) {
            NbtCompound root = component.copyNbt();
            if (root.contains("durable")) {
                info.setReturnValue(info.getReturnValue() + (root.getInt("durable") > 0 ? root.getInt("durable") : (int) (root.getFloat("durable") * info.getReturnValue())));
            }
        }
    }

    @Inject(method = "onCraftByPlayer", at = @At("TAIL"))
    private void onCraftByPlayerMixin(World world, PlayerEntity player, int amount, CallbackInfo info) {
        ItemStack stack = (ItemStack) (Object) this;
        Tierify.LOGGER.info("ItemStack created via onCraftByPlayer for {} x{}", net.minecraft.registry.Registries.ITEM.getId(stack.getItem()), amount);
        if (!world.isClient() && !stack.isEmpty() && Tierify.CONFIG.craftingModifier) {
            ModifierUtils.applyTierToItem(stack);
            ModifierUtils.logTierDebug("crafting", stack);
        }
    }
}

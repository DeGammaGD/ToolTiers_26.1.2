package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemFrame.class)
public class ItemFrameEntityMixin {

    @Inject(method = "setItem(Lnet/minecraft/world/item/ItemStack;Z)V", at = @At("HEAD"))
    private void setItemMixin(ItemStack value, boolean update, CallbackInfo info) {
        ItemFrame self = (ItemFrame) (Object) this;
        if (!self.level().isClientSide() && !update && Tierify.CONFIG.lootContainerModifier && !value.isEmpty()) {
            ModifierUtils.applyTierIfNeeded(value);
            ModifierUtils.logTierDebug("generated_item_frame", value);
        }
    }
}

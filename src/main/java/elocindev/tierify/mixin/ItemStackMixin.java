package elocindev.tierify.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
                int flat = root.getInt("durable").orElse(0);
                float scaled = root.getFloat("durable").orElse(0.0f);
                info.setReturnValue(info.getReturnValue() + (flat > 0 ? flat : (int) (scaled * info.getReturnValue())));
            }
        }
    }

}

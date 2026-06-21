package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;

@Mixin(MerchantMenu.class)
public abstract class MerchantScreenHandlerMixin extends AbstractContainerMenu {

    public MerchantScreenHandlerMixin(MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    @ModifyVariable(method = "quickMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/MerchantScreenHandler;insertItem(Lnet/minecraft/item/ItemStack;IIZ)Z", ordinal = 0), ordinal = 1)
    private ItemStack quickMoveMixin(ItemStack original) {
        if (Tierify.CONFIG.merchantModifier) {
            ModifierUtils.setItemStackAttribute(null, original, false);
        }
        return original;
    }
}

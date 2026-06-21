package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(ItemFrame.class)
public abstract class ItemFrameEntityMixin extends HangingEntity {

    public ItemFrameEntityMixin(EntityType<? extends HangingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;setAsStackHolder(Lnet/minecraft/item/ItemStack;)V"))
    private void setHeldItemStackMixin(ItemStack value, boolean update, CallbackInfo info) {
        if (!this.level().isClientSide() && !update && Tierify.CONFIG.lootContainerModifier) {
            ModifierUtils.setItemStackAttribute(null, value, false);
        }
    }
}

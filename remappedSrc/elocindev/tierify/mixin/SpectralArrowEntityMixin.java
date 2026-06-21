package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.util.AttributeHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(SpectralArrow.class)
public abstract class SpectralArrowEntityMixin extends AbstractArrow {

    public SpectralArrowEntityMixin(EntityType<? extends AbstractArrow> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    private void initMixin(Level world, LivingEntity owner, ItemStack stack, ItemStack shotFrom, CallbackInfo info) {
        if (this.getOwner() instanceof ServerPlayer) {
            this.setBaseDamage(AttributeHelper.getExtraRangeDamage((Player) this.getOwner(), (float) this.getBaseDamage()));
        }
    }
}

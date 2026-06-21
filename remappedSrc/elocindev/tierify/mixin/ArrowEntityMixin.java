package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.util.AttributeHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(Arrow.class)
public abstract class ArrowEntityMixin extends AbstractArrow {

    public ArrowEntityMixin(EntityType<? extends AbstractArrow> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "setStack", at = @At("TAIL"))
    private void setStackMixin(ItemStack stack, CallbackInfo info) {
        if (this.getOwner() instanceof ServerPlayer) {
            this.setBaseDamage(AttributeHelper.getExtraRangeDamage((Player) this.getOwner(), (float) this.getBaseDamage()));
        }
    }

}

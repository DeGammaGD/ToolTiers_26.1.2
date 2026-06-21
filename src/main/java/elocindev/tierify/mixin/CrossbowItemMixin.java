package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.util.AttributeHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "shoot", at = @At("TAIL"))
    private void shootMixin(LivingEntity shooter, ProjectileEntity projectile, int index, float speed, float divergence, float velocity, LivingEntity target, CallbackInfo info) {
        if (projectile instanceof PersistentProjectileEntity persistentProjectileEntity && shooter instanceof PlayerEntity player) {
            persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() + AttributeHelper.getExtraCritDamage(player, (float) persistentProjectileEntity.getDamage()));
        }
    }

}

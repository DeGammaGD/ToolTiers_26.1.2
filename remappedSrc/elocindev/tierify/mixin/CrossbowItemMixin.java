package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.util.AttributeHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "shoot", at = @At("TAIL"))
    private void shootMixin(LivingEntity shooter, Projectile projectile, int index, float speed, float divergence, float velocity, LivingEntity target, CallbackInfo info) {
        if (projectile instanceof AbstractArrow persistentProjectileEntity && shooter instanceof Player player) {
            persistentProjectileEntity.setBaseDamage(persistentProjectileEntity.getBaseDamage() + AttributeHelper.getExtraCritDamage(player, (float) persistentProjectileEntity.getBaseDamage()));
        }
    }

}

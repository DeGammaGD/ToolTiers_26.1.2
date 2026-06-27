package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Boost Efficiency: scales the firework rocket boost duration by {@code (1 + boost_efficiency)} when the rocket is
 * used to power elytra flight, reading the attribute from the glider's equipped elytra (chest slot).
 */
@Mixin(net.minecraft.world.entity.projectile.FireworkRocketEntity.class)
public class FireworkRocketEntityMixin {

    @Shadow
    private int lifetime;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("TAIL"))
    private void tierify$boostEfficiency(Level level, ItemStack stack, LivingEntity shooter, CallbackInfo ci) {
        if (shooter == null) {
            return;
        }
        ItemStack chest = shooter.getItemBySlot(EquipmentSlot.CHEST);
        double boostEfficiency = AttributeHelper.getItemAttributeAmount(chest, CustomEntityAttributes.BOOST_EFFICIENCY);
        if (boostEfficiency > 0.0D) {
            this.lifetime = (int) Math.round(this.lifetime * (1.0D + boostEfficiency));
        }
    }
}

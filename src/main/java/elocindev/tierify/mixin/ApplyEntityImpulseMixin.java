package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.ApplyEntityImpulse;
import net.minecraft.world.phys.Vec3;

/**
 * Amplifies the forward momentum of the vanilla Lunge enchantment by the {@code tiered:generic.lunge} attribute.
 *
 * <p>The vanilla Lunge enchantment applies its forward impulse through {@link ApplyEntityImpulse#apply}, computing
 * an impulse vector via {@code direction...scale(magnitude)} before adding it to the wielder's delta movement. This
 * hook intercepts the final scaled impulse vector and multiplies it by {@code (1 + lunge)}, so the distance traveled
 * grows with the attribute while preserving vanilla behavior when the attribute is absent.</p>
 *
 * <p>This only runs when the Lunge enchantment actually fires its impulse effect, so the bonus is naturally gated on
 * the presence of the Lunge enchantment.</p>
 */
@Mixin(ApplyEntityImpulse.class)
public class ApplyEntityImpulseMixin {

    @ModifyExpressionValue(method = "apply(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 tierify$lungeBoost(Vec3 impulse, ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        double lunge = AttributeHelper.getItemAttributeAmount(item.itemStack(), CustomEntityAttributes.LUNGE);
        if (lunge <= 0.0D) {
            return impulse;
        }

        return impulse.scale(1.0D + lunge);
    }
}

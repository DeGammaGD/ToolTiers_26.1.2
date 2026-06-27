package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Glide Speed: scales elytra glide velocity by {@code (1 + glide_speed)}, reading the attribute from the equipped
 * elytra (chest slot). Hooks the per-tick elytra movement update so vanilla physics are preserved and only boosted.
 */
@Mixin(LivingEntity.class)
public class LivingEntityElytraMixin {

    @ModifyReturnValue(method = "updateFallFlyingMovement(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"))
    private Vec3 tierify$glideSpeed(Vec3 movement) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack chest = self.getItemBySlot(EquipmentSlot.CHEST);
        double glideSpeed = AttributeHelper.getItemAttributeAmount(chest, CustomEntityAttributes.GLIDE_SPEED);
        if (glideSpeed <= 0.0D) {
            return movement;
        }
        return movement.scale(1.0D + glideSpeed);
    }
}

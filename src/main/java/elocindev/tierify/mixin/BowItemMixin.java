package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Makes the {@code tiered:generic.quick_draw} attribute speed up bow drawing as a percentage of the charge time:
 * {@code effectiveChargeTime = baseChargeTime * (1 - quick_draw)}. Because the bow's power is derived from how long
 * it has been charging via {@link BowItem#getPowerForTime(int)}, we divide the elapsed charge ticks by
 * {@code (1 - quick_draw)} so full power is reached in the shortened time. The factor is clamped to a 5% floor.
 */
@Mixin(BowItem.class)
public class BowItemMixin {

    @ModifyExpressionValue(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
    private float tierify$quickDrawBow(float originalPower, ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        double quickDraw = AttributeHelper.getItemAttributeAmount(stack, CustomEntityAttributes.QUICK_DRAW);
        if (quickDraw <= 0.0D) {
            return originalPower;
        }

        double factor = 1.0D - quickDraw;
        if (factor < 0.05D) {
            factor = 0.05D;
        }
        int elapsed = ((BowItem) (Object) this).getUseDuration(stack, entity) - timeLeft;
        int charge = (int) Math.round(elapsed / factor);
        return BowItem.getPowerForTime(charge);
    }
}

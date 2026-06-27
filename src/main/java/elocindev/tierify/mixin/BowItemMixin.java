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
 * Makes the {@code tiered:generic.quick_draw} attribute behave like Quick Charge on bows: the bow draws faster,
 * gaining 5 effective charge ticks (the 0.25s/level Quick Charge grants on crossbows) per level. Implemented by
 * recomputing {@link BowItem#getPowerForTime(int)} with the boosted charge time at release.
 */
@Mixin(BowItem.class)
public class BowItemMixin {

    @ModifyExpressionValue(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
    private float tierify$quickDrawBow(float originalPower, ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        double quickDraw = AttributeHelper.getItemAttributeAmount(stack, CustomEntityAttributes.QUICK_DRAW);
        if (quickDraw <= 0.0D) {
            return originalPower;
        }

        int bonusTicks = (int) Math.round(5.0D * quickDraw);
        int charge = ((BowItem) (Object) this).getUseDuration(stack, entity) - timeLeft + bonusTicks;
        return BowItem.getPowerForTime(charge);
    }
}

package elocindev.tierify.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(UseDuration.class)
public class BowUseDurationMixin {

    @ModifyReturnValue(method = "useDuration(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"))
    private static int tierify$quickDrawBowPullProgress(int elapsedTicks, ItemStack stack, LivingEntity entity) {
        if (!(stack.getItem() instanceof BowItem)) {
            return elapsedTicks;
        }

        double quickDraw = AttributeHelper.getItemAttributeAmount(stack, CustomEntityAttributes.QUICK_DRAW);
        if (quickDraw <= 0.0D) {
            return elapsedTicks;
        }

        double factor = 1.0D - quickDraw;
        if (factor < 0.05D) {
            factor = 0.05D;
        }

        return Math.max(0, (int) Math.round(elapsedTicks / factor));
    }
}

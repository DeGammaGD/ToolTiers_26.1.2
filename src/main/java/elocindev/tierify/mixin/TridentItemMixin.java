package elocindev.tierify.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TridentItem.class)
public class TridentItemMixin {

    private static double tierify$getRiptideMovementFactor(ItemStack stack, Level level, LivingEntity entity) {
        if (level == null) {
            return 1.0D;
        }

        Holder<Enchantment> riptide = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.RIPTIDE);
        if (EnchantmentHelper.getItemEnchantmentLevel(riptide, stack) <= 0) {
            return 1.0D;
        }

        double modifier = AttributeHelper.getItemAttributeAmount(stack, CustomEntityAttributes.RIPTIDE_POWER);
        if (modifier <= 0.0D) {
            return 1.0D;
        }

        return 1.0D + modifier;
    }

    @ModifyExpressionValue(
            method = "releaseUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getTridentSpinAttackStrength(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)F"))
    private float tierify$scaleRiptideSpinStrength(float spinStrength, ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (spinStrength <= 0.0F) {
            return spinStrength;
        }
        double factor = tierify$getRiptideMovementFactor(stack, level, entity);
        return (float) (spinStrength * factor);
    }
}

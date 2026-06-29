package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;

/**
 * Applies the ToolTiers protection attributes ({@code protection}, {@code fire_protection}, {@code blast_protection},
 * {@code projectile_protection}) as a percentage multiplier on the damage that remains after vanilla armor and
 * enchantment protection, instead of using enchantment levels.
 *
 * <p>The combined reduction at this stage is capped so that at most 95% of the incoming (post-vanilla) damage can be
 * removed by ToolTiers protection, satisfying the total protection cap.</p>
 */
@Mixin(LivingEntity.class)
public class LivingEntityProtectionMixin {

    @ModifyReturnValue(method = "getAttackRangeWith(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/component/AttackRange;", at = @At("RETURN"))
    private AttackRange tierify$applySpearReach(AttackRange originalRange, ItemStack weapon) {
        double spearReach = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.SPEAR_REACH);
        if (spearReach == 0.0D) {
            return originalRange;
        }

        float maxReach = (float) Math.max(0.0D, originalRange.maxReach() + spearReach);
        float maxCreativeReach = (float) Math.max(0.0D, originalRange.maxCreativeReach() + spearReach);
        return new AttackRange(
                originalRange.minReach(),
                maxReach,
                originalRange.minCreativeReach(),
                maxCreativeReach,
                originalRange.hitboxMargin(),
                originalRange.mobFactor());
    }

    @ModifyReturnValue(method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("RETURN"))
    private float tierify$tieredProtection(float damageAfterVanilla, DamageSource source, float damageBeforeMagic) {
        LivingEntity self = (LivingEntity) (Object) this;
        double percent = AttributeHelper.getProtectionReductionPercent(self, source);
        if (percent <= 0.0D) {
            return damageAfterVanilla;
        }

        float reduced = damageAfterVanilla * (float) (1.0D - percent);
        // Total protection cap: never remove more than 95% of the damage entering the magic-absorb stage.
        float floor = damageBeforeMagic * (float) (1.0D - AttributeHelper.MAX_PROTECTION_REDUCTION);
        return Math.max(reduced, floor);
    }
}

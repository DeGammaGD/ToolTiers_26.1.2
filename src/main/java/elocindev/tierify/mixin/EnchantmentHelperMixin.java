package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Makes the {@code tiered:generic.looting} and {@code tiered:generic.fortune} custom attributes behave like
 * the vanilla Looting and Fortune enchantments without requiring the enchantment to be present. Vanilla loot
 * functions (enchanted_count_increase / apply_bonus) read the enchantment level through
 * {@link EnchantmentHelper#getItemEnchantmentLevel}, so adding the tiered attribute value on top of the real
 * level transparently grants the bonus drops.
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @ModifyReturnValue(method = "getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemInstance;)I", at = @At("RETURN"))
    private static int tierify$addTieredEnchantmentLevels(int original, Holder<Enchantment> enchantment, ItemInstance item) {
        int bonus = 0;

        if (enchantment.is(Enchantments.LOOTING)) {
            bonus = (int) Math.floor(AttributeHelper.getItemAttributeAmount(item, CustomEntityAttributes.LOOTING));
        } else if (enchantment.is(Enchantments.FORTUNE)) {
            bonus = (int) Math.floor(AttributeHelper.getItemAttributeAmount(item, CustomEntityAttributes.FORTUNE));
        }

        if (bonus <= 0) {
            return original;
        }

        return original + bonus;
    }

    /**
     * Adds the ToolTiers protection attributes ({@code protection}, {@code fire_protection}, {@code blast_protection},
     * {@code projectile_protection}) on top of the vanilla enchantment protection points, so they reduce incoming
     * damage exactly like the matching protection enchantments without requiring the enchantment.
     */
    @ModifyReturnValue(method = "getDamageProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)F", at = @At("RETURN"))
    private static float tierify$addTieredProtection(float original, ServerLevel level, LivingEntity entity, DamageSource source) {
        return original + AttributeHelper.getProtectionBonus(entity, source);
    }

    /**
     * Mirrors the vanilla Breach enchantment ({@code armor_effectiveness} add {@code -0.15} per level) for the
     * {@code tiered:generic.breach} attribute, reducing the target's armor effectiveness when attacking.
     */
    @ModifyReturnValue(method = "modifyArmorEffectiveness(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("RETURN"))
    private static float tierify$addTieredBreach(float effectiveness, ServerLevel level, ItemStack weapon, Entity victim, DamageSource source) {
        double breach = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.BREACH);
        if (breach <= 0.0D) {
            return effectiveness;
        }
        return Math.max(0.0f, effectiveness - (float) (0.15D * breach));
    }

    /**
     * Mirrors the vanilla Power enchantment ({@code damage} add {@code base 1.0, +0.5 per level above first},
     * gated on the direct attacker being an arrow) for the {@code tiered:generic.power} attribute on bows/crossbows.
     * {@code AbstractArrow.onHitEntity} routes its damage through {@code modifyDamage} with the firing weapon, so the
     * bonus stacks on top of any real Power enchantment.
     */
    @ModifyReturnValue(method = "modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("RETURN"))
    private static float tierify$addTieredPower(float damage, ServerLevel level, ItemStack weapon, Entity victim, DamageSource source) {
        if (source == null || !(source.getDirectEntity() instanceof AbstractArrow)) {
            return damage;
        }
        double power = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.POWER);
        if (power <= 0.0D) {
            return damage;
        }
        return damage + (float) (0.5D * power + 0.5D);
    }

    /**
     * Mirrors the vanilla Density enchantment ({@code smash_damage_per_fallen_block} add {@code 0.5} per level) for
     * the {@code tiered:generic.density} attribute. {@code MaceItem.getAttackDamageBonus} routes the mace smash
     * damage through {@code modifyFallBasedDamage}, so we add {@code 0.5 * density * fallenBlocks} on top.
     */
    @ModifyReturnValue(method = "modifyFallBasedDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("RETURN"))
    private static float tierify$addTieredDensity(float fallDamage, ServerLevel level, ItemStack weapon, Entity attacker, DamageSource source) {
        double density = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.DENSITY);
        if (density <= 0.0D || attacker == null) {
            return fallDamage;
        }
        double fallenBlocks = attacker.fallDistance;
        if (fallenBlocks <= 0.0D) {
            return fallDamage;
        }
        return fallDamage + (float) (0.5D * density * fallenBlocks);
    }

    /**
     * Mirrors the vanilla Quick Charge enchantment ({@code crossbow_charge_time} add {@code -0.25} per level) for the
     * {@code tiered:generic.quick_draw} attribute, shortening crossbow charge time (in seconds).
     */
    @ModifyReturnValue(method = "modifyCrossbowChargingTime(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;F)F", at = @At("RETURN"))
    private static float tierify$quickDrawCrossbow(float chargeTime, ItemStack weapon, LivingEntity entity) {
        double quickDraw = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.QUICK_DRAW);
        if (quickDraw <= 0.0D) {
            return chargeTime;
        }
        return Math.max(0.0f, chargeTime - (float) (0.25D * quickDraw));
    }
}

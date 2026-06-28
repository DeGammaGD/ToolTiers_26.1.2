package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.AttributeHelper;
import elocindev.tierify.util.CombatContextHelper;
import elocindev.tierify.util.LuckyShotHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.function.Consumer;

/**
 * Connects several ToolTiers combat attributes to the matching vanilla enchantment mechanics without requiring the
 * enchantment to be present. Looting and Fortune are handled separately (as percentage drop multipliers in
 * {@code LootTableMixin}); this mixin covers the aggregate combat hooks (Breach, Power, Density) and crossbow
 * Quick Draw. Protection is handled in {@code LivingEntityProtectionMixin} as a percentage damage reduction.
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    private static int tierify$getEnchantmentLevel(ServerLevel level, ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> enchantmentKey) {
        Holder<Enchantment> enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
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
     * Applies tiered ranged-weapon damage modifiers at the same final projectile damage hook used by vanilla
     * enchantment logic. {@code tiered:generic.power} mirrors the vanilla Power enchantment bonus, while
     * {@code tiered:generic.critical_damage} acts as a pure multiplier on the resulting arrow damage. This keeps
     * vanilla charging, velocity, and random melee-style crit logic out of the ranged path.
     */
    @ModifyReturnValue(method = "modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("RETURN"))
    private static float tierify$addTieredPowerAndCriticalDamage(float damage, ServerLevel level, ItemStack weapon, Entity victim, DamageSource source) {
        if (source == null) {
            return damage;
        }

        if (source.getDirectEntity() instanceof Player player) {
            if (CombatContextHelper.wasVanillaMeleeCrit()) {
                return damage;
            }
            if (AttributeHelper.shouldMeleeCrit(player)) {
                float modifiedDamage = damage * (1.5F + AttributeHelper.getCriticalDamageModifier(player));
                player.crit(victim);
                return modifiedDamage;
            }
            return damage;
        }

        if (!(source.getDirectEntity() instanceof AbstractArrow)) {
            return damage;
        }

        float modifiedDamage = damage;
        double power = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.POWER);
        if (power <= 0.0D) {
            double criticalDamage = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.CRITICAL_DAMAGE);
            if (criticalDamage <= 0.0D) {
                return modifiedDamage;
            }
            return modifiedDamage * (1.0f + (float) criticalDamage);
        }

        modifiedDamage += (float) (0.5D * power + 0.5D);

        double criticalDamage = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.CRITICAL_DAMAGE);
        if (criticalDamage <= 0.0D) {
            return modifiedDamage;
        }
        return modifiedDamage * (1.0f + (float) criticalDamage);
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
     * Speeds up crossbow charging for the {@code tiered:generic.quick_draw} attribute using a percentage of the
     * base charge time: {@code newTime = baseTime * (1 - quick_draw)}. The charge time is clamped so it can never
     * drop below 5% of the base, preventing instant/zero charge times.
     */
    @ModifyReturnValue(method = "modifyCrossbowChargingTime(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;F)F", at = @At("RETURN"))
    private static float tierify$quickDrawCrossbow(float chargeTime, ItemStack weapon, LivingEntity entity) {
        double quickDraw = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.QUICK_DRAW);
        if (quickDraw <= 0.0D) {
            return chargeTime;
        }
        double factor = 1.0D - quickDraw;
        if (factor < 0.05D) {
            factor = 0.05D;
        }
        return chargeTime * (float) factor;
    }

    @Inject(method = "doPostAttackEffectsWithItemSourceOnBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)V", at = @At("TAIL"))
    private static void tierify$applyChannelingChance(ServerLevel level,
                                                       Entity victim,
                                                       DamageSource source,
                                                       ItemStack weapon,
                                                       Consumer<Item> onBreak,
                                                       CallbackInfo ci) {
        if (source == null || !(source.getDirectEntity() instanceof ThrownTrident)) {
            return;
        }

        if (victim == null || !victim.isAlive()) {
            return;
        }

        if (tierify$getEnchantmentLevel(level, weapon, Enchantments.CHANNELING) <= 0) {
            return;
        }

        double chance = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.CHANNELING_CHANCE);
        if (chance <= 0.0D) {
            return;
        }

        double normalizedChance = Math.max(0.0D, Math.min(1.0D, chance));
        if (level.getRandom().nextDouble() > normalizedChance) {
            return;
        }

        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.snapTo(victim.getX(), victim.getY(), victim.getZ());
        if (source.getEntity() instanceof ServerPlayer player) {
            bolt.setCause(player);
        }
        level.addFreshEntity(bolt);
    }

    @Inject(method = "onProjectileSpawned(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/projectile/Projectile;Ljava/util/function/Consumer;)V", at = @At("TAIL"))
    private static void tierify$applyLuckyShot(ServerLevel level, ItemStack weapon, Projectile projectile, Consumer<Item> onBreak, CallbackInfo ci) {
        if (!(projectile instanceof Arrow arrow)) {
            return;
        }

        if (!(projectile.getOwner() instanceof Player player)) {
            return;
        }

        if (!LuckyShotHelper.isEligibleNormalArrow(arrow)) {
            return;
        }

        double chance = LuckyShotHelper.getLuckyShotChance(weapon);
        if (chance <= 0.0D || player.getRandom().nextDouble() > chance) {
            return;
        }

        LuckyShotHelper.LuckyShotEffect effect = LuckyShotHelper.getRandomLuckyShotEffect(player.getRandom());
        LuckyShotHelper.applyLuckyShotEffect(arrow, effect);
        Tierify.LOGGER.info("LUCKY SHOT: {}", effect.debugName());
    }
}

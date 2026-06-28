package elocindev.tierify.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.AttributeHelper;
import elocindev.tierify.util.CombatContextHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerEntityMixin {

    private static final Identifier TIERIFY_GLIDE_SPEED_ID = Tierify.id("glide_speed");

    /**
     * Registers all ToolTiers custom attributes onto the player's attribute map.
     * Without this, player.getAttribute(CRITICAL_CHANCE/CRITICAL_DAMAGE/...) returns null
     * and no weapon modifiers are ever applied to the player's live attribute instances.
     */
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void tierify$addCustomAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> info) {
        AttributeSupplier.Builder builder = info.getReturnValue();
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.CRITICAL_CHANCE));
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.LEGACY_CRIT_CHANCE));
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.CRITICAL_DAMAGE));
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.RANGED_ATTACK_DAMAGE));
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.LEGACY_RANGE_ATTACK_DAMAGE));
        // Players don't have the flying-speed attribute by default; add it so glide_speed can drive it safely.
        builder.add(Attributes.FLYING_SPEED);
    }

    /**
     * Glide Speed: instead of mutating elytra physics directly, drive the vanilla {@code flying_speed} attribute.
     * While the player is gliding with an elytra carrying {@code tiered:generic.glide_speed}, a transient
     * {@code ADD_MULTIPLIED_BASE} modifier equal to the attribute value is kept in sync on the flying-speed
     * attribute; it is removed as soon as the player stops gliding or unequips the elytra.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tierify$syncGlideSpeed(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        AttributeInstance instance = self.getAttribute(Attributes.FLYING_SPEED);
        if (instance == null) {
            return;
        }

        double glideSpeed = AttributeHelper.getItemAttributeAmount(self.getItemBySlot(EquipmentSlot.CHEST),
                CustomEntityAttributes.GLIDE_SPEED);
        boolean shouldApply = glideSpeed > 0.0D && self.isFallFlying();

        AttributeModifier existing = instance.getModifier(TIERIFY_GLIDE_SPEED_ID);
        if (shouldApply) {
            if (existing == null || existing.amount() != glideSpeed) {
                instance.removeModifier(TIERIFY_GLIDE_SPEED_ID);
                instance.addTransientModifier(new AttributeModifier(TIERIFY_GLIDE_SPEED_ID, glideSpeed,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        } else if (existing != null) {
            instance.removeModifier(TIERIFY_GLIDE_SPEED_ID);
        }
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void tierify$beginMeleeAttackContext(Entity target, CallbackInfo ci) {
        CombatContextHelper.beginMeleeAttack();
    }

    @ModifyExpressionValue(
            method = "attack(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canCriticalAttack(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean tierify$recordVanillaCrit(boolean vanillaCriticalAttack) {
        CombatContextHelper.markVanillaMeleeCrit(vanillaCriticalAttack);
        return vanillaCriticalAttack;
    }

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void tierify$endMeleeAttackContext(Entity target, CallbackInfo ci) {
        CombatContextHelper.endMeleeAttack();
    }

    @ModifyConstant(method = "attack(Lnet/minecraft/world/entity/Entity;)V", constant = @Constant(floatValue = 1.5F))
    private float tierify$modifyCriticalMultiplier(float vanillaCriticalMultiplier) {
        return vanillaCriticalMultiplier + AttributeHelper.getCriticalDamageModifier((Player) (Object) this);
    }

    @ModifyArg(
            method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"),
            index = 0)
    private double tierify$modifySweepRangeX(double vanillaSweepRange) {
        return tierify$getScaledSweepRange(vanillaSweepRange);
    }

    @ModifyArg(
            method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"),
            index = 2)
    private double tierify$modifySweepRangeZ(double vanillaSweepRange) {
        return tierify$getScaledSweepRange(vanillaSweepRange);
    }

    @ModifyVariable(
            method = "stabAttack(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private float tierify$modifyChargedSpearBaseDamage(float baseDamage,
                                                       EquipmentSlot slot,
                                                       Entity target,
                                                       float enchantmentDamage,
                                                       boolean vanillaCriticalAttack,
                                                       boolean sweepingAttack,
                                                       boolean sprintKnockbackAttack) {
        Player self = (Player) (Object) this;
        ItemStack weapon = self.getItemBySlot(slot);
        if (!tierify$isSpearItem(weapon)) {
            return baseDamage;
        }

        double chargeDamage = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.CHARGE_DAMAGE);
        double factor = Math.max(0.0D, 1.0D + chargeDamage);
        float damage = baseDamage * (float) factor;

        // Keep vanilla critical/sweep/sprint branching intact by not overriding the vanilla crit flag.
        // Spears use stabAttack where vanilla crit behavior differs, so apply tiered crit chance directly to damage.
        return damage;
    }

    private double tierify$getScaledSweepRange(double vanillaSweepRange) {
        Player self = (Player) (Object) this;
        ItemStack weapon = self.getWeaponItem();
        double sweepingRange = AttributeHelper.getItemAttributeAmount(weapon, CustomEntityAttributes.SWEEPING_RANGE);
        double factor = Math.max(0.0D, 1.0D + sweepingRange);
        return vanillaSweepRange * factor;
    }

    private boolean tierify$isSpearItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return "minecraft:trident".equals(itemId) || itemId.endsWith(":spear") || itemId.contains("_spear");
    }
}
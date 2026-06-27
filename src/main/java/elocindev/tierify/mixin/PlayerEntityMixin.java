package elocindev.tierify.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerEntityMixin {

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
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.HASTE));
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.LEGACY_DIG_SPEED));
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.RANGED_ATTACK_DAMAGE));
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.LEGACY_RANGE_ATTACK_DAMAGE));
    }

    /**
     * Overrides whether the melee hit is a critical hit.
     *
     * <p>Targets the {@code canCriticalAttack(Entity)} call inside {@code attack(Entity)} instead of the
     * {@code criticalAttack} local variable. The previous {@code @ModifyVariable} relied on local-variable-table
     * analysis, which is unreliable when the mixin class version (Java 25) is higher than the declared mixin
     * {@code compatibilityLevel}. This INVOKE-based hook is LVT-independent and fires exactly when vanilla evaluates
     * the crit condition (i.e. only at full attack strength), preserving vanilla jump-crits.
     */
    @ModifyExpressionValue(
            method = "attack(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canCriticalAttack(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean tierify$applyCritChance(boolean vanillaCriticalAttack) {
        return vanillaCriticalAttack || AttributeHelper.shouldMeleeCrit((Player) (Object) this);
    }

    @ModifyConstant(method = "attack(Lnet/minecraft/world/entity/Entity;)V", constant = @Constant(floatValue = 1.5F))
    private float tierify$modifyCriticalMultiplier(float vanillaCriticalMultiplier) {
        return vanillaCriticalMultiplier + AttributeHelper.getCriticalDamageModifier((Player) (Object) this);
    }
}
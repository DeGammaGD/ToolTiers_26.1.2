package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.CustomEntityAttributes;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    private PlayerEntityMixin(EntityType<? extends LivingEntity> type, Level world) {
        super(type, world);
    }

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"))
    private static void createPlayerAttributesMixin(CallbackInfoReturnable<AttributeSupplier.Builder> info) {
        info.getReturnValue().add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.CRIT_CHANCE));
        info.getReturnValue().add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.DIG_SPEED));
        info.getReturnValue().add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.DURABLE));
        info.getReturnValue().add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.RANGE_ATTACK_DAMAGE));
    }

    @ModifyVariable(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/effect/StatusEffectUtil;hasHaste(Lnet/minecraft/entity/LivingEntity;)Z"), index = 2)
    private float getBlockBreakingSpeedMixin(float f) {
        return AttributeHelper.getExtraDigSpeed((Player) (Object) this, f);
    }

    @ModifyVariable(method = "attack", at = @At(value = "JUMP", ordinal = 2), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSprinting()Z", ordinal = 1)), index = 8)
    private boolean attackMixin(boolean bl3) {
        return bl3 || AttributeHelper.shouldMeeleCrit((Player) (Object) this);
    }
}
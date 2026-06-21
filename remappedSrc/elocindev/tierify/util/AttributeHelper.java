package elocindev.tierify.util;

import draylar.tiered.api.CustomEntityAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class AttributeHelper {

    public static boolean shouldMeeleCrit(Player playerEntity) {
        AttributeInstance instance = playerEntity.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.CRIT_CHANCE));
        if (instance != null) {
            float critChance = 0.0f;
            for (AttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.amount();
                critChance += amount;
            }
            return playerEntity.getRandom().nextDouble() < critChance;
        }
        return false;
    }

    public static float getExtraDigSpeed(Player playerEntity, float oldDigSpeed) {
        AttributeInstance instance = playerEntity.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.DIG_SPEED));
        if (instance != null) {
            float extraDigSpeed = oldDigSpeed;
            for (AttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.amount();

                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    extraDigSpeed += amount;
                } else {
                    extraDigSpeed *= (amount + 1);
                }
            }
            return extraDigSpeed;
        }

        return oldDigSpeed;
    }

    public static float getExtraRangeDamage(Player playerEntity, float oldDamage) {
        AttributeInstance instance = playerEntity.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.RANGE_ATTACK_DAMAGE));
        if (instance != null) {
            float rangeDamage = oldDamage;
            for (AttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.amount();

                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    rangeDamage += amount;
                } else {
                    rangeDamage *= (amount + 1.0f);
                }
            }
            return Math.min(rangeDamage, Integer.MAX_VALUE);
        }
        return oldDamage;
    }

    public static float getExtraCritDamage(Player playerEntity, float oldDamage) {
        AttributeInstance instance = playerEntity.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.CRIT_CHANCE));
        if (instance != null) {
            float customChance = 0.0f;
            for (AttributeModifier modifier : instance.getModifiers()) {
                customChance += (float) modifier.amount();
            }
            if (playerEntity.level().getRandom().nextFloat() > (1.0f - Math.abs(customChance))) {
                float extraCrit = oldDamage;
                if (customChance < 0.0f) {
                    extraCrit = extraCrit / 2.0f;
                }
                return oldDamage + Math.min(customChance > 0.0f ? extraCrit : -extraCrit, Integer.MAX_VALUE);
            }
        }
        return oldDamage;
    }

}

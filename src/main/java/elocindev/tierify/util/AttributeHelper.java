package elocindev.tierify.util;

import draylar.tiered.api.CustomEntityAttributes;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class AttributeHelper {

    /**
     * Sums the raw amounts of every {@link ItemAttributeModifiers} entry on the given item that targets
     * the supplied custom attribute. Reads the baked {@code DataComponents.ATTRIBUTE_MODIFIERS} component,
     * so it returns the tier-generated value regardless of equipment slot.
     */
    public static double getItemAttributeAmount(DataComponentGetter item, Attribute attribute) {
        if (item == null || attribute == null) {
            return 0.0D;
        }
        ItemAttributeModifiers component = item.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (component == null) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (ItemAttributeModifiers.Entry entry : component.modifiers()) {
            if (entry.attribute().value() == attribute) {
                sum += entry.modifier().amount();
            }
        }
        return sum;
    }

    /**
     * Maximum fraction of damage the ToolTiers protection attributes are allowed to remove. Acts as the total
     * protection cap requested by the design (95%).
     */
    public static final double MAX_PROTECTION_REDUCTION = 0.95D;

    /**
     * Computes the percentage of damage that ToolTiers protection attributes should remove for the given damage
     * source, summed across the entity's equipment. Generic {@code protection} applies to all damage; the typed
     * protections ({@code fire}/{@code blast}/{@code projectile}) add their amount only when the damage source
     * matches. The result is a fraction in {@code [0, 0.95]} and is applied as a multiplier on the existing
     * (post-vanilla) damage rather than as enchantment levels.
     */
    public static double getProtectionReductionPercent(net.minecraft.world.entity.LivingEntity entity,
                                                       net.minecraft.world.damagesource.DamageSource source) {
        if (entity == null) {
            return 0.0D;
        }

        boolean isFire = source != null && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE);
        boolean isExplosion = source != null && source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        boolean isProjectile = source != null && source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);

        double percent = 0.0D;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            percent += getItemAttributeAmount(stack, CustomEntityAttributes.PROTECTION);
            if (isFire) {
                percent += getItemAttributeAmount(stack, CustomEntityAttributes.FIRE_PROTECTION);
            }
            if (isExplosion) {
                percent += getItemAttributeAmount(stack, CustomEntityAttributes.BLAST_PROTECTION);
            }
            if (isProjectile) {
                percent += getItemAttributeAmount(stack, CustomEntityAttributes.PROJECTILE_PROTECTION);
            }
        }

        if (percent < 0.0D) {
            return 0.0D;
        }
        if (percent > MAX_PROTECTION_REDUCTION) {
            return MAX_PROTECTION_REDUCTION;
        }
        return percent;
    }

    private static float normalizeChance(float rawChance) {
        float normalized = rawChance;
        if (Math.abs(normalized) > 1.0f) {
            normalized = normalized / 100.0f;
        }
        if (normalized < 0.0f) {
            return 0.0f;
        }
        if (normalized > 1.0f) {
            return 1.0f;
        }
        return normalized;
    }

    private static float getMainhandCriticalChanceFallback(Player playerEntity) {
        ItemStack mainhand = playerEntity.getMainHandItem();
        if (mainhand == null || mainhand.isEmpty()) {
            return 0.0f;
        }

        final float[] total = new float[] { 0.0f };
        mainhand.forEachModifier(EquipmentSlot.MAINHAND, (attributeHolder, modifier) -> {
            var attribute = attributeHolder.value();
            for (var criticalChanceAttribute : CustomEntityAttributes.CRITICAL_CHANCE_ATTRIBUTES) {
                if (attribute == criticalChanceAttribute) {
                    total[0] += (float) modifier.amount();
                    break;
                }
            }
        });
        return total[0];
    }

    private static float sumAttributeModifiers(Player playerEntity, net.minecraft.world.entity.ai.attributes.Attribute[] attributes) {
        float total = 0.0f;
        for (var attribute : attributes) {
            AttributeInstance instance = playerEntity.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
            if (instance == null) {
                continue;
            }

            for (AttributeModifier modifier : instance.getModifiers()) {
                total += (float) modifier.amount();
            }
        }
        return total;
    }

    public static float getCriticalChance(Player playerEntity) {
        float rawChance = sumAttributeModifiers(playerEntity, CustomEntityAttributes.CRITICAL_CHANCE_ATTRIBUTES);
        if (rawChance == 0.0f) {
            rawChance = getMainhandCriticalChanceFallback(playerEntity);
        }
        return normalizeChance(rawChance);
    }

    public static float getCriticalDamageModifier(Player playerEntity) {
        AttributeInstance instance = playerEntity.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(CustomEntityAttributes.CRITICAL_DAMAGE));
        if (instance == null) {
            return 0.0f;
        }

        float bonus = 0.0f;
        for (AttributeModifier modifier : instance.getModifiers()) {
            bonus += (float) modifier.amount();
        }
        return bonus;
    }

    public static float getCriticalDamageBonus(Player playerEntity) {
        return getCriticalDamageModifier(playerEntity);
    }

    public static boolean shouldMeleeCrit(Player playerEntity) {
        // Custom melee crit chance roll. Vanilla crit state is tracked separately and handled by the
        // attack pipeline context to avoid stacking custom and vanilla crit multipliers.
        float criticalChance = getCriticalChance(playerEntity);
        return criticalChance > 0.0f && playerEntity.getRandom().nextFloat() < criticalChance;
    }

    public static boolean shouldMeeleCrit(Player playerEntity) {
        return shouldMeleeCrit(playerEntity);
    }

    public static float getExtraRangeDamage(Player playerEntity, float oldDamage) {
        float rangeDamage = oldDamage;
        boolean foundModifier = false;
        for (var attribute : CustomEntityAttributes.RANGED_ATTACK_DAMAGE_ATTRIBUTES) {
            AttributeInstance instance = playerEntity.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
            if (instance == null) {
                continue;
            }

            for (AttributeModifier modifier : instance.getModifiers()) {
                foundModifier = true;
                float amount = (float) modifier.amount();

                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    rangeDamage += amount;
                } else {
                    rangeDamage *= (amount + 1.0f);
                }
            }
        }

        if (foundModifier) {
            return Math.min(rangeDamage, Integer.MAX_VALUE);
        }
        return oldDamage;
    }

    public static float getExtraCritDamage(Player playerEntity, float oldDamage) {
        float customChance = getCriticalChance(playerEntity);
        if (customChance != 0.0f) {
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

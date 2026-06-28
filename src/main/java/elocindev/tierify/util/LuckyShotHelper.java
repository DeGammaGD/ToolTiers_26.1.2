package elocindev.tierify.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class LuckyShotHelper {

    private LuckyShotHelper() {
    }

    public static final String LUCKY_SHOT_ATTRIBUTE_ID = "tiered:generic.lucky_shot";

    public static double getLuckyShotChance(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            return 0.0D;
        }

        ItemAttributeModifiers component = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (component == null) {
            return 0.0D;
        }

        double chance = 0.0D;
        for (ItemAttributeModifiers.Entry entry : component.modifiers()) {
            Attribute attribute = entry.attribute().value();
            String attributeId = BuiltInRegistries.ATTRIBUTE.getKey(attribute).toString();
            if (!LUCKY_SHOT_ATTRIBUTE_ID.equals(attributeId)) {
                continue;
            }

            AttributeModifier modifier = entry.modifier();
            chance += modifier.amount();
        }

        if (Math.abs(chance) > 1.0D) {
            chance /= 100.0D;
        }

        return Mth.clamp(chance, 0.0D, 1.0D);
    }

    public static boolean isEligibleNormalArrow(Arrow arrow) {
        ItemStack pickup = arrow.getPickupItemStackOrigin();
        if (!pickup.is(Items.ARROW)) {
            return false;
        }

        PotionContents potion = pickup.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return potion.equals(PotionContents.EMPTY);
    }

    public static LuckyShotEffect getRandomLuckyShotEffect(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> new LuckyShotEffect("POISON", new MobEffectInstance(MobEffects.POISON, 100));
            case 1 -> new LuckyShotEffect("WEAKNESS", new MobEffectInstance(MobEffects.WEAKNESS, 220));
            case 2 -> new LuckyShotEffect("SLOWNESS", new MobEffectInstance(MobEffects.SLOWNESS, 220));
            default -> new LuckyShotEffect("WIND CHARGING", new MobEffectInstance(MobEffects.WIND_CHARGED, 440));
        };
    }

    public static void applyLuckyShotEffect(Arrow arrow, LuckyShotEffect effect) {
        arrow.addEffect(effect.mobEffect());
    }

    public record LuckyShotEffect(String debugName, MobEffectInstance mobEffect) {
    }
}

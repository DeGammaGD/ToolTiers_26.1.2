package draylar.tiered.api;

import elocindev.tierify.Tierify;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

@SuppressWarnings({"null"})
public class CustomEntityAttributes {

    public static final String HASTE_ID = "tiered:generic.haste";
    public static final String LEGACY_DIG_SPEED_ID = "tiered:generic.dig_speed";
    public static final String CRITICAL_CHANCE_ID = "tiered:generic.critical_chance";
    public static final String LEGACY_CRIT_CHANCE_ID = "tiered:generic.crit_chance";
    public static final String DURABILITY_ID = "tiered:generic.durability";
    public static final String LEGACY_DURABLE_ID = "tiered:generic.durable";
    public static final String RANGED_ATTACK_DAMAGE_ID = "tiered:generic.ranged_attack_damage";
    public static final String LEGACY_RANGE_ATTACK_DAMAGE_ID = "tiered:generic.range_attack_damage";
    public static final String PROTECTION_FAMILY_ID = "tiered:generic.protection_family";
    public static final String PROTECTION_ID = "tiered:generic.protection";
    public static final String FIRE_PROTECTION_ID = "tiered:generic.fire_protection";
    public static final String BLAST_PROTECTION_ID = "tiered:generic.blast_protection";
    public static final String PROJECTILE_PROTECTION_ID = "tiered:generic.projectile_protection";
    public static final String POWER_ID = "tiered:generic.power";
    public static final String QUICK_DRAW_ID = "tiered:generic.quick_draw";

    public static final Attribute HASTE = register("generic.haste", new RangedAttribute("generic.haste", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final Attribute LEGACY_DIG_SPEED = register("generic.dig_speed", new RangedAttribute("generic.dig_speed", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final Attribute CRITICAL_CHANCE = register("generic.critical_chance", new RangedAttribute("generic.critical_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));
    public static final Attribute LEGACY_CRIT_CHANCE = register("generic.crit_chance", new RangedAttribute("generic.crit_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));
    public static final Attribute DURABILITY = register("generic.durability", new RangedAttribute("generic.durability", 0.0D, 0.0D, 1.0D).setSyncable(true));
    public static final Attribute LEGACY_DURABLE = register("generic.durable", new RangedAttribute("generic.durable", 0.0D, 0.0D, 1.0D).setSyncable(true));
    public static final Attribute RANGED_ATTACK_DAMAGE = register("generic.ranged_attack_damage", new RangedAttribute("generic.ranged_attack_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final Attribute LEGACY_RANGE_ATTACK_DAMAGE = register("generic.range_attack_damage", new RangedAttribute("generic.range_attack_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final Attribute PROTECTION_FAMILY = register("generic.protection_family", new RangedAttribute("generic.protection_family", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute PROTECTION = register("generic.protection", new RangedAttribute("generic.protection", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute FIRE_PROTECTION = register("generic.fire_protection", new RangedAttribute("generic.fire_protection", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute BLAST_PROTECTION = register("generic.blast_protection", new RangedAttribute("generic.blast_protection", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute PROJECTILE_PROTECTION = register("generic.projectile_protection", new RangedAttribute("generic.projectile_protection", 0.0D, 0.0D, 1024.0D).setSyncable(true));

    public static final Attribute FORTUNE = register("generic.fortune", new RangedAttribute("generic.fortune", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute REACH = register("generic.reach", new RangedAttribute("generic.reach", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute MASTERY = register("generic.mastery", new RangedAttribute("generic.mastery", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute CRITICAL_DAMAGE = register("generic.critical_damage", new RangedAttribute("generic.critical_damage", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute LOOTING = register("generic.looting", new RangedAttribute("generic.looting", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute SWEEPING_RANGE = register("generic.sweeping_range", new RangedAttribute("generic.sweeping_range", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute MOBILITY = register("generic.mobility", new RangedAttribute("generic.mobility", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute HEALTH = register("generic.health", new RangedAttribute("generic.health", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute GLIDE_SPEED = register("generic.glide_speed", new RangedAttribute("generic.glide_speed", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute BOOST_EFFICIENCY = register("generic.boost_efficiency", new RangedAttribute("generic.boost_efficiency", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute DENSITY = register("generic.density", new RangedAttribute("generic.density", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute BREACH = register("generic.breach", new RangedAttribute("generic.breach", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute HANDLING = register("generic.handling", new RangedAttribute("generic.handling", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute LUNGE = register("generic.lunge", new RangedAttribute("generic.lunge", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute CHARGE_DAMAGE = register("generic.charge_damage", new RangedAttribute("generic.charge_damage", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute POWER = register("generic.power", new RangedAttribute("generic.power", 0.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final Attribute QUICK_DRAW = register("generic.quick_draw", new RangedAttribute("generic.quick_draw", 0.0D, 0.0D, 1024.0D).setSyncable(true));

    public static final Attribute[] PLAYER_ATTRIBUTES = new Attribute[] {
            HASTE,
            LEGACY_DIG_SPEED,
            CRITICAL_CHANCE,
            LEGACY_CRIT_CHANCE,
            DURABILITY,
            LEGACY_DURABLE,
            RANGED_ATTACK_DAMAGE,
            LEGACY_RANGE_ATTACK_DAMAGE
    };

    public static final Attribute[] HASTE_ATTRIBUTES = new Attribute[] { HASTE, LEGACY_DIG_SPEED };
    public static final Attribute[] CRITICAL_CHANCE_ATTRIBUTES = new Attribute[] { CRITICAL_CHANCE, LEGACY_CRIT_CHANCE };
    public static final Attribute[] DURABILITY_ATTRIBUTES = new Attribute[] { DURABILITY, LEGACY_DURABLE };
    public static final Attribute[] RANGED_ATTACK_DAMAGE_ATTRIBUTES = new Attribute[] { RANGED_ATTACK_DAMAGE, LEGACY_RANGE_ATTACK_DAMAGE };
        public static final String[] PROTECTION_FAMILY_MEMBERS = new String[] {
            PROTECTION_ID,
            FIRE_PROTECTION_ID,
            BLAST_PROTECTION_ID,
            PROJECTILE_PROTECTION_ID
        };

    public static void init() {
        // NO-OP
    }

    public static boolean isDurabilityAttributeId(String attributeTypeId) {
        return DURABILITY_ID.equals(attributeTypeId) || LEGACY_DURABLE_ID.equals(attributeTypeId);
    }

    public static boolean isProtectionFamilyAttributeId(String attributeTypeId) {
        return PROTECTION_FAMILY_ID.equals(attributeTypeId);
    }

    public static Identifier canonicalId(String path) {
        return Tierify.id(path);
    }

    private static Attribute register(String idPath, Attribute attribute) {
        return Registry.register(BuiltInRegistries.ATTRIBUTE, Tierify.id(idPath), attribute);
    }
}

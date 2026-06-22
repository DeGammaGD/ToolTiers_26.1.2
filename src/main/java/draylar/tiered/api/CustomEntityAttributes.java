package draylar.tiered.api;

import elocindev.tierify.Tierify;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class CustomEntityAttributes {

    public static final Attribute DIG_SPEED = register("generic.dig_speed", new RangedAttribute("generic.dig_speed", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final Attribute CRIT_CHANCE = register("generic.crit_chance", new RangedAttribute("generic.crit_chance", 0.0D, 0.0D, 1D).setSyncable(true));
    public static final Attribute DURABLE = register("generic.durable", new RangedAttribute("generic.durable", 0.0D, 0.0D, 1D).setSyncable(true));
    public static final Attribute RANGE_ATTACK_DAMAGE = register("generic.range_attack_damage", new RangedAttribute("generic.range_attack_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));

    public static void init() {
        // NO-OP
    }

    private static Attribute register(String idPath, Attribute attribute) {
        return Registry.register(BuiltInRegistries.ATTRIBUTE, Tierify.id(idPath), attribute);
    }
}

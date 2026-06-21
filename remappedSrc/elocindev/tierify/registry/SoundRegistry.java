package elocindev.tierify.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class SoundRegistry {

    public static SoundEvent REFORGE_SOUND_COMMON = register("reforge_sound_common");
    public static SoundEvent REFORGE_SOUND_UNCOMMON = register("reforge_sound_uncommon");
    public static SoundEvent REFORGE_SOUND_RARE = register("reforge_sound_rare");
    public static SoundEvent REFORGE_SOUND_EPIC = register("reforge_sound_epic");
    public static SoundEvent REFORGE_SOUND_LEGENDARY = register("reforge_sound_legendary");
    public static SoundEvent REFORGE_SOUND_MYTHIC = register("reforge_sound_mythic");


    public static void registerSounds() {
    }

    private static SoundEvent register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("tiered", name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

}

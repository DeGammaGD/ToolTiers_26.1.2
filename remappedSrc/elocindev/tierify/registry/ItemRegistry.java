package elocindev.tierify.registry;

import elocindev.tierify.Tierify;
import elocindev.tierify.item.ReforgeAddition;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class ItemRegistry {
    
    public static final Item LIMESTONE_CHUNK = register(new ReforgeAddition(new Item.Properties(), 1), "limestone_chunk");
    public static final Item RAW_PYRITE = register(new ReforgeAddition(new Item.Properties(), 2), "pyrite_chunk");
    public static final Item RAW_GALENA = register(new ReforgeAddition(new Item.Properties(), 3), "galena_chunk");

    public static void init() {}

    public static Item register(Item item, String name) {
        return Registry.register(BuiltInRegistries.ITEM, Tierify.id(name), item);
    }
}

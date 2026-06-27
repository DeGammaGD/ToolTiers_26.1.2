package draylar.tiered.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

@SuppressWarnings({"null"})
public class TieredItemTags {
    public static final TagKey<Item> MAIN_OFFHAND_ITEM = register("main_offhand_item");

    private TieredItemTags() {
    }

    public static void init() {
    }

    private static TagKey<Item> register(String id) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("tiered", id));
    }
}

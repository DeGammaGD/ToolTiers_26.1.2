package draylar.tiered.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TieredItemTags {
    public static final TagKey<Item> MAIN_OFFHAND_ITEM = register("main_offhand_item");
    public static final TagKey<Item> REFORGE_BASE_ITEM = register("reforge_base_item");

    public static final TagKey<Item> TIER_1_ITEM = register("reforge_tier_1");
    public static final TagKey<Item> TIER_2_ITEM = register("reforge_tier_2");
    public static final TagKey<Item> TIER_3_ITEM = register("reforge_tier_3");

    private TieredItemTags() {
    }

    public static void init() {
    }

    private static TagKey<Item> register(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tiered", id));
    }
}

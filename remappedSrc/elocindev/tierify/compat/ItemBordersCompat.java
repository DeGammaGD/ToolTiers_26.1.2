package elocindev.tierify.compat;

import elocindev.tierify.Tierify;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class ItemBordersCompat {
    
    public static void addBorder(ItemStack stack, String color) {
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = component != null ? component.copyTag() : new CompoundTag();
        CompoundTag nbt = root.contains("itemborders_colors") ? root.getCompound("itemborders_colors") : new CompoundTag();
        nbt.putString("top", color);
        root.put("itemborders_colors", nbt);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static void addBorder(ItemStack stack, String topColor, String bottomColor) {
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = component != null ? component.copyTag() : new CompoundTag();
        CompoundTag nbt = root.contains("itemborders_colors") ? root.getCompound("itemborders_colors") : new CompoundTag();
        nbt.putString("top", topColor);
        nbt.putString("bottom", bottomColor);
        root.put("itemborders_colors", nbt);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /*
     * Might return null if the identifier is not valid
     */
    public static String getColorForIdentifier(ResourceLocation identifier) {
        String tier = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(identifier).getID();
        if (tier == null) return null;
        
        switch(Component.translatable(tier + ".label").getString().toLowerCase()) {
            case "common":
                return "0xc7c7c7";
            case "uncommon":
                return "0x76c462";
            case "rare":
                return "0x6293c4";
            case "epic":
                return "0xa762c4";
            case "legendary":
                return "0xcf9e44";
            case "mythic":
                return "0xb53f3f";
        }

        return String.valueOf(Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(ResourceLocation.parse(identifier.toString())).getStyle().getColor().getValue());
    }
}
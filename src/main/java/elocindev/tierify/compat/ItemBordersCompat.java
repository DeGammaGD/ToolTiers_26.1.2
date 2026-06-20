package elocindev.tierify.compat;

import elocindev.tierify.Tierify;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ItemBordersCompat {
    
    public static void addBorder(ItemStack stack, String color) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound root = component != null ? component.getNbt() : new NbtCompound();
        NbtCompound nbt = root.contains("itemborders_colors") ? root.getCompound("itemborders_colors") : new NbtCompound();
        nbt.putString("top", color);
        root.put("itemborders_colors", nbt);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
    }

    public static void addBorder(ItemStack stack, String topColor, String bottomColor) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound root = component != null ? component.getNbt() : new NbtCompound();
        NbtCompound nbt = root.contains("itemborders_colors") ? root.getCompound("itemborders_colors") : new NbtCompound();
        nbt.putString("top", topColor);
        nbt.putString("bottom", bottomColor);
        root.put("itemborders_colors", nbt);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
    }

    /*
     * Might return null if the identifier is not valid
     */
    public static String getColorForIdentifier(Identifier identifier) {
        String tier = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(identifier).getID();
        if (tier == null) return null;
        
        switch(Text.translatable(tier + ".label").getString().toLowerCase()) {
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

        return String.valueOf(Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(Identifier.of(identifier.toString())).getStyle().getColor().getRgb());
    }
}
package elocindev.tierify.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import elocindev.tierify.Tierify;

public class ReforgeAddition extends Item {
    private int tier = -1;

    public ReforgeAddition(Properties settings, int tier) {
        super(settings);
        this.tier = tier;
    }
    
    public int getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        ArrayList<String> qualities = new ArrayList<String>();

        switch(getTier()) {
            case 1:
                qualities = Tierify.CONFIG.tier_1_qualities;
                break;
            case 2:
                qualities = Tierify.CONFIG.tier_2_qualities;
                break;
            case 3:
                qualities = Tierify.CONFIG.tier_3_qualities;
                break;
        }

        if (qualities.size() == 0) return;
        
        
        tooltip.add(Component.literal("Reforging Qualities:").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
        for (String quality : qualities) {
            MutableComponent separator = Component.literal(" - ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));

            tooltip.add(separator.append(Component.literal(quality).setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY))));
        }
    }
}

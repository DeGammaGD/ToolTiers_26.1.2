package elocindev.tierify.screen.client.widget;

import org.jetbrains.annotations.Nullable;

import elocindev.tierify.network.TieredClientPacket;
import net.libz.api.InventoryTab;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AnvilTab extends InventoryTab {

    public AnvilTab(Component title, @Nullable ResourceLocation texture, int preferedPos, Class<?>... screenClasses) {
        super(title, texture, preferedPos, screenClasses);
    }

    @Override
    public void onClick(Minecraft client) {
        TieredClientPacket.writeC2SScreenPacket((int) client.mouseHandler.xpos(), (int) client.mouseHandler.ypos(), false);
    }

}

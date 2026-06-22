package elocindev.tierify.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "tierify-client")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class ClientConfig implements ConfigData {
    @ConfigEntry.Category("client_settings")
    public boolean tieredTooltip = true;

    @Comment("Swaps the text with a plate displayed on the item's name.")
    @ConfigEntry.Category("client_settings")
    public boolean showPlatesOnName = true;

    @ConfigEntry.Category("client_settings")
    public boolean centerName = true;
}

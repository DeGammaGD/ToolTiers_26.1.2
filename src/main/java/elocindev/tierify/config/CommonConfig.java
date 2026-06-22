package elocindev.tierify.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "tierify-common")
public class CommonConfig implements ConfigData {

    @Comment("Items in for example mineshaft chests get modifiers")
    public boolean lootContainerModifier = true;
    @Comment("Equipped items on entities get modifiers")
    public boolean entityItemModifier = true;
    @Comment("Crafted items get modifiers")
    public boolean craftingModifier = true;
    @Comment("Merchant items get modifiers")
    public boolean merchantModifier = true;
}

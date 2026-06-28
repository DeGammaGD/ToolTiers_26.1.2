package draylar.tiered.util;

import net.minecraft.world.entity.player.Player;

public class AttributeHelper {

    @Deprecated()
    public static float getExtraRangeDamage(Player playerEntity, float oldDamage) {
        return elocindev.tierify.util.AttributeHelper.getExtraRangeDamage(playerEntity, oldDamage);
    }

    @Deprecated()
    public static float getExtraCritDamage(Player playerEntity, float oldDamage) {
        return elocindev.tierify.util.AttributeHelper.getExtraCritDamage(playerEntity, oldDamage);
    }

    @Deprecated()
    public static float getCriticalChance(Player playerEntity) {
        return elocindev.tierify.util.AttributeHelper.getCriticalChance(playerEntity);
    }

    @Deprecated()
    public static float getCriticalDamageModifier(Player playerEntity) {
        return elocindev.tierify.util.AttributeHelper.getCriticalDamageModifier(playerEntity);
    }

}

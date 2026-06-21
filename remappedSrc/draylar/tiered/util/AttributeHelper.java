package draylar.tiered.util;

import net.minecraft.world.entity.player.Player;

public class AttributeHelper {
    
    @Deprecated()
    public static boolean shouldMeeleCrit(Player playerEntity) {
        return elocindev.tierify.util.AttributeHelper.shouldMeeleCrit(playerEntity);
    }

    @Deprecated()
    public static float getExtraDigSpeed(Player playerEntity, float oldDigSpeed) {
        return elocindev.tierify.util.AttributeHelper.getExtraDigSpeed(playerEntity, oldDigSpeed);
    }

    @Deprecated()
    public static float getExtraRangeDamage(Player playerEntity, float oldDamage) {
        return elocindev.tierify.util.AttributeHelper.getExtraRangeDamage(playerEntity, oldDamage);
    }

    @Deprecated()
    public static float getExtraCritDamage(Player playerEntity, float oldDamage) {
        return elocindev.tierify.util.AttributeHelper.getExtraCritDamage(playerEntity, oldDamage);
    }

}

package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;

@Mixin(LootTable.class)
public class LootTableMixin {

    @Inject(method = "fill", at = @At("TAIL"))
    private void supplyInventoryMixin(Container inventory, LootParams parameters, long seed, CallbackInfo info) {
        if (parameters.getLevel().isClientSide() || !Tierify.CONFIG.lootContainerModifier) {
            return;
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            ModifierUtils.applyTierToItem(itemStack);
            ModifierUtils.logTierDebug("loot_generation", itemStack);
        }
    }
}

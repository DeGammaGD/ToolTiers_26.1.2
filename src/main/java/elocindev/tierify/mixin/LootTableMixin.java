package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;

@Mixin(LootTable.class)
public class LootTableMixin {

    @Inject(method = "supplyInventory", at = @At("TAIL"))
    private void supplyInventoryMixin(Inventory inventory, LootContextParameterSet parameters, long seed, CallbackInfo info) {
        if (parameters.getWorld().isClient() || !Tierify.CONFIG.lootContainerModifier) {
            return;
        }

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            ModifierUtils.applyTierToItem(itemStack);
            ModifierUtils.logTierDebug("loot_generation", itemStack);
        }
    }
}

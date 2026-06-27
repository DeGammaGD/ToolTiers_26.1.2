package elocindev.tierify.mixin;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public class InventoryMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void tierifyShiftCraftOutput(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != 0 || player.level().isClientSide() || !Tierify.CONFIG.craftingModifier) {
            return;
        }

        Slot resultSlot = ((AbstractContainerMenu) (Object) this).getSlot(0);
        if (!resultSlot.hasItem()) {
            Tierify.LOGGER.info("[CraftDebug] InventoryMenu.quickMoveStack slot0 has no item");
            return;
        }

        ItemStack stack = resultSlot.getItem();
        if (stack.isEmpty()) {
            Tierify.LOGGER.info("[CraftDebug] InventoryMenu.quickMoveStack slot0 item is empty");
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean hadTierBefore = ModifierUtils.hasTier(stack);
        Tierify.LOGGER.info("[CraftDebug] InventoryMenu.quickMoveStack pre-transfer item={} count={} hadTierBefore={}", itemId, stack.getCount(), hadTierBefore);
        ModifierUtils.applyTierIfNeeded(stack);
        boolean hasTierAfter = ModifierUtils.hasTier(stack);
        Tierify.LOGGER.info("[CraftDebug] InventoryMenu.quickMoveStack post-assign item={} hasTierAfter={} generated={}", itemId, hasTierAfter, !hadTierBefore && hasTierAfter);
        ModifierUtils.logTierDebug("crafting-shift", stack);
    }
}

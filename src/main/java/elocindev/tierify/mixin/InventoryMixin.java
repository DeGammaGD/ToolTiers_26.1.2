package elocindev.tierify.mixin;

import draylar.tiered.api.ModifierUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Shadow @Final public Player player;

    @Inject(method = "setItem", at = @At("TAIL"), require = 0)
    private void tierify$applyTierIfNeededOnSetItem(int slot, ItemStack stack, CallbackInfo info) {
        if (this.player.level().isClientSide() || stack == null || stack.isEmpty()) {
            return;
        }

        ModifierUtils.applyTierIfNeeded(stack);
    }
}

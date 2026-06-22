package elocindev.tierify.mixin;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public class ResultSlotMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void tierifyCraftResultOnTake(Player player, ItemStack stack, CallbackInfo info) {
        String itemId = stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Tierify.LOGGER.info("[CraftDebug] ResultSlot.onTake item={} count={} player={} clientSide={}", itemId, stack.getCount(), player.getClass().getSimpleName(), player.level().isClientSide());

        if (!Tierify.CONFIG.craftingModifier || stack.isEmpty()) {
            Tierify.LOGGER.info("[CraftDebug] Skipping tier assignment (craftingModifier={}, empty={})", Tierify.CONFIG.craftingModifier, stack.isEmpty());
            return;
        }

        if (!(player instanceof ServerPlayer)) {
            Tierify.LOGGER.info("[CraftDebug] Skipping tier assignment because player is not ServerPlayer");
            return;
        }

        boolean hadTier = ModifierUtils.hasTier(stack);
        Tierify.LOGGER.info("[CraftDebug] Assign tier called for item={} hadTierBefore={}", itemId, hadTier);
        ModifierUtils.applyTierToItem(stack);
        boolean hasTierAfter = ModifierUtils.hasTier(stack);
        Tierify.LOGGER.info("[CraftDebug] Tier assignment result item={} hasTierAfter={} generated={}", itemId, hasTierAfter, !hadTier && hasTierAfter);
        ModifierUtils.logTierDebug("crafting", stack);
    }
}

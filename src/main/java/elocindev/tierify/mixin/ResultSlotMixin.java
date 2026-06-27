package elocindev.tierify.mixin;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public class ResultSlotMixin {

    @Shadow @Final private Player player;

    private void finalizeCraftedStack(Player player, ItemStack stack, String source) {
        if (!Tierify.CONFIG.craftingModifier || stack.isEmpty() || player.level().isClientSide()) {
            return;
        }

        if (!(player instanceof ServerPlayer)) {
            Tierify.LOGGER.info("[CraftDebug] Skipping {} tier assignment because player is not ServerPlayer", source);
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean hadTier = ModifierUtils.hasTier(stack);
        Tierify.LOGGER.info("[CraftDebug] {} assign tier called for item={} hadTierBefore={}", source, itemId, hadTier);
        ModifierUtils.applyTierIfNeeded(stack);
        boolean hasTierAfter = ModifierUtils.hasTier(stack);
        Tierify.LOGGER.info("[CraftDebug] {} tier assignment result item={} hasTierAfter={} generated={}", source, itemId, hasTierAfter, !hadTier && hasTierAfter);
        ModifierUtils.logTierDebug(source, stack);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void tierifyCraftResultOnTake(Player player, ItemStack stack, CallbackInfo info) {
        String itemId = stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Tierify.LOGGER.info("[CraftDebug] ResultSlot.onTake item={} count={} player={} clientSide={}", itemId, stack.getCount(), player.getClass().getSimpleName(), player.level().isClientSide());
        finalizeCraftedStack(player, stack, "crafting");
    }

    @Inject(method = "onQuickCraft(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"), require = 0)
    private void tierifyCraftResultOnQuickCraftCount(ItemStack stack, int amount, CallbackInfo info) {
        Tierify.LOGGER.info("[CraftDebug] ResultSlot.onQuickCraft(ItemStack,int) item={} count={} craftedAmount={}",
                stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), amount);
        finalizeCraftedStack(this.player, stack, "crafting-shift");
    }

    @Inject(method = "onQuickCraft(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), require = 0)
    private void tierifyCraftResultOnQuickCraftStack(ItemStack craftedStack, ItemStack originalStack, CallbackInfo info) {
        Tierify.LOGGER.info("[CraftDebug] ResultSlot.onQuickCraft(ItemStack,ItemStack) item={} count={} originalCount={}",
                craftedStack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(craftedStack.getItem()), craftedStack.getCount(), originalStack.getCount());
        finalizeCraftedStack(this.player, craftedStack, "crafting-shift");
    }
}
